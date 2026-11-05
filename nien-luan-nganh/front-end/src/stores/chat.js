import { defineStore } from 'pinia'
import * as roomApi from '@/api/room.api'
import * as messageApi from '@/api/message.api'
import { wsService, subscribeToRoom } from '@/services/websocket'
import { useAuthStore } from './auth'

const DEFAULT_SAMPLE_ROOMS = []
const DEFAULT_SAMPLE_MESSAGES = {}

function loadDemoRooms() {
  try {
    const raw = localStorage.getItem('bluespace_demo_rooms')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) return parsed
    }
  } catch {}
  return []
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
  return {}
}

function saveDemoMessages(messagesMap) {
  try {
    localStorage.setItem('bluespace_demo_messages', JSON.stringify(messagesMap))
  } catch {}
}

// Lưu trữ trạng thái hoạt động mới nhất của phòng (đặc biệt khi ghim/bỏ ghim)
// để syncRoomsSilently (chạy mỗi 4s) không ghi đè về trạng thái cũ của Backend
function loadRoomOverrides() {
  try {
    const raw = localStorage.getItem('bluespace_room_overrides')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') return parsed
    }
  } catch {}
  return {}
}

function saveRoomOverrides(overrides) {
  try {
    localStorage.setItem('bluespace_room_overrides', JSON.stringify(overrides))
  } catch {}
}

// Lưu danh sách sự kiện ghim / bỏ ghim trong khung chat theo từng phòng
function loadRoomEvents() {
  try {
    localStorage.removeItem('bluespace_room_events')
  } catch {}
  return {}
}

function saveRoomEvents() {
  try {
    localStorage.removeItem('bluespace_room_events')
  } catch {}
}

// Lưu thông tin người vừa bấm ghim/bỏ ghim theo messageId để đồng bộ tức thời giữa các tab và chống race-condition WebSocket
function recordRecentPinActor(messageId, actorUsername, actorUserId, type) {
  try {
    const raw = localStorage.getItem('bluespace_recent_pin_actors')
    const map = raw ? JSON.parse(raw) : {}
    map[String(messageId)] = {
      actorUsername,
      actorUserId,
      type,
      timestamp: Date.now(),
    }
    localStorage.setItem('bluespace_recent_pin_actors', JSON.stringify(map))
  } catch {}
}

function getRecentPinActor(messageId) {
  try {
    const raw = localStorage.getItem('bluespace_recent_pin_actors')
    if (!raw) return null
    const map = JSON.parse(raw)
    const entry = map?.[String(messageId)]
    if (entry && Date.now() - entry.timestamp < 20000) {
      return entry
    }
  } catch {}
  return null
}

// Áp dụng trạng thái hoạt động mới nhất (ghim/bỏ ghim/tin mới) lên danh sách phòng từ Backend và sắp xếp mới nhất lên đầu
function applyRoomOverridesAndSort(backendRooms, overrides, activeRoomId, existingRooms = []) {
  const merged = backendRooms.map((r) => {
    const rId = String(r.roomId)
    const existing = existingRooms.find((old) => String(old.roomId) === rId)
    const isOnline = r.isOnline !== undefined && r.isOnline !== null ? r.isOnline : (existing?.isOnline || false)
    const lastSeenAt = r.lastSeenAt || existing?.lastSeenAt || null
    const isActive = rId === String(activeRoomId)

    let hasUnreadVal = isActive ? false : Boolean(r.hasUnread)
    let unreadCountVal = isActive ? 0 : (r.unreadCount || 0)

    if (isActive) {
      hasUnreadVal = false
      unreadCountVal = 0
    } else if (existing && existing.hasUnread === false && (existing.unreadCount || 0) === 0) {
      // Đã được người dùng đọc trên client -> giữ nguyên đã đọc (hết in đậm)
      hasUnreadVal = false
      unreadCountVal = 0
    } else if (existing && existing.hasUnread !== undefined) {
      hasUnreadVal = Boolean(existing.hasUnread)
      unreadCountVal = existing.unreadCount || 0
    }

    let roomObj = {
      ...r,
      isOnline,
      lastSeenAt,
      hasUnread: hasUnreadVal,
      unreadCount: unreadCountVal,
    }

    const ov = overrides?.[rId]
    if (ov && ov.lastMessageAt) {
      const ovTime = new Date(ov.lastMessageAt).getTime()
      const backendTime = r.lastMessageAt ? new Date(r.lastMessageAt).getTime() : 0
      // Nếu trạng thái ghim/bỏ ghim trên client mới hơn hoặc bằng trạng thái từ Backend -> giữ nguyên trạng thái mới!
      if (ovTime >= backendTime - 2000) {
        roomObj = {
          ...roomObj,
          lastMessageId: ov.lastMessageId || roomObj.lastMessageId,
          lastMessageContent: ov.lastMessageContent,
          lastMessageSenderName: ov.lastMessageSenderName,
          lastMessageAt: ov.lastMessageAt,
        }
      }
    }

    return roomObj
  })

  merged.sort((a, b) => {
    const timeA = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0
    const timeB = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0
    return timeB - timeA
  })

  return merged
}

// Hàm chuẩn hóa nội dung hiển thị trích dẫn (Snippet Helper) dùng chung
export const getReplySnippet = (msg) => {
  if (!msg) return ''
  if (msg.isRevoked) return 'Tin nhắn đã bị thu hồi'
  if (msg.messageType === 'IMAGE') return '[Hình ảnh]'
  if (msg.messageType === 'FILE') return '[Tệp đính kèm]'
  return msg.content || ''
}

// Trích xuất danh sách tin nhắn an toàn từ các cấu trúc phản hồi khác nhau của Backend/Axios
function extractMessageList(res) {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.data)) return res.data
  if (Array.isArray(res.result)) return res.result
  if (Array.isArray(res.content)) return res.content
  if (res.data && Array.isArray(res.data.data)) return res.data.data
  if (res.data && Array.isArray(res.data.result)) return res.data.result
  if (res.data && Array.isArray(res.data.content)) return res.data.content
  return []
}

// Trích xuất thông tin con trỏ phân trang an toàn
function extractCursorInfo(res) {
  if (!res) return { nextCursor: null, hasMore: false }
  const cursorObj = (res.data && typeof res.data === 'object' && !Array.isArray(res.data)) ? res.data : res
  return {
    nextCursor: cursorObj.nextCursor || null,
    hasMore: Boolean(cursorObj.hasMore),
  }
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
    loadingMoreMessages: false, // Trạng thái đang tải thêm tin nhắn cũ hơn (tương thích ngược)
    isLoadingOlder: false, // Trạng thái đang tải tin cũ hơn (cuộn lên trên)
    isLoadingNewer: false, // Trạng thái đang tải tin mới hơn (cuộn xuống dưới)
    hasMoreOlder: true, // Còn tin cũ hơn ở trên không?
    hasMoreNewer: false, // Còn tin mới hơn ở dưới không?
    nextCursor: null, // Con trỏ phân trang tin nhắn cũ hơn
    hasMoreMessages: false,
    isViewingOlderSegment: false, // Đang xem một đoạn tin nhắn cũ (sau khi bấm tin nhắn ghim)
    isDetachedFromBottom: false, // Cờ đánh dấu đang tách khỏi đáy (đang xem khúc giữa lịch sử quanh tin ghim)

    // Danh sách tin nhắn đã ghim trong phòng đang mở (tối đa 5 tin)
    pinnedMessages: [],

    // Kho lưu trữ tin nhắn mẫu dự phòng (nếu dùng các phòng mẫu '1', '2', '3')
    demoMessagesMap: loadDemoMessages(),

    // Lưu trữ trạng thái gõ chữ: { [roomId]: { [userId]: { username, avatarUrl, timer } } }
    typingMap: {},

    // Thông báo ghim tin nhắn tạm thời ngoài Sidebar: { [roomId]: string }
    pinNotices: {},

    // Lưu trạng thái hoạt động mới nhất của từng phòng để syncRoomsSilently không ghi đè về trạng thái cũ
    roomActivityOverrides: loadRoomOverrides(),

    // Lưu lịch sử các sự kiện ghim/bỏ ghim trong khung chat theo từng phòng
    roomEventsMap: loadRoomEvents(),

    // Ghi nhận hành động ghim/bỏ ghim do chính tab hiện tại vừa thực hiện: { [messageId]: { type, actorUsername, actorUserId, timestamp } }
    myRecentPinActions: {},

    // Thông tin tin nhắn đang được chọn để Trả lời (Reply)
    replyingMessage: null,
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
        ? state.pinnedMessages[0]
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
          this.roomActivityOverrides = {
            ...loadRoomOverrides(),
            ...this.roomActivityOverrides,
          }
          this.rooms = applyRoomOverridesAndSort(
            backendRooms,
            this.roomActivityOverrides,
            this.activeRoomId,
            this.rooms,
          )
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

          this.roomActivityOverrides = {
            ...loadRoomOverrides(),
            ...this.roomActivityOverrides,
          }
          this.rooms = applyRoomOverridesAndSort(
            backendRooms,
            this.roomActivityOverrides,
            this.activeRoomId,
            this.rooms,
          )

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
      if (!isSameRoom) {
        this.clearReplyingMessage()
      }
      this.activeRoomId = roomId
      this.isViewingOlderSegment = false
      this.isDetachedFromBottom = false

      // Xóa cờ chưa đọc cho phòng vừa chọn
      const rId = String(roomId)
      const index = this.rooms.findIndex((r) => String(r.roomId) === rId)
      if (index !== -1) {
        this.rooms[index] = {
          ...this.rooms[index],
          hasUnread: false,
          unreadCount: 0,
        }
        if (['1', '2', '3'].includes(rId)) {
          saveDemoRooms(this.rooms)
        }
      }

      if (this.roomActivityOverrides[rId]) {
        this.roomActivityOverrides[rId] = {
          ...this.roomActivityOverrides[rId],
          hasUnread: false,
          unreadCount: 0,
        }
        saveRoomOverrides(this.roomActivityOverrides)
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
        await this.markRoomAsRead(roomId)
      }
    },

    // 2.1. Đánh dấu đã đọc phòng chat (Read Watermark - cập nhật xuống MySQL và Pinia)
    async markRoomAsRead(roomId, messageId = null) {
      if (!roomId) return
      const rId = String(roomId)

      // Cập nhật Optimistic trên state Pinia ngay lập tức
      const index = this.rooms.findIndex((r) => String(r.roomId) === rId)
      if (index !== -1) {
        this.rooms[index] = {
          ...this.rooms[index],
          hasUnread: false,
          unreadCount: 0,
        }
        if (['1', '2', '3'].includes(rId)) {
          saveDemoRooms(this.rooms)
        }
      }

      if (this.roomActivityOverrides[rId]) {
        this.roomActivityOverrides[rId] = {
          ...this.roomActivityOverrides[rId],
          hasUnread: false,
          unreadCount: 0,
        }
        saveRoomOverrides(this.roomActivityOverrides)
      }

      // Nếu là phòng demo thì không gọi API
      if (['1', '2', '3'].includes(rId)) return

      let targetId = messageId
      if (!targetId && String(this.activeRoomId) === rId && this.messages.length > 0) {
        const latestReal = [...this.messages].reverse().find(
          (m) => m && !m.isEvent && m.messageId && !String(m.messageId).startsWith('event-') && !String(m.messageId).startsWith('mock-')
        )
        if (latestReal) {
          targetId = latestReal.messageId
        }
      }

      try {
        await messageApi.markAsRead(roomId, targetId)
      } catch (err) {
        // Bỏ qua lỗi
      }
    },

    // 3. Tải lịch sử tin nhắn của một phòng
    async fetchMessages(roomId) {
      if (!roomId) return
      const key = String(roomId)
      this.loadingMessages = true
      this.isViewingOlderSegment = false
      this.isDetachedFromBottom = false

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
        const res = await messageApi.getMessages(roomId, { limit: 20 })
        const rawList = extractMessageList(res)
        // Backend trả về DESC (mới nhất trước) -> Đảo ngược lại để hiển thị từ cũ -> mới (cuộn xuống dưới)
        const list = [...rawList].reverse()
        const authStore = useAuthStore()

        // Danh sách tin nhắn trực tiếp từ CSDL (đã bao gồm các tin nhắn SYSTEM)
        this.messages = list

        const cursorInfo = extractCursorInfo(res)
        const oldestRealMessage = rawList.length > 0 ? rawList[rawList.length - 1] : null
        this.nextCursor = cursorInfo.nextCursor || oldestRealMessage?.messageId || null
        
        // Nếu backend có trả về trường hasMore cụ thể thì dùng, còn không thì dựa trên số lượng tin (>= 20 nghĩa là còn tin cũ)
        const hasMoreCalculated = cursorInfo.hasMore !== undefined && cursorInfo.hasMore !== false
          ? Boolean(cursorInfo.hasMore)
          : (rawList.length >= 20)
        this.hasMoreOlder = hasMoreCalculated
        this.hasMoreNewer = false
        this.hasMoreMessages = hasMoreCalculated
        this.isDetachedFromBottom = false
        this.isViewingOlderSegment = false

        // Đánh dấu đã đọc trên backend với mốc tin nhắn mới nhất
        const latestRealMessage = [...this.messages].reverse().find(
          (m) => m && !m.isEvent && m.messageId && !String(m.messageId).startsWith('event-') && !String(m.messageId).startsWith('mock-')
        )
        await this.markRoomAsRead(roomId, latestRealMessage?.messageId || null)
      } catch (err) {
        console.error('Lỗi tải danh sách tin nhắn:', err)
        this.messages = []
      } finally {
        this.loadingMessages = false
      }
    },

    // 3.1. Tải thêm tin nhắn CŨ HƠN khi cuộn lên đỉnh (Prepends vào đầu mảng)
    async loadOlderMessages() {
      if (!this.activeRoomId || this.isLoadingOlder || !this.hasMoreOlder || this.messages.length === 0) {
        return false
      }
      const rId = String(this.activeRoomId)
      if (['1', '2', '3'].includes(rId)) {
        this.hasMoreOlder = false
        this.hasMoreMessages = false
        return false
      }

      this.isLoadingOlder = true
      this.loadingMoreMessages = true

      // Tìm tin nhắn thật (không phải sự kiện ghim/hệ thống) cũ nhất hiện tại trong mảng messages
      const firstRealMessage = this.messages.find(
        (m) => m && !m.isEvent && m.messageId && !String(m.messageId).startsWith('event-') && !String(m.messageId).startsWith('mock-')
      )
      const topCursor = this.nextCursor || firstRealMessage?.messageId
      if (!topCursor) {
        this.isLoadingOlder = false
        this.loadingMoreMessages = false
        this.hasMoreOlder = false
        return false
      }

      try {
        const res = await messageApi.getMessagesBefore(this.activeRoomId, topCursor, 20)
        const rawList = extractMessageList(res)
        const olderBatch = [...rawList].reverse()

        const cursorInfo = extractCursorInfo(res)
        const hasMoreCalculated = cursorInfo.hasMore !== undefined && cursorInfo.hasMore !== false
          ? Boolean(cursorInfo.hasMore)
          : (rawList.length >= 20)
        this.hasMoreOlder = hasMoreCalculated
        this.hasMoreMessages = hasMoreCalculated

        if (olderBatch.length > 0) {
          const existingIds = new Set(this.messages.map((m) => String(m.messageId)))
          const uniqueOlder = olderBatch.filter((m) => !existingIds.has(String(m.messageId)))
          this.messages = [...uniqueOlder, ...this.messages]
        }

        // Cập nhật nextCursor cho lần cuộn tiếp theo
        const newFirstReal = this.messages.find(
          (m) => m && !m.isEvent && m.messageId && !String(m.messageId).startsWith('event-')
        )
        this.nextCursor = cursorInfo.nextCursor || newFirstReal?.messageId || null
        return true
      } catch (err) {
        console.error('Lỗi tải thêm tin nhắn cũ:', err)
        return false
      } finally {
        this.isLoadingOlder = false
        this.loadingMoreMessages = false
      }
    },

    // 3.2. Tải thêm tin nhắn MỚI HƠN khi cuộn xuống đáy (Appends vào cuối mảng)
    async loadNewerMessages() {
      if (!this.activeRoomId || this.isLoadingNewer || !this.hasMoreNewer || this.messages.length === 0) {
        return false
      }
      const rId = String(this.activeRoomId)
      if (['1', '2', '3'].includes(rId)) {
        this.hasMoreNewer = false
        this.isDetachedFromBottom = false
        return false
      }

      this.isLoadingNewer = true
      const bottomCursor = this.messages[this.messages.length - 1]?.messageId
      if (!bottomCursor) {
        this.isLoadingNewer = false
        return false
      }

      try {
        const res = await messageApi.getMessagesAfter(this.activeRoomId, bottomCursor, 20)
        const rawList = extractMessageList(res)
        const newerBatch = Array.isArray(rawList) ? rawList : []

        if (!newerBatch || newerBatch.length < 20) {
          // Trả về ít hơn 20 tin tức là đã chạm tới tin mới nhất của phòng
          this.hasMoreNewer = false
          this.isDetachedFromBottom = false
          this.isViewingOlderSegment = false
        }

        if (newerBatch.length > 0) {
          const existingIds = new Set(this.messages.map((m) => String(m.messageId)))
          const uniqueNewer = newerBatch.filter((m) => !existingIds.has(String(m.messageId)))
          this.messages = [...this.messages, ...uniqueNewer]
        }
        return true
      } catch (err) {
        console.error('Lỗi tải thêm tin nhắn mới hơn:', err)
        return false
      } finally {
        this.isLoadingNewer = false
      }
    },

    // 3.3. Tải 1 đoạn ~20 tin nhắn xung quanh tin nhắn đã ghim (nhảy tới tin nhắn ghim)
    async fetchMessagesAround(messageId) {
      if (!this.activeRoomId || !messageId) return false
      const key = String(this.activeRoomId)
      if (['1', '2', '3'].includes(key)) {
        return false
      }

      this.loadingMessages = true
      try {
        const res = await messageApi.getMessages(this.activeRoomId, {
          around: messageId,
          limit: 20,
        })
        const rawList = extractMessageList(res)
        const list = [...rawList].reverse()
        this.messages = list
        const cursorInfo = extractCursorInfo(res)
        this.nextCursor = cursorInfo.nextCursor
        this.hasMoreOlder = Boolean(cursorInfo.hasMore && this.nextCursor)
        this.hasMoreNewer = true
        this.hasMoreMessages = this.hasMoreOlder
        this.isViewingOlderSegment = true
        this.isDetachedFromBottom = true
        return true
      } catch (err) {
        console.error('Lỗi tải tin nhắn xung quanh tin ghim:', err)
        return false
      } finally {
        this.loadingMessages = false
      }
    },

    // 3.4. Lấy cửa sổ ngữ cảnh 20 tin nhắn bao quanh tin ghim (10 trước + target + 10 sau)
    async jumpToPinnedMessageContext(messageId) {
      if (!this.activeRoomId || !messageId) return false
      const key = String(this.activeRoomId)
      if (['1', '2', '3'].includes(key)) {
        return false
      }

      this.loadingMessages = true
      try {
        const res = await messageApi.getMessageContext(this.activeRoomId, messageId)
        const list = extractMessageList(res)
        if (list.length > 0) {
          this.messages = list
          // Bật cờ phân trang 2 chiều vì đang đứng ở khúc giữa lịch sử quanh tin ghim
          this.hasMoreOlder = true
          this.hasMoreNewer = true
          this.hasMoreMessages = true
          this.isDetachedFromBottom = true
          this.isViewingOlderSegment = true
          // nextCursor là ID của tin nhắn cũ nhất (phần tử đầu tiên trong list theo thứ tự ASC)
          this.nextCursor = list[0]?.messageId || null
          return true
        }
        return false
      } catch (err) {
        console.error('Lỗi lấy cửa sổ ngữ cảnh tin nhắn ghim:', err)
        return false
      } finally {
        this.loadingMessages = false
      }
    },

    // 3.5. Quản lý trạng thái Trả lời (Reply) tin nhắn
    setReplyingMessage(message) {
      if (!message || message.isRevoked || message.messageType === 'SYSTEM') return
      this.replyingMessage = {
        messageId: message.messageId,
        sender: message.sender,
        content: message.content,
        messageType: message.messageType || 'TEXT',
        isRevoked: Boolean(message.isRevoked),
      }
    },

    clearReplyingMessage() {
      this.replyingMessage = null
    },

    // 4. Gửi tin nhắn mới
    async sendChatMessage(content) {
      if (!this.activeRoomId || !content || !content.trim()) return
      const text = content.trim()
      const authStore = useAuthStore()
      const roomKey = String(this.activeRoomId)

      const repliedMessageId = this.replyingMessage?.messageId || null
      const replyingSnapshot = this.replyingMessage
        ? {
            messageId: this.replyingMessage.messageId,
            senderId: this.replyingMessage.sender?.userId || null,
            senderUsername: this.replyingMessage.sender?.username || 'Bạn',
            content: this.replyingMessage.content,
          }
        : null

      // Hủy trạng thái reply ngay khi gửi
      this.clearReplyingMessage()

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
          repliedMessage: replyingSnapshot,
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
        const payload = {
          content: text,
          messageType: 'TEXT',
        }
        if (repliedMessageId) {
          payload.repliedMessageId = repliedMessageId
        }
        const sentMsg = await messageApi.sendMessage(this.activeRoomId, payload)
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
      const msgId = String(message.messageId)
      const isCurrentRoom = String(this.activeRoomId) === msgRoomId

      const targetRoom = this.rooms.find((r) => String(r.roomId) === msgRoomId)
      const existingMsg = isCurrentRoom
        ? this.messages.find((m) => String(m.messageId) === msgId)
        : null
      const wasPinned = isCurrentRoom
        ? this.pinnedMessages.some((p) => String(p.messageId) === msgId)
        : false

      const isMyMsg =
        (authStore.user?.userId &&
          String(message.sender?.userId) === String(authStore.user.userId)) ||
        (authStore.user?.username && message.sender?.username === authStore.user.username) ||
        message.sender?.userId === 'me'

      const myPinRecord = this.myRecentPinActions[msgId]
      const hasValidMyPinRecord = Boolean(myPinRecord && Date.now() - myPinRecord.timestamp < 20000)
      const sharedPinActor = getRecentPinActor(msgId)
      const msgAgeMs = message.createdAt ? Math.abs(Date.now() - new Date(message.createdAt).getTime()) : 0

      // Xác định xem message broadcast này là hành động Ghim hay Bỏ ghim
      const isSystemPin = message.messageType === 'SYSTEM' && Boolean(message.content && message.content.includes('đã ghim') && !message.content.includes('đã bỏ ghim'))
      const isSystemUnpin = message.messageType === 'SYSTEM' && Boolean(message.content && message.content.includes('đã bỏ ghim'))

      const isPinAction = Boolean(
        isSystemPin ||
        (message.isPinned &&
        !message.isRevoked &&
        (!wasPinned || (hasValidMyPinRecord && myPinRecord.type === 'pin') || sharedPinActor?.type === 'pin')),
      )
      const isUnpinAction = Boolean(
        isSystemUnpin ||
        (message.isPinned === false &&
        !message.isRevoked &&
        (wasPinned ||
          existingMsg?.isPinned ||
          (hasValidMyPinRecord && myPinRecord.type === 'unpin') ||
          sharedPinActor?.type === 'unpin' ||
          (message.pinnedBy && msgAgeMs > 1500))),
      )
      const isPinOrUnpin = isPinAction || isUnpinAction

      // Xác định chính xác NGƯỜI THỰC HIỆN GHIM / BỎ GHIM (tuyệt đối không lấy nhầm tác giả tin nhắn cũ)
      let isMyPinAction = false
      let actorUsername = 'Thành viên'
      let actorUserId = null

      if (message.messageType === 'SYSTEM') {
        isMyPinAction = isMyMsg
        actorUsername = isMyMsg ? (authStore.user?.username || 'Bạn') : (message.sender?.username || 'Thành viên')
        actorUserId = message.sender?.userId || null
      } else if (hasValidMyPinRecord) {
        isMyPinAction = true
        actorUsername = myPinRecord.actorUsername || authStore.user?.username || 'Bạn'
        actorUserId = myPinRecord.actorUserId || authStore.user?.userId || 'me'
      } else if (sharedPinActor) {
        actorUsername = sharedPinActor.actorUsername || 'Thành viên'
        actorUserId = sharedPinActor.actorUserId || null
        isMyPinAction =
          (authStore.user?.userId && actorUserId && String(actorUserId) === String(authStore.user.userId)) ||
          (authStore.user?.username && actorUsername === authStore.user.username)
      } else if (isPinOrUnpin && message.pinnedBy && (message.pinnedBy.username || message.pinnedBy.userId)) {
        actorUsername = message.pinnedBy.username || 'Thành viên'
        actorUserId = message.pinnedBy.userId || null
        isMyPinAction =
          (authStore.user?.userId && actorUserId && String(actorUserId) === String(authStore.user.userId)) ||
          (authStore.user?.username && actorUsername === authStore.user.username)
      } else if (isPinOrUnpin && !isSystemPin && !isSystemUnpin) {
        // Tab này không bấm ghim -> người bấm ghim là thành viên khác trong phòng
        isMyPinAction = false
        if (targetRoom?.roomType === 'DIRECT') {
          // Chat 1-1: tên đối phương chính là targetRoom.roomName
          actorUsername = (!isMyMsg && message.sender?.username)
            ? message.sender.username
            : (targetRoom?.roomName || 'Thành viên')
        } else {
          // Chat nhóm (GROUP): Tuyệt đối không lấy targetRoom.roomName (vì đó là tên nhóm)
          actorUsername = message.pinnedBy?.username || (!isMyMsg && message.sender?.username) || 'Thành viên'
        }
      }

      const pinnerName = isMyPinAction ? 'Bạn' : actorUsername

      // 6.1. Nếu tin nhắn thuộc phòng đang mở -> thêm vào hoặc cập nhật danh sách hiển thị
      if (isCurrentRoom) {
        const existsIndex = this.messages.findIndex((m) => String(m.messageId) === msgId)
        if (existsIndex !== -1) {
          this.messages[existsIndex] = { ...this.messages[existsIndex], ...message }
        } else {
          this.messages.push(message)
        }

        // Nếu là tin nhắn của chính mình gửi đi -> đưa trạng thái xem về sát đáy mới nhất
        if (isMyMsg) {
          this.isDetachedFromBottom = false
          this.hasMoreNewer = false
        }

        // Cập nhật nội dung trích dẫn của các tin nhắn đang reply tin này nếu vừa được sửa hoặc thu hồi
        if (message.isRevoked) {
          this.handleMessageRevoked(msgId)
        } else if (message.isEdited) {
          this.handleMessageEdited(message)
        }

        // Xử lý tin nhắn hệ thống (SYSTEM) cho sự kiện Ghim / Bỏ ghim
        if (message.messageType === 'SYSTEM') {
          const targetMsgId = message.repliedMessage?.messageId
          const isPin = message.content && message.content.includes('đã ghim') && !message.content.includes('đã bỏ ghim')
          const isUnpin = message.content && message.content.includes('đã bỏ ghim')

          if (targetMsgId) {
            // Xóa sự kiện tạm thời do chính tab này bấm ghim/bỏ ghim trước đó
            this.messages = this.messages.filter(
              (m) =>
                !m.isEvent ||
                (!String(m.messageId).includes(`event-pin-${targetMsgId}`) &&
                  !String(m.messageId).includes(`event-unpin-${targetMsgId}`)),
            )

            const targetIdx = this.messages.findIndex((m) => String(m.messageId) === String(targetMsgId))
            if (targetIdx !== -1) {
              this.messages[targetIdx] = {
                ...this.messages[targetIdx],
                isPinned: isPin && !isUnpin,
              }
            }
          }

          if (isPin) {
            this.fetchPinnedMessages(msgRoomId)
          } else if (isUnpin && targetMsgId) {
            this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== String(targetMsgId))
          }
        }

        // Đồng bộ danh sách pinnedMessages nếu nhận được cập nhật tin nhắn thông thường
        if (message.messageType !== 'SYSTEM') {
          if (message.isPinned && !message.isRevoked) {
            const pinIdx = this.pinnedMessages.findIndex((p) => String(p.messageId) === msgId)
            if (pinIdx !== -1) {
              this.pinnedMessages[pinIdx] = message
            } else {
              this.pinnedMessages.push(message)
            }
          } else if (message.isPinned === false || message.isRevoked) {
            this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== msgId)
          }
        }
      }

      // 6.2. Cập nhật thông tin phòng trên Sidebar bên trái và đưa phòng lên vị trí đầu tiên
      const index = this.rooms.findIndex((r) => String(r.roomId) === msgRoomId)
      if (index !== -1) {
        const roomToUpdate = this.rooms[index]

        let displayContent = message.content
        if (message.isRevoked) {
          displayContent = 'Tin nhắn đã bị thu hồi'
        } else if (isPinAction) {
          displayContent = 'đã ghim 1 tin nhắn'
        } else if (isUnpinAction) {
          displayContent = 'đã bỏ ghim 1 tin nhắn'
        }

        const senderDisplayName = isPinOrUnpin
          ? (isMyPinAction ? 'Bạn' : actorUsername)
          : (isMyMsg ? 'Bạn' : (message.sender?.username || 'Thành viên'))

        // Xóa thông báo ghim tạm thời khi có tin nhắn mới tới
        if (this.pinNotices[msgRoomId]) {
          delete this.pinNotices[msgRoomId]
          this.pinNotices = { ...this.pinNotices }
        }

        const nowIso = new Date().toISOString()
        const activityTime = isPinOrUnpin ? nowIso : (message.createdAt || nowIso)
        const isUnreadForMe = !isCurrentRoom && (!isPinOrUnpin || !isMyPinAction)

        let nextUnreadCount = 0
        if (isUnreadForMe) {
          if (isPinOrUnpin) {
            nextUnreadCount = Math.max(1, roomToUpdate.unreadCount || 0)
          } else {
            nextUnreadCount = (roomToUpdate.unreadCount || 0) + 1
          }
        } else if (!isCurrentRoom) {
          nextUnreadCount = roomToUpdate.unreadCount || 0
        }

        const updatedRoom = {
          ...roomToUpdate,
          lastMessageId: message.messageId,
          lastMessageContent: displayContent,
          lastMessageSenderName: senderDisplayName,
          lastMessageAt: activityTime,
          hasUnread: isUnreadForMe,
          unreadCount: nextUnreadCount,
        }

        // Lưu override để syncRoomsSilently (4s/lần) giữ nguyên trạng thái mới ở đầu danh sách
        this.roomActivityOverrides[msgRoomId] = {
          lastMessageId: updatedRoom.lastMessageId,
          lastMessageContent: updatedRoom.lastMessageContent,
          lastMessageSenderName: isPinOrUnpin ? actorUsername : updatedRoom.lastMessageSenderName,
          lastMessageAt: updatedRoom.lastMessageAt,
          hasUnread: updatedRoom.hasUnread,
          unreadCount: updatedRoom.unreadCount,
        }
        saveRoomOverrides(this.roomActivityOverrides)

        // Tạo mảng mới với phòng vừa hoạt động nằm ở vị trí đầu tiên (index 0)
        const remainingRooms = this.rooms.filter((_, idx) => idx !== index)
        this.rooms = [updatedRoom, ...remainingRooms]

        if (['1', '2', '3'].includes(msgRoomId)) {
          saveDemoRooms(this.rooms)
        }

        // Nếu người dùng đang mở xem chính phòng này thì tự động đánh dấu đã đọc ngay lập tức
        if (isCurrentRoom && !message.isRevoked && message.messageId && !String(message.messageId).startsWith('mock-')) {
          this.markRoomAsRead(msgRoomId, message.messageId)
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

    // 8.1. Nhận sự kiện thả/gỡ cảm xúc trên tin nhắn từ WebSocket
    handleReactionEvent(event) {
      if (!event || !event.messageId) return
      const msgId = String(event.messageId)
      const targetMessage = this.messages.find((m) => String(m.messageId) === msgId)
      if (!targetMessage) return

      if (!Array.isArray(targetMessage.reactions)) {
        targetMessage.reactions = []
      }

      const userIdStr = String(event.userId)
      const existingIdx = targetMessage.reactions.findIndex((r) => String(r.userId) === userIdStr)
      const isRemoved = !event.reactionType || event.isRemoved

      if (isRemoved) {
        if (existingIdx !== -1) {
          targetMessage.reactions.splice(existingIdx, 1)
        }
      } else {
        if (existingIdx !== -1) {
          targetMessage.reactions[existingIdx] = {
            ...targetMessage.reactions[existingIdx],
            reactionType: event.reactionType,
            username: event.username || targetMessage.reactions[existingIdx].username,
          }
        } else {
          targetMessage.reactions.push({
            userId: event.userId,
            reactionType: event.reactionType,
            username: event.username,
            avatarUrl: event.avatarUrl,
          })
        }
      }

      // Kích hoạt tính phản ứng của Vue
      this.messages = [...this.messages]
    },

    // 8.2. Thả hoặc gỡ cảm xúc với Optimistic UI (Phản hồi 0ms)
    async toggleReaction(messageId, targetType) {
      if (!this.activeRoomId || !messageId || !targetType) return
      const authStore = useAuthStore()
      const currentUserId = authStore.user?.userId
      const currentUsername = authStore.user?.username || 'Bạn'
      const roomKey = String(this.activeRoomId)

      const targetMessage = this.messages.find((m) => String(m.messageId) === String(messageId))
      if (!targetMessage) return

      if (!Array.isArray(targetMessage.reactions)) {
        targetMessage.reactions = []
      }

      const previousReactions = JSON.parse(JSON.stringify(targetMessage.reactions))
      const existingIdx = targetMessage.reactions.findIndex((r) => String(r.userId) === String(currentUserId))
      const isSameType = existingIdx !== -1 && targetMessage.reactions[existingIdx].reactionType === targetType

      // 1. CẬP NHẬT GIAO DIỆN LẠC QUAN (OPTIMISTIC UI - 0ms)
      if (isSameType) {
        // Gỡ cảm xúc khi click lại đúng icon đã thả
        targetMessage.reactions.splice(existingIdx, 1)
      } else if (existingIdx !== -1) {
        // Đổi loại cảm xúc
        targetMessage.reactions[existingIdx].reactionType = targetType
      } else {
        // Thả mới cảm xúc
        targetMessage.reactions.push({
          userId: currentUserId,
          reactionType: targetType,
          username: currentUsername,
        })
      }
      this.messages = [...this.messages]

      if (['1', '2', '3'].includes(roomKey)) {
        return
      }

      // 2. GỌI API BACKEND ĐỒNG BỘ
      try {
        if (isSameType) {
          await messageApi.removeReaction(this.activeRoomId, messageId)
        } else {
          await messageApi.reactMessage(this.activeRoomId, messageId, { reactionType: targetType })
        }
      } catch (error) {
        console.error('Lỗi cập nhật cảm xúc:', error)
        // Hoàn tác lại trạng thái cũ nếu API báo lỗi
        targetMessage.reactions = previousReactions
        this.messages = [...this.messages]
      }
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
      const msgId = String(messageId)

      // Giới hạn tối đa 5 tin ghim mỗi phòng
      if (this.pinnedMessages.length >= 5) {
        alert('Phòng chat đã đạt giới hạn tối đa 5 tin ghim. Vui lòng bỏ ghim bớt trước khi ghim tin mới!')
        return
      }

      const authStore = useAuthStore()
      const senderName = authStore.user?.username || 'Bạn'
      const senderId = authStore.user?.userId || 'me'
      const nowIso = new Date().toISOString()

      // Ghi nhận NGAY LẬP TỨC mình là người bấm ghim trước khi gọi API (chống race-condition với WebSocket)
      this.myRecentPinActions[msgId] = {
        type: 'pin',
        actorUsername: senderName,
        actorUserId: senderId,
        timestamp: Date.now(),
      }
      recordRecentPinActor(msgId, senderName, senderId, 'pin')

      // Đánh dấu ghim ngay lập tức trên giao diện
      const target = this.messages.find((m) => String(m.messageId) === msgId)
      if (target) {
        target.isPinned = true
        if (!this.pinnedMessages.some((p) => String(p.messageId) === msgId)) {
          this.pinnedMessages.push(target)
        }
      }

      // Đưa phòng lên đầu Sidebar và lưu vào roomActivityOverrides để giữ nguyên ở đầu
      const roomIdx = this.rooms.findIndex((r) => String(r.roomId) === rId)
      if (roomIdx !== -1) {
        const targetRoom = this.rooms[roomIdx]
        const updatedRoom = {
          ...targetRoom,
          lastMessageId: messageId,
          lastMessageContent: 'đã ghim 1 tin nhắn',
          lastMessageSenderName: 'Bạn',
          lastMessageAt: nowIso,
          hasUnread: false,
          unreadCount: 0,
        }
        this.roomActivityOverrides[rId] = {
          lastMessageId: messageId,
          lastMessageContent: 'đã ghim 1 tin nhắn',
          lastMessageSenderName: senderName,
          lastMessageAt: nowIso,
        }
        saveRoomOverrides(this.roomActivityOverrides)

        const remainingRooms = this.rooms.filter((_, idx) => idx !== roomIdx)
        this.rooms = [updatedRoom, ...remainingRooms]
        if (['1', '2', '3'].includes(rId)) {
          saveDemoRooms(this.rooms)
        }
      }

      if (['1', '2', '3'].includes(rId)) {
        return
      }

      try {
        const systemMsg = await messageApi.pinMessage(this.activeRoomId, messageId)
        await this.fetchPinnedMessages(this.activeRoomId)
        if (systemMsg && !this.messages.some((m) => String(m.messageId) === String(systemMsg.messageId))) {
          this.messages.push(systemMsg)
        }
      } catch (err) {
        console.error('Lỗi khi ghim tin nhắn:', err)
      }
    },

    // 11. Bỏ ghim một tin nhắn
    async unpinMessage(messageId) {
      if (!this.activeRoomId || !messageId) return
      const authStore = useAuthStore()
      const rId = String(this.activeRoomId)
      const msgId = String(messageId)
      const senderName = authStore.user?.username || 'Bạn'
      const senderId = authStore.user?.userId || 'me'
      const nowIso = new Date().toISOString()

      // Ghi nhận NGAY LẬP TỨC mình là người bấm bỏ ghim trước khi gọi API
      this.myRecentPinActions[msgId] = {
        type: 'unpin',
        actorUsername: senderName,
        actorUserId: senderId,
        timestamp: Date.now(),
      }
      recordRecentPinActor(msgId, senderName, senderId, 'unpin')

      if (this.pinNotices[rId]) {
        delete this.pinNotices[rId]
        this.pinNotices = { ...this.pinNotices }
      }

      // Đánh dấu gỡ ghim ngay lập tức trên giao diện
      this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== msgId)
      const target = this.messages.find((m) => String(m.messageId) === msgId)
      if (target) target.isPinned = false

      // Đưa phòng lên đầu Sidebar và lưu vào roomActivityOverrides để giữ nguyên ở đầu
      const roomIdx = this.rooms.findIndex((r) => String(r.roomId) === rId)
      if (roomIdx !== -1) {
        const targetRoom = this.rooms[roomIdx]
        const updatedRoom = {
          ...targetRoom,
          lastMessageId: messageId,
          lastMessageContent: 'đã bỏ ghim 1 tin nhắn',
          lastMessageSenderName: 'Bạn',
          lastMessageAt: nowIso,
          hasUnread: false,
          unreadCount: 0,
        }
        this.roomActivityOverrides[rId] = {
          lastMessageId: messageId,
          lastMessageContent: 'đã bỏ ghim 1 tin nhắn',
          lastMessageSenderName: senderName,
          lastMessageAt: nowIso,
        }
        saveRoomOverrides(this.roomActivityOverrides)

        const remainingRooms = this.rooms.filter((_, idx) => idx !== roomIdx)
        this.rooms = [updatedRoom, ...remainingRooms]
        if (['1', '2', '3'].includes(rId)) {
          saveDemoRooms(this.rooms)
        }
      }

      if (['1', '2', '3'].includes(rId)) {
        return
      }

      try {
        const systemMsg = await messageApi.unpinMessage(this.activeRoomId, messageId)
        if (systemMsg && !this.messages.some((m) => String(m.messageId) === String(systemMsg.messageId))) {
          this.messages.push(systemMsg)
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
        this.handleMessageEdited({ messageId, content: text, isEdited: true })
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
          this.handleMessageEdited(updatedMsg)
          const room = this.rooms.find((r) => String(r.roomId) === rId)
          if (room && String(room.lastMessageId) === String(messageId)) {
            room.lastMessageContent = text
          }
        }
      } catch (err) {
        console.error('Lỗi khi chỉnh sửa tin nhắn:', err)
      }
    },

    // 12.1. Cập nhật đồng bộ các tin nhắn đang reply khi tin nhắn gốc được sửa
    handleMessageEdited(updatedMessage) {
      if (!updatedMessage) return
      const targetId = String(updatedMessage.messageId)
      const newContent = updatedMessage.content

      this.messages.forEach((msg) => {
        // 1. Cập nhật chính tin nhắn gốc được sửa
        if (String(msg.messageId) === targetId) {
          msg.content = newContent
          msg.isEdited = true
          if (updatedMessage.updatedAt) {
            msg.updatedAt = updatedMessage.updatedAt
          }
        }

        // 2. Cập nhật nội dung trích dẫn của các tin nhắn đang reply tin này
        if (msg.repliedMessage && String(msg.repliedMessage.messageId) === targetId) {
          msg.repliedMessage.content = newContent
          msg.repliedMessage.isEdited = true
        }
      })

      // Cập nhật nếu đang trong thanh Reply banner ở ô ChatInput
      if (this.replyingMessage && String(this.replyingMessage.messageId) === targetId) {
        this.replyingMessage.content = newContent
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
        this.handleMessageRevoked(messageId)
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
        this.handleMessageRevoked(messageId)
        this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== String(messageId))
        const room = this.rooms.find((r) => String(r.roomId) === rId)
        if (room && String(room.lastMessageId) === String(messageId)) {
          room.lastMessageContent = 'Tin nhắn đã bị thu hồi'
        }
      } catch (err) {
        console.error('Lỗi khi thu hồi tin nhắn:', err)
      }
    },

    // 13.1. Cập nhật đồng bộ các tin nhắn đang reply khi tin nhắn gốc bị thu hồi
    handleMessageRevoked(revokedMessageId) {
      if (!revokedMessageId) return
      const targetId = String(revokedMessageId)

      this.messages.forEach((msg) => {
        // 1. Đánh dấu thu hồi tin gốc
        if (String(msg.messageId) === targetId) {
          msg.isRevoked = true
          msg.content = 'Tin nhắn đã được thu hồi'
          msg.isPinned = false
        }

        // 2. Cập nhật khối quote của các tin đang reply tin vừa bị thu hồi
        if (msg.repliedMessage && String(msg.repliedMessage.messageId) === targetId) {
          msg.repliedMessage.isRevoked = true
          msg.repliedMessage.content = 'Tin nhắn đã được thu hồi'
        }
      })

      // Đồng bộ pinnedMessages nếu tin bị thu hồi đang được ghim
      this.pinnedMessages = this.pinnedMessages.filter((p) => String(p.messageId) !== targetId)

      // Hủy reply banner nếu tin đang chọn reply bị thu hồi
      if (this.replyingMessage && String(this.replyingMessage.messageId) === targetId) {
        this.clearReplyingMessage()
      }
    },

    // 14. Lấy lịch sử các lần chỉnh sửa của tin nhắn
    async fetchMessageHistory(messageId) {
      if (!this.activeRoomId || !messageId) return []
      const rId = String(this.activeRoomId)
      if (['1', '2', '3'].includes(rId)) {
        return [
          {
            messageHistoryId: 'hist-demo-1',
            messageId,
            content: 'Nội dung ban đầu trước khi sửa',
            editedAt: new Date(Date.now() - 3600000).toISOString(),
          },
        ]
      }
      try {
        const list = await messageApi.getMessageHistory(this.activeRoomId, messageId)
        return Array.isArray(list) ? list : []
      } catch (err) {
        console.error('Lỗi khi tải lịch sử chỉnh sửa:', err)
        return []
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
