import { useChatStore } from '@/stores/chat'

// Map lưu trữ các hàm hủy đăng ký (unsubscribe) theo từng phòng chat (key = roomId)
const roomUnsubscribers = new Map()

/**
 * =========================================================================
 * Lightweight Native STOMP WebSocket Client
 * =========================================================================
 * - Sử dụng WebSocket API chuẩn của trình duyệt (không phụ thuộc thư viện bên ngoài như stompjs hay sockjs-client).
 * - Kết nối trực tiếp vào Native WebSocket endpoint của Spring Boot: ws://localhost:8080/ws-raw
 * - Hỗ trợ xác thực qua Bearer JWT Token ở Header STOMP CONNECT frame.
 * - Tự động duy trì nhịp tim ping/pong (Heartbeat) định kỳ 10 giây để chống rớt mạng (Zombie Connection).
 * - Tự động kết nối lại (Auto-Reconnect) sau 2.5 giây khi phát hiện socket bị ngắt hoặc mạng lag.
 * - Tự động tái đăng ký toàn bộ các topic (/topic/room.*, /topic/users.presence) sau khi kết nối lại thành công.
 */
class NativeStompClient {
  constructor() {
    this.ws = null // Đối tượng WebSocket gốc của trình duyệt
    this.connected = false // Trạng thái đã handshake STOMP thành công (nhận CONNECTED frame)
    this.connecting = false // Cờ tránh tạo kết nối trùng lặp khi đang trong tiến trình kết nối
    this.shouldReconnect = true // Cờ cho phép tự động thử kết nối lại (sẽ tắt khi user chủ động logout)
    this.reconnectTimer = null // Bộ đếm thời gian cho việc tự động kết nối lại
    this.subscriptions = new Map() // Lưu trữ danh sách đăng ký topic: subId -> { destination, callback }
    this.subIdCounter = 0 // Bộ đếm tự tăng để tạo ID subscription duy nhất
    this.token = null // JWT Token dùng để xác thực người dùng
    this.heartbeatTimer = null // Bộ đếm gửi gói tin ping '\n' giữ kết nối
    this.onConnectedCallbacks = [] // Danh sách callback sẽ được gọi khi kết nối thành công
  }

  /**
   * Khởi tạo kết nối WebSocket STOMP tới Backend.
   * @param {string} token JWT Access Token
   * @param {Function} onConnected Callback thực thi khi kết nối thành công
   * @param {Function} onError Callback thực thi khi xảy ra lỗi kết nối
   */
  connect(token, onConnected, onError) {
    if (token) {
      this.token = token
    }
    if (onConnected) {
      this.onConnectedCallbacks.push(onConnected)
    }

    // Nếu socket đã kết nối và đang ở trạng thái OPEN -> thực thi ngay callback và dừng lại
    if (this.connected && this.ws && this.ws.readyState === WebSocket.OPEN) {
      if (onConnected) onConnected()
      return
    }

    // Nếu đang trong quá trình kết nối -> chờ socket sẵn sàng, tránh tạo kết nối song song
    if (this.connecting && this.ws && this.ws.readyState === WebSocket.CONNECTING) {
      return
    }

    this.shouldReconnect = true
    clearTimeout(this.reconnectTimer)

    // Xác định đường dẫn WebSocket (Hỗ trợ cả môi trường Local, LAN IP và Cloudflare Tunnel)
    const rawUrl = import.meta.env.VITE_WS_URL || '/ws-raw'
    let wsUrl = ''
    if (rawUrl.startsWith('/') || !rawUrl.includes('://')) {
      const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      wsUrl = `${proto}//${window.location.host}${rawUrl.startsWith('/') ? '' : '/'}${rawUrl}`
    } else {
      wsUrl = rawUrl.replace(/^http/, 'ws').replace(/\/ws$/, '/ws-raw')
    }

    console.log('[WebSocket] Đang kết nối tới:', wsUrl)
    this.connecting = true

    try {
      this.ws = new WebSocket(wsUrl)
    } catch (e) {
      console.error('[WebSocket] Lỗi khởi tạo WebSocket:', e)
      this.connecting = false
      this.scheduleReconnect()
      if (onError) onError(e)
      return
    }

    // 1. Khi socket TCP/HTTP Handshake mở thành công -> gửi STOMP CONNECT frame
    this.ws.onopen = () => {
      console.log('[WebSocket] Socket đã mở, đang gửi STOMP CONNECT frame...')
      // Gửi frame STOMP CONNECT kèm Bearer Token và thỏa thuận heartbeat 10s
      const connectFrame = `CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\nAuthorization:Bearer ${this.token}\n\n\0`
      this.ws.send(connectFrame)
    }

    // 2. Lắng nghe các frame STOMP phản hồi từ Message Broker
    this.ws.onmessage = (event) => {
      this.handleIncoming(event.data)
    }

    // 3. Xử lý khi có lỗi kết nối
    this.ws.onerror = (err) => {
      console.error('[WebSocket] Lỗi kết nối:', err)
      this.connecting = false
      if (onError) onError(err)
    }

    // 4. Xử lý khi socket bị đóng (do rớt mạng, reload trang hoặc server khởi động lại)
    this.ws.onclose = (evt) => {
      console.warn('[WebSocket] Đã ngắt kết nối (code:', evt.code, ', reason:', evt.reason || 'None', ')')
      this.connected = false
      this.connecting = false
      clearInterval(this.heartbeatTimer)

      // Tự động lên lịch kết nối lại nếu người dùng không chủ động đăng xuất
      if (this.shouldReconnect && this.token) {
        this.scheduleReconnect()
      }
    }
  }

  /**
   * Lên lịch tự động thử kết nối lại sau 2.5 giây.
   */
  scheduleReconnect() {
    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => {
      if (this.shouldReconnect && this.token && !this.connected && !this.connecting) {
        console.log('[WebSocket] 🔄 Tự động kết nối lại...')
        this.connect(this.token)
      }
    }, 2500)
  }

  /**
   * Bóc tách và xử lý các frame dữ liệu STOMP gửi từ Broker:
   * - Frame CONNECTED: Báo hiệu xác thực và kết nối Message Broker thành công.
   * - Frame MESSAGE: Chứa nội dung tin nhắn gửi vào topic.
   */
  handleIncoming(raw) {
    if (!raw || !raw.trim()) return // Bỏ qua gói tin ping-pong rỗng (heartbeat)

    // Tách các frame phân tách bằng ký tự kết thúc '\0' (STOMP NUL byte)
    const frames = raw.split('\0')
    for (const rawFrame of frames) {
      const trimmedFrame = rawFrame.trimStart()
      if (!trimmedFrame) continue

      const lines = trimmedFrame.split(/\r?\n/)
      const command = lines[0].trim()

      // Gói tin xác thực thành công từ Spring Message Broker
      if (command === 'CONNECTED') {
        this.connected = true
        this.connecting = false
        console.log('✅ [WebSocket STOMP] Kết nối thành công!')

        // Tự động tái đăng ký lại toàn bộ các topic đã lưu trước đó
        this.subscriptions.forEach((sub, subId) => {
          this.sendSubscribeFrame(subId, sub.destination)
        })

        // Duy trì nhịp tim ping mỗi 10 giây (nhỏ hơn mốc 15s của backend để tránh timeout)
        clearInterval(this.heartbeatTimer)
        this.heartbeatTimer = setInterval(() => {
          if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send('\n') // STOMP heartbeat ping byte
          }
        }, 10000)

        // Thực thi các callback đã đăng ký khi kết nối hoàn tất
        const callbacks = [...this.onConnectedCallbacks]
        this.onConnectedCallbacks = []
        callbacks.forEach((cb) => {
          try {
            cb()
          } catch (e) {
            console.error('[WebSocket] Lỗi callback onConnected:', e)
          }
        })
        continue
      }

      // Gói tin tin nhắn hoặc sự kiện đẩy từ server về topic
      if (command === 'MESSAGE') {
        let destination = ''
        let headerEndIdx = 1

        // Đọc các cặp header (destination, content-type, ...)
        for (let i = 1; i < lines.length; i++) {
          const line = lines[i].trim()
          if (line === '') {
            headerEndIdx = i + 1
            break
          }
          const colonIdx = line.indexOf(':')
          if (colonIdx !== -1) {
            const key = line.slice(0, colonIdx).trim()
            const val = line.slice(colonIdx + 1).trim()
            if (key === 'destination') destination = val
          }
        }

        // Bóc tách nội dung Payload (JSON)
        const bodyStr = lines.slice(headerEndIdx).join('\n').trim()
        let bodyData = bodyStr
        try {
          bodyData = JSON.parse(bodyStr)
        } catch {
          // Giữ nguyên chuỗi nếu không phải JSON
        }

        // Điều phối dữ liệu tới đúng hàm callback đã đăng ký topic này
        this.subscriptions.forEach((sub) => {
          if (sub.destination === destination) {
            try {
              sub.callback(bodyData)
            } catch (err) {
              console.error(`[WebSocket] Lỗi subscriber xử lý gói tin từ ${destination}:`, err)
            }
          }
        })
      }
    }
  }

  /**
   * Gửi frame SUBSCRIBE theo đúng chuẩn giao thức STOMP 1.2
   */
  sendSubscribeFrame(subId, destination) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const frame = `SUBSCRIBE\nid:${subId}\ndestination:${destination}\n\n\0`
      this.ws.send(frame)
    }
  }

  /**
   * Đăng ký lắng nghe một topic STOMP (ví dụ: /topic/room.{roomId}).
   * @param {string} destination Tên topic cần lắng nghe
   * @param {Function} callback Hàm xử lý khi có tin nhắn mới tới topic
   * @returns {Function} Hàm hủy đăng ký (unsubscribe)
   */
  subscribe(destination, callback) {
    const subId = `sub-${this.subIdCounter++}`
    this.subscriptions.set(subId, { destination, callback })

    if (this.connected) {
      this.sendSubscribeFrame(subId, destination)
    }

    return () => {
      this.subscriptions.delete(subId)
      if (this.connected && this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(`UNSUBSCRIBE\nid:${subId}\n\n\0`)
      }
    }
  }

  /**
   * Gửi frame SEND qua STOMP (ví dụ: gửi trạng thái gõ phím /app/chat.typing).
   * @param {string} destination Điểm đến (prefix /app/...)
   * @param {object|string} payload Dữ liệu gửi đi
   */
  send(destination, payload) {
    if (this.connected && this.ws && this.ws.readyState === WebSocket.OPEN) {
      const body = typeof payload === 'string' ? payload : JSON.stringify(payload)
      const frame = `SEND\ndestination:${destination}\ncontent-type:application/json\n\n${body}\0`
      this.ws.send(frame)
    } else {
      console.warn('[WebSocket] Chưa kết nối, không thể gửi tới:', destination)
    }
  }

  /**
   * Ngắt kết nối sạch sẽ khi người dùng chủ động Đăng xuất (Logout).
   * Hủy bỏ toàn bộ timer heartbeat và không tự động kết nối lại.
   */
  disconnect() {
    this.shouldReconnect = false
    clearTimeout(this.reconnectTimer)
    clearInterval(this.heartbeatTimer)
    if (this.ws) {
      try {
        this.ws.close()
      } catch {}
      this.ws = null
    }
    this.connected = false
    this.connecting = false
    this.subscriptions.clear()
    this.onConnectedCallbacks = []
    roomUnsubscribers.clear()
  }
}

// Khởi tạo một phiên STOMP WebSocket dùng chung toàn hệ thống
export const wsService = new NativeStompClient()

/**
 * Hàm khởi tạo và kết nối WebSocket toàn cục cho ứng dụng chat.
 * Tự động đăng ký lắng nghe sự kiện Online/Offline của bạn bè và các phòng chat.
 * @param {string} token JWT Access Token của tài khoản hiện tại
 */
export function initGlobalWebSocket(token) {
  if (!token) return

  wsService.connect(token, () => {
    const chatStore = useChatStore()

    // 1. Lắng nghe trạng thái Online / Offline của bạn bè từ Broker: /topic/users.presence
    wsService.subscribe('/topic/users.presence', (presenceEvent) => {
      console.log('📡 [Event Online/Offline]:', presenceEvent)
      if (presenceEvent && presenceEvent.userId) {
        const isOnline = presenceEvent.isOnline ?? presenceEvent.online ?? false
        chatStore.updatePresence(presenceEvent.userId, isOnline, presenceEvent.lastSeenAt)
      }
    })

    // 2. Lắng nghe các phòng chat hiện có trong store để cập nhật tin nhắn mới
    if (Array.isArray(chatStore.rooms)) {
      chatStore.rooms.forEach((room) => {
        subscribeToRoom(room.roomId)
      })
    }
  })
}

/**
 * Đăng ký lắng nghe các sự kiện diễn ra trong một phòng chat cụ thể:
 * 1. /topic/room.{roomId}: Tin nhắn mới, tin nhắn được ghim/bỏ ghim, reaction...
 * 2. /topic/room.{roomId}.typing: Trạng thái đang soạn tin nhắn của đối phương.
 * @param {string} roomId Khóa chính UUID của phòng chat
 */
export function subscribeToRoom(roomId) {
  if (!roomId || ['1', '2', '3'].includes(String(roomId)) || roomUnsubscribers.has(String(roomId))) {
    return
  }

  const chatStore = useChatStore()
  const key = String(roomId)

  // 1. Lắng nghe tin nhắn mới và sự kiện trong phòng
  const unsubMsg = wsService.subscribe(`/topic/room.${key}`, (message) => {
    console.log(`🔔 [Tin nhắn mới phòng ${key}]:`, message)
    chatStore.handleNewMessage(message)
  })

  // 2. Lắng nghe trạng thái đang soạn tin nhắn (Typing indicator)
  const unsubTyping = wsService.subscribe(`/topic/room.${key}.typing`, (typingEvent) => {
    console.log(`✍️ [Typing event phòng ${key}]:`, typingEvent)
    chatStore.handleTypingEvent(typingEvent)
  })

  // Lưu lại hàm unsubscribe để dọn dẹp khi rời phòng hoặc đổi tài khoản
  roomUnsubscribers.set(key, () => {
    unsubMsg()
    unsubTyping()
  })
}
