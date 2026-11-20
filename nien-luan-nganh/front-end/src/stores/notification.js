import { defineStore } from 'pinia'
import * as notificationApi from '@/api/notification.api'
import { playNotificationSound } from '@/utils/sound'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [],
    unreadCount: 0,
    currentPage: 0,
    totalPages: 1,
    hasMore: false,
    loading: false,
    loadingMore: false,
    filter: 'ALL', // 'ALL' | 'UNREAD'
    isPanelOpen: false,
    toasts: [], // Danh sách các popup toast hiện tại: [{ id, ...notification }]
  }),

  getters: {
    filteredNotifications: (state) => {
      if (state.filter === 'UNREAD') {
        return state.notifications.filter((n) => !n.isRead)
      }
      return state.notifications
    },
  },

  actions: {
    // Khởi tạo và nạp dữ liệu trang đầu tiên + số lượng chưa đọc
    async fetchInitial() {
      this.loading = true
      try {
        const [pageData, count] = await Promise.all([
          notificationApi.getMyNotifications(0, 20),
          notificationApi.getUnreadNotificationCount(),
        ])
        this.notifications = pageData?.content || []
        this.currentPage = pageData?.number || 0
        this.totalPages = pageData?.totalPages || 1
        this.hasMore = this.currentPage + 1 < this.totalPages
        this.unreadCount = typeof count === 'number' ? count : 0
      } catch (err) {
        console.warn('Lỗi tải thông báo:', err)
      } finally {
        this.loading = false
      }
    },

    // Chỉ cập nhật lại số lượng chưa đọc
    async fetchUnreadCount() {
      try {
        const count = await notificationApi.getUnreadNotificationCount()
        this.unreadCount = typeof count === 'number' ? count : 0
      } catch (err) {
        console.warn('Lỗi đếm thông báo chưa đọc:', err)
      }
    },

    // Tải thêm thông báo cũ hơn (phân trang khi cuộn)
    async fetchMore() {
      if (this.loadingMore || !this.hasMore) return
      this.loadingMore = true
      try {
        const nextPage = this.currentPage + 1
        const pageData = await notificationApi.getMyNotifications(nextPage, 20)
        const newItems = pageData?.content || []

        // Tránh trùng lặp id
        const existingIds = new Set(this.notifications.map((n) => String(n.notificationId)))
        newItems.forEach((item) => {
          if (!existingIds.has(String(item.notificationId))) {
            this.notifications.push(item)
          }
        })

        this.currentPage = nextPage
        this.totalPages = pageData?.totalPages || this.totalPages
        this.hasMore = this.currentPage + 1 < this.totalPages
      } catch (err) {
        console.warn('Lỗi tải thêm thông báo:', err)
      } finally {
        this.loadingMore = false
      }
    },

    // Xử lý thông báo đến theo thời gian thực (từ WebSocket)
    handleIncomingNotification(notif) {
      if (!notif) return

      const notifId = String(notif.notificationId || '')
      // Chống đếm trùng: nếu thông báo này đã có trong danh sách thì bỏ qua không đếm lặp
      if (notifId && this.notifications.some((n) => String(n.notificationId) === notifId)) {
        return
      }

      // 1. Phát âm thanh chuông "ting" nhẹ nhàng
      playNotificationSound()

      // 2. Tăng số lượng thông báo chưa đọc
      this.unreadCount = (this.unreadCount || 0) + 1

      // 3. Đưa thông báo mới lên đầu danh sách
      this.notifications.unshift({
        ...notif,
        isRead: false,
      })

      // 4. Đẩy vào danh sách Toast popup hiển thị 3-4 giây
      const toastId = notifId || `temp-${Date.now()}`
      const toastItem = {
        ...notif,
        id: toastId,
      }

      // Giới hạn tối đa 3 toast cùng lúc để không choán màn hình
      if (this.toasts.length >= 3) {
        this.toasts.pop()
      }
      this.toasts.unshift(toastItem)

      // Tự biến mất sau 3.5 giây
      setTimeout(() => {
        this.removeToast(toastId)
      }, 3500)
    },

    // Đánh dấu 1 thông báo là đã đọc
    async markAsRead(notificationId) {
      if (!notificationId) return
      const item = this.notifications.find((n) => String(n.notificationId) === String(notificationId))
      if (item && !item.isRead) {
        item.isRead = true
        if (this.unreadCount > 0) {
          this.unreadCount--
        }
      }
      try {
        await notificationApi.markNotificationAsRead(notificationId)
      } catch (err) {
        console.warn('Lỗi đánh dấu đã đọc thông báo:', err)
      }
    },

    // Đánh dấu tất cả thông báo là đã đọc
    async markAllAsRead() {
      this.notifications.forEach((n) => {
        n.isRead = true
      })
      this.unreadCount = 0
      try {
        await notificationApi.markAllNotificationsAsRead()
      } catch (err) {
        console.warn('Lỗi đánh dấu tất cả đã đọc:', err)
      }
    },

    // Đóng 1 toast popup
    removeToast(toastId) {
      this.toasts = this.toasts.filter((t) => String(t.id) !== String(toastId))
    },

    // Đóng/mở panel danh sách thông báo
    togglePanel() {
      this.isPanelOpen = !this.isPanelOpen
      if (this.isPanelOpen && this.notifications.length === 0) {
        this.fetchInitial()
      }
    },

    openPanel() {
      this.isPanelOpen = true
      if (this.notifications.length === 0) {
        this.fetchInitial()
      }
    },

    closePanel() {
      this.isPanelOpen = false
    },

    setFilter(newFilter) {
      this.filter = newFilter
    },

    resetState() {
      this.notifications = []
      this.unreadCount = 0
      this.currentPage = 0
      this.totalPages = 1
      this.hasMore = false
      this.loading = false
      this.loadingMore = false
      this.filter = 'ALL'
      this.isPanelOpen = false
      this.toasts = []
    },
  },
})
