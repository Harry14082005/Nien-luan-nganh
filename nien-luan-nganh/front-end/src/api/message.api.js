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
