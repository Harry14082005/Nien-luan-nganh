import { http } from './http'

/**
 * Lấy lịch sử tin nhắn trong phòng theo phân trang con trỏ (Cursor Pagination).
 * @param {string} roomId UUID của phòng chat
 * @param {object} params { cursor, before, limit }
 */
export const getMessages = (roomId, params = {}) => {
  return http.get(`/rooms/${roomId}/messages`, { params }).then((res) => res.data?.data ?? res.data)
}

/**
 * Lấy danh sách tin nhắn cũ hơn con trỏ cursor (phân trang lên trên).
 * @param {string} roomId UUID của phòng chat
 * @param {string} cursor UUID tin nhắn làm mốc con trỏ
 * @param {number} limit Số lượng tin nhắn cần lấy
 */
export const getMessagesBefore = (roomId, cursor, limit = 20) => {
  return http
    .get(`/rooms/${roomId}/messages`, { params: { cursor, limit } })
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Lấy danh sách tin nhắn mới hơn con trỏ cursor (phân trang hai chiều khi cuộn xuống).
 * @param {string} roomId UUID của phòng chat
 * @param {string} cursor UUID tin nhắn làm mốc con trỏ
 * @param {number} limit Số lượng tin nhắn cần lấy
 */
export const getMessagesAfter = (roomId, cursor, limit = 20) => {
  return http
    .get(`/rooms/${roomId}/messages/after`, { params: { cursor, limit } })
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Lấy cửa sổ ngữ cảnh 20 tin nhắn bao quanh một tin nhắn cụ thể (ví dụ tin nhắn ghim).
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn mục tiêu
 */
export const getMessageContext = (roomId, messageId) => {
  return http
    .get(`/rooms/${roomId}/messages/${messageId}/context`)
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Tìm kiếm tin nhắn trong phòng chat theo từ khóa (Lightweight Metadata).
 * @param {string} roomId UUID của phòng chat
 * @param {string} q Từ khóa tìm kiếm
 */
export const searchMessages = (roomId, q) => {
  return http
    .get(`/rooms/${roomId}/messages/search`, { params: { q } })
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Tìm kiếm tin nhắn chi tiết cho Sidebar Drawer bên phải (Search Preview).
 * @param {string} roomId UUID của phòng chat
 * @param {string} q Từ khóa tìm kiếm
 */
export const searchMessagesSidebar = (roomId, q) => {
  return http
    .get(`/rooms/${roomId}/messages/search/sidebar`, { params: { q } })
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Gửi tin nhắn vào phòng chat qua REST API (dự phòng khi WebSocket chưa sẵn sàng).
 * @param {string} roomId UUID của phòng chat
 * @param {object} data { content, messageType, repliedMessageId }
 */
export const sendMessage = (roomId, data) => {
  return http.post(`/rooms/${roomId}/messages`, data).then((res) => res.data?.data ?? res.data)
}

/**
 * Đánh dấu đã đọc tất cả tin nhắn trong phòng chat.
 * @param {string} roomId UUID của phòng chat
 * @param {string|null} messageId UUID của tin nhắn mốc đã đọc (tùy chọn)
 */
export const markAsRead = (roomId, messageId = null) => {
  return http
    .put(`/rooms/${roomId}/members/me/read`, messageId ? { messageId } : {})
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Lấy danh sách tin nhắn đã ghim trong phòng chat.
 * @param {string} roomId UUID của phòng chat
 */
export const getPinnedMessages = (roomId) => {
  return http
    .get(`/rooms/${roomId}/messages/pins`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Ghim một tin nhắn trong phòng chat.
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn cần ghim
 */
export const pinMessage = (roomId, messageId) => {
  return http
    .post(`/rooms/${roomId}/messages/${messageId}/pins`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Bỏ ghim một tin nhắn trong phòng chat.
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn cần bỏ ghim
 */
export const unpinMessage = (roomId, messageId) => {
  return http
    .delete(`/rooms/${roomId}/messages/${messageId}/pins`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Chỉnh sửa nội dung tin nhắn của chính mình.
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn cần sửa
 * @param {string} content Nội dung văn bản mới
 */
export const updateMessage = (roomId, messageId, content) => {
  return http
    .put(`/rooms/${roomId}/messages/${messageId}`, { content })
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Thu hồi tin nhắn (xóa mềm).
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn cần thu hồi
 */
export const revokeMessage = (roomId, messageId) => {
  return http
    .delete(`/rooms/${roomId}/messages/${messageId}`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Lấy danh sách lịch sử các lần chỉnh sửa của tin nhắn.
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn
 */
export const getMessageHistory = (roomId, messageId) => {
  return http
    .get(`/rooms/${roomId}/messages/${messageId}/history`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Thả hoặc đổi cảm xúc trên tin nhắn (LIKE, LOVE, HAHA, WOW, SAD, ANGRY).
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn
 * @param {object} data { reactionType: 'LIKE' | 'LOVE' | 'HAHA' | 'WOW' | 'SAD' | 'ANGRY' }
 */
export const reactMessage = (roomId, messageId, data) => {
  return http
    .post(`/rooms/${roomId}/messages/${messageId}/reactions`, data)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Gỡ cảm xúc đã thả trên tin nhắn.
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn
 */
export const removeReaction = (roomId, messageId) => {
  return http
    .delete(`/rooms/${roomId}/messages/${messageId}/reactions`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Xem danh sách cảm xúc đã thả trên tin nhắn.
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn
 */
export const getMessageReactions = (roomId, messageId) => {
  return http
    .get(`/rooms/${roomId}/messages/${messageId}/reactions`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Gửi hình ảnh vào phòng chat qua REST API.
 * @param {string} roomId UUID của phòng chat
 * @param {FormData} formData Chứa file ảnh và optional repliedMessageId
 */
export const sendImageMessage = (roomId, formData) => {
  return http
    .post(`/rooms/${roomId}/messages/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * Gửi tệp đính kèm vào phòng chat qua REST API.
 * @param {string} roomId UUID của phòng chat
 * @param {FormData} formData Chứa file tài liệu và optional repliedMessageId
 */
export const sendFileMessage = (roomId, formData) => {
  return http
    .post(`/rooms/${roomId}/messages/files`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}
