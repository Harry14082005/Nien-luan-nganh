<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import ChatItem from './ChatItem.vue'
import NewChatModal from './NewChatModal.vue'
import SearchBar from '../common/SearchBar.vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { initGlobalWebSocket } from '@/services/websocket'

defineProps({
  Label: {
    type: String,
    default: 'Tin nhắn đã ghim',
  },
  Content_PinnedMessage: {
    type: String,
    default:
      'Họp review giao diện lúc 14:00 thứ Sáu tuần này nha cả nhà thân yêu của em em yêu cả nhà nhiều lắm hihi dài đủ chưa vậy',
  },
  NoMessages: {
    type: [Number, Object],
    default: 5,
  },
  currentFilter: {
    type: String,
    default: 'SelectAll',
  },
})

const chatStore = useChatStore()
const authStore = useAuthStore()

const activeFilter = ref('SelectAll') // 'SelectAll' | 'Unread'
const searchQuery = ref('') // Từ khóa tìm kiếm phòng chat
const showNewChatModal = ref(false) // Trạng thái hiển thị popup tạo chat mới
let syncTimer = null // Timer chạy ngầm đồng bộ phòng định kỳ mỗi 4 giây

onMounted(async () => {
  // 0. Đảm bảo thông tin user đã sẵn sàng để đối chiếu chính xác "Bạn" vs "Tên đối phương"
  if (!authStore.user) {
    await authStore.initUser()
  }

  // 1. Tải danh sách phòng từ Backend
  await chatStore.fetchRooms()

  // 2. Kích hoạt kết nối WebSocket STOMP toàn cục nếu đã có Access Token
  if (authStore.accessToken) {
    initGlobalWebSocket(authStore.accessToken)
  }

  // 3. Đồng bộ ngầm mỗi 4s để tự động nhận phòng mới khi người khác tạo phòng với mình mà không cần F5
  syncTimer = setInterval(() => {
    chatStore.syncRoomsSilently()
  }, 4000)
})

onUnmounted(() => {
  // Dọn dẹp timer khi component bị unmount
  if (syncTimer) clearInterval(syncTimer)
})

/**
 * Định dạng thời gian hiển thị gọn gàng (vd: 14:30 hôm nay, Hôm qua, hoặc 02/11)
 * @param {string} isoString Chuỗi thời gian chuẩn ISO
 */
function formatTime(isoString) {
  if (!isoString) return ''
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return ''

  const now = new Date()
  const isToday =
    date.getDate() === now.getDate() &&
    date.getMonth() === now.getMonth() &&
    date.getFullYear() === now.getFullYear()

  if (isToday) {
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes}`
  }

  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  const isYesterday =
    date.getDate() === yesterday.getDate() &&
    date.getMonth() === yesterday.getMonth() &&
    date.getFullYear() === yesterday.getFullYear()

  if (isYesterday) return 'Hôm qua'

  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${day}/${month}`
}

/**
 * Tính toán nội dung văn bản hiển thị cho dòng tin nhắn cuối của phòng chat trên Sidebar
 * Ưu tiên:
 * 1. Trạng thái đang soạn tin nhắn (Typing indicator)
 * 2. Thông báo ghim tin nhắn ("A đã ghim 1 tin nhắn")
 * 3. Nội dung tin nhắn bình thường kèm tên người gửi
 */
function getLastMessageText(room) {
  if (!room) return 'Chưa có tin nhắn'

  // 1. Kiểm tra xem có thành viên nào đang soạn tin nhắn trong phòng này không
  const roomTyping = chatStore.typingMap[String(room.roomId)]
  if (roomTyping) {
    const users = Object.values(roomTyping)
    if (users.length > 0) {
      return users.length === 1 ? `${users[0].username} đang soạn...` : 'Đang soạn tin nhắn...'
    }
  }

  // 2. Hiển thị thông báo ghim tin nhắn tạm thời (khi vừa bấm ghim)
  if (chatStore.pinNotices && chatStore.pinNotices[String(room.roomId)]) {
    return chatStore.pinNotices[String(room.roomId)]
  }
  if (!room.lastMessageContent) return 'Chưa có tin nhắn'

  // 2. Hiển thị thông báo khi có tin nhắn được ghim hoặc gỡ ghim (SYSTEM Message hoặc Preview)
  const content = room.lastMessageContent || ''
  if (content.includes('ghim') || content.includes('gỡ')) {
    const myUsername = authStore.user?.username
    const senderName = room.lastMessageSenderName
    const isMyAction =
      senderName === 'Bạn' ||
      (myUsername && (senderName === myUsername || content.startsWith(myUsername)))

    let sender = isMyAction ? 'Bạn' : senderName
    if (!sender || sender === 'Thành viên') {
      // Nếu senderName chưa có, trích xuất từ nội dung "Username đã ghim..."
      const firstWord = content.split(' ')[0]
      sender =
        firstWord && !firstWord.includes('đã') && !firstWord.includes('ghim')
          ? firstWord
          : 'Thành viên'
    }

    const isUnpin = content.includes('bỏ') || content.includes('gỡ')
    return `${sender} ${isUnpin ? 'đã bỏ ghim 1 tin nhắn' : 'đã ghim 1 tin nhắn'}`
  }

  // 3. Hiển thị thông báo khi có tin nhắn được ghim hoặc gỡ ghim
  if (
    room.lastMessageContent &&
    (room.lastMessageContent.includes('ghim') || room.lastMessageContent.includes('gỡ'))
  ) {
    const isMyAction =
      (authStore.user?.username && room.lastMessageSenderName === authStore.user.username) ||
      room.lastMessageSenderName === 'Bạn'
    const sender = isMyAction ? 'Bạn' : room.lastMessageSenderName || 'Thành viên'
    const actionText =
      room.lastMessageContent.includes('bỏ') || room.lastMessageContent.includes('gỡ')
        ? 'đã bỏ ghim 1 tin nhắn'
        : 'đã ghim 1 tin nhắn'
    return `${sender}: ${actionText}`
  }

  // 4. Tin nhắn bình thường (luôn là tin nhắn mới nhất)
  if (room.lastMessageSenderName) {
    return `${room.lastMessageSenderName}: ${room.lastMessageContent}`
  }
  return room.lastMessageContent
}

/**
 * Danh sách phòng chat sau khi lọc theo Tab (Tất cả / Chưa đọc) và Từ khóa tìm kiếm
 */
const displayRooms = computed(() => {
  let list = [...chatStore.rooms]

  // Luôn sắp xếp phòng có hoạt động mới nhất lên vị trí đầu tiên (giống như khi có tin nhắn mới)
  list.sort((a, b) => {
    const timeA = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0
    const timeB = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0
    return timeB - timeA
  })

  if (activeFilter.value === 'Unread') {
    list = list.filter((r) => r.hasUnread || (r.unreadCount && r.unreadCount > 0))
  }

  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    list = list.filter(
      (r) =>
        (r.roomName && r.roomName.toLowerCase().includes(q)) ||
        (r.lastMessageContent && r.lastMessageContent.toLowerCase().includes(q)),
    )
  }

  return list
})
</script>

<template>
  <div class="ChatListSidebar">
    <section class="nameApp">
      <div class="logo-title">bluespace.</div>
      <font-awesome-icon icon="bars" class="menu-icon" />
    </section>
    <section class="namePage">
      <section class="left-namePage">
        <div>Tin nhắn</div>
        <div class="NumberOfMessages">{{ chatStore.totalUnreadCount }}</div>
      </section>
      <section
        class="right-namePage"
        style="cursor: pointer"
        title="Tạo cuộc trò chuyện / nhóm mới"
        @click="showNewChatModal = true"
      >
        <font-awesome-icon icon="plus"></font-awesome-icon>
      </section>
    </section>
    <div class="search_bar"><SearchBar v-model="searchQuery"></SearchBar></div>
    <div class="filter">
      <div
        class="SelectedFilter"
        :class="{ active: activeFilter === 'SelectAll' }"
        @click="activeFilter = 'SelectAll'"
      >
        Tất cả
      </div>
      <div
        class="SelectedFilter"
        :class="{ active: activeFilter === 'Unread' }"
        @click="activeFilter = 'Unread'"
      >
        Chưa đọc
      </div>
    </div>
    <span>GẦN ĐÂY</span>
    <div class="ChatList">
      <ChatItem
        v-for="c in displayRooms"
        :key="c.roomId"
        :name="c.roomName || 'Cuộc trò chuyện'"
        :avatar="c.roomAvatarURL || ''"
        :last-message="getLastMessageText(c)"
        :time="formatTime(c.lastMessageAt)"
        :unread="c.unreadCount || 0"
        :has-unread="Boolean(c.hasUnread || (c.unreadCount && c.unreadCount > 0))"
        :online="c.roomType === 'DIRECT' ? !!c.isOnline : false"
        :active="c.roomId === chatStore.activeRoomId"
        @select="chatStore.selectRoom(c.roomId)"
      />
    </div>
    <NewChatModal v-if="showNewChatModal" @close="showNewChatModal = false" />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
.ChatListSidebar {
  display: flex;
  flex-direction: column;
  font-family: 'Quicksand', sans-serif;
  width: 100%;
  height: 100%;
  background-color: #ffffff;
  border-right: 1px solid #e6eaf2;
  box-sizing: border-box;
}
.nameApp {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 8px 24px;
}
section.nameApp {
  font-size: 18px;
  color: #94a3b8;
  cursor: pointer;
  transition: color 0.2s ease;
}

section.nameApp:hover {
  color: #3b82f6;
}

.logo-title {
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(
    to right,
    #0a192f 0%,
    #1e3a8a 25%,
    #2563eb 50%,
    #3b82f6 75%,
    #60a5fa 100%
  );
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}
.namePage {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  padding-bottom: 8px;
}
.left-namePage {
  display: flex;
  font-size: 26px;
  float: left;
  color: #223451;
  font-weight: 700;
}
.right-namePage {
  background-color: #e6edfd;
  padding: 9px;
  border-radius: 0.7rem;
  cursor: pointer;
  color: #2563eb;
}
.NumberOfMessages {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #3b82f6;
  background-color: #e6edfd;
  border-radius: 8px;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  margin-left: 8px;
}
.filter {
  display: flex;
  gap: 10px;
  font-size: 15px;
  padding-right: 24px;
  padding-left: 24px;
  padding-top: 8px;
  padding-bottom: 15px;
  border-bottom: 1px solid#e6edfd;
  font-weight: 700;
}
.SelectedFilter {
  padding: 7px;
  cursor: pointer;
  color: #4a5a78;
}
.SelectedFilter.active {
  border-radius: 0.8rem;
  background-color: #e6edfd;
  padding: 7px;
  cursor: pointer;
  color: #2f5fd0;
}
.search_bar {
  display: flex;
  align-self: center;
  width: 100%;
  padding: 16px 24px;
  padding-bottom: 8px;
  box-sizing: border-box;
  margin-bottom: 10px;
}
span {
  padding: 0 24px;
  margin-top: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #b4bccb;
  margin-bottom: 8px;
}
.ChatList {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ChatList::-webkit-scrollbar {
  width: 6px;
}

.ChatList::-webkit-scrollbar-track {
  background: transparent;
}

.ChatList::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 4px;
}

.ChatList::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}
</style>
