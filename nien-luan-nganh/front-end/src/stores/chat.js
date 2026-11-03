import { defineStore } from 'pinia'
import * as roomApi from '@/api/room.api'
import * as messageApi from '@/api/message.api'
import { wsService, subscribeToRoom } from '@/services/websocket'
import { useAuthStore } from './auth'

const DEFAULT_SAMPLE_ROOMS = [
  {
    roomId: '1',
    roomType: 'GROUP',
    roomName: 'Nhóm Thiết kế sản phẩm',
    roomAvatarURL: '',
    lastMessageContent: 'Mình đã cập nhật bản thiết kế',
    lastMessageSenderName: 'Minh Anh',
    lastMessageAt: new Date().toISOString(),
    unreadCount: 2,
    hasUnread: true,
    isOnline: false,
  },
  {
    roomId: '2',
    roomType: 'DIRECT',
    roomName: 'Minh Anh',
    roomAvatarURL: 'https://i.pravatar.cc/100?img=5',
    peerId: 'mock-peer-2',
    lastMessageContent: 'Gửi bạn tài liệu của buổi họp nhé',
    lastMessageSenderName: 'Minh Anh',
    lastMessageAt: new Date(Date.now() - 3600000).toISOString(),
    unreadCount: 0,
    hasUnread: false,
    isOnline: true,
  },
  {
    roomId: '3',
    roomType: 'GROUP',
    roomName: 'Dự án Horizon',
    roomAvatarURL: '',
    lastMessageContent: 'Tuyệt vời, cảm ơn mọi người!',
    lastMessageSenderName: 'Bạn',
    lastMessageAt: new Date(Date.now() - 86400000).toISOString(),
    unreadCount: 0,
    hasUnread: false,
    isOnline: false,
  },
]

const DEFAULT_SAMPLE_MESSAGES = {
  '1': [
    {
      messageId: 'mock-1-1',
      roomId: '1',
      sender: { userId: 'mock-user-1', username: 'Minh Anh' },
      content: 'Chào cả nhóm, mình vừa gửi tài liệu thiết kế mới nhé.',
      createdAt: new Date(Date.now() - 3600000).toISOString(),
    },
    {
      messageId: 'mock-1-2',
      roomId: '1',
      sender: { userId: 'me', username: 'Bạn' },
      content: 'Cảm ơn Minh Anh, mình đang xem qua rồi.',
      createdAt: new Date(Date.now() - 1800000).toISOString(),
    },
    {
      messageId: 'mock-1-3',
      roomId: '1',
      sender: { userId: 'mock-user-1', username: 'Minh Anh' },
      content: 'Mình đã cập nhật bản thiết kế',
      createdAt: new Date().toISOString(),
    },
  ],
  '2': [
    {
      messageId: 'mock-2-1',
      roomId: '2',
      sender: { userId: 'mock-user-2', username: 'Minh Anh' },
      content: 'Gửi bạn tài liệu của buổi họp nhé',
      createdAt: new Date(Date.now() - 3600000).toISOString(),
    },
  ],
  '3': [
    {
      messageId: 'mock-3-1',
      roomId: '3',
      sender: { userId: 'mock-user-3', username: 'Trọng Nghĩa' },
      content: 'Dự án đã hoàn thành các mục tiêu đề ra!',
      createdAt: new Date(Date.now() - 86400000).toISOString(),
    },
    {
      messageId: 'mock-3-2',
      roomId: '3',
      sender: { userId: 'me', username: 'Bạn' },
      content: 'Tuyệt vời, cảm ơn mọi người!',
      createdAt: new Date(Date.now() - 86400000).toISOString(),
    },
  ],
}

function loadDemoRooms() {
  try {
    const raw = localStorage.getItem('bluespace_demo_rooms')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) return parsed
    }
  } catch {}
  return JSON.parse(JSON.stringify(DEFAULT_SAMPLE_ROOMS))
}

function saveDemoRooms(rooms) {
  try {
    localStorage.setItem('bluespace_demo_rooms', JSON.stringify(rooms))
  } catch {}
}

function loadDemoMessages() {
  try {
    const raw = localStorage.getItem('bluespace_demo_messages')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') return parsed
    }
  } catch {}
  return JSON.parse(JSON.stringify(DEFAULT_SAMPLE_MESSAGES))
}

function saveDemoMessages(messagesMap) {
  try {
    localStorage.setItem('bluespace_demo_messages', JSON.stringify(messagesMap))
  } catch {}
}

/**
 * =========================================================================
 * Pinia Store: useChatStore
 * =========================================================================
 * Quản lý toàn bộ trạng thái (State) và luồng xử lý (Actions) của phân hệ Chat:
 * 1. Danh sách phòng chat (rooms) & phòng đang mở (activeRoomId).
 * 2. Lịch sử tin nhắn theo phòng (messages) & phân trang con trỏ (Cursor Pagination).
 * 3. Đồng bộ trạng thái Online / Offline thời gian thực của bạn bè (updatePresence).
 * 4. Trạng thái đang soạn tin nhắn (typingMap, activeTypingText, sendTypingStatus).
 * 5. Danh sách tin nhắn đã ghim (pinnedMessages, fetchPinnedMessages, pinMessage, unpinMessage).
 * 6. Cơ chế chạy ngầm (syncRoomsSilently) tự động nhận phòng mới mà không cần reload trang.
 * 7. Cơ chế dữ liệu mẫu dự phòng (Mock/Demo Fallback) khi Backend chưa khởi động.
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    // Danh sách các phòng chat của người dùng
    rooms: [],
    // Khóa chính UUID của phòng chat đang được chọn mở
    activeRoomId: null,
    // Từ khóa tìm kiếm trên thanh SearchBar
    searchQuery: '',
    // Bộ lọc hiển thị: 'SelectAll' (Tất cả) hoặc 'Unread' (Chưa đọc)
    currentFilter: 'SelectAll',
    loading: false,
    error: null,

    // Danh sách tin nhắn thuộc phòng chat đang mở
    messages: [],
    loadingMessages: false,
    nextCursor: null, // Con trỏ phân trang tin nhắn cũ hơn
    hasMoreMessages: false,

    // Danh sách tin nhắn đã ghim trong phòng đang mở (tối đa 5 tin)
    pinnedMessages: [],

    // Kho lưu trữ tin nhắn mẫu dự phòng (nếu dùng các phòng mẫu '1', '2', '3')
    demoMessagesMap: loadDemoMessages(),

    // Lưu trữ trạng thái gõ chữ: { [roomId]: { [userId]: { username, avatarUrl, timer } } }
    typingMap: {},

    // Thông báo ghim tin nhắn tạm thời ngoài Sidebar: { [roomId]: string }
    pinNotices: {},
  }),

  getters: {
    // Đoạn text thông báo người đang soạn tin nhắn cho phòng active
    activeTypingText: (state) => {
      const roomTyping = state.typingMap[String(state.activeRoomId)]
      if (!roomTyping) return ''
      const users = Object.values(roomTyping)
      if (users.length === 0) return ''
      if (users.length === 1) return `${users[0].username} đang soạn tin nhắn...`
      if (users.length === 2) return `${users[0].username} và ${users[1].username} đang soạn tin nhắn...`
      return `${users[0].username} và ${users.length - 1} người khác đang soạn tin nhắn...`
    },

    // Danh sách phòng đã qua bộ lọc (Tất cả / Chưa đọc / Tìm kiếm)
    filteredRooms: (state) => {
      let result = [...state.rooms]

      if (state.currentFilter === 'Unread') {
        result = result.filter((r) => r.hasUnread || (r.unreadCount && r.unreadCount > 0))
      }

      const q = state.searchQuery.trim().toLowerCase()
      if (q) {
        result = result.filter(
          (r) =>
            (r.roomName && r.roomName.toLowerCase().includes(q)) ||
            (r.lastMessageContent && r.lastMessageContent.toLowerCase().includes(q)),
        )
      }

      return result
    },

    // Phòng chat đang mở
    activeRoom: (state) => {
      return state.rooms.find((r) => String(r.roomId) === String(state.activeRoomId)) || null
    },

    // Tổng số tin nhắn chưa đọc của tất cả các phòng
    totalUnreadCount: (state) => {
      return state.rooms.reduce((acc, r) => acc + (r.unreadCount || 0), 0)
    },

    // Tin nhắn đã ghim mới nhất của phòng đang mở
    latestPinnedMessage: (state) => {
      return state.pinnedMessages.length > 0
        ? state.pinnedMessages[state.pinnedMessages.length - 1]
        : null
    },
  },

  actions: {
    // 1. Tải danh sách phòng chat từ Backend (Có fallback mẫu)
    async fetchRooms() {
      this.loading = true
      this.error = null
      try {
        const pageData = await roomApi.getRooms({ pageNo: 0, pageSize: 50 })
        const backendRooms = pageData?.content || []

        if (backendRooms.length > 0) {
          this.rooms = backendRooms
          // Đăng ký lắng nghe WebSocket cho các phòng thực tế
          this.rooms.forEach((r) => subscribeToRoom(r.roomId))
        } else {
          this.rooms = loadDemoRooms()
        }

        // Tự động chọn phòng đầu tiên nếu chưa có phòng nào được chọn
        const currentExists = this.rooms.some(
          (r) => String(r.roomId) === String(this.activeRoomId),
        )
        if ((!this.activeRoomId || !currentExists) && this.rooms.length > 0) {
          await this.selectRoom(this.rooms[0].roomId)
        }
      } catch (err) {
        console.warn('Lỗi gọi API rooms, kích hoạt danh sách mẫu dự phòng:', err)
        this.rooms = loadDemoRooms()
        if (!this.activeRoomId && this.rooms.length > 0) {
          await this.selectRoom(this.rooms[0].roomId)
        }
      } finally {
        this.loading = false
      }
    },

    // 1.1. Đồng bộ ngầm danh sách phòng (giúp người nhận tự thấy phòng mới được tạo mà không cần F5)
    async syncRoomsSilently() {
      try {
        const pageData = await roomApi.getRooms({ pageNo: 0, pageSize: 50 })
        const backendRooms = pageData?.content || []
        if (backendRooms.length > 0) {
          const wasUsingDemo = this.rooms.some((r) => ['1', '2', '3'].includes(String(r.roomId)))

          // Đăng ký WebSocket cho các phòng mới
          backendRooms.forEach((r) => subscribeToRoom(r.roomId))

          this.rooms = backendRooms.map((r) => {
            const existing = this.rooms.find((old) => String(old.roomId) === String(r.roomId))
            const isOnline = r.isOnline !== undefined && r.isOnline !== null ? r.isOnline : (existing?.isOnline || false)
            const lastSeenAt = r.lastSeenAt || existing?.lastSeenAt || null
            if (String(r.roomId) === String(this.activeRoomId)) {
              return { ...r, isOnline, lastSeenAt, hasUnread: false, unreadCount: 0 }
            }
            return { ...r, isOnline, lastSeenAt }
          })

          // Nếu trước đó đang ở phòng mẫu mà vừa có người tạo phòng thật với mình -> mở ngay phòng thật!
          if (wasUsingDemo || !this.activeRoomId) {
            await this.selectRoom(this.rooms[0].roomId)
          }
        }
      } catch {
        // Bỏ qua lỗi khi đồng bộ ngầm
      }
    },

    // 1.2. Tạo (hoặc mở) phòng chat 1-1 trực tiếp với một người dùng
    async startDirectChat(userId) {
      if (!userId) return null
      const detail = await roomApi.createDirectRoom(userId)
      await this.fetchRooms()
      if (detail?.roomId) {
        await this.selectRoom(detail.roomId)
      }
      return detail
    },

    // 1.3. Tạo nhóm chat mới (Group Chat)
    async createGroupChat({ roomName, memberIds, roomAvatarURL = '' }) {
      const detail = await roomApi.createGroup({
        roomName,
        roomAvatarURL,
        memberIds,
      })
      await this.fetchRooms()
      if (detail?.roomId) {
        await this.selectRoom(detail.roomId)
      }
      return detail
    },

    // 2. Chọn một phòng chat từ Sidebar
    async selectRoom(roomId) {
      if (!roomId) return
      const isSameRoom = String(this.activeRoomId) === String(roomId)
      this.activeRoomId = roomId

      // Xóa cờ chưa đọc cho phòng vừa chọn
      const index = this.rooms.findIndex((r) => String(r.roomId) === String(roomId))
      if (index !== -1) {
        this.rooms[index] = {
          ...this.rooms[index],
          hasUnread: false,
          unreadCount: 0,
        }
        if (['1', '2', '3'].includes(String(roomId))) {
          saveDemoRooms(this.rooms)
        }
      }

      // Đăng ký nhận tin nhắn WebSocket cho phòng này
      subscribeToRoom(roomId)

      // Tải tin nhắn và tin nhắn ghim của phòng
      if (!isSameRoom || this.messages.length === 0) {
        await Promise.all([
          this.fetchMessages(roomId),
          this.fetchPinnedMessages(roomId),
        ])
      } else {
        await this.fetchPinnedMessages(roomId)
      }
    },

    // 3. Tải lịch sử tin nhắn của một phòng
    async fetchMessages(roomId) {
      if (!roomId) return
      const key = String(roomId)
      this.loadingMessages = true

      // Trường hợp là phòng mẫu dự phòng ('1', '2', '3')
      if (['1', '2', '3'].includes(key)) {
        this.demoMessagesMap = loadDemoMessages()
        this.messages = [...(this.demoMessagesMap[key] || [])]
        this.loadingMessages = false
        this.nextCursor = null
        this.hasMoreMessages = false
        return
      }

      try {
        const res = await messageApi.getMessages(roomId, { limit: 50 })
        // Backend trả về DESC (mới nhất trước) -> Đảo ngược lại để hiển thị từ cũ -> mới (cuộn xuống dưới)
        const list = [...(res?.data || [])].reverse()
        this.messages = list
        this.nextCursor = res?.nextCursor || null
        this.hasMoreMessages = !!res?.hasMore

        // Đánh dấu đã đọc trên backend
        try {
          await messageApi.markAsRead(roomId)
        } catch {
          // Bỏ qua lỗi đánh dấu đọc nếu có
        }
      } catch (err) {
        console.error('Lỗi tải danh sách tin nhắn:', err)
        this.messages = []
      } finally {
        this.loadingMessages = false
      }
    },

    // 4. Gửi tin nhắn mới
    async sendChatMessage(content) {
      if (!this.activeRoomId || !content || !content.trim()) return
      const text = content.trim()
      const authStore = useAuthStore()
      const roomKey = String(this.activeRoomId)

      // Trường hợp 1: Phòng mẫu dự phòng ('1', '2', '3')
      if (['1', '2', '3'].includes(roomKey)) {
        const mockMsg = {
          messageId: 'mock-' + Date.now(),
          roomId: roomKey,
          content: text,
          sender: {
            userId: authStore.user?.userId || 'me',
            username: authStore.user?.username || 'Bạn',
          },
          createdAt: new Date().toISOString(),
        }

        // Lưu vào kho tin nhắn mẫu & localStorage để không bị mất khi chuyển phòng hoặc F5
        if (!this.demoMessagesMap[roomKey]) {
          this.demoMessagesMap[roomKey] = []
        }
        this.demoMessagesMap[roomKey].push(mockMsg)
        saveDemoMessages(this.demoMessagesMap)

        // Cập nhật ngay khung chat VÀ cập nhật Sidebar bên trái (nhảy lên đầu)
        this.handleNewMessage(mockMsg)
        return
      }

      // Trường hợp 2: Phòng thật trong Database
      // Gọi POST /api/v1/rooms/{roomId}/messages:
      // Backend vừa lưu DB, vừa tự động broadcast qua WebSocket (/topic/room.{roomId}), vừa trả về MessageResponse
      try {
        const sentMsg = await messageApi.sendMessage(this.activeRoomId, {
          content: text,
          messageType: 'TEXT',
        })
        if (sentMsg) {
          this.handleNewMessage(sentMsg)
        }
      } catch (err) {
        console.error('Lỗi gửi tin nhắn:', err)
      }
    },

    // 5. Cập nhật trạng thái Online / Offline từ WebSocket
    updatePresence(userId, isOnline, lastSeenAt) {
      if (!userId) return
      const targetId = String(userId).toLowerCase()

      this.rooms = this.rooms.map((room) => {
        if (room.roomType === 'DIRECT' && room.peerId && String(room.peerId).toLowerCase() === targetId) {
          return {
            ...room,
            isOnline: !!isOnline,
            lastSeenAt: isOnline ? null : (lastSeenAt || room.lastSeenAt || new Date().toISOString()),
          }
        }
        return room
      })
    },

    // 6. Nhận tin nhắn mới -> Cập nhật khung chat + Cập nhật Sidebar trái & Đưa phòng lên đầu
    handleNewMessage(message) {
      if (!message || !message.roomId) return
      const authStore = useAuthStore()
      const msgRoomId = String(message.roomId)
      const isCurrentRoom = String(this.activeRoomId) === msgRoomId

      // 6.1. Nếu tin nhắn thuộc phòng đang mở -> thêm vào hoặc cập nhật danh sách hiển thị
      if (isCurrentRoom) {
        const existsIndex = this.messages.findIndex((m) => String(m.messageId) === String(message.messageId))
        if (existsIndex !== -1) {
          this.messages[existsIndex] = { ...this.messages[existsIndex], ...message }
        } else {
          this.messages.push(message)
        }

        // Tự động đồng bộ danh sách tin nhắn đã ghim
        if (message.isPinned && !message.isRevoked) {
          const pinIdx = this.pinnedMessages.findIndex((p) => String(p.messageId) === String(message.messageId))
          if (pinIdx !== -1) {
            this.pinnedMessages[pinIdx] = message
          } else {
            this.pinnedMessages.push(message)
          }
        } else if (message.isPinned === false || message.isRevoked) {
          this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== String(message.messageId))
        }
      }

      // 6.2. Cập nhật thông tin phòng trên Sidebar bên trái và đưa phòng lên vị trí đầu tiên
      const index = this.rooms.findIndex((r) => String(r.roomId) === msgRoomId)
      if (index !== -1) {
        const targetRoom = this.rooms[index]
        const isMyMsg =
          (authStore.user?.userId &&
            String(message.sender?.userId) === String(authStore.user.userId)) ||
          message.sender?.userId === 'me'

        const senderDisplayName = isMyMsg ? 'Bạn' : message.sender?.username || 'Thành viên'
        const displayContent = message.isRevoked
          ? 'Tin nhắn đã bị thu hồi'
          : message.content // Luôn hiển thị nội dung tin nhắn mới nhất

        // Xóa thông báo ghim tạm thời khi có tin nhắn mới tới
        if (this.pinNotices[msgRoomId]) {
          delete this.pinNotices[msgRoomId]
          this.pinNotices = { ...this.pinNotices }
        }

        const updatedRoom = {
          ...targetRoom,
          lastMessageId: message.messageId,
          lastMessageContent: displayContent,
          lastMessageSenderName: senderDisplayName,
          lastMessageAt: message.createdAt || new Date().toISOString(),
          hasUnread: !isCurrentRoom,
          unreadCount: isCurrentRoom ? 0 : (targetRoom.unreadCount || 0) + 1,
        }

        // Tạo mảng mới với phòng vừa nhắn nằm ở vị trí đầu tiên (index 0)
        const remainingRooms = this.rooms.filter((_, idx) => idx !== index)
        this.rooms = [updatedRoom, ...remainingRooms]

        if (['1', '2', '3'].includes(msgRoomId)) {
          saveDemoRooms(this.rooms)
        }
      } else {
        // Nếu là phòng mới chưa có trong danh sách -> tải lại danh sách phòng
        this.fetchRooms()
      }
    },

    // 7. Gửi tín hiệu đang gõ phím / ngừng gõ phím qua WebSocket STOMP
    sendTypingStatus(isTyping) {
      if (!this.activeRoomId || ['1', '2', '3'].includes(String(this.activeRoomId))) return
      if (wsService.connected) {
        wsService.send('/app/chat.typing', {
          roomId: this.activeRoomId,
          isTyping: !!isTyping,
        })
      }
    },

    // 8. Nhận tín hiệu đang gõ phím từ thành viên khác trong phòng
    handleTypingEvent(event) {
      if (!event || !event.roomId) return
      const authStore = useAuthStore()

      // Bỏ qua nếu event xuất phát từ chính tài khoản đang đăng nhập
      if (
        event.userId &&
        authStore.user?.userId &&
        String(event.userId) === String(authStore.user.userId)
      ) {
        return
      }

      const rId = String(event.roomId)
      if (!this.typingMap[rId]) {
        this.typingMap[rId] = {}
      }

      const uId = String(event.userId || event.username)

      if (event.isTyping) {
        // Xóa bộ đếm thời gian cũ nếu còn
        if (this.typingMap[rId][uId]?.timer) {
          clearTimeout(this.typingMap[rId][uId].timer)
        }

        // Tự động hủy trạng thái sau 3.5 giây nếu đối phương ngừng gõ hoặc rớt mạng
        const timer = setTimeout(() => {
          if (this.typingMap[rId]?.[uId]) {
            delete this.typingMap[rId][uId]
            this.typingMap = { ...this.typingMap }
          }
        }, 3500)

        this.typingMap[rId][uId] = {
          username: event.username || 'Thành viên',
          avatarUrl: event.avatarUrl,
          timer,
        }
      } else {
        // Đối phương đã ngừng gõ hoặc đã bấm gửi tin nhắn
        if (this.typingMap[rId]?.[uId]) {
          clearTimeout(this.typingMap[rId][uId].timer)
          delete this.typingMap[rId][uId]
        }
      }

      // Kích hoạt tính phản ứng của Vue (Reactivity)
      this.typingMap = { ...this.typingMap }
    },

    // 9. Lấy danh sách tin nhắn đã ghim trong phòng
    async fetchPinnedMessages(roomId) {
      if (!roomId) {
        this.pinnedMessages = []
        return
      }
      if (['1', '2', '3'].includes(String(roomId))) {
        this.pinnedMessages = [
          {
            messageId: 'mock-pin-1',
            roomId: String(roomId),
            sender: { username: 'Minh Anh' },
            content: 'Họp review giao diện lúc 14:00 thứ Sáu tuần này',
            isPinned: true,
          },
        ]
        return
      }
      try {
        const pins = await messageApi.getPinnedMessages(roomId)
        this.pinnedMessages = Array.isArray(pins) ? pins : []
      } catch (err) {
        console.warn('Lỗi tải tin nhắn đã ghim:', err)
        this.pinnedMessages = []
      }
    },

    // 10. Ghim một tin nhắn
    async pinMessage(messageId) {
      if (!this.activeRoomId || !messageId) return
      const rId = String(this.activeRoomId)

      // Giới hạn tối đa 5 tin ghim mỗi phòng
      if (this.pinnedMessages.length >= 5) {
        alert('Phòng chat đã đạt giới hạn tối đa 5 tin ghim. Vui lòng bỏ ghim bớt trước khi ghim tin mới!')
        return
      }

      const authStore = useAuthStore()
      const senderName = authStore.user?.username || 'Bạn'

      if (['1', '2', '3'].includes(rId)) {
        const target = this.messages.find((m) => String(m.messageId) === String(messageId))
        if (target) {
          target.isPinned = true
          if (!this.pinnedMessages.some((p) => String(p.messageId) === String(messageId))) {
            this.pinnedMessages.push(target)
          }
        }
        const room = this.rooms.find((r) => String(r.roomId) === rId)
        if (room && String(room.lastMessageId) === String(messageId)) {
          room.lastMessageContent = 'đã ghim 1 tin nhắn'
          room.lastMessageSenderName = senderName
          room.lastMessageAt = new Date().toISOString()
        } else {
          this.pinNotices[rId] = `📌 ${senderName} đã ghim 1 tin nhắn`
          this.pinNotices = { ...this.pinNotices }
          setTimeout(() => {
            if (this.pinNotices[rId]) {
              delete this.pinNotices[rId]
              this.pinNotices = { ...this.pinNotices }
            }
          }, 4000)
        }
        return
      }
      try {
        const updatedMsg = await messageApi.pinMessage(this.activeRoomId, messageId)
        const idx = this.messages.findIndex((m) => String(m.messageId) === String(messageId))
        if (idx !== -1) {
          this.messages[idx] = { ...this.messages[idx], isPinned: true }
        }
        if (updatedMsg && !this.pinnedMessages.some((p) => String(p.messageId) === String(messageId))) {
          this.pinnedMessages.push(updatedMsg)
        }

        const roomIdx = this.rooms.findIndex((r) => String(r.roomId) === rId)
        if (roomIdx !== -1) {
          const targetRoom = this.rooms[roomIdx]
          const isLast = String(targetRoom.lastMessageId) === String(messageId)
          if (isLast) {
            // Nếu là tin nhắn cuối cùng: giữ preview "đã ghim 1 tin nhắn"
            targetRoom.lastMessageContent = 'đã ghim 1 tin nhắn'
            targetRoom.lastMessageSenderName = senderName
            targetRoom.lastMessageAt = new Date().toISOString()
          } else {
            // Nếu không phải tin cuối: hiện thông báo tạm 4 giây ngoài sidebar
            this.pinNotices[rId] = `📌 ${senderName} đã ghim 1 tin nhắn`
            this.pinNotices = { ...this.pinNotices }
            setTimeout(() => {
              if (this.pinNotices[rId]) {
                delete this.pinNotices[rId]
                this.pinNotices = { ...this.pinNotices }
              }
            }, 4000)
          }
        }
      } catch (err) {
        console.error('Lỗi khi ghim tin nhắn:', err)
      }
    },

    // 11. Bỏ ghim một tin nhắn
    async unpinMessage(messageId) {
      if (!this.activeRoomId || !messageId) return
      const rId = String(this.activeRoomId)
      if (this.pinNotices[rId]) {
        delete this.pinNotices[rId]
        this.pinNotices = { ...this.pinNotices }
      }
      if (['1', '2', '3'].includes(rId)) {
        this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== String(messageId))
        const target = this.messages.find((m) => String(m.messageId) === String(messageId))
        if (target) target.isPinned = false
        return
      }
      try {
        await messageApi.unpinMessage(this.activeRoomId, messageId)
        this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== String(messageId))
        const idx = this.messages.findIndex((m) => String(m.messageId) === String(messageId))
        if (idx !== -1) {
          this.messages[idx] = { ...this.messages[idx], isPinned: false }
        }
      } catch (err) {
        console.error('Lỗi khi bỏ ghim tin nhắn:', err)
      }
    },

    // 12. Chỉnh sửa tin nhắn
    async editMessage(messageId, newContent) {
      if (!this.activeRoomId || !messageId || !newContent || !newContent.trim()) return
      const text = newContent.trim()
      const rId = String(this.activeRoomId)

      // Trường hợp phòng demo
      if (['1', '2', '3'].includes(rId)) {
        const msg = this.messages.find((m) => String(m.messageId) === String(messageId))
        if (msg) {
          msg.content = text
          msg.isEdited = true
          if (this.demoMessagesMap[rId]) {
            const dMsg = this.demoMessagesMap[rId].find((m) => String(m.messageId) === String(messageId))
            if (dMsg) {
              dMsg.content = text
              dMsg.isEdited = true
            }
            saveDemoMessages(this.demoMessagesMap)
          }
        }
        const room = this.rooms.find((r) => String(r.roomId) === rId)
        if (room && String(room.lastMessageId) === String(messageId)) {
          room.lastMessageContent = text
        }
        return
      }

      try {
        const updatedMsg = await messageApi.updateMessage(this.activeRoomId, messageId, text)
        if (updatedMsg) {
          const idx = this.messages.findIndex((m) => String(m.messageId) === String(messageId))
          if (idx !== -1) {
            this.messages[idx] = { ...this.messages[idx], ...updatedMsg }
          }
          const room = this.rooms.find((r) => String(r.roomId) === rId)
          if (room && String(room.lastMessageId) === String(messageId)) {
            room.lastMessageContent = text
          }
        }
      } catch (err) {
        console.error('Lỗi khi chỉnh sửa tin nhắn:', err)
      }
    },

    // 13. Thu hồi tin nhắn (xóa mềm)
    async revokeMessage(messageId) {
      if (!this.activeRoomId || !messageId) return
      const rId = String(this.activeRoomId)

      // Trường hợp phòng demo
      if (['1', '2', '3'].includes(rId)) {
        const msg = this.messages.find((m) => String(m.messageId) === String(messageId))
        if (msg) {
          msg.content = 'Tin nhắn đã được thu hồi'
          msg.isRevoked = true
          msg.isPinned = false
          this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== String(messageId))
          if (this.demoMessagesMap[rId]) {
            const dMsg = this.demoMessagesMap[rId].find((m) => String(m.messageId) === String(messageId))
            if (dMsg) {
              dMsg.content = 'Tin nhắn đã được thu hồi'
              dMsg.isRevoked = true
            }
            saveDemoMessages(this.demoMessagesMap)
          }
        }
        const room = this.rooms.find((r) => String(r.roomId) === rId)
        if (room && String(room.lastMessageId) === String(messageId)) {
          room.lastMessageContent = 'Tin nhắn đã bị thu hồi'
        }
        return
      }

      try {
        const revokedMsg = await messageApi.revokeMessage(this.activeRoomId, messageId)
        const idx = this.messages.findIndex((m) => String(m.messageId) === String(messageId))
        if (idx !== -1) {
          this.messages[idx] = {
            ...this.messages[idx],
            ...(revokedMsg || {}),
            content: 'Tin nhắn đã được thu hồi',
            isRevoked: true,
            isPinned: false,
          }
        }
        this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== String(messageId))
        const room = this.rooms.find((r) => String(r.roomId) === rId)
        if (room && String(room.lastMessageId) === String(messageId)) {
          room.lastMessageContent = 'Tin nhắn đã bị thu hồi'
        }
      } catch (err) {
        console.error('Lỗi khi thu hồi tin nhắn:', err)
      }
    },

    setFilter(filterName) {
      this.currentFilter = filterName
    },

    setSearchQuery(query) {
      this.searchQuery = query
    },
  },
})
