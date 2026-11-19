import { http } from './http'

// =============================================================================
// ██████████  PHẦN 1: QUẢN LÝ PHÒNG CHAT (DIRECT & GROUP ROOMS)  ██████████████
// =============================================================================

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

// =============================================================================
// ████  PHẦN 2: QUẢN LÝ THÀNH VIÊN NHÓM, PHÊ DUYỆT & RỜI NHÓM (MEMBERS)  ██████
// =============================================================================

/**
 * Lấy danh sách thành viên trong phòng chat.
 * @param {string} roomId
 * @param {string} status 'ACTIVE' | 'PENDING'
 */
export const getRoomMembers = (roomId, status = 'ACTIVE') => {
  const params = status ? { status } : {}
  return http.get(`/rooms/${roomId}/members`, { params }).then((res) => res.data?.data ?? res.data)
}

/**
 * Lấy danh sách bạn bè có thể mời vào nhóm (đã kết bạn và chưa ở trong nhóm).
 * Backend: GET /api/v1/rooms/{roomId}/members/invitable
 * @param {string} roomId
 */
export const getInvitableMembers = (roomId) => {
  return http.get(`/rooms/${roomId}/members/invitable`).then((res) => res.data?.data ?? res.data)
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
 * Phê duyệt một thành viên đang ở trạng thái PENDING vào nhóm.
 * @param {string} roomId
 * @param {string} targetUserId
 */
export const approveRoomMember = (roomId, targetUserId) => {
  return http
    .put(`/rooms/${roomId}/members/${targetUserId}/approve`, { targetUserId })
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Xoá một thành viên khỏi nhóm hoặc từ chối yêu cầu tham gia đang chờ duyệt.
 * @param {string} roomId
 * @param {string} targetUserId
 */
export const removeRoomMember = (roomId, targetUserId) => {
  return http
    .delete(`/rooms/${roomId}/members/${targetUserId}`, {
      data: { targetUserId },
    })
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Thay đổi vai trò của một thành viên trong nhóm ('ADMIN' | 'MEMBER').
 * @param {string} roomId
 * @param {string} userId
 * @param {'ADMIN'|'MEMBER'} role
 */
export const changeRoomMemberRole = (roomId, userId, role) => {
  return http
    .put(`/rooms/${roomId}/members/${userId}/role`, { role })
    .then((res) => res.data?.data ?? res.data)
}

/**
 * Bật / Tắt chế độ xét duyệt thành viên mới vào nhóm.
 * @param {string} roomId
 * @param {boolean} requireApproval
 */
export const toggleRoomApproval = (roomId, requireApproval) => {
  return http
    .put(
      `/rooms/${roomId}/members/approval`,
      { requireApproval: Boolean(requireApproval) },
      {
        params: { requireApproval: Boolean(requireApproval) },
      },
    )
    .then((res) => (res.data?.data !== undefined ? res.data.data : res.data))
}

/**
 * Rời khỏi nhóm chat hiện tại.
 * @param {string} roomId
 * @param {string|null} newAdminId UUID của thành viên kế nhiệm nếu người rời là Admin duy nhất
 */
export const leaveRoom = (roomId, newAdminId = null) => {
  return http
    .delete(`/rooms/${roomId}/members/leave`, {
      data: newAdminId ? { newAdminId } : {},
    })
    .then((res) => res.data?.data ?? res.data)
}

// █████████████████████████████████████████████████████████████████████████████
// PHẦN 3: API CẬP NHẬT THÔNG TIN PHÒNG / NHÓM CHAT (AVATAR & TÊN NHÓM)
// █████████████████████████████████████████████████████████████████████████████

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
