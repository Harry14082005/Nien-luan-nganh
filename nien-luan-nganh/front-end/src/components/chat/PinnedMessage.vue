<script setup>
/**
 * =========================================================================
 * Component: PinnedMessage.vue
 * =========================================================================
 * - Hiển thị thanh "Tin nhắn đã ghim" trên đầu khung chat.
 * - Tự động hiển thị tin nhắn ghim mới nhất kèm số lượng tin ghim khác (+N tin khác).
 * - Click vào nội dung ghim -> phát sự kiện scrollToMessage để khung chat cuộn tới tin đó.
 * - Hỗ trợ nút sổ xuống (chevron-down) xem toàn bộ danh sách tin đã ghim (tối đa 5 tin).
 * - Cho phép bấm nút 'x' để bỏ ghim nhanh tin nhắn.
 */
import { ref, computed } from 'vue'
import { useChatStore } from '@/stores/chat'

// Phát sự kiện cuộn tới tin nhắn lên component cha (ChatView.vue)
const emit = defineEmits(['scrollToMessage'])
const chatStore = useChatStore()

// Trạng thái bật/tắt menu popup danh sách toàn bộ tin đã ghim
const showList = ref(false)

// Tin nhắn ghim mới nhất (lấy từ Pinia Store)
const pinnedMsg = computed(() => chatStore.latestPinnedMessage)

/**
 * Xử lý bỏ ghim một tin nhắn bất kỳ.
 * @param {string} messageId UUID của tin nhắn cần gỡ ghim
 */
const handleUnpin = async (messageId) => {
  if (messageId) {
    await chatStore.unpinMessage(messageId)
    // Nếu đã gỡ hết tin ghim thì tự động đóng menu popup
    if (chatStore.pinnedMessages.length === 0) {
      showList.value = false
    }
  }
}

/**
 * Khi click vào thanh ghim chính -> cuộn khung chat tới vị trí tin nhắn đó
 */
const handleClickPinned = () => {
  if (pinnedMsg.value?.messageId) {
    emit('scrollToMessage', pinnedMsg.value.messageId)
  }
}

/**
 * Khi click vào 1 mục trong menu danh sách ghim -> cuộn tới tin nhắn và đóng menu
 * @param {string} messageId UUID của tin nhắn được chọn
 */
const handleItemClick = (messageId) => {
  emit('scrollToMessage', messageId)
  showList.value = false
}
</script>

<template>
<div v-if="pinnedMsg" class="pinned-container">
  <div class="chatheader">
    <div class="left-header" @click="handleClickPinned" title="Nhảy tới tin nhắn này trong khung chat">
      <div class="AvatarRoom">
        <font-awesome-icon icon="thumbtack"/>
      </div>
      <div class="info-group">
        <div class="Label">
          Tin nhắn đã ghim
          <span v-if="chatStore.pinnedMessages.length > 1" class="pin-badge">
            (+{{ chatStore.pinnedMessages.length - 1 }} tin khác)
          </span>
        </div>
        <div class="Content_PinnedMessage">
          {{ (pinnedMsg.sender?.username ? pinnedMsg.sender.username + ': ' : '') + (pinnedMsg.content || '') }}
        </div>
      </div>
    </div>
    <div class="right-header">
      <div 
        v-if="chatStore.pinnedMessages.length > 1" 
        class="action-btn" 
        :title="showList ? 'Đóng danh sách ghim' : 'Xem tất cả tin ghim'"
        @click.stop="showList = !showList"
      >
        <font-awesome-icon :icon="showList ? 'chevron-up' : 'chevron-down'"/>
      </div>
      <div 
        class="action-btn" 
        title="Bỏ ghim tin nhắn này" 
        @click.stop="handleUnpin(pinnedMsg?.messageId)"
      >
        <font-awesome-icon icon="x"/>
      </div>
    </div>
  </div>

  <!-- Menu sổ xuống danh sách tất cả tin nhắn ghim -->
  <div v-if="showList" class="pinned-dropdown">
    <div class="dropdown-header">
      <span class="dropdown-title">
        <font-awesome-icon icon="thumbtack" style="margin-right: 5px; color: #4b7bec;"/>
        Danh sách tin đã ghim ({{ chatStore.pinnedMessages.length }}/5)
      </span>
      <span class="dropdown-close" @click="showList = false">✕</span>
    </div>
    <div class="dropdown-body">
      <div 
        v-for="(pin, idx) in chatStore.pinnedMessages" 
        :key="pin.messageId"
        class="dropdown-item"
        @click="handleItemClick(pin.messageId)"
      >
        <div class="item-left">
          <span class="item-badge">#{{ idx + 1 }}</span>
          <div class="item-text-wrap">
            <span class="item-sender">{{ pin.sender?.username || 'Thành viên' }}</span>
            <span class="item-content">{{ pin.content }}</span>
          </div>
        </div>
        <div 
          class="item-unpin-btn" 
          title="Bỏ ghim" 
          @click.stop="handleUnpin(pin.messageId)"
        >
          <font-awesome-icon icon="x"/>
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.pinned-container {
  position: relative;
  width: 100%;
  flex-shrink: 0;
  z-index: 10;
}

.AvatarRoom{
    display: flex;
    flex-shrink: 0;
    background-color:rgb(243, 247, 255);
    padding-top:10px;
    padding-bottom:10px;
    font-family:"Quicksand", sans-serif; 
    font-size: 13px;
    font-weight: 500;
    width: 28px;
    height: 28px;
    border-radius: 8px;
    background-color: rgb(228, 237, 255);
    color: rgb(75, 123, 236);
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}
.pinned-bar{
    display: flex;
    background-color: #f0f5ff;
    padding: 8px 24px;
    width: 100%;
    box-sizing: border-box;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #e1ebfd;
}
.left-header{
    display: flex;
    cursor: pointer;
    flex: 1;
    align-items: center;
    min-width: 0;
}
.right-header{
    display: flex;
    color: #94a3b8;
    margin-left: 12px;
    flex-shrink: 0;
}
.btn-close-pin {
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    transition: color 0.15s;
}
.btn-close-pin:hover {
    color: #1e293b;
}
.action-btn {
    cursor: pointer;
    padding: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: color 0.15s;
}
.action-btn:hover {
    color: #4b7bec;
}
.info-group{
    display: flex;
    flex-direction: column;
    font-family:"Quicksand", sans-serif; 
    margin-left: 10px;
    align-self: center;
    justify-items: center;
    align-content: center;
    justify-content: left; 
    overflow: hidden;
}
.Label{
    display: flex;
    align-items: center;
    margin-left: 12px;
    min-width: 0;
}
.Label{
    font-family:"Quicksand", sans-serif; 
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: #4b7bec;
}
.pin-badge {
    font-weight: 500;
    font-size: 11px;
    color: #4b7bec;
    margin-left: 6px;
}
.Content_PinnedMessage{
    color:rgb(34, 52, 81);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 90%;
}

/* Dropdown sổ xuống danh sách ghim */
.pinned-dropdown {
    position: absolute;
    top: 100%;
    left: 12px;
    right: 12px;
    background-color: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(15, 37, 71, 0.12);
    margin-top: 4px;
    max-height: 280px;
    overflow-y: auto;
    font-family: "Quicksand", sans-serif;
}
.dropdown-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    background-color: #f8fafd;
    border-bottom: 1px solid #edf2f7;
    font-size: 12.5px;
    font-weight: 600;
    color: #2d3748;
}
.dropdown-close {
    cursor: pointer;
    font-size: 13px;
    color: #a0aec0;
}
.dropdown-close:hover {
    color: #4a5568;
}
.dropdown-body {
    padding: 4px 0;
}
.dropdown-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 14px;
    cursor: pointer;
    transition: background-color 0.15s;
    border-bottom: 1px solid #f7fafc;
}
.dropdown-item:hover {
    background-color: #f3f6fd;
}
.item-left {
    display: flex;
    align-items: center;
    gap: 10px;
    flex: 1;
    min-width: 0;
}
.item-badge {
    font-size: 11px;
    font-weight: 700;
    color: #4b7bec;
    background-color: #eef2ff;
    padding: 2px 6px;
    border-radius: 4px;
}
.item-text-wrap {
    display: flex;
    flex-direction: column;
    min-width: 0;
    flex: 1;
}
.item-sender {
    font-size: 11.5px;
    font-weight: 600;
    color: #4a5568;
}
.item-content {
    font-size: 12px;
    color: #718096;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.item-unpin-btn {
    cursor: pointer;
    font-size: 11px;
    color: #cbd5e0;
    padding: 4px 6px;
    border-radius: 4px;
    transition: all 0.15s;
}
.item-unpin-btn:hover {
    color: #e01d1d;
    background-color: #fff5f5;
    font-size: 13px;
    color: #334155;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>
