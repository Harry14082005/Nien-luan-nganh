import { http } from './http'

/**
 * 0. Lấy TẤT CẢ lịch hẹn của người dùng hiện tại từ tất cả các phòng
 *    Chiến lược: lấy danh sách phòng trước, rồi lấy reminder của từng phòng song song.
 *    @param {{ filterToday?: boolean }} options
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
          return (Array.isArray(list) ? list : []).map((r) => ({
            ...r,
            roomId: room.roomId,
            roomName: room.roomName,
            roomAvatarURL: room.roomAvatarURL,
          }))
        })
        .catch(() => []),
    ),
  )

  // Gom kết quả, bỏ qua lỗi
  let all = results.flatMap((r) => (r.status === 'fulfilled' ? r.value : []))

  // Chỉ lấy reminder chưa bị huỷ
  all = all.filter((r) => r.status !== 'CANCELLED')

  // Lọc theo ngày hôm nay nếu cần
  if (options.filterToday) {
    const todayStr = new Date().toISOString().slice(0, 10) // 'YYYY-MM-DD'
    all = all.filter((r) => {
      const start = r.startAt ? r.startAt.slice(0, 10) : null
      return start === todayStr
    })
  } else {
    // Chỉ lấy reminder tương lai (startAt >= hôm nay)
    const now = new Date()
    now.setHours(0, 0, 0, 0)
    all = all.filter((r) => {
      if (!r.startAt) return false
      return new Date(r.startAt) >= now
    })
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
