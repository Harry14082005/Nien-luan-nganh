import { defineStore } from 'pinia'
import * as roomApi from '@/api/room.api'
import * as messageApi from '@/api/message.api'
import * as reminderApi from '@/api/reminder.api'
import * as pollApi from '@/api/poll.api'
import * as aiSuggestionApi from '@/api/aiSuggestion.api'
import { wsService, subscribeToRoom } from '@/services/websocket'
import { useAuthStore } from './auth'

// =============================================================================
// ██████████████████  CÁC HÀM BỔ TRỢ DÙNG CHUNG (HELPERS)  ████████████████████
// =============================================================================

// -----------------------------------------------------------------------------
// [HELPER 1] DỮ LIỆU DEMO DỰ PHÒNG & GHI ĐÈ TRẠNG THÁI PHÒNG CHAT (ROOMS)
// -----------------------------------------------------------------------------

function loadDemoRooms() {
  try {
    const raw = localStorage.getItem('bluespace_demo_rooms')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) return parsed
    }
  } catch { }
  return []
}

function saveDemoRooms(rooms) {
  try {
    localStorage.setItem('bluespace_demo_rooms', JSON.stringify(rooms))
  } catch { }
}

function loadDemoMessages() {
  try {
    const raw = localStorage.getItem('bluespace_demo_messages')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') return parsed
    }
  } catch { }
  return {}
}

function saveDemoMessages(messagesMap) {
  try {
    localStorage.setItem('bluespace_demo_messages', JSON.stringify(messagesMap))
  } catch { }
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
  } catch { }
  return {}
}

function saveRoomOverrides(overrides) {
  try {
    localStorage.setItem('bluespace_room_overrides', JSON.stringify(overrides))
  } catch { }
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

    const ov = overrides?.[rId]
    const effectiveRequireApproval =
      ov && ov.requireApproval !== undefined
        ? Boolean(ov.requireApproval)
        : r.requireApproval !== undefined
          ? Boolean(r.requireApproval)
          : Boolean(existing?.requireApproval)

    let roomObj = {
      ...r,
      createdBy: r.createdBy || existing?.createdBy || null,
      requireApproval: effectiveRequireApproval,
      isOnline,
      lastSeenAt,
      hasUnread: hasUnreadVal,
      unreadCount: unreadCountVal,
    }

    if (ov && ov.lastMessageAt) {
      const ovTime = new Date(ov.lastMessageAt).getTime()
      const backendTime = r.lastMessageAt ? new Date(r.lastMessageAt).getTime() : 0
      // Nếu trạng thái ghim/bỏ ghim trên client mới hơn hoặc bằng trạng thái từ Backend -> giữ nguyên trạng thái mới!
      if (ovTime >= backendTime - 2000) {
        roomObj = {
          ...roomObj,
          lastMessageId: ov.lastMessageId || roomObj.lastMessageId,
          lastMessageContent: ov.lastMessageContent,
          lastMessageSenderId: ov.lastMessageSenderId !== undefined ? ov.lastMessageSenderId : roomObj.lastMessageSenderId,
          lastMessageSenderName: ov.lastMessageSenderName,
          lastMessageAt: ov.lastMessageAt,
        }
      }
    }

    return roomObj
  })

  merged.sort((a, b) => {
    const timeA = Math.max(
      a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0,
      a.updatedAt ? new Date(a.updatedAt).getTime() : 0
    )
    const timeB = Math.max(
      b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0,
      b.updatedAt ? new Date(b.updatedAt).getTime() : 0
    )
    return timeB - timeA
  })

  return merged
}

// -----------------------------------------------------------------------------
// [HELPER 2] GHIM TIN NHẮN, TÌM KIẾM, HỆ THỐNG & PHÂN TRANG (MESSAGES & PIN)
// -----------------------------------------------------------------------------

// Lưu danh sách sự kiện ghim / bỏ ghim trong khung chat theo từng phòng
function loadRoomEvents() {
  try {
    localStorage.removeItem('bluespace_room_events')
  } catch { }
  return {}
}

function saveRoomEvents() {
  try {
    localStorage.removeItem('bluespace_room_events')
  } catch { }
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
  } catch { }
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
  } catch { }
  return null
}

// Hàm chuẩn hóa nội dung hiển thị trích dẫn (Snippet Helper) dùng chung
export const getReplySnippet = (msg) => {
  if (!msg) return ''
  if (msg.isRevoked) return 'Tin nhắn đã bị thu hồi'

  const content = msg.content || ''
  const isVid = msg.messageType === 'VIDEO' || /\.(mp4|mov|webm|avi|mkv|m4v)$/i.test(content)
  const isImg = !isVid && (msg.messageType === 'IMAGE' || /\.(jpg|jpeg|png|gif|webp|bmp|svg)$/i.test(content))

  if (isVid) return '[Video]'
  if (isImg) return '[Hình ảnh]'
  if (msg.messageType === 'FILE' || msg.messageType === 'DOCUMENT') return '[Tệp đính kèm]'
  if (msg.messageType === 'REMINDER') return `[Lịch hẹn] ${content}`
  if (msg.messageType === 'POLL') return `[Bình chọn] ${content}`
  return content
}

// Chuyển ký tự đặc biệt tránh tấn công XSS (dùng chung cho MessageList và MessageSearchSidebar)
export const escapeHtml = (text) => {
  if (!text) return ''
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

// Đánh dấu từ khóa tìm kiếm khớp bên trong chuỗi tin nhắn (dùng chung)
export const highlightKeyword = (text, keyword, markClass = 'chat-search-mark') => {
  if (!text) return ''
  const escapedText = escapeHtml(text)
  if (!keyword || !keyword.trim()) return escapedText
  const escapedKw = escapeHtml(keyword.trim()).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedKw})`, 'gi')
  return escapedText.replace(regex, `<mark class="${markClass}">$1</mark>`)
}

// Định dạng nội dung tin nhắn hệ thống (SYSTEM) dùng chung cho ChatView và ChatListSidebar
export const formatSystemMessageText = (
  content,
  { senderUserId = null, senderName = '', actorUsername = '', currentUser = null, isPreview = false } = {},
) => {
  if (!content) return ''
  let text = String(content)
  const myUsername = currentUser?.username
  const myUserId = currentUser?.userId

  const isMe =
    (myUserId && senderUserId && String(senderUserId) === String(myUserId)) ||
    senderName === 'Bạn' ||
    actorUsername === 'Bạn' ||
    (myUsername && (senderName === myUsername || text.startsWith(myUsername)))

  if (text.includes('ghim') || text.includes('gỡ')) {
    const isUnpin = text.includes('bỏ') || text.includes('gỡ')
    if (isMe) {
      return isPreview
        ? `Bạn ${isUnpin ? 'đã bỏ ghim 1 tin nhắn' : 'đã ghim 1 tin nhắn'}`
        : `Bạn ${isUnpin ? 'đã bỏ ghim một tin nhắn' : 'đã ghim một tin nhắn'}`
    }
    if (isPreview) {
      let sender = senderName
      if (!sender || sender === 'Thành viên') {
        const firstWord = text.split(' ')[0]
        sender =
          firstWord && !firstWord.includes('đã') && !firstWord.includes('ghim')
            ? firstWord
            : 'Thành viên'
      }
      return `${sender} ${isUnpin ? 'đã bỏ ghim 1 tin nhắn' : 'đã ghim 1 tin nhắn'}`
    }
    return text
  }

  if (isMe) {
    if (
      text.includes('đã tạo một cuộc bình chọn') ||
      text.includes('đã thêm phương án') ||
      text.includes('đã thực hiện bình chọn') ||
      text.includes('đã khóa cuộc bình chọn') ||
      text.includes('đã khoá cuộc bình chọn') ||
      text.includes('đã khóa bình chọn') ||
      text.includes('đã khoá bình chọn') ||
      text.includes('đã thêm') ||
      text.includes('đã phê duyệt') ||
      text.includes('đã xóa') ||
      text.includes('đã rời') ||
      text.includes('đã bật') ||
      text.includes('đã tắt') ||
      text.includes('đã đổi tên nhóm') ||
      text.includes('đã thay đổi ảnh đại diện')
    ) {
      if (myUsername && text.startsWith(myUsername)) {
        return 'Bạn ' + text.slice(myUsername.length).trim()
      }
      return text.replace(/^.*?\s+(đã\s+)/, 'Bạn $1')
    }
  } else if (myUsername && text.includes(myUsername)) {
    text = text.replace(new RegExp(`\\b${myUsername}\\b`, 'g'), 'bạn')
  }

  return text
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

// -----------------------------------------------------------------------------
// [HELPER 3] LỊCH HẸN & NHẮC NHỞ (REMINDER HELPER)
// -----------------------------------------------------------------------------

// Chuẩn hóa thông tin ReminderResponse và tính lại myRsvp theo đúng tài khoản đang đăng nhập
function normalizeReminderForUser(reminder, currentUserId) {
  if (!reminder) return null
  const participants = Array.isArray(reminder.participants) ? reminder.participants : []
  let myRsvp = reminder.myRsvp || null
  if (currentUserId && participants.length > 0) {
    // Ham nay bo qua hoan toan gia tri event.reminder.myRsvp Backend gui, ma tu tra cuu trong participants
    const me = participants.find((p) => String(p.userId) === String(currentUserId))
    myRsvp = me ? me.response : null
  }
  return {
    ...reminder,
    participants,
    acceptedCount: participants.filter((p) => p.response === 'ACCEPTED').length,
    declinedCount: participants.filter((p) => p.response === 'DECLINED').length,
    pendingCount: participants.filter((p) => p.response === 'PENDING').length,
    myRsvp,
  }
}

/**
 * =============================================================================
 * ██████████████████████  PINIA STORE: useChatStore  ██████████████████████████
 * =============================================================================
 * Quản lý toàn bộ trạng thái (State), bộ lấy dữ liệu (Getters) và luồng xử lý (Actions):
 * - PHẦN 1: QUẢN LÝ PHÒNG CHAT, BỘ LỌC & ONLINE/OFFLINE (ROOMS & PRESENCE)
 * - PHẦN 2: LỊCH SỬ TIN NHẮN, PHÂN TRANG & GỬI TIN NHẮN (MESSAGES & PAGINATION)
 * - PHẦN 3: TRẠNG THÁI ĐANG GÕ PHÍM & THẢ CẢM XÚC (TYPING & REACTIONS)
 * - PHẦN 4: CHỈNH SỬA, THU HỒI & LỊCH SỬ CHỈNH SỬA TIN NHẮN (EDIT, REVOKE & HISTORY)
 * - PHẦN 5: GHIM TIN NHẮN & KHO FILE PHƯƠNG TIỆN (PINNED MESSAGES & ROOM MEDIA)
 * - PHẦN 6: LỊCH HẸN & NHẮC NHỞ (REMINDER MODULE)
 * - PHẦN 7: QUẢN LÝ THÀNH VIÊN & ĐÃ GỬI / ĐÃ NHẬN / ĐÃ XEM (MEMBERS & RECEIPTS)
 */
export const useChatStore = defineStore('chat', {
  // ===========================================================================
  // ████████████████████████████  KHỐI 1: STATE  ██████████████████████████████
  // ===========================================================================
  state: () => ({
    // --- [1] PHÒNG CHAT & BỘ LỌC (ROOMS & FILTERS) ---
    rooms: [], // Danh sách các phòng chat của người dùng
    activeRoomId: null, // Khóa chính UUID của phòng chat đang được chọn mở
    searchQuery: '', // Từ khóa tìm kiếm trên thanh SearchBar
    currentFilter: 'SelectAll', // Bộ lọc hiển thị: 'SelectAll' (Tất cả) hoặc 'Unread' (Chưa đọc)
    loading: false,
    error: null,
    roomActivityOverrides: loadRoomOverrides(), // Lưu trạng thái hoạt động mới nhất của từng phòng để syncRoomsSilently không ghi đè

    // --- [2] TIN NHẮN & PHÂN TRANG HAI CHIỀU (MESSAGES & PAGINATION) ---
    messages: [], // Danh sách tin nhắn thuộc phòng chat đang mở
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
    isJumpingToContext: false, // Cờ đang tải ngữ cảnh tin nhắn ở xa để không giật khung chat
    replyingMessage: null, // Thông tin tin nhắn đang được chọn để Trả lời (Reply)
    demoMessagesMap: loadDemoMessages(), // Kho lưu trữ tin nhắn mẫu dự phòng (nếu dùng các phòng mẫu '1', '2', '3')

    // --- [3] ĐANG GÕ PHÍM (TYPING INDICATOR) ---
    typingMap: {}, // Lưu trữ trạng thái gõ chữ: { [roomId]: { [userId]: { username, avatarUrl, timer } } }

    // --- [4] GHIM TIN NHẮN & FILE PHƯƠNG TIỆN (PINNED & MEDIA) ---
    pinnedMessages: [], // Danh sách tin nhắn đã ghim trong phòng đang mở (tối đa 5 tin)
    pinNotices: {}, // Thông báo ghim tin nhắn tạm thời ngoài Sidebar: { [roomId]: string }
    roomEventsMap: loadRoomEvents(), // Lưu lịch sử các sự kiện ghim/bỏ ghim trong khung chat theo từng phòng
    myRecentPinActions: {}, // Ghi nhận hành động ghim/bỏ ghim do chính tab hiện tại vừa thực hiện
    roomMedia: { images: [], videos: [], files: [] }, // Danh sách toàn bộ file phương tiện (Ảnh, Video, Tài liệu) của phòng đang mở

    // --- [5] LỊCH HẸN & NHẮC NHỞ (REMINDER) ---
    remindersByRoom: {}, // Danh sách lịch hẹn & nhắc nhở theo từng phòng: { [roomId]: ReminderResponse[] }
    loadingReminders: false,

    // --- [5.5] BÌNH CHỌN (POLL) ---
    pollsByRoom: {}, // Danh sách bình chọn theo từng phòng: { [roomId]: PollResponse[] }
    loadingPolls: false,

    // --- [6] THÀNH VIÊN & ĐÃ GỬI / ĐÃ NHẬN / ĐÃ XEM (MEMBERS & RECEIPTS) ---
    roomMembersByRoom: {}, // Danh sách thành viên theo từng phòng (kèm lastDeliveredMessageId & lastReadMessageId)
    pendingApprovalVersion: 0, // Phiên bản danh sách chờ duyệt phục vụ realtime sync
  }),

  // ===========================================================================
  // ███████████████████████████  KHỐI 2: GETTERS  █████████████████████████████
  // ===========================================================================
  getters: {
    // -------------------------------------------------------------------------
    // [GETTER - PHÒNG CHAT & BỘ LỌC (ROOMS)]
    // -------------------------------------------------------------------------

    // Phòng chat đang mở
    activeRoom: (state) => {
      return state.rooms.find((r) => String(r.roomId) === String(state.activeRoomId)) || null
    },

    // Danh sách phòng đã qua bộ lọc (Tất cả / Chưa đọc / Tìm kiếm) và luôn sắp xếp hoạt động mới nhất lên đầu
    filteredRooms: (state) => {
      let result = [...state.rooms]

      result.sort((a, b) => {
        const timeA = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0
        const timeB = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0
        return timeB - timeA
      })

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

    // Tổng số tin nhắn chưa đọc của tất cả các phòng
    totalUnreadCount: (state) => {
      return state.rooms.reduce((acc, r) => acc + (r.unreadCount || 0), 0)
    },

    // -------------------------------------------------------------------------
    // [GETTER - ĐANG GÕ PHÍM (TYPING)]
    // -------------------------------------------------------------------------

    // Đoạn text thông báo người đang soạn tin nhắn cho phòng active
    activeTypingText: (state) => {
      if (!state.activeRoomId) return ''
      const activeKey = String(state.activeRoomId).toLowerCase()
      const roomTyping = state.typingMap[activeKey] || state.typingMap[String(state.activeRoomId)]
      if (!roomTyping) return ''
      const users = Object.values(roomTyping)
      if (users.length === 0) return ''
      if (users.length === 1) return `${users[0].username} đang soạn tin nhắn...`
      if (users.length === 2) return `${users[0].username} và ${users[1].username} đang soạn tin nhắn...`
      return `${users[0].username} và ${users.length - 1} người khác đang soạn tin nhắn...`
    },

    // -------------------------------------------------------------------------
    // [GETTER - GHIM TIN NHẮN (PINNED MESSAGES)]
    // -------------------------------------------------------------------------

    // Tin nhắn đã ghim mới nhất của phòng đang mở
    // ĐOẠN CODE MỚI:
    latestPinnedMessage: (state) => {
      if (!state.pinnedMessages || state.pinnedMessages.length === 0) return null

      // 1. Quét tìm thông báo hệ thống ghim tin gần đây nhất trong khung chat
      const lastPinSystemMsg = [...state.messages]
        .reverse()
        .find(
          (m) =>
            m.messageType === 'SYSTEM' &&
            m.repliedMessage?.messageId &&
            (m.content?.includes('đã ghim') || m.content?.includes('pinned'))
        )

      if (lastPinSystemMsg?.repliedMessage?.messageId) {
        const targetId = String(lastPinSystemMsg.repliedMessage.messageId)
        const found = state.pinnedMessages.find((p) => String(p.messageId) === targetId)
        if (found) return found
      }

      // 2. Nếu không tìm thấy thông báo hệ thống, lấy phần tử ghim mới nhất
      return state.pinnedMessages[state.pinnedMessages.length - 1] || state.pinnedMessages[0]
    },

    // -------------------------------------------------------------------------
    // [GETTER - REMINDER (LỊCH HẸN & NHẮC NHỞ)]
    // -------------------------------------------------------------------------

    // Danh sách lịch hẹn của phòng đang mở (lọc lịch cá nhân của người khác nếu có)
    roomReminders: (state) => {
      if (!state.activeRoomId) return []
      const authStore = useAuthStore()
      const currentUserId = authStore.user?.userId
      // doc truc tiep state.remindersByRoom[activeRoomId]
      const list = state.remindersByRoom[String(state.activeRoomId)] || []
      return list.filter(
        (r) =>
          r.reminderType !== 'PERSONAL' ||
          !currentUserId ||
          String(r.createdById) === String(currentUserId),
      )
    },

    // Danh sách lịch hẹn sắp tới (chỉ lấy PENDING, chưa bị hủy và chưa vượt qua mốc endAt/startAt)
    upcomingRoomReminders() {
      const now = Date.now()
      return [...this.roomReminders]
        .filter((r) => {
          if (r.reminderStatus !== 'PENDING') return false
          // Tính mốc hết hạn: ưu tiên endAt, nếu không có lấy startAt
          const deadline = new Date(r.endAt || r.startAt).getTime()
          return !isNaN(deadline) && deadline > now
        })
        .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime())
    },

    // -------------------------------------------------------------------------
    // [GETTER - POLL (BÌNH CHỌN NHÓM)]
    // -------------------------------------------------------------------------
    roomPolls: (state) => {
      if (!state.activeRoomId) return []
      return state.pollsByRoom[String(state.activeRoomId)] || []
    },

    activeRoomPolls() {
      return [...this.roomPolls].filter((p) => p.isActive)
    },

    closedRoomPolls() {
      return [...this.roomPolls].filter((p) => !p.isActive)
    },

    // -------------------------------------------------------------------------
    // [GETTER - THÀNH VIÊN & PHÂN QUYỀN (MEMBERS & PERMISSIONS)]
    // -------------------------------------------------------------------------

    // Danh sách thành viên của phòng đang mở
    activeRoomMembers: (state) => {
      if (!state.activeRoomId) return []
      return state.roomMembersByRoom[String(state.activeRoomId)] || []
    },

    // Kiểm tra quyền Quản trị viên (ADMIN) hoặc Người tạo nhóm (createdBy) của người dùng hiện tại trong phòng đang mở
    canManageActiveRoom() {
      const authStore = useAuthStore()
      const myId = authStore.user?.userId
      if (!myId) return false
      if (this.activeRoom?.createdBy && String(this.activeRoom.createdBy) === String(myId)) {
        return true
      }
      const list = this.activeRoomMembers
      if (!Array.isArray(list)) return false
      const me = list.find((m) => String(m.userId) === String(myId))
      const role = me?.role || me?.roomMemberRole
      return role === 'ADMIN'
    },
  },

  // ===========================================================================
  // ███████████████████████████  KHỐI 3: ACTIONS  █████████████████████████████
  // ===========================================================================
  actions: {
    // =========================================================================
    // ██████████  PHẦN 1: QUẢN LÝ PHÒNG CHAT, BỘ LỌC & ONLINE/OFFLINE  ████████
    // =========================================================================

    // 1.0. Xóa sạch dữ liệu trong store khi đăng xuất hoặc đổi tài khoản
    resetState() {
      this.rooms = []
      this.activeRoomId = null
      this.messages = []
      this.pinnedMessages = []
      this.pinNotices = {}
      this.roomEventsMap = {}
      this.myRecentPinActions = {}
      this.roomMedia = { images: [], videos: [], files: [] }
      this.remindersByRoom = {}
      this.pollsByRoom = {}
      this.roomMembersByRoom = {}
      this.pendingApprovalVersion = 0
      this.typingMap = {}
      this.demoMessagesMap = {}
      this.searchQuery = ''
      this.currentFilter = 'SelectAll'
      this.nextCursor = null
      this.hasMoreOlder = true
      this.hasMoreNewer = false
      this.replyingMessage = null
    },

    //Dem so nguoi dang can duyet
    incrementPendingVersion() {
      this.pendingApprovalVersion = (this.pendingApprovalVersion || 0) + 1
    },

    // 1.1. Tải danh sách phòng chat từ Backend (Có fallback mẫu)
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
          this.rooms = []
          this.activeRoomId = null
          this.messages = []
        }

        // Tự động chọn phòng đầu tiên nếu có phòng và chưa có phòng nào được chọn
        const currentExists = this.rooms.some(
          (r) => String(r.roomId) === String(this.activeRoomId),
        )
        if (this.rooms.length > 0) {
          if (!this.activeRoomId || !currentExists) {
            await this.selectRoom(this.rooms[0].roomId)
          }
        } else {
          this.activeRoomId = null
          this.messages = []
        }
      } catch (err) {
        console.warn('Lỗi gọi API rooms:', err)
        this.rooms = []
        this.activeRoomId = null
        this.messages = []
      } finally {
        this.loading = false
      }
    },

    // 1.2. Đồng bộ ngầm danh sách phòng (giúp người nhận tự thấy phòng mới được tạo mà không cần F5)
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

    // 1.3. Tạo (hoặc mở) phòng chat 1-1 trực tiếp với một người dùng
    async startDirectChat(userId) {
      if (!userId) return null
      const detail = await roomApi.createDirectRoom(userId)
      await this.fetchRooms()
      if (detail?.roomId) {
        await this.selectRoom(detail.roomId)
      }
      return detail
    },

    // 1.4. Tạo nhóm chat mới (Group Chat)
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

    // 1.5. Chọn một phòng chat từ Sidebar
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

      // Tải tin nhắn, tin nhắn ghim, lịch hẹn, bình chọn, thành viên và toàn bộ file phương tiện của phòng
      if (!isSameRoom || this.messages.length === 0) {
        await Promise.all([
          this.fetchMessages(roomId),
          this.fetchPinnedMessages(roomId),
          this.fetchRoomMedia(roomId),
          this.fetchRoomReminders(roomId),
          this.fetchRoomPolls(roomId),
          this.fetchRoomMembers(roomId),
        ])
      } else {
        await Promise.all([
          this.fetchPinnedMessages(roomId),
          this.fetchRoomMedia(roomId),
          this.fetchRoomReminders(roomId),
          this.fetchRoomPolls(roomId),
          this.fetchRoomMembers(roomId),
          this.markRoomAsRead(roomId),
        ])
      }
    },

    // 1.6. Đặt bộ lọc danh sách phòng ('SelectAll' | 'Unread')
    setFilter(filterName) {
      this.currentFilter = filterName
    },

    // 1.7. Đặt từ khóa tìm kiếm phòng chat
    setSearchQuery(query) {
      this.searchQuery = query
    },

    // 1.8. Cập nhật trạng thái Online / Offline từ WebSocket
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

    // 1.9. Cập nhật Avatar người dùng thời gian thực khi có sự kiện USER_AVATAR_UPDATED
    updateUserAvatar(userId, newAvatarUrl) {
      if (!userId || !newAvatarUrl) return
      const targetId = String(userId).toLowerCase()

      // Cập nhật avatar cho các phòng trực tiếp (DIRECT) của người này trong danh sách phòng
      this.rooms = this.rooms.map((room) => {
        if (room.roomType === 'DIRECT' && room.peerId && String(room.peerId).toLowerCase() === targetId) {
          return {
            ...room,
            roomAvatarURL: newAvatarUrl,
            avatarUrl: newAvatarUrl,
            avatar: newAvatarUrl,
          }
        }
        return room
      })

      // Cập nhật avatar của sender trong danh sách tin nhắn hiện tại
      this.messages = this.messages.map((msg) => {
        if (msg.sender && String(msg.sender.userId).toLowerCase() === targetId) {
          return {
            ...msg,
            sender: {
              ...msg.sender,
              avatarUrl: newAvatarUrl,
              avatar: newAvatarUrl,
            },
          }
        }
        return msg
      })
    },

    // 1.10. Cập nhật Avatar phòng thời gian thực khi có sự kiện ROOM_AVATAR_UPDATED
    updateRoomAvatar(roomId, newAvatarUrl) {
      if (!roomId || !newAvatarUrl) return
      const targetRoomId = String(roomId).toLowerCase()

      this.rooms = this.rooms.map((room) => {
        if (String(room.roomId || room.id).toLowerCase() === targetRoomId) {
          return {
            ...room,
            roomAvatarURL: newAvatarUrl,
            avatarUrl: newAvatarUrl,
          }
        }
        return room
      })
    },

    // 1.11. Cập nhật Tên phòng thời gian thực khi có sự kiện ROOM_NAME_UPDATED
    updateRoomName(roomId, newRoomName) {
      if (!roomId || !newRoomName) return
      const targetRoomId = String(roomId).toLowerCase()

      this.rooms = this.rooms.map((room) => {
        if (String(room.roomId || room.id).toLowerCase() === targetRoomId) {
          return {
            ...room,
            roomName: newRoomName,
            name: newRoomName,
          }
        }
        return room
      })
    },

    // =========================================================================
    // ██████  PHẦN 2: LỊCH SỬ TIN NHẮN, PHÂN TRANG & GỬI / NHẬN TIN NHẮN  █████
    // =========================================================================

    // 2.1. Tải lịch sử tin nhắn của một phòng
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

    // 2.2. Tải thêm tin nhắn CŨ HƠN khi cuộn lên đỉnh (Prepends vào đầu mảng)
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

    // 2.3. Tải thêm tin nhắn MỚI HƠN khi cuộn xuống đáy (Appends vào cuối mảng)
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

    // 2.4. Tải 1 đoạn ~20 tin nhắn xung quanh tin nhắn đã ghim (nhảy tới tin nhắn ghim)
    async fetchMessagesAround(messageId) {
      if (!this.activeRoomId || !messageId) return false
      const key = String(this.activeRoomId)
      if (['1', '2', '3'].includes(key)) {
        return false
      }

      this.isJumpingToContext = true
      try {
        const res = await messageApi.getMessages(this.activeRoomId, {
          around: messageId,
          limit: 20,
        })
        const rawList = extractMessageList(res)
        const list = [...rawList].reverse()
        const cursorInfo = extractCursorInfo(res)
        this.nextCursor = cursorInfo.nextCursor
        this.hasMoreOlder = Boolean(cursorInfo.hasMore && this.nextCursor)
        this.hasMoreNewer = true
        this.hasMoreMessages = this.hasMoreOlder
        this.isViewingOlderSegment = true
        this.isDetachedFromBottom = true
        this.messages = list
        return true
      } catch (err) {
        console.error('Lỗi tải tin nhắn xung quanh tin ghim:', err)
        return false
      } finally {
        this.isJumpingToContext = false
      }
    },

    // 2.5. Lấy cửa sổ ngữ cảnh 20 tin nhắn bao quanh tin ghim (10 trước + target + 10 sau)
    async jumpToPinnedMessageContext(messageId) {
      if (!this.activeRoomId || !messageId) return false
      const key = String(this.activeRoomId)
      if (['1', '2', '3'].includes(key)) {
        return false
      }

      this.isJumpingToContext = true
      try {
        const res = await messageApi.getMessageContext(this.activeRoomId, messageId)
        const rawList = extractMessageList(res)
        // Khử trùng lặp theo messageId nếu có hai tin trùng mốc thời gian
        const seen = new Set()
        const list = (Array.isArray(rawList) ? rawList : []).filter((m) => {
          if (!m?.messageId) return false
          const id = String(m.messageId)
          if (seen.has(id)) return false
          seen.add(id)
          return true
        })
        if (list.length > 0) {
          // Bật cờ phân trang 2 chiều TRƯỚC khi gán messages để watcher không tự cuộn về đáy
          this.hasMoreOlder = true
          this.hasMoreNewer = true
          this.hasMoreMessages = true
          this.isDetachedFromBottom = true
          this.isViewingOlderSegment = true
          // nextCursor là ID của tin nhắn cũ nhất (phần tử đầu tiên trong list theo thứ tự ASC)
          this.nextCursor = list[0]?.messageId || null
          this.messages = list
          return true
        }
        return false
      } catch (err) {
        console.error('Lỗi lấy cửa sổ ngữ cảnh tin nhắn ghim:', err)
        return false
      } finally {
        this.isJumpingToContext = false
      }
    },

    // 2.6. Quản lý trạng thái Trả lời (Reply) tin nhắn
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

    // 2.7. Gửi tin nhắn văn bản mới
    async sendChatMessage(content, explicitRepliedMessageId = null) {
      if (!this.activeRoomId || !content || !content.trim()) return
      const text = content.trim()
      const authStore = useAuthStore()
      const roomKey = String(this.activeRoomId)

      const repliedMessageId = explicitRepliedMessageId || this.replyingMessage?.messageId || null
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

    // 2.8. Gửi tin nhắn dạng hình ảnh (hỗ trợ đính kèm file và reply)
    async sendImageMessage(roomId, file, repliedMessageId = null) {
      if (!roomId || !file) return
      const roomKey = String(roomId)

      // 1. Nếu là phòng mẫu dự phòng ('1', '2', '3') thì giả lập lưu local
      if (['1', '2', '3'].includes(roomKey)) {
        const mockMsg = {
          messageId: 'mock-img-' + Date.now() + Math.random(),
          roomId: roomKey,
          content: URL.createObjectURL(file), // tạm thời dùng object URL để hiển thị demo
          messageType: 'IMAGE',
          sender: {
            userId: 'me',
            username: 'Bạn',
          },
          createdAt: new Date().toISOString(),
        }
        if (!this.demoMessagesMap[roomKey]) {
          this.demoMessagesMap[roomKey] = []
        }
        this.demoMessagesMap[roomKey].push(mockMsg)
        saveDemoMessages(this.demoMessagesMap)
        this.handleNewMessage(mockMsg)
        return mockMsg
      }

      // 2. Phòng thật: Gọi API POST /rooms/{roomId}/messages/images với FormData
      try {
        const formData = new FormData()
        formData.append('file', file)
        if (repliedMessageId) {
          formData.append('repliedMessageId', repliedMessageId)
        }

        // Gọi trực tiếp qua axios hoặc qua messageApi
        // Dưới đây dùng messageApi nếu bạn đã cấu hình, hoặc axios trực tiếp:
        const sentMsg = await messageApi.sendImageMessage(roomId, formData)
        if (sentMsg) {
          this.handleNewMessage(sentMsg)
        }
        return sentMsg
      } catch (err) {
        console.error('Lỗi khi gửi ảnh:', err)
        throw err
      }
    },

    // 2.9. Gửi tin nhắn dạng tệp đính kèm (hỗ trợ đính kèm file và reply)
    async sendFileMessage(roomId, file, repliedMessageId = null) {
      if (!roomId || !file) return
      const roomKey = String(roomId)

      if (['1', '2', '3'].includes(roomKey)) {
        const mockMsg = {
          messageId: 'mock-file-' + Date.now() + Math.random(),
          roomId: roomKey,
          content: file.name,
          messageType: 'FILE',
          sender: {
            userId: 'me',
            username: 'Bạn',
          },
          createdAt: new Date().toISOString(),
        }
        if (!this.demoMessagesMap[roomKey]) {
          this.demoMessagesMap[roomKey] = []
        }
        this.demoMessagesMap[roomKey].push(mockMsg)
        saveDemoMessages(this.demoMessagesMap)
        this.handleNewMessage(mockMsg)
        return mockMsg
      }

      try {
        const formData = new FormData()
        formData.append('file', file)
        if (repliedMessageId) {
          formData.append('repliedMessageId', repliedMessageId)
        }

        const sentMsg = await messageApi.sendFileMessage(roomId, formData)
        if (sentMsg) {
          this.handleNewMessage(sentMsg)
        }
        return sentMsg
      } catch (err) {
        console.error('Lỗi khi gửi file:', err)
        throw err
      }
    },

    // 2.10. Nhận tin nhắn mới (từ API hoặc WebSocket) -> Cập nhật khung chat + Cập nhật Sidebar trái & Đưa phòng lên đầu
    handleNewMessage(message) {
      if (!message) return

      // Xử lý gói tin broadcast cập nhật avatar phòng
      if (message.type === 'ROOM_AVATAR_UPDATED' && message.roomId) {
        this.updateRoomAvatar(message.roomId, message.roomAvatarURL)
        return
      }

      // Xử lý gói tin broadcast cập nhật tên phòng
      if (message.type === 'ROOM_NAME_UPDATED' && message.roomId) {
        this.updateRoomName(message.roomId, message.roomName)
        return
      }

      // Xử lý gói tin broadcast liên quan đến phê duyệt và thành viên phòng
      if (message.type === 'ROOM_MEMBER_PENDING_ADDED' || message.type === 'ROOM_MEMBER_APPROVED' || message.type === 'ROOM_MEMBER_REMOVED') {
        this.incrementPendingVersion()
        if (message.roomId) {
          this.fetchRoomMembers(message.roomId)
        }
        const authStore = useAuthStore()
        const myUserId = authStore.user?.userId
        if (message.type === 'ROOM_MEMBER_APPROVED' && myUserId && String(message.targetUserId) === String(myUserId)) {
          this.fetchRooms()
        }
        if (message.type === 'ROOM_MEMBER_REMOVED' && myUserId && String(message.targetUserId) === String(myUserId)) {
          this.fetchRooms()
        }
        return
      }

      if (!message.roomId) return

      // Bỏ qua tin nhắn log mốc thời gian dạng SYSTEM cũ nếu còn sót lại (FE tự render mốc giờ)
      if (message.messageType === 'SYSTEM' && /^\d{1,2}:\d{2}/.test(String(message.content || '').trim())) {
        return
      }

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

      // Nếu tin nhắn thuộc phòng đang mở -> thêm vào hoặc cập nhật danh sách hiển thị
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

        // Nếu nhận được tin nhắn REMINDER mới mà chưa có dữ liệu chi tiết lịch hẹn trong store -> tải bổ sung
        if (message.messageType === 'REMINDER') {
          const roomRemList = this.remindersByRoom[msgRoomId] || []
          const hasRem = roomRemList.some(
            (r) =>
              (message.reminderId && String(r.reminderId) === String(message.reminderId)) ||
              String(r.messageId) === msgId,
          )
          if (!hasRem) {
            this.fetchRoomReminders(msgRoomId)
          }
        }

        // Nếu nhận được tin nhắn POLL mới mà chưa có dữ liệu chi tiết poll trong store -> tải bổ sung
        if (message.messageType === 'POLL') {
          const roomPollList = this.pollsByRoom[msgRoomId] || []
          const hasPoll = roomPollList.some(
            (p) =>
              (message.pollId && String(p.pollId) === String(message.pollId)) ||
              String(p.messageId) === msgId,
          )
          if (!hasPoll) {
            this.fetchRoomPolls(msgRoomId)
          }
        }
      }

      // Cập nhật thông tin phòng trên Sidebar bên trái và đưa phòng lên vị trí đầu tiên
      const index = this.rooms.findIndex((r) => String(r.roomId) === msgRoomId)
      if (index !== -1) {
        const roomToUpdate = this.rooms[index]

        let displayContent = message.content
        const isVidMsg = message.messageType === 'VIDEO' || /\.(mp4|mov|webm|avi|mkv|m4v)$/i.test(message.content || '')
        const isImgMsg = !isVidMsg && (message.messageType === 'IMAGE' || /\.(jpg|jpeg|png|gif|webp|bmp|svg)$/i.test(message.content || ''))

        if (message.isRevoked) {
          displayContent = 'Tin nhắn đã bị thu hồi'
        } else if (isPinAction) {
          displayContent = 'đã ghim 1 tin nhắn'
        } else if (isUnpinAction) {
          displayContent = 'đã bỏ ghim 1 tin nhắn'
        } else if (isVidMsg) {
          displayContent = '[Video]'
        } else if (isImgMsg) {
          displayContent = '[Hình ảnh]'
        } else if (message.messageType === 'FILE' || message.messageType === 'DOCUMENT') {
          displayContent = '[Tệp đính kèm]'
        } else if (message.messageType === 'REMINDER') {
          displayContent = message.content?.startsWith('[Lịch')
            ? message.content
            : `[Lịch hẹn] ${message.content || ''}`
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

        const senderUserId = message.sender?.userId || (isMyMsg ? myUserId : roomToUpdate.lastMessageSenderId)

        const updatedRoom = {
          ...roomToUpdate,
          lastMessageId: message.messageId,
          lastMessageContent: displayContent,
          lastMessageSenderId: senderUserId,
          lastMessageSenderName: senderDisplayName,
          lastMessageAt: activityTime,
          hasUnread: isUnreadForMe,
          unreadCount: nextUnreadCount,
        }

        // Đồng bộ trạng thái bật/tắt phê duyệt thành viên nếu có thông báo hệ thống
        if (message.messageType === 'SYSTEM') {
          const sysText = String(message.content || '')
          if (sysText.includes('đã bật phê duyệt')) {
            updatedRoom.requireApproval = true
          } else if (sysText.includes('đã tắt phê duyệt')) {
            updatedRoom.requireApproval = false
          }
          if (sysText.includes('vào nhóm') || sysText.includes('khỏi nhóm')) {
            this.fetchRoomMembers(msgRoomId)
          }
        }

        // Lưu override để syncRoomsSilently (4s/lần) giữ nguyên trạng thái mới ở đầu danh sách
        this.roomActivityOverrides[msgRoomId] = {
          ...(this.roomActivityOverrides[msgRoomId] || {}),
          lastMessageId: updatedRoom.lastMessageId,
          lastMessageContent: updatedRoom.lastMessageContent,
          lastMessageSenderId: updatedRoom.lastMessageSenderId,
          lastMessageSenderName: isPinOrUnpin ? actorUsername : updatedRoom.lastMessageSenderName,
          lastMessageAt: updatedRoom.lastMessageAt,
          hasUnread: updatedRoom.hasUnread,
          unreadCount: updatedRoom.unreadCount,
          requireApproval: updatedRoom.requireApproval,
        }
        saveRoomOverrides(this.roomActivityOverrides)

        // Tạo mảng mới với phòng vừa hoạt động nằm ở vị trí đầu tiên (index 0)
        const remainingRooms = this.rooms.filter((_, idx) => idx !== index)
        this.rooms = [updatedRoom, ...remainingRooms]

        if (['1', '2', '3'].includes(msgRoomId)) {
          saveDemoRooms(this.rooms)
        }

        // Nếu người dùng đang mở xem chính phòng này thì tự động đánh dấu đã đọc ngay lập tức
        // Nếu người dùng đang online nhưng ở phòng khác (và không phải tin do chính mình gửi) -> đánh dấu Đã nhận (DELIVERED)
        if (!message.isRevoked && message.messageId && !String(message.messageId).startsWith('mock-')) {
          if (isCurrentRoom) {
            this.markRoomAsRead(msgRoomId, message.messageId)
          } else if (!isMyMsg) {
            this.markRoomAsDelivered(msgRoomId, message.messageId)
          }
        }

        // Nếu tin nhắn thuộc phòng đang mở và là ảnh/video/file -> tự động đồng bộ realtime vào tab Media của ChatInfoSidebar
        if (isCurrentRoom && !message.isRevoked) {
          const content = message.content || ''
          const isMedia =
            ['IMAGE', 'VIDEO', 'FILE', 'DOCUMENT'].includes(message.messageType) ||
            /\.(jpg|jpeg|png|gif|webp|mp4|mov|webm|avi|mkv|m4v)$/i.test(content)
          if (isMedia) {
            this.syncRoomMediaFromCurrentMessages()
          }
        }
      } else {
        // Nếu là phòng mới chưa có trong danh sách -> tải lại danh sách phòng
        this.fetchRooms()
      }
    },

    // =========================================================================
    // ██████████  PHẦN 3: TRẠNG THÁI ĐANG GÕ PHÍM & THẢ CẢM XÚC  ██████████████
    // =========================================================================

    // 3.1. Gửi tín hiệu đang gõ phím / ngừng gõ phím qua WebSocket STOMP
    sendTypingStatus(isTyping) {
      if (!this.activeRoomId || ['1', '2', '3'].includes(String(this.activeRoomId))) return
      if (wsService.connected) {
        wsService.send('/app/chat.typing', {
          roomId: this.activeRoomId,
          isTyping: !!isTyping,
        })
      }
    },

    // 3.2. Nhận tín hiệu đang gõ phím từ thành viên khác trong phòng
    handleTypingEvent(event) {
      if (!event || !event.roomId) return
      const authStore = useAuthStore()

      // Bỏ qua nếu event xuất phát từ chính tài khoản đang đăng nhập (chỉ hiện trên giao diện đối phương)
      const isMe = Boolean(
        (event.userId &&
          authStore.user?.userId &&
          String(event.userId).toLowerCase() === String(authStore.user.userId).toLowerCase()) ||
        (event.username &&
          authStore.user?.username &&
          event.username.toLowerCase() === authStore.user.username.toLowerCase())
      )

      if (isMe) {
        return
      }

      const isTyping = event.isTyping !== undefined ? Boolean(event.isTyping) : Boolean(event.typing)
      const rId = String(event.roomId).toLowerCase()
      const uId = String(event.userId || event.username || 'unknown').toLowerCase()

      const currentRoomTyping = { ...(this.typingMap[rId] || {}) }

      if (isTyping) {
        // Xóa bộ đếm thời gian cũ nếu còn
        if (currentRoomTyping[uId]?.timer) {
          clearTimeout(currentRoomTyping[uId].timer)
        }

        // Tự động hủy trạng thái sau 2giây nếu đối phương ngừng gõ hoặc rớt mạng
        const timer = setTimeout(() => {
          const nextRoom = { ...(this.typingMap[rId] || {}) }
          delete nextRoom[uId]
          this.typingMap = {
            ...this.typingMap,
            [rId]: nextRoom,
          }
        }, 2000)

        currentRoomTyping[uId] = {
          username: event.username || 'Thành viên',
          avatarUrl: event.avatarUrl,
          timer,
        }
        this.typingMap = {
          ...this.typingMap,
          [rId]: currentRoomTyping,
        }
      } else {
        // Đối phương đã ngừng gõ hoặc đã bấm gửi tin nhắn
        if (currentRoomTyping[uId]?.timer) {
          clearTimeout(currentRoomTyping[uId].timer)
        }
        delete currentRoomTyping[uId]
        this.typingMap = {
          ...this.typingMap,
          [rId]: currentRoomTyping,
        }
      }
    },

    // 3.3. Nhận sự kiện thả/gỡ cảm xúc trên tin nhắn từ WebSocket
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

    // 3.4. Thả hoặc gỡ cảm xúc với Optimistic UI (Phản hồi 0ms)
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

    // =========================================================================
    // ██████  PHẦN 4: CHỈNH SỬA, THU HỒI & LỊCH SỬ CHỈNH SỬA TIN NHẮN  ████████
    // =========================================================================

    // 4.1. Chỉnh sửa tin nhắn
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

    // 4.2. Cập nhật đồng bộ các tin nhắn đang reply khi tin nhắn gốc được sửa
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

    // 4.3. Thu hồi tin nhắn (xóa mềm)
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
            ...revokedMsg,
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

    // 4.4. Cập nhật đồng bộ các tin nhắn đang reply khi tin nhắn gốc bị thu hồi
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

    // 4.5. Lấy lịch sử các lần chỉnh sửa của tin nhắn
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

    // =========================================================================
    // ██████████  PHẦN 5: GHIM TIN NHẮN & KHO FILE PHƯƠNG TIỆN  ███████████████
    // =========================================================================

    // 5.1. Lấy danh sách tin nhắn đã ghim trong phòng
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

    // 5.2. Ghim một tin nhắn
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
          updatedAt: nowIso,
          hasUnread: false,
          unreadCount: 0,
        }
        this.roomActivityOverrides[rId] = {
          lastMessageId: messageId,
          lastMessageContent: 'đã ghim 1 tin nhắn',
          lastMessageSenderName: senderName,
          lastMessageAt: nowIso,
          updatedAt: nowIso,
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

    // 5.3. Bỏ ghim một tin nhắn
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
          updatedAt: nowIso,
          hasUnread: false,
          unreadCount: 0,
        }
        this.roomActivityOverrides[rId] = {
          lastMessageId: messageId,
          lastMessageContent: 'đã bỏ ghim 1 tin nhắn',
          lastMessageSenderName: senderName,
          lastMessageAt: nowIso,
          updatedAt: nowIso,
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

    // 5.4. Lấy toàn bộ file phương tiện (Ảnh, Video, Tài liệu) trong phòng chat cho Sidebar Thông tin
    async fetchRoomMedia(roomId) {
      if (!roomId) {
        this.roomMedia = { images: [], videos: [], files: [] }
        return
      }

      try {
        const rawMedia = await messageApi.getRoomMedia(roomId)
        const list = Array.isArray(rawMedia) ? rawMedia : []
        const images = []
        const videos = []
        const files = []

        list.forEach((m) => {
          if (m.isRevoked) return
          const content = m.content || ''
          const isVid = m.messageType === 'VIDEO' || content.match(/\.(mp4|mov|webm|avi|mkv|m4v)$/i)
          const isImg = !isVid && (m.messageType === 'IMAGE' || content.match(/\.(jpg|jpeg|png|gif|webp|bmp|svg)$/i))

          if (isVid) {
            videos.push({
              id: m.messageId,
              url: m.content,
              name: 'Video',
              type: 'VIDEO',
              createdAt: m.createdAt,
            })
          } else if (isImg) {
            images.push({
              id: m.messageId,
              url: m.content,
              name: 'Hình ảnh',
              type: 'IMAGE',
              createdAt: m.createdAt,
            })
          } else {
            files.push({
              id: m.messageId,
              url: m.content,
              name: m.content?.split('/').pop() || 'Tài liệu',
              size: 'Đính kèm',
              type: 'FILE',
              createdAt: m.createdAt,
            })
          }
        })

        const sortByDateDesc = (a, b) => {
          const tA = a.createdAt ? new Date(a.createdAt).getTime() : 0
          const tB = b.createdAt ? new Date(b.createdAt).getTime() : 0
          return tB - tA
        }

        images.sort(sortByDateDesc)
        videos.sort(sortByDateDesc)
        files.sort(sortByDateDesc)

        this.roomMedia = { images, videos, files }
      } catch (err) {
        console.warn('Lỗi tải danh sách file phương tiện phòng:', err)
        // Fallback: quét từ messages hiện tại nếu API chưa sẵn sàng
        this.syncRoomMediaFromCurrentMessages()
      }
    },

    // 5.5. Quét bổ sung file phương tiện từ messages hiện tại (khi có tin nhắn mới gửi đến)
    syncRoomMediaFromCurrentMessages() {
      const images = [...(this.roomMedia?.images || [])]
      const videos = [...(this.roomMedia?.videos || [])]
      const files = [...(this.roomMedia?.files || [])]
      const seenIds = new Set([
        ...images.map((i) => String(i.id)),
        ...videos.map((v) => String(v.id)),
        ...files.map((f) => String(f.id)),
      ])

        ; (this.messages || []).forEach((m) => {
          if (m.isRevoked || seenIds.has(String(m.messageId))) return
          const content = m.content || ''
          const isVid = m.messageType === 'VIDEO' || content.match(/\.(mp4|mov|webm|avi|mkv|m4v)$/i)
          const isImg = !isVid && (m.messageType === 'IMAGE' || content.match(/\.(jpg|jpeg|png|gif|webp|bmp|svg)$/i))

          if (isVid) {
            videos.unshift({
              id: m.messageId,
              url: m.content,
              name: 'Video',
              type: 'VIDEO',
              createdAt: m.createdAt,
            })
            seenIds.add(String(m.messageId))
          } else if (isImg) {
            images.unshift({
              id: m.messageId,
              url: m.content,
              name: 'Hình ảnh',
              type: 'IMAGE',
              createdAt: m.createdAt,
            })
            seenIds.add(String(m.messageId))
          } else if (m.messageType === 'FILE' || m.messageType === 'DOCUMENT') {
            files.unshift({
              id: m.messageId,
              url: m.content,
              name: m.content?.split('/').pop() || 'Tài liệu',
              size: 'Đính kèm',
              type: 'FILE',
              createdAt: m.createdAt,
            })
            seenIds.add(String(m.messageId))
          }
        })

      const sortByDateDesc = (a, b) => {
        const tA = a.createdAt ? new Date(a.createdAt).getTime() : 0
        const tB = b.createdAt ? new Date(b.createdAt).getTime() : 0
        return tB - tA
      }
      images.sort(sortByDateDesc)
      videos.sort(sortByDateDesc)
      files.sort(sortByDateDesc)

      this.roomMedia = { images, videos, files }
    },

    // =========================================================================
    // ██████████████  PHẦN 6: REMINDER (LỊCH HẸN & NHẮC NHỞ)  █████████████████
    // =========================================================================

    // 6.1. Tìm thông tin chi tiết Reminder ứng với một tin nhắn REMINDER trên dòng chat
    getReminderForMessage(message) {
      if (!message) return null
      const rId = String(message.roomId || this.activeRoomId || '')
      const list = this.remindersByRoom[rId] || []
      return (
        list.find(
          (r) =>
            (message.reminderId && String(r.reminderId) === String(message.reminderId)) ||
            (message.messageId && String(r.messageId) === String(message.messageId)),
        ) || null
      )
    },

    // 6.2. Tải danh sách lịch hẹn của phòng chat
    async fetchRoomReminders(roomId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId) return []
      const rKey = String(targetRoomId)
      if (['1', '2', '3'].includes(rKey)) {
        if (!this.remindersByRoom[rKey]) {
          this.remindersByRoom = { ...this.remindersByRoom, [rKey]: [] }
        }
        return this.remindersByRoom[rKey]
      }

      this.loadingReminders = true
      try {
        const authStore = useAuthStore()
        const currentUserId = authStore.user?.userId
        const res = await reminderApi.getRoomReminders(targetRoomId)
        const rawList = Array.isArray(res) ? res : Array.isArray(res?.result) ? res.result : []
        const normalized = rawList
          .map((item) => normalizeReminderForUser(item, currentUserId))
          .filter(Boolean)

        this.remindersByRoom = {
          ...this.remindersByRoom,
          [rKey]: normalized,
        }
        return normalized
      } catch (err) {
        console.warn('Lỗi tải danh sách lịch hẹn phòng chat:', err)
        return this.remindersByRoom[rKey] || []
      } finally {
        this.loadingReminders = false
      }
    },

    // 6.3. Tạo mới lịch hẹn / nhắc nhở trong phòng chat
    async createRoomReminder(roomId, payload) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId) return null
      const rKey = String(targetRoomId)
      const authStore = useAuthStore()
      const currentUserId = authStore.user?.userId

      const created = await reminderApi.createReminder(targetRoomId, payload)
      if (created) {
        const normalized = normalizeReminderForUser(created, currentUserId)
        const existing = this.remindersByRoom[rKey] || []
        const filtered = existing.filter((r) => String(r.reminderId) !== String(normalized.reminderId))
        this.remindersByRoom = {
          ...this.remindersByRoom,
          [rKey]: [normalized, ...filtered],
        }
        return normalized
      }
      return null
    },

    // 6.3b. Bóc tách thông tin lịch hẹn bằng AI từ tin nhắn
    async suggestReminderWithAi(roomId, messageId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !messageId) return null
      try {
        const res = await reminderApi.suggestReminderWithAi(targetRoomId, messageId)
        return res
      } catch (err) {
        console.error('Lỗi gọi AI trích xuất lịch hẹn:', err)
        throw err
      }
    },

    // 6.4. Phản hồi Tham gia (ACCEPTED) hoặc Từ chối (DECLINED) với Optimistic UI
    async respondReminderRsvp(roomId, reminderId, responseStatus) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !reminderId || !responseStatus) return null
      const rKey = String(targetRoomId)
      const authStore = useAuthStore()
      const currentUserId = authStore.user?.userId
      const currentUsername = authStore.user?.username || 'Bạn'
      const currentAvatar = authStore.user?.avatarUrl || null

      const list = [...(this.remindersByRoom[rKey] || [])]
      const idx = list.findIndex((r) => String(r.reminderId) === String(reminderId))
      const snapshot = idx !== -1 ? JSON.parse(JSON.stringify(list[idx])) : null

      // Optimistic UI Update (0ms)
      if (idx !== -1 && currentUserId) {
        const item = { ...list[idx] }
        const participants = Array.isArray(item.participants) ? [...item.participants] : []
        const pIdx = participants.findIndex((p) => String(p.userId) === String(currentUserId))
        if (pIdx !== -1) {
          participants[pIdx] = { ...participants[pIdx], response: responseStatus }
        } else {
          participants.push({
            userId: currentUserId,
            username: currentUsername,
            avatarUrl: currentAvatar,
            response: responseStatus,
          })
        }
        item.participants = participants
        list[idx] = normalizeReminderForUser(item, currentUserId)
        this.remindersByRoom = { ...this.remindersByRoom, [rKey]: list }
      }

      try {
        const updated = await reminderApi.respondRsvp(targetRoomId, reminderId, responseStatus)
        if (updated) {
          const normalized = normalizeReminderForUser(updated, currentUserId)
          const latestList = [...(this.remindersByRoom[rKey] || [])]
          const targetIdx = latestList.findIndex((r) => String(r.reminderId) === String(reminderId))
          if (targetIdx !== -1) {
            latestList[targetIdx] = normalized
          } else {
            latestList.unshift(normalized)
          }
          this.remindersByRoom = { ...this.remindersByRoom, [rKey]: latestList }
          return normalized
        }
      } catch (err) {
        // Hoàn tác nếu lỗi
        if (snapshot && idx !== -1) {
          const revertList = [...(this.remindersByRoom[rKey] || [])]
          revertList[idx] = snapshot
          this.remindersByRoom = { ...this.remindersByRoom, [rKey]: revertList }
        }
        throw err
      }
      return null
    },

    // 6.5. Hủy lịch hẹn
    async cancelRoomReminder(roomId, reminderId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !reminderId) return false
      const rKey = String(targetRoomId)

      await reminderApi.cancelReminder(targetRoomId, reminderId)

      const list = [...(this.remindersByRoom[rKey] || [])]
      const idx = list.findIndex((r) => String(r.reminderId) === String(reminderId))
      if (idx !== -1) {
        list[idx] = {
          ...list[idx],
          reminderStatus: 'CANCELLED',
        }
        this.remindersByRoom = { ...this.remindersByRoom, [rKey]: list }
      }
      return true
    },

    // 6.6. Xử lý sự kiện Realtime từ WebSocket doi voi REMINDER(/topic/room.{roomId}.reminder)
    handleReminderEvent(event) {
      if (!event || !event.roomId || !event.reminder) return
      const rKey = String(event.roomId)
      const authStore = useAuthStore()
      const currentUserId = authStore.user?.userId

      const normalized = normalizeReminderForUser(event.reminder, currentUserId)
      if (!normalized) return

      const currentList = [...(this.remindersByRoom[rKey] || [])]
      // Neu nguoi reminderId ton tai (user vua nhan ket qua POST tu http, ngay sau do nhan them websocket || co nguoi vua bam RSVP) -> ghi de va update thong tin moi
      const existingIdx = currentList.findIndex(
        (r) => String(r.reminderId) === String(normalized.reminderId),
      )

      if (existingIdx !== -1) {
        currentList[existingIdx] = {
          ...currentList[existingIdx],
          ...normalized,
        }
        // neu chua ton tai, chen len dau danh sach
      } else {
        currentList.unshift(normalized)
      }
      //Cap nhat State bat bien, tao tham chieu object moi cho reminderByRoom de dam bao hethong Reactivity cua Vue luon nhan dien duoc thay doi va render lai giao dien ngay lap tuc
      this.remindersByRoom = { ...this.remindersByRoom, [rKey]: currentList }
    },

    // -------------------------------------------------------------------------
    // Lấy thông tin chi tiết của Poll tương ứng với một tin nhắn type = POLL
    // -------------------------------------------------------------------------
    getPollForMessage(msg) {
      if (!msg) return null
      const rKey = String(msg.roomId || this.activeRoomId || '')
      const list = this.pollsByRoom[rKey] || []

      // 1. Khớp theo pollId nếu message có lưu
      if (msg.pollId) {
        const found = list.find((p) => String(p.pollId) === String(msg.pollId))
        if (found) return found
      }

      // 2. Khớp theo messageId
      if (msg.messageId) {
        const found = list.find((p) => String(p.messageId) === String(msg.messageId))
        if (found) return found
      }

      // 3. Khớp theo nội dung câu hỏi
      const found = list.find((p) => p.question === msg.content)
      if (found) return found

      return null
    },

    // =========================================================================
    // ███████  PHẦN 6.5: QUẢN LÝ BÌNH CHỌN (POLL TRONG NHÓM CHAT)  ████████████
    // =========================================================================

    // Tải danh sách bình chọn của phòng chat
    async fetchRoomPolls(roomId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId) return []
      const rKey = String(targetRoomId)

      try {
        this.loadingPolls = true
        const polls = await pollApi.getRoomPolls(targetRoomId)
        const authStore = useAuthStore()
        const myUserId = authStore.user?.userId

        // Chuẩn hóa cờ votedByMe của từng option dựa theo userId của chính người dùng hiện tại
        const normalized = (polls || []).map((poll) => ({
          ...poll,
          options: (poll.options || []).map((opt) => {
            let isVotedByMe = false
            if (opt.voters && Array.isArray(opt.voters) && myUserId) {
              isVotedByMe = opt.voters.some((v) => String(v.userId) === String(myUserId))
            } else if (opt.votedByMe !== undefined && !opt.voters) {
              isVotedByMe = Boolean(opt.votedByMe)
            }
            return {
              ...opt,
              votedByMe: isVotedByMe,
            }
          }),
        }))

        this.pollsByRoom = {
          ...this.pollsByRoom,
          [rKey]: normalized,
        }
        return normalized
      } catch (err) {
        console.error(`Lỗi tải danh sách bình chọn phòng ${targetRoomId}:`, err)
        return []
      } finally {
        this.loadingPolls = false
      }
    },

    // Tạo bình chọn mới trong phòng chat
    async createRoomPoll(roomId, pollData) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId) return null
      const created = await pollApi.createPoll(targetRoomId, pollData)
      if (created) {
        const rKey = String(targetRoomId)
        const currentList = this.pollsByRoom[rKey] || []
        const filtered = currentList.filter((p) => String(p.pollId) !== String(created.pollId))
        this.pollsByRoom = {
          ...this.pollsByRoom,
          [rKey]: [created, ...filtered],
        }
        return created
      }
      return null
    },

    // Bỏ phiếu / Đổi phiếu bầu (Optimistic UI)
    async voteRoomPoll(roomId, pollId, optionIds) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId) return null
      const rKey = String(targetRoomId)
      const list = [...(this.pollsByRoom[rKey] || [])]
      const idx = list.findIndex((p) => String(p.pollId) === String(pollId))
      const snapshot = idx !== -1 ? JSON.parse(JSON.stringify(list[idx])) : null

      // Optimistic UI update
      if (idx !== -1) {
        const currentPoll = { ...list[idx] }
        const wantedSet = new Set(optionIds.map(String))
        let diffUserCount = 0
        const hadAnyVoteBefore = currentPoll.options.some((o) => o.votedByMe)
        const willHaveVote = wantedSet.size > 0

        currentPoll.options = currentPoll.options.map((opt) => {
          const isWanted = wantedSet.has(String(opt.pollOptionId))
          const wasVoted = Boolean(opt.votedByMe)
          let newVoteCount = opt.voteCount || 0
          if (isWanted && !wasVoted) newVoteCount++
          if (!isWanted && wasVoted) newVoteCount = Math.max(0, newVoteCount - 1)
          return {
            ...opt,
            votedByMe: isWanted,
            voteCount: newVoteCount,
          }
        })

        if (!hadAnyVoteBefore && willHaveVote) diffUserCount = 1
        if (hadAnyVoteBefore && !willHaveVote) diffUserCount = -1
        currentPoll.totalVoters = Math.max(0, (currentPoll.totalVoters || 0) + diffUserCount)

        list[idx] = currentPoll
        this.pollsByRoom = { ...this.pollsByRoom, [rKey]: list }
      }

      try {
        const updated = await pollApi.votePoll(targetRoomId, pollId, optionIds)
        if (updated) {
          this.handlePollEvent({ type: 'POLL_VOTED', roomId: targetRoomId, poll: updated })
        }
        return updated
      } catch (err) {
        if (snapshot && idx !== -1) {
          const revertList = [...(this.pollsByRoom[rKey] || [])]
          revertList[idx] = snapshot
          this.pollsByRoom = { ...this.pollsByRoom, [rKey]: revertList }
        }
        throw err
      }
    },

    // Thêm lựa chọn mới vào Poll
    async addOptionToRoomPoll(roomId, pollId, optionText) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId || !optionText) return null
      const updated = await pollApi.addPollOption(targetRoomId, pollId, optionText)
      if (updated) {
        this.handlePollEvent({ type: 'POLL_OPTION_ADDED', roomId: targetRoomId, poll: updated })
      }
      return updated
    },

    // Khóa / Kết thúc bình chọn
    async closeRoomPoll(roomId, pollId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId) return false
      const updated = await pollApi.closePoll(targetRoomId, pollId)
      if (updated) {
        this.handlePollEvent({ type: 'POLL_CLOSED', roomId: targetRoomId, poll: updated })
      }
      return true
    },

    // Xử lý sự kiện Realtime từ WebSocket đối với POLL (/topic/room.{roomId}.poll)
    handlePollEvent(event) {
      if (!event || !event.roomId || !event.poll) return
      const rKey = String(event.roomId)
      const rawPoll = event.poll
      const authStore = useAuthStore()
      const myUserId = authStore.user?.userId

      // Chuẩn hóa cờ votedByMe của từng option dựa theo userId của chính người dùng hiện tại
      const normalizedOptions = (rawPoll.options || []).map((opt) => {
        let isVotedByMe = false
        if (opt.voters && Array.isArray(opt.voters) && myUserId) {
          isVotedByMe = opt.voters.some((v) => String(v.userId) === String(myUserId))
        } else if (opt.votedByMe !== undefined && !opt.voters) {
          isVotedByMe = Boolean(opt.votedByMe)
        }
        return {
          ...opt,
          votedByMe: isVotedByMe,
        }
      })

      const poll = {
        ...rawPoll,
        options: normalizedOptions,
      }

      const currentList = [...(this.pollsByRoom[rKey] || [])]
      const existingIdx = currentList.findIndex((p) => String(p.pollId) === String(poll.pollId))

      if (existingIdx !== -1) {
        currentList[existingIdx] = {
          ...currentList[existingIdx],
          ...poll,
        }
      } else {
        currentList.unshift(poll)
      }
      this.pollsByRoom = { ...this.pollsByRoom, [rKey]: currentList }
    },

    // =========================================================================
    // ██████████████  PHẦN 6.5: POLL (BÌNH CHỌN)  █████████████████████████████
    // =========================================================================

    // Tìm thông tin chi tiết Poll ứng với một tin nhắn POLL trên dòng chat
    getPollForMessage(message) {
      if (!message) return null
      const rId = String(message.roomId || this.activeRoomId || '')
      const list = this.pollsByRoom[rId] || []
      return (
        list.find(
          (p) =>
            (message.pollId && String(p.pollId) === String(message.pollId)) ||
            (message.messageId && String(p.messageId) === String(message.messageId)) ||
            (message.content && p.question && message.content.trim() === p.question.trim()),
        ) || null
      )
    },

    // Tải danh sách bình chọn của phòng chat
    async fetchRoomPolls(roomId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId) return []
      const rKey = String(targetRoomId)
      if (['1', '2', '3'].includes(rKey)) {
        if (!this.pollsByRoom[rKey]) {
          this.pollsByRoom = { ...this.pollsByRoom, [rKey]: [] }
        }
        return this.pollsByRoom[rKey]
      }

      this.loadingPolls = true
      try {
        const rawList = await pollApi.getRoomPolls(targetRoomId)
        const authStore = useAuthStore()
        const myUserId = authStore.user?.userId
        const list = (Array.isArray(rawList) ? rawList : []).map((p) => {
          return {
            ...p,
            options: (p.options || []).map((opt) => ({
              ...opt,
              votedByMe:
                opt.voters && myUserId
                  ? opt.voters.some((v) => String(v.userId) === String(myUserId))
                  : Boolean(opt.votedByMe),
            })),
          }
        })
        this.pollsByRoom = {
          ...this.pollsByRoom,
          [rKey]: list,
        }
        return list
      } catch (err) {
        console.warn('Lỗi tải danh sách bình chọn phòng chat:', err)
        return this.pollsByRoom[rKey] || []
      } finally {
        this.loadingPolls = false
      }
    },

    // Tạo mới bình chọn
    async createRoomPoll(roomId, payload) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId) return null
      const rKey = String(targetRoomId)
      const created = await pollApi.createPoll(targetRoomId, payload)
      if (created) {
        const existing = this.pollsByRoom[rKey] || []
        const filtered = existing.filter((p) => String(p.pollId) !== String(created.pollId))
        this.pollsByRoom = {
          ...this.pollsByRoom,
          [rKey]: [created, ...filtered],
        }
        return created
      }
      return null
    },

    // Bỏ phiếu / Đổi phiếu bầu (Optimistic UI)
    async voteRoomPoll(roomId, pollId, optionIds) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId) return null
      const rKey = String(targetRoomId)
      const list = [...(this.pollsByRoom[rKey] || [])]
      const idx = list.findIndex((p) => String(p.pollId) === String(pollId))
      const snapshot = idx !== -1 ? JSON.parse(JSON.stringify(list[idx])) : null

      // Optimistic UI update
      if (idx !== -1) {
        const currentPoll = { ...list[idx] }
        const wantedSet = new Set(optionIds.map(String))
        let diffUserCount = 0
        const hadAnyVoteBefore = currentPoll.options.some((o) => o.votedByMe)
        const willHaveVote = wantedSet.size > 0

        currentPoll.options = currentPoll.options.map((opt) => {
          const isWanted = wantedSet.has(String(opt.pollOptionId))
          const wasVoted = Boolean(opt.votedByMe)
          let newVoteCount = opt.voteCount || 0
          if (isWanted && !wasVoted) newVoteCount++
          if (!isWanted && wasVoted) newVoteCount = Math.max(0, newVoteCount - 1)
          return {
            ...opt,
            votedByMe: isWanted,
            voteCount: newVoteCount,
          }
        })

        if (!hadAnyVoteBefore && willHaveVote) diffUserCount = 1
        if (hadAnyVoteBefore && !willHaveVote) diffUserCount = -1
        currentPoll.totalVoters = Math.max(0, (currentPoll.totalVoters || 0) + diffUserCount)

        list[idx] = currentPoll
        this.pollsByRoom = { ...this.pollsByRoom, [rKey]: list }
      }

      try {
        const updated = await pollApi.votePoll(targetRoomId, pollId, optionIds)
        if (updated) {
          this.handlePollEvent({ type: 'POLL_VOTED', roomId: targetRoomId, poll: updated })
        }
        return updated
      } catch (err) {
        if (snapshot && idx !== -1) {
          const revertList = [...(this.pollsByRoom[rKey] || [])]
          revertList[idx] = snapshot
          this.pollsByRoom = { ...this.pollsByRoom, [rKey]: revertList }
        }
        throw err
      }
    },

    // Thêm lựa chọn mới vào Poll
    async addOptionToRoomPoll(roomId, pollId, optionText) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId || !optionText) return null
      const updated = await pollApi.addPollOption(targetRoomId, pollId, optionText)
      if (updated) {
        this.handlePollEvent({ type: 'POLL_OPTION_ADDED', roomId: targetRoomId, poll: updated })
      }
      return updated
    },

    // Khóa / Kết thúc bình chọn
    async closeRoomPoll(roomId, pollId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId) return false
      const updated = await pollApi.closePoll(targetRoomId, pollId)
      if (updated) {
        this.handlePollEvent({ type: 'POLL_CLOSED', roomId: targetRoomId, poll: updated })
      }
      return true
    },

    // Xử lý sự kiện Realtime từ WebSocket đối với POLL (/topic/room.{roomId}.poll)
    handlePollEvent(event) {
      if (!event || !event.roomId || !event.poll) return
      const rKey = String(event.roomId)
      const rawPoll = event.poll
      const authStore = useAuthStore()
      const myUserId = authStore.user?.userId

      // Chuẩn hóa cờ votedByMe của từng option dựa theo userId của chính người dùng hiện tại
      const normalizedOptions = (rawPoll.options || []).map((opt) => {
        let isVotedByMe = false
        if (opt.voters && Array.isArray(opt.voters) && myUserId) {
          isVotedByMe = opt.voters.some((v) => String(v.userId) === String(myUserId))
        } else if (opt.votedByMe !== undefined && !opt.voters) {
          isVotedByMe = Boolean(opt.votedByMe)
        }
        return {
          ...opt,
          votedByMe: isVotedByMe,
        }
      })

      const poll = {
        ...rawPoll,
        options: normalizedOptions,
      }

      const currentList = [...(this.pollsByRoom[rKey] || [])]
      const existingIdx = currentList.findIndex((p) => String(p.pollId) === String(poll.pollId))

      if (existingIdx !== -1) {
        currentList[existingIdx] = {
          ...currentList[existingIdx],
          ...poll,
        }
      } else {
        currentList.unshift(poll)
      }
      this.pollsByRoom = { ...this.pollsByRoom, [rKey]: currentList }
    },

    // =========================================================================
    // ██████████████  PHẦN 6.5: POLL (BÌNH CHỌN)  █████████████████████████████
    // =========================================================================

    // Tìm thông tin chi tiết Poll ứng với một tin nhắn POLL trên dòng chat
    getPollForMessage(message) {
      if (!message) return null
      const rId = String(message.roomId || this.activeRoomId || '')
      const list = this.pollsByRoom[rId] || []
      return (
        list.find(
          (p) =>
            (message.pollId && String(p.pollId) === String(message.pollId)) ||
            (message.messageId && String(p.messageId) === String(message.messageId)) ||
            (message.content && p.question && message.content.trim() === p.question.trim()),
        ) || null
      )
    },

    // Tải danh sách bình chọn của phòng chat
    async fetchRoomPolls(roomId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId) return []
      const rKey = String(targetRoomId)
      if (['1', '2', '3'].includes(rKey)) {
        if (!this.pollsByRoom[rKey]) {
          this.pollsByRoom = { ...this.pollsByRoom, [rKey]: [] }
        }
        return this.pollsByRoom[rKey]
      }

      this.loadingPolls = true
      try {
        const rawList = await pollApi.getRoomPolls(targetRoomId)
        const authStore = useAuthStore()
        const myUserId = authStore.user?.userId
        const list = (Array.isArray(rawList) ? rawList : []).map((p) => {
          return {
            ...p,
            options: (p.options || []).map((opt) => ({
              ...opt,
              votedByMe:
                opt.voters && myUserId
                  ? opt.voters.some((v) => String(v.userId) === String(myUserId))
                  : Boolean(opt.votedByMe),
            })),
          }
        })
        this.pollsByRoom = {
          ...this.pollsByRoom,
          [rKey]: list,
        }
        return list
      } catch (err) {
        console.warn('Lỗi tải danh sách bình chọn phòng chat:', err)
        return this.pollsByRoom[rKey] || []
      } finally {
        this.loadingPolls = false
      }
    },

    // Tạo mới bình chọn
    async createRoomPoll(roomId, payload) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId) return null
      const rKey = String(targetRoomId)
      const created = await pollApi.createPoll(targetRoomId, payload)
      if (created) {
        const existing = this.pollsByRoom[rKey] || []
        const filtered = existing.filter((p) => String(p.pollId) !== String(created.pollId))
        this.pollsByRoom = {
          ...this.pollsByRoom,
          [rKey]: [created, ...filtered],
        }
        return created
      }
      return null
    },

    // Bỏ phiếu / Đổi phiếu bầu (Optimistic UI)
    async voteRoomPoll(roomId, pollId, optionIds) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId) return null
      const rKey = String(targetRoomId)
      const list = [...(this.pollsByRoom[rKey] || [])]
      const idx = list.findIndex((p) => String(p.pollId) === String(pollId))
      const snapshot = idx !== -1 ? JSON.parse(JSON.stringify(list[idx])) : null

      // Optimistic UI update
      if (idx !== -1) {
        const currentPoll = { ...list[idx] }
        const wantedSet = new Set(optionIds.map(String))
        let diffUserCount = 0
        const hadAnyVoteBefore = currentPoll.options.some((o) => o.votedByMe)
        const willHaveVote = wantedSet.size > 0

        currentPoll.options = currentPoll.options.map((opt) => {
          const isWanted = wantedSet.has(String(opt.pollOptionId))
          const wasVoted = Boolean(opt.votedByMe)
          let newVoteCount = opt.voteCount || 0
          if (isWanted && !wasVoted) newVoteCount++
          if (!isWanted && wasVoted) newVoteCount = Math.max(0, newVoteCount - 1)
          return {
            ...opt,
            votedByMe: isWanted,
            voteCount: newVoteCount,
          }
        })

        if (!hadAnyVoteBefore && willHaveVote) diffUserCount = 1
        if (hadAnyVoteBefore && !willHaveVote) diffUserCount = -1
        currentPoll.totalVoters = Math.max(0, (currentPoll.totalVoters || 0) + diffUserCount)

        list[idx] = currentPoll
        this.pollsByRoom = { ...this.pollsByRoom, [rKey]: list }
      }

      try {
        const updated = await pollApi.votePoll(targetRoomId, pollId, optionIds)
        if (updated) {
          this.handlePollEvent({ type: 'POLL_VOTED', roomId: targetRoomId, poll: updated })
        }
        return updated
      } catch (err) {
        if (snapshot && idx !== -1) {
          const revertList = [...(this.pollsByRoom[rKey] || [])]
          revertList[idx] = snapshot
          this.pollsByRoom = { ...this.pollsByRoom, [rKey]: revertList }
        }
        throw err
      }
    },

    // Thêm lựa chọn mới vào Poll
    async addOptionToRoomPoll(roomId, pollId, optionText) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId || !optionText) return null
      const updated = await pollApi.addPollOption(targetRoomId, pollId, optionText)
      if (updated) {
        this.handlePollEvent({ type: 'POLL_OPTION_ADDED', roomId: targetRoomId, poll: updated })
      }
      return updated
    },

    // Khóa / Kết thúc bình chọn
    async closeRoomPoll(roomId, pollId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !pollId) return false
      const updated = await pollApi.closePoll(targetRoomId, pollId)
      if (updated) {
        this.handlePollEvent({ type: 'POLL_CLOSED', roomId: targetRoomId, poll: updated })
      }
      return true
    },

    // Xử lý sự kiện Realtime từ WebSocket đối với POLL (/topic/room.{roomId}.poll)
    handlePollEvent(event) {
      if (!event || !event.roomId || !event.poll) return
      const rKey = String(event.roomId)
      const rawPoll = event.poll
      const authStore = useAuthStore()
      const myUserId = authStore.user?.userId

      // Chuẩn hóa cờ votedByMe của từng option dựa theo userId của chính người dùng hiện tại
      const normalizedOptions = (rawPoll.options || []).map((opt) => {
        let isVotedByMe = false
        if (opt.voters && Array.isArray(opt.voters) && myUserId) {
          isVotedByMe = opt.voters.some((v) => String(v.userId) === String(myUserId))
        } else if (opt.votedByMe !== undefined && !opt.voters) {
          isVotedByMe = Boolean(opt.votedByMe)
        }
        return {
          ...opt,
          votedByMe: isVotedByMe,
        }
      })

      const poll = {
        ...rawPoll,
        options: normalizedOptions,
      }

      const currentList = [...(this.pollsByRoom[rKey] || [])]
      const existingIdx = currentList.findIndex((p) => String(p.pollId) === String(poll.pollId))

      if (existingIdx !== -1) {
        currentList[existingIdx] = {
          ...currentList[existingIdx],
          ...poll,
        }
      } else {
        currentList.unshift(poll)
      }
      this.pollsByRoom = { ...this.pollsByRoom, [rKey]: currentList }
    },

    // =========================================================================
    // ████  PHẦN 7: QUẢN LÝ THÀNH VIÊN & ĐÃ GỬI / ĐÃ NHẬN / ĐÃ XEM (RECEIPTS) ██
    // =========================================================================

    // 7.1. Tải danh sách thành viên ACTIVE của phòng (kèm mốc lastDeliveredMessageId & lastReadMessageId)
    async fetchRoomMembers(roomId) {
      if (!roomId) return []
      const rKey = String(roomId)
      // if (['1', '2', '3'].includes(rKey)) return []
      try {
        const list = await roomApi.getRoomMembers(roomId, 'ACTIVE')
        const members = Array.isArray(list) ? list : []
        this.roomMembersByRoom = {
          ...this.roomMembersByRoom,
          [rKey]: members,
        }
        return members
      } catch (err) {
        console.warn('Lỗi tải danh sách thành viên phòng:', err)
        return this.roomMembersByRoom[rKey] || []
      }
    },

    // 7.2. Đánh dấu đã đọc phòng chat (Read Watermark - cập nhật xuống MySQL và Pinia)
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

    // 7.3. Đánh dấu đã nhận tin nhắn (Delivered Watermark)
    async markRoomAsDelivered(roomId, messageId = null) {
      if (!roomId) return
      const rKey = String(roomId)
      if (['1', '2', '3'].includes(rKey)) return
      try {
        await messageApi.markAsDelivered(roomId, messageId)
      } catch {
        // Bỏ qua lỗi mạng ngầm
      }
    },

    // 7.4. Nhận sự kiện Realtime Đã nhận / Đã xem từ WebSocket (/topic/room.{roomId}.receipts)
    handleReceiptEvent(event) {
      if (!event || !event.roomId || !event.userId || !event.messageId) return
      const rKey = String(event.roomId)
      const currentMembers = [...(this.roomMembersByRoom[rKey] || [])]
      const idx = currentMembers.findIndex((m) => String(m.userId) === String(event.userId))

      const isRead = event.receiptType === 'READ'
      if (idx !== -1) {
        const updated = { ...currentMembers[idx] }
        if (event.username) updated.username = event.username
        if (event.avatarUrl) updated.avatarUrl = event.avatarUrl
        updated.lastDeliveredMessageId = event.messageId
        if (isRead) {
          updated.lastReadMessageId = event.messageId
        }
        currentMembers[idx] = updated
      } else {
        currentMembers.push({
          roomId: event.roomId,
          userId: event.userId,
          username: event.username || 'Thành viên',
          avatarUrl: event.avatarUrl || '',
          lastDeliveredMessageId: event.messageId,
          lastReadMessageId: isRead ? event.messageId : null,
        })
      }

      this.roomMembersByRoom = {
        ...this.roomMembersByRoom,
        [rKey]: currentMembers,
      }
    },

    // 7.5. Rời khỏi nhóm chat hiện tại
    async leaveGroupRoom(roomId, newAdminId = null) {
      if (!roomId) return
      const rKey = String(roomId)
      await roomApi.leaveRoom(roomId, newAdminId)

      // Xoá phòng khỏi danh sách hiển thị
      this.rooms = this.rooms.filter((r) => String(r.roomId) !== rKey)
      delete this.roomActivityOverrides[rKey]
      saveRoomOverrides(this.roomActivityOverrides)

      if (String(this.activeRoomId) === rKey) {
        this.activeRoomId = null
        this.messages = []
        if (this.rooms.length > 0) {
          await this.selectRoom(this.rooms[0].roomId)
        }
      }
    },

    // 7.6. Cập nhật cờ requireApproval trên store của phòng
    updateRoomRequireApproval(roomId, requireApproval) {
      if (!roomId) return
      const rKey = String(roomId)
      const boolVal = Boolean(requireApproval)
      this.rooms = this.rooms.map((r) =>
        String(r.roomId) === rKey ? { ...r, requireApproval: boolVal } : r,
      )
      this.roomActivityOverrides[rKey] = {
        ...(this.roomActivityOverrides[rKey] || {}),
        requireApproval: boolVal,
      }
      saveRoomOverrides(this.roomActivityOverrides)
    },

    // =========================================================================
    // ████████████  PHẦN 8: GỢI Ý NỘI DUNG TỪ AI (POLL & REMINDER)  ███████████
    // =========================================================================
    async suggestAiContent(roomId, type, anchorMessageId) {
      const targetRoomId = roomId || this.activeRoomId
      if (!targetRoomId || !type || !anchorMessageId) return null
      return await aiSuggestionApi.getAiSuggestions(targetRoomId, type, anchorMessageId)
    },
  },
})
