import { http } from './http'

// =============================================================================
// ██████████████  API: REMINDER (LỊCH HẸN & NHẮC NHỞ PHÒNG CHAT)  █████████████
// =============================================================================

/**
 * Kiểm tra xem lịch hẹn đã kết thúc hay chưa:
 * - Trạng thái là DONE hoặc CANCELLED
 * - Hoặc thời gian deadline (endAt nếu có, hoặc startAt) đã trôi qua so với hiện tại
 */
export const isReminderEnded = (rem) => {
  if (!rem) return true
  const status = rem.reminderStatus || rem.status
  if (status === 'DONE' || status === 'CANCELLED') return true
  const deadline = new Date(rem.endAt || rem.startAt).getTime()
  return !isNaN(deadline) && deadline <= Date.now()
}

/**
 * Kiểm tra xem ngày có phải là hôm nay (theo giờ local của trình duyệt)
 */
export const isToday = (dateInput) => {
  if (!dateInput) return false
  const d = new Date(dateInput)
  if (isNaN(d.getTime())) return false
  const now = new Date()
  return (
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  )
}

/**
 * 0. Lấy TẤT CẢ lịch hẹn của người dùng hiện tại từ tất cả các phòng
 *    Chiến lược: lấy danh sách phòng trước, rồi lấy reminder của từng phòng song song.
 *    @param {{ filterToday?: boolean, includePast?: boolean }} options
 */
export const getAllMyReminders = async (options = {}) => {
  // Lấy danh sách phòng (tối đa 50 phòng)
  const roomsRes = await http.get('/rooms', { params: { pageNo: 0, pageSize: 50 } })
  const roomsData = roomsRes.data?.data ?? roomsRes.data
  const rooms = Array.isArray(roomsData)
    ? roomsData
    : roomsData?.content ?? []

  if (!rooms.length) return []

  // Lấy reminder song song từ tất cả phòng
  const results = await Promise.allSettled(
    rooms.map((room) =>
      http
        .get(`/rooms/${room.roomId}/reminders`)
        .then((res) => {
          const list = res.data?.data ?? res.data?.result ?? res.data ?? []
          return (Array.isArray(list) ? list : []).map((r) => {
            const rawStatus = r.reminderStatus || r.status || 'PENDING'
            return {
              ...r,
              status: rawStatus,
              reminderStatus: rawStatus,
              roomId: room.roomId,
              roomName: room.roomName,
              roomAvatarURL: room.roomAvatarURL,
            }
          })
        })
        .catch(() => []),
    ),
  )

  // Gom kết quả, bỏ qua lỗi
  let all = results.flatMap((r) => (r.status === 'fulfilled' ? r.value : []))

  // Chỉ lấy reminder chưa bị huỷ
  all = all.filter((r) => r.status !== 'CANCELLED' && r.reminderStatus !== 'CANCELLED')

  // Lọc theo ngày hôm nay nếu cần
  if (options.filterToday) {
    all = all.filter((r) => {
      // 1. Phải diễn ra trong ngày hôm nay theo giờ local
      const occursToday = isToday(r.startAt) || (r.endAt && isToday(r.endAt))
      if (!occursToday) return false

      // 2. Phải là sự kiện sắp tới (chưa kết thúc/chưa DONE/chưa quá giờ)
      if (!options.includePast && isReminderEnded(r)) return false
      return true
    })
  } else if (!options.includePast) {
    // Mặc định chỉ lấy các lịch hẹn sắp tới (chưa kết thúc)
    all = all.filter((r) => !isReminderEnded(r))
  }

  // Sắp xếp theo startAt tăng dần
  all.sort((a, b) => new Date(a.startAt) - new Date(b.startAt))

  return all
}

/**
 * 1. Tạo mới lịch hẹn / nhắc nhở trong phòng chat
 * @param {string} roomId UUID của phòng chat
 * @param {object} data { title, description, startAt, endAt, reminderType, participantIds }
 */
export const createReminder = (roomId, data) => {
  return http
    .post(`/rooms/${roomId}/reminders`, data)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * 2. Lấy danh sách toàn bộ lịch hẹn trong phòng chat
 * @param {string} roomId UUID của phòng chat
 */
export const getRoomReminders = (roomId) => {
  return http
    .get(`/rooms/${roomId}/reminders`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * 3. Xem chi tiết một lịch hẹn cụ thể
 * @param {string} roomId UUID của phòng chat
 * @param {string} reminderId UUID của lịch hẹn
 */
export const getReminderDetail = (roomId, reminderId) => {
  return http
    .get(`/rooms/${roomId}/reminders/${reminderId}`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * 4. Phản hồi tham gia / từ chối lịch hẹn (RSVP: 'ACCEPTED' | 'DECLINED')
 * @param {string} roomId UUID của phòng chat
 * @param {string} reminderId UUID của lịch hẹn
 * @param {string} response 'ACCEPTED' | 'DECLINED'
 */
export const respondRsvp = (roomId, reminderId, response) => {
  return http
    .put(`/rooms/${roomId}/reminders/${reminderId}/rsvp`, { response })
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * 5. Hủy lịch hẹn
 * @param {string} roomId UUID của phòng chat
 * @param {string} reminderId UUID của lịch hẹn
 */
export const cancelReminder = (roomId, reminderId) => {
  return http
    .delete(`/rooms/${roomId}/reminders/${reminderId}`)
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}

/**
 * 6. Trích xuất gợi ý lịch hẹn bằng AI từ tin nhắn
 * @param {string} roomId UUID của phòng chat
 * @param {string} messageId UUID của tin nhắn anchor
 */
export const suggestReminderWithAi = (roomId, messageId) => {
  return http
    .post(`/rooms/${roomId}/reminders/ai-suggest`, { messageId })
    .then((res) => res.data?.data ?? res.data?.result ?? res.data)
}
