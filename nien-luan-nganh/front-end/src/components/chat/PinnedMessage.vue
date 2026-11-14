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

// █████████████████████████████████████████████████████████████████████████████
// PHẦN 1: IMPORT, EMITS & STATE QUẢN LÝ DANH SÁCH GHIM
// █████████████████████████████████████████████████████████████████████████████
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { resolveFileUrl } from '@/api/storage.api';


// Phát sự kiện cuộn tới tin nhắn lên component cha (ChatView.vue)
const emit = defineEmits(['scrollToMessage'])
const chatStore = useChatStore()

// Trạng thái bật/tắt menu popup danh sách toàn bộ tin đã ghim
const showList = ref(false)
// ID tin nhắn ghim đang được người dùng bấm xem
const activePinnedId = ref(null)

watch(
  () => chatStore.activeRoomId,
  () => {
    showList.value = false
    activePinnedId.value = null
  },
)

// Tin nhắn ghim mới nhất lấy trực tiếp từ getter dùng chung của chatStore
const pinnedMsg = computed(() => chatStore.latestPinnedMessage)

// █████████████████████████████████████████████████████████████████████████████
// PHẦN 2: XỬ LÝ TƯƠNG TÁC GHIM / BỎ GHIM & CUỘN TỚI TIN NHẮN
// █████████████████████████████████████████████████████████████████████████████

/**
 * Xử lý bỏ ghim một tin nhắn bất kỳ.
 * @param {string} messageId UUID của tin nhắn cần gỡ ghim
 */
const handleUnpin = async (messageId) => {
  if (messageId) {
    await chatStore.unpinMessage(messageId)
    if (String(activePinnedId.value) === String(messageId)) {
      activePinnedId.value = null
    }
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
    activePinnedId.value = String(pinnedMsg.value.messageId)
    emit('scrollToMessage', pinnedMsg.value.messageId)
  }
}

/**
 * Khi click vào 1 mục trong menu danh sách ghim -> cuộn tới tin nhắn và TỰ ĐỘNG ĐÓNG danh sách ghim
 * để không che khuất tin nhắn và hiệu ứng nảy sáng trong khung chat
 * @param {string} messageId UUID của tin nhắn được chọn
 */
const handleItemClick = (messageId) => {
  activePinnedId.value = String(messageId)
  emit('scrollToMessage', messageId)
  showList.value = false
}

// Tự động đóng danh sách ghim khi click chuột ra bên ngoài vùng ghim
const handleDocumentClick = (e) => {
  const container = document.querySelector('.pinned-container')
  if (showList.value && container && !container.contains(e.target)) {
    showList.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
})

// █████████████████████████████████████████████████████████████████████████████
// PHẦN 3: PHÂN LOẠI & ĐỊNH DẠNG NỘI DUNG TIN NHẮN GHIM
// █████████████████████████████████████████████████████████████████████████████

const isImageMessage = (msg) => {
  if (!msg) return false
  const type = msg.messageType
  const content = msg.content || ''
  return type === 'IMAGE' || Boolean(content.match(/\.(jpg|jpeg|png|gif|webp)$/i))
}

const isVideoMessage = (msg) => {
  if (!msg) return false
  const type = msg.messageType
  const content = msg.content || ''
  return type === 'VIDEO' || Boolean(content.match(/\.(mp4|mov|webm|avi)$/i))
}

const isFileMessage = (msg) => {
  if (!msg) return false
  const type = msg.messageType
  const content = msg.content || ''
  return type === 'FILE' || type === 'DOCUMENT' || Boolean(content.match(/\.(pdf|doc|docx|xls|xlsx|txt|zip|rar)$/i))
}

const getFileName = (content) => {
  if (!content) return 'Tài liệu đính kèm'
  return content.split('/').pop() || 'Tài liệu đính kèm'
}

/**
 * Chuyển đổi nội dung hiển thị của tin ghim tùy theo messageType hoặc định dạng tệp
 */
const formatPinContent = (msg) => {
  if (!msg) return '';
  if (msg.messageType === 'REMINDER') return `[Lịch hẹn] ${msg.content || ''}`;
  if (isImageMessage(msg)) return '[Hình ảnh]';
  if (isVideoMessage(msg)) return '[Video]';
  if (isFileMessage(msg)) return `[Tệp: ${getFileName(msg.content)}]`;
  return msg.content || '';
};

</script>

<template>
<div v-if="pinnedMsg" class="pinned-container">
  <div class="chatheader">
    <div class="left-header" @click="handleClickPinned" title="Nhảy tới tin nhắn này trong khung chat">
      <div class="pin-icon-badge">
        <font-awesome-icon icon="thumbtack" class="pin-thumbtack-icon" />
      </div>
      <div class="info-group">
        <div class="pin-label-row">
          <span class="pin-label-title">Tin nhắn đã ghim</span>
          <span v-if="chatStore.pinnedMessages.length > 1" class="pin-count-badge">
            +{{ chatStore.pinnedMessages.length - 1 }} tin khác
          </span>
        </div>
        <div class="pin-content-row">
          <span class="pin-sender-name">{{ pinnedMsg.sender?.username || 'Thành viên' }}</span>
          <span class="pin-colon">:</span>

          <!-- Phân biệt rõ loại tin trên thanh ghim: Lịch hẹn, Ảnh, Video, Tệp hoặc Văn bản -->
          <span v-if="pinnedMsg.messageType === 'REMINDER'" class="pin-type-pill pin-type-pill--reminder">
            <font-awesome-icon icon="calendar-check" />
            <span class="pin-type-text">Lịch hẹn: {{ pinnedMsg.content }}</span>
          </span>
          <span v-else-if="isImageMessage(pinnedMsg)" class="pin-type-pill pin-type-pill--media">
            <font-awesome-icon icon="image" />
            <span class="pin-type-text">[Hình ảnh]</span>
          </span>
          <span v-else-if="isVideoMessage(pinnedMsg)" class="pin-type-pill pin-type-pill--media">
            <font-awesome-icon icon="video" />
            <span class="pin-type-text">[Video]</span>
          </span>
          <span v-else-if="isFileMessage(pinnedMsg)" class="pin-type-pill pin-type-pill--file">
            <font-awesome-icon icon="file-lines" />
            <span class="pin-type-text">{{ getFileName(pinnedMsg.content) }}</span>
          </span>
          <span v-else class="pin-text-content">{{ pinnedMsg.content }}</span>
        </div>
      </div>
    </div>
    <div class="right-header">
      <button 
        v-if="chatStore.pinnedMessages.length > 1" 
        type="button"
        class="pin-action-btn" 
        :class="{ 'pin-action-btn--active': showList }"
        :title="showList ? 'Đóng danh sách ghim' : 'Xem tất cả tin ghim'"
        @click.stop="showList = !showList"
      >
        <font-awesome-icon :icon="showList ? 'chevron-up' : 'chevron-down'"/>
      </button>
      <button 
        type="button"
        class="pin-action-btn pin-action-btn--unpin" 
        title="Bỏ ghim tin nhắn này" 
        @click.stop="handleUnpin(pinnedMsg?.messageId)"
      >
        <font-awesome-icon icon="x"/>
      </button>
    </div>
  </div>

  <!-- Menu sổ xuống danh sách tất cả tin nhắn ghim (Chỉ đóng khi bấm nút Thu gọn / Đóng) -->
  <div v-if="showList" class="pinned-dropdown" @click.stop>
    <div class="dropdown-header">
      <div class="dropdown-title-wrap">
        <div class="dropdown-thumbtack-circle">
          <font-awesome-icon icon="thumbtack" />
        </div>
        <span class="dropdown-title">Danh sách tin đã ghim</span>
        <span class="dropdown-count-pill">{{ chatStore.pinnedMessages.length }}/5</span>
      </div>
      <button
        type="button"
        class="dropdown-collapse-btn"
        title="Đóng danh sách ghim"
        @click.stop="showList = false"
      >
        <span>Thu gọn</span>
        <font-awesome-icon icon="chevron-up" />
      </button>
    </div>
    <div class="dropdown-body">
      <div 
        v-for="(pin, idx) in chatStore.pinnedMessages" 
        :key="pin.messageId"
        :class="['dropdown-item', { 'dropdown-item-active': String(activePinnedId) === String(pin.messageId) }]"
        @click="handleItemClick(pin.messageId)"
      >
        <div class="item-left">
          <span :class="['item-badge', { 'item-badge-active': String(activePinnedId) === String(pin.messageId) }]">#{{ idx + 1 }}</span>
          <div class="item-text-wrap">
            <div class="item-sender-row">
              <span class="item-sender">{{ pin.sender?.username || 'Thành viên' }}</span>
              <span v-if="String(activePinnedId) === String(pin.messageId)" class="active-viewing-pill">
                Đang xem
              </span>
            </div>
            <!-- Khung hiển thị trực quan chỉ có trong dropdown -->
            <div class="item-content-wrapper">
              <div v-if="pin.messageType === 'REMINDER'" class="pinned-rem-preview">
                <font-awesome-icon icon="calendar-check" class="pin-rem-icon" />
                <span class="pin-rem-text">Lịch hẹn: {{ pin.content }}</span>
              </div>
              <div v-else-if="isImageMessage(pin)" class="pinned-media-preview dropdown-media">
                <img :src="resolveFileUrl(pin.content)" alt="img" class="pinned-thumbnail" />
              </div>
              <div v-else-if="isVideoMessage(pin)" class="pinned-media-preview dropdown-media">
                <video :src="resolveFileUrl(pin.content)" class="pinned-thumbnail" muted></video>
                <span class="pin-play-icon"><font-awesome-icon icon="play" /></span>
              </div>
              <div v-else-if="isFileMessage(pin)" class="pinned-file-preview">
                <font-awesome-icon icon="file-lines" class="pin-file-icon" />
                <span class="pin-file-name">{{ getFileName(pin.content) }}</span>
              </div>
              <span v-else class="item-content">{{ formatPinContent(pin) }}</span>
            </div>
          </div>
        </div>
        <button 
          type="button"
          class="item-unpin-btn" 
          title="Bỏ ghim" 
          @click.stop="handleUnpin(pin.messageId)"
        >
          <font-awesome-icon icon="x"/>
        </button>
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
  background: linear-gradient(180deg, #f8faff 0%, #edf3fc 100%);
  border-bottom: 1px solid #dce6f6;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.chatheader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  gap: 12px;
  min-height: 48px;
  box-sizing: border-box;
}

.left-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.18s ease;
}

.left-header:hover {
  background-color: rgba(37, 99, 235, 0.05);
}

.pin-icon-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  font-size: 13px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.28);
}

.pin-thumbtack-icon {
  transform: rotate(-25deg);
}

.info-group {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
  gap: 2px;
  font-family: 'Quicksand', sans-serif;
}

.pin-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.2;
}

.pin-label-title {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #2563eb;
}

.pin-count-badge {
  font-size: 10.5px;
  font-weight: 700;
  color: #1d4ed8;
  background-color: #dbeafe;
  border: 1px solid #bfdbfe;
  padding: 1px 7px;
  border-radius: 999px;
  line-height: 1.35;
}

.pin-content-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  font-size: 12.5px;
  color: #1e293b;
  line-height: 1.3;
}

.pin-sender-name {
  font-weight: 700;
  color: #1e293b;
  flex-shrink: 0;
}

.pin-colon {
  font-weight: 600;
  color: #64748b;
  flex-shrink: 0;
  margin-right: 2px;
}

.pin-text-content {
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.pin-type-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 1px 8px;
  border-radius: 5px;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.pin-type-pill--media {
  background-color: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

.pin-type-pill--file {
  background-color: #f1f5f9;
  color: #334155;
  border: 1px solid #cbd5e1;
}

.pin-type-pill--reminder {
  background-color: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

.pin-type-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.right-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.pin-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid #dbe4f3;
  background-color: #ffffff;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.pin-action-btn:hover {
  background-color: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
  transform: scale(1.05);
}

.pin-action-btn--active {
  background-color: #2563eb;
  border-color: #2563eb;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
}

.pin-action-btn--active:hover {
  background-color: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
}

.pin-action-btn--unpin:hover {
  background-color: #fef2f2;
  border-color: #fca5a5;
  color: #dc2626;
}

/* Dropdown sổ xuống danh sách ghim */
.pinned-dropdown {
  position: absolute;
  top: 100%;
  left: 16px;
  right: 16px;
  background-color: #ffffff;
  border: 1px solid #dbe4f3;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(15, 37, 71, 0.12), 0 2px 8px rgba(15, 37, 71, 0.04);
  margin-top: 6px;
  max-height: 420px;
  overflow-y: auto;
  font-family: 'Quicksand', sans-serif;
  z-index: 50;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: linear-gradient(180deg, #f8fafd 0%, #f1f5fa 100%);
  border-bottom: 1px solid #e2e8f0;
  border-radius: 12px 12px 0 0;
}

.dropdown-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dropdown-thumbtack-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background-color: #dbeafe;
  color: #2563eb;
  font-size: 11px;
}

.dropdown-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #1e293b;
}

.dropdown-count-pill {
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 1px 7px;
  border-radius: 999px;
}

.dropdown-collapse-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid #dbe4f3;
  background-color: #ffffff;
  color: #3b66d4;
  font-family: 'Quicksand', sans-serif;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dropdown-collapse-btn:hover {
  background-color: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.dropdown-body {
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.dropdown-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  border-radius: 8px;
  border: 1px solid transparent;
  border-left: 3px solid transparent;
}

.dropdown-item:hover {
  background-color: #f3f6fd;
  border-color: #e2e8f0;
}

.dropdown-item-active {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.12) 0%, rgba(239, 246, 255, 0.75) 100%);
  border-left-color: #2563eb;
  border-color: #bfdbfe;
}

.dropdown-item-active:hover {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.16) 0%, rgba(239, 246, 255, 0.9) 100%);
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
  padding: 2px 7px;
  border-radius: 5px;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.item-badge-active {
  background-color: #2563eb;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.28);
}

.item-text-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.item-sender-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.item-sender {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.dropdown-item-active .item-sender {
  color: #1e40af;
  font-weight: 700;
}

.active-viewing-pill {
  font-size: 10px;
  font-weight: 700;
  color: #1d4ed8;
  background-color: #dbeafe;
  padding: 1px 6px;
  border-radius: 999px;
  line-height: 1.4;
  border: 1px solid #bfdbfe;
}

.item-content {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-item-active .item-content {
  color: #1e293b;
  font-weight: 600;
}

.item-unpin-btn {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: none;
  background: transparent;
  font-size: 11.5px;
  color: #94a3b8;
  transition: all 0.15s ease;
  flex-shrink: 0;
  margin-left: 8px;
}

.item-unpin-btn:hover {
  background-color: #fee2e2;
  color: #dc2626;
  transform: scale(1.08);
}

/* Khung hiển thị chi tiết nội dung đính kèm trong dropdown */
.item-content-wrapper {
  display: flex;
  align-items: center;
  min-width: 0;
  margin-top: 2px;
}

.pinned-rem-preview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #fef3c7;
  border: 1px solid #fde68a;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11.5px;
  color: #92400e;
  font-weight: 600;
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 3px;
}

.pin-rem-icon {
  color: #d97706;
  font-size: 12px;
  flex-shrink: 0;
}

.pin-rem-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pinned-media-preview {
  position: relative;
  max-width: 150px;
  border-radius: 6px;
  overflow: hidden;
  background: #f1f5f9;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #cbd5e1;
  margin-top: 4px;
}

.pinned-thumbnail {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  display: block;
}

.pin-play-icon {
  position: absolute;
  font-size: 9px;
  color: #fff;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pinned-file-preview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11.5px;
  color: #1e293b;
  max-width: 220px;
}

.pin-file-icon {
  color: #3b82f6;
  font-size: 12px;
  flex-shrink: 0;
}

.pin-file-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
