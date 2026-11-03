<script setup>
/**
 * =========================================================================
 * View: ChatView.vue
 * =========================================================================
 * - Màn hình chính của ứng dụng Chat (Bố cục 3 cột: NavigationSidebar | ChatListSidebar | ChatView).
 * - Kết nối dữ liệu phòng đang mở (activeRoom), danh sách tin nhắn (messages), và tin ghim (pinnedMessages).
 * - Tự động cuộn xuống đáy khi có tin nhắn mới hoặc khi chuyển phòng (scrollToBottom).
 * - Tự động cuộn mượt và highlight tới tin nhắn khi bấm vào thanh ghim (handleScrollToMessage).
 * - Lắng nghe sự kiện ghim/bỏ ghim tin nhắn bất kỳ (handleTogglePin).
 * - Hiển thị trạng thái đang gõ phím của đối phương với hiệu ứng chấm nhảy (Typing indicator).
 */
import { ref, nextTick, watch, onMounted } from 'vue';
import { useChatStore } from '@/stores/chat';
import { useAuthStore } from '@/stores/auth';
import ChatHeader from '@/components/chat/ChatHeader.vue';
import PinnedMessage from '@/components/chat/PinnedMessage.vue';
import MessageList from '@/components/chat/MessageList.vue';
import ChatInput from '@/components/chat/ChatInput.vue';
import NavigationSidebar from '@/components/common/NavigationSidebar.vue';
import ChatListSidebar from '@/components/chat/ChatListSidebar.vue';
import { initGlobalWebSocket } from '@/services/websocket';

const chatStore = useChatStore();
const authStore = useAuthStore();
const chatBodyRef = ref(null); // Tham chiếu tới vùng cuộn tin nhắn (.chat-body)

/**
 * Cuộn khung chat xuống đáy mượt mà sau khi DOM cập nhật
 */
const scrollToBottom = async () => {
  await nextTick();
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
  }
};

/**
 * Kiểm tra xem tin nhắn có phải do chính người dùng hiện tại gửi đi hay không
 * @param {object} msg Đối tượng tin nhắn (MessageResponse)
 */
const isMyMessage = (msg) => {
  if (!msg || !msg.sender) return false;
  const currentUserId = authStore.user?.userId;
  if (currentUserId && msg.sender.userId) {
    return String(msg.sender.userId) === String(currentUserId);
  }
  if (authStore.user?.username && msg.sender.username) {
    return msg.sender.username === authStore.user.username;
  }
  return msg.sender.userId === 'me';
};

/**
 * Định dạng chuỗi thời gian tin nhắn dạng HH:mm (vd: 15:45)
 */
const formatTime = (isoString) => {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '';
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  } catch {
    return '';
  }
};

onMounted(async () => {
  // 1. Tải thông tin tài khoản nếu chưa có
  if (!authStore.user) {
    await authStore.initUser();
  }
  // 2. Kích hoạt kết nối WebSocket STOMP
  const token = authStore.accessToken || localStorage.getItem('accessToken');
  if (token) {
    initGlobalWebSocket(token);
  }
  // 3. Tải tin nhắn và tin ghim của phòng đang mở
  if (chatStore.activeRoomId) {
    if (chatStore.messages.length === 0) {
      await chatStore.fetchMessages(chatStore.activeRoomId);
    }
    await chatStore.fetchPinnedMessages(chatStore.activeRoomId);
  }
  scrollToBottom();
});

/**
 * Xử lý sự kiện Ghim hoặc Bỏ ghim tin nhắn bất kỳ từ danh sách tin nhắn
 * @param {string} messageId UUID của tin nhắn
 */
const handleTogglePin = async (messageId) => {
  if (!messageId) return;
  const msg = chatStore.messages.find((m) => String(m.messageId) === String(messageId));
  if (msg?.isPinned) {
    await chatStore.unpinMessage(messageId);
  } else {
    await chatStore.pinMessage(messageId);
  }
};

/**
 * Cuộn khung chat tới vị trí tin nhắn được ghim và kích hoạt hiệu ứng chớp sáng (flash)
 * @param {string} messageId UUID của tin nhắn cần tìm
 */
const handleScrollToMessage = (messageId) => {
  if (!messageId) return;
  const el = document.getElementById('msg-' + messageId);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    el.classList.add('highlight-msg');
    setTimeout(() => {
      el.classList.remove('highlight-msg');
    }, 1800);
  }
};

// Tự động cuộn xuống đáy khi có tin nhắn mới được thêm vào
watch(
  () => chatStore.messages.length,
  () => {
    scrollToBottom();
  }
);

// Tự động cuộn xuống đáy khi chuyển đổi phòng chat
watch(
  () => chatStore.activeRoomId,
  () => {
    scrollToBottom();
  }
);

// Tự động cuộn xuống đáy khi đối phương bắt đầu soạn tin nhắn
watch(
  () => chatStore.activeTypingText,
  (newVal) => {
    if (newVal) {
      scrollToBottom();
    }
  }
);
</script>
<template>
  <div class="app-container">
  <NavigationSidebar></NavigationSidebar>
  <div class="ChatListSidebar">
    <ChatListSidebar></ChatListSidebar>
  </div>
  <div class="chat-view">
    <ChatHeader 
      :RoomName="chatStore.activeRoom?.roomName || 'Cuộc trò chuyện'"
      :StatusRoom="chatStore.activeRoom?.isOnline ? 'Đang hoạt động' : (chatStore.activeRoom?.roomType === 'GROUP' ? 'Nhóm trò chuyện' : 'Offline')"
    />
    <PinnedMessage @scrollToMessage="handleScrollToMessage" />

    <div class="chat-body" ref="chatBodyRef">
      <div 
        v-if="chatStore.loadingMessages" 
        style="text-align: center; color: #a0aec0; margin-top: 20px; font-family: 'Quicksand', sans-serif;"
      >
        Đang tải tin nhắn...
      </div>
      <div 
        v-else-if="chatStore.messages.length === 0" 
        style="text-align: center; color: #a0aec0; margin-top: 40px; font-family: 'Quicksand', sans-serif;"
      >
        Chưa có tin nhắn nào. Hãy gửi lời chào đầu tiên! 👋
      </div>
      <MessageList 
        v-for="msg in chatStore.messages" 
        :key="msg.messageId"
        :messageId="msg.messageId"
        :isPinned="!!msg.isPinned"
        :Content_Message="msg.content"
        :Time="formatTime(msg.createdAt)"
        :Sender="isMyMessage(msg) ? '' : (msg.sender?.username || 'Thành viên')"
        :variant="isMyMessage(msg) ? 'SendTo' : 'default'"
        @togglePin="handleTogglePin"
      />

      <!-- Trạng thái đang soạn tin nhắn (Typing indicator) -->
      <div v-if="chatStore.activeTypingText" class="typing-indicator">
        <span class="typing-text">{{ chatStore.activeTypingText }}</span>
        <span class="dots">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </span>
      </div>
    </div>
    <div class="input_text"><ChatInput/></div>
  </div> 
  </div>
</template>
<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  background-color: #f8f9fd;
  border-right: 1px solid #e6eaf2;   
  border-left: 1px solid #e6eaf2;
}

.chat-body {
  flex: 1 1 0;                
  min-height: 0;          
  overflow-y: auto; 
  overflow-x: hidden;
  padding: 10px;  
}
.app-container{
  display: grid;
  grid-template-columns: 90px 450px 1fr;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}
.input_text{
  flex-shrink: 0;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: white;
}

.typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  margin: 8px 0 4px 6px;
  background-color: #ffffff;
  border: 1px solid #e6eaf2;
  border-radius: 14px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  color: #718096;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.typing-text {
  font-weight: 500;
}

.dots {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.dot {
  width: 5px;
  height: 5px;
  background-color: #4b7bec;
  border-radius: 50%;
  display: inline-block;
  animation: typingBounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typingBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1.1);
    opacity: 1;
  }
}
</style>
