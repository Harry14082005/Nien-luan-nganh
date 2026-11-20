import { http } from './http'

/**
 * API: THÔNG BÁO (NOTIFICATIONS)
 */

// Lấy danh sách thông báo của người dùng đăng nhập (phân trang)
export const getMyNotifications = async (page = 0, size = 20) => {
  const res = await http.get('/notifications', { params: { page, size } })
  return res.data?.data ?? res.data?.result ?? res.data ?? { content: [], totalElements: 0 }
}

// Lấy số lượng thông báo chưa đọc
export const getUnreadNotificationCount = async () => {
  const res = await http.get('/notifications/unread-count')
  const data = res.data?.data ?? res.data?.result ?? res.data
  if (typeof data?.unreadCount === 'number') return data.unreadCount
  if (typeof data?.count === 'number') return data.count
  if (typeof data === 'number') return data
  return 0
}

// Đánh dấu 1 thông báo là đã đọc
export const markNotificationAsRead = async (notificationId) => {
  const res = await http.patch(`/notifications/${notificationId}/read`)
  return res.data?.data ?? res.data?.result ?? res.data
}

// Đánh dấu tất cả thông báo là đã đọc
export const markAllNotificationsAsRead = async () => {
  const res = await http.patch('/notifications/read-all')
  return res.data?.data ?? res.data?.result ?? res.data
}
