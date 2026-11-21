import router from '@/router'
import { useChatStore } from '@/stores/chat'
import { useNotificationStore } from '@/stores/notification'

/**
 * Điều hướng người dùng tới đúng ngữ cảnh khi bấm vào 1 thông báo
 * @param {Object} notif Thông tin thông báo
 */
export async function handleNotificationClick(notif) {
  if (!notif) return

  const notificationStore = useNotificationStore()

  // 1. Đánh dấu đã đọc
  if (notif.notificationId) {
    await notificationStore.markAsRead(notif.notificationId)
  }

  // 2. Đóng panel thông báo
  notificationStore.closePanel()

  const type = String(notif.notificationType || '')

  // 3. Sự kiện bạn bè -> Chuyển sang trang Danh bạ
  if (type.startsWith('FRIEND_')) {
    if (router.currentRoute.value.path !== '/contacts') {
      await router.push('/contacts')
    }
    return
  }

  // 4. Sự kiện phòng chat, lịch hẹn, bình chọn có roomId
  if (notif.roomId) {
    const chatStore = useChatStore()
    if (router.currentRoute.value.path !== '/chat') {
      await router.push('/chat')
    }

    if (!chatStore.rooms || chatStore.rooms.length === 0) {
      await chatStore.fetchRooms()
    }

    await chatStore.selectRoom(notif.roomId)

    // Nếu là thông báo xin gia nhập phòng -> Tự động mở Info Sidebar và tăng pendingVersion
    if (type === 'ROOM_JOIN_REQUEST') {
      localStorage.setItem('isChatInfoSidebarOpen', 'true')
      chatStore.incrementPendingVersion()
    }

    // Nếu là thông báo được nhắc tên (@mention) -> Cuộn tới đúng tin nhắn
    if (type === 'MESSAGE_MENTION' && notif.referenceId) {
      setTimeout(() => {
        const el = document.getElementById('msg-' + notif.referenceId)
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' })
          el.classList.add('highlight-msg')
          setTimeout(() => el.classList.remove('highlight-msg'), 2000)
        }
      }, 400)
    }
  }
}