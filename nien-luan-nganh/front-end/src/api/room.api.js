import { http } from './http'

/**
 * Lấy danh sách các phòng chat của người dùng hiện tại (hỗ trợ phân trang và lọc theo type).
 * @param {Object} params { type?: 'DIRECT' | 'GROUP', pageNo?: number, pageSize?: number }
 */
export const getRooms = (params = {}) => {
  return http.get('/rooms', { params }).then((res) => res.data?.data ?? res.data)
}

/**
 * Lấy thông tin chi tiết một phòng chat.
 * @param {string} roomId
 */
export const getRoom = (roomId) => {
  return http.get(`/rooms/${roomId}`).then((res) => res.data?.data ?? res.data)
}

/**
 * Lấy danh sách thành viên trong phòng chat.
 * @param {string} roomId
 */
export const getRoomMembers = (roomId) => {
  return http.get(`/rooms/${roomId}/members`, { params: { status: 'ACTIVE' } }).then((res) => res.data?.data ?? res.data)
}

/**
 * Tạo hoặc lấy phòng chat 1-1 (DIRECT) với một người dùng khác.
 * Backend: POST /api/v1/rooms/direct — Body: { userId }
 * @param {string} userId UUID của người dùng muốn nhắn tin cùng
 */
export const createDirectRoom = (userId) => {
  return http.post('/rooms/direct', { userId }).then((res) => res.data?.data ?? res.data)
}

/**
 * Tạo nhóm chat mới (GROUP).
 * Backend: POST /api/v1/rooms/group — Body: { roomName, roomAvatarURL, memberIds }
 * @param {Object} body { roomName: string, roomAvatarURL?: string, memberIds: string[] }
 */
export const createGroup = (body) => {
  return http.post('/rooms/group', body).then((res) => res.data?.data ?? res.data)
}

/**
 * Thêm thành viên vào nhóm chat hiện tại.
 * Backend: POST /api/rooms/{roomId}/members — Body: { memberIds: string[] }
 * @param {string} roomId
 * @param {string[]} memberIds
 */
export const addRoomMembers = (roomId, memberIds) => {
  return http.post(`/rooms/${roomId}/members`, { memberIds }).then((res) => res.data?.data ?? res.data)
}

/**
 * Cập nhật avatar của phòng / nhóm chat.
 * Backend: POST /api/v1/rooms/{roomId}/avatar — FormData: file
 * @param {string} roomId
 * @param {File} file
 */
export const uploadRoomAvatar = (roomId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/rooms/${roomId}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then((res) => res.data?.data ?? res.data)
}

/**
 * Cập nhật tên của phòng / nhóm chat.
 * Backend: PUT /api/v1/rooms/{roomId}/name — Body: { roomName }
 * @param {string} roomId
 * @param {string} roomName
 */
export const updateRoomName = (roomId, roomName) => {
  return http.put(`/rooms/${roomId}/name`, { roomName }).then((res) => res.data?.data ?? res.data)
}

