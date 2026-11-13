import { http } from './http'

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
