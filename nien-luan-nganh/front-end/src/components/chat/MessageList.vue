<script setup>
/**
 * =========================================================================
 * Component: MessageList.vue
 * =========================================================================
 * - Hiển thị từng dòng tin nhắn trong khung chat (bong bóng chat, tên người gửi, thời gian).
 * - Nút 3 chấm (...) và nút Ghim nằm ngang hàng ngay sau/bên cạnh bong bóng tin nhắn (hiện khi hover).
 * - Giao diện chỉnh sửa inline đẹp mắt, giữ nguyên hình dạng và màu sắc của bong bóng tin nhắn bình thường.
 * - Hỗ trợ xem lịch sử các lần chỉnh sửa tin nhắn.
 */
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import MessageBubble from './MessageBubble.vue'

const props = defineProps({
  // Mốc thời gian gửi tin nhắn (vd: 14:30)
  Time: {
    type: String,
    default: '12:35',
  },
  // Nội dung văn bản của tin nhắn
  Content_Message: {
    type: String,
    default: '',
  },
  // Tên người gửi (ẩn đi nếu là tin nhắn của chính mình)
  Sender: {
    type: String,
    default: 'Minh Anh',
  },
  // Kiểu hiển thị: 'default' (người khác) hoặc 'SendTo' (chính mình)
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'SendTo'].includes(v),
  },
  // Khóa chính UUID của tin nhắn
  messageId: {
    type: String,
    default: '',
  },
  // Trạng thái tin nhắn đã được ghim hay chưa
  isPinned: {
    type: Boolean,
    default: false,
  },
  // Trạng thái tin nhắn đã bị thu hồi hay chưa
  isRevoked: {
    type: Boolean,
    default: false,
  },
  // Tin nhắn liên tiếp từ cùng 1 người gửi (ẩn avatar, ẩn tên, thu hẹp khoảng cách)
  isConsecutive: {
    type: Boolean,
    default: false,
  },
  // Hiển thị mốc thời gian (chỉ hiển thị ở tin nhắn cuối của cụm tin nhắn liên tiếp)
  showTime: {
    type: Boolean,
    default: true,
  },
  // Vị trí của tin nhắn trong cụm tin nhắn liên tiếp ('single', 'first', 'middle', 'last')
  groupPosition: {
    type: String,
    default: 'single',
    validator: (v) => ['single', 'first', 'middle', 'last'].includes(v),
  },
  // Trạng thái tin nhắn đã từng được chỉnh sửa hay chưa
  isEdited: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['togglePin', 'editMessage', 'revokeMessage', 'viewHistory'])

const showMenu = ref(false) // Trạng thái mở menu 3 chấm
const isEditing = ref(false) // Trạng thái đang chỉnh sửa inline
const editContent = ref('') // Nội dung văn bản đang sửa
const editTextareaRef = ref(null) // Ref tới ô textarea sửa

// Tự động điều chỉnh chiều cao textarea theo nội dung gõ
const autoResize = () => {
  nextTick(() => {
    if (editTextareaRef.value) {
      editTextareaRef.value.style.height = 'auto'
      editTextareaRef.value.style.height = editTextareaRef.value.scrollHeight + 'px'
    }
  })
}

// Bắt đầu chỉnh sửa
const handleStartEdit = () => {
  showMenu.value = false
  editContent.value = props.Content_Message
  isEditing.value = true
  nextTick(() => {
    if (editTextareaRef.value) {
      editTextareaRef.value.focus()
      autoResize()
    }
  })
}

// Lưu nội dung đã sửa
const handleSaveEdit = () => {
  const trimmed = editContent.value.trim()
  if (trimmed && trimmed !== props.Content_Message) {
    emit('editMessage', { messageId: props.messageId, content: trimmed })
  }
  isEditing.value = false
}

// Hủy chỉnh sửa
const handleCancelEdit = () => {
  isEditing.value = false
  editContent.value = ''
}

// Kích hoạt thu hồi tin nhắn
const handleRevoke = () => {
  showMenu.value = false
  emit('revokeMessage', props.messageId)
}

// Mở popup lịch sử chỉnh sửa
const handleViewHistory = () => {
  showMenu.value = false
  emit('viewHistory', {
    messageId: props.messageId,
    content: props.Content_Message,
    sender: props.Sender || 'Bạn',
  })
}

// Đóng menu 3 chấm khi click bên ngoài
const handleDocumentClick = () => {
  showMenu.value = false
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<template>
  <div
    :id="messageId ? 'msg-' + messageId : null"
    :class="['container', `container--${variant}`, { 'container--consecutive': isConsecutive }]"
  >
    <MessageBubble v-if="!isConsecutive" :type="variant"></MessageBubble>
    <div v-else-if="variant !== 'SendTo'" class="bubble-spacer"></div>
    <div
      :class="[
        'wrapper',
        `wrapper--${variant}`,
        { 'wrapper--consecutive': isConsecutive, 'wrapper--editing': isEditing },
      ]"
    >
      <div class="sender" v-if="Sender && !isConsecutive">{{ Sender }}</div>

      <!-- Hàng bong bóng tin nhắn kèm nút thao tác nằm ngang hàng ngay bên cạnh -->
      <div
        class="bubble-row"
        :class="[`bubble-row--${variant}`, { 'bubble-row--editing': isEditing }]"
      >
        <!-- 1. Trạng thái tin nhắn đã bị thu hồi -->
        <div v-if="isRevoked" :class="['message', 'message--revoked']">
          <i>Tin nhắn đã được thu hồi</i>
        </div>

        <!-- 2. Trạng thái đang chỉnh sửa: giữ nguyên màu sắc & phong cách bong bóng bình thường -->
        <div v-else-if="isEditing" :class="['message', `message--${variant}`, 'message--editing']">
          <textarea
            ref="editTextareaRef"
            v-model="editContent"
            class="inline-edit-textarea"
            rows="1"
            @input="autoResize"
            @keydown.enter.exact.prevent="handleSaveEdit"
            @keydown.esc="handleCancelEdit"
          ></textarea>
          <div class="edit-toolbar">
            <span class="edit-hint"></span>
            <div class="edit-btn-group">
              <button class="edit-mini-btn btn-save" title="Lưu" @click="handleSaveEdit">
                <font-awesome-icon icon="check" />
              </button>
              <button class="edit-mini-btn btn-cancel" title="Hủy" @click="handleCancelEdit">
                <font-awesome-icon icon="x" />
              </button>
            </div>
          </div>
        </div>

        <!-- 3. Tin nhắn bình thường -->
        <div
          v-else
          :class="['message', `message--${variant}`, `message--pos-${groupPosition}`]"
          @dblclick="variant === 'SendTo' ? handleStartEdit() : null"
          :title="variant === 'SendTo' ? 'Bấm 2 lần để sửa nhanh' : ''"
        >
          {{ Content_Message }}
        </div>

        <!-- Nút ... và nút Ghim để NGAY BÊN CẠNH / SAU TIN NHẮN (hiển thị khi hover) -->
        <div v-if="!isRevoked" class="message-actions" :class="`message-actions--${variant}`">
          <!-- Nút Ghim -->
          <span
            class="action-btn pin-btn"
            :class="{ 'is-pinned': isPinned }"
            :title="isPinned ? 'Bỏ ghim tin nhắn' : 'Ghim tin nhắn'"
            @click="emit('togglePin', messageId)"
          >
            <font-awesome-icon icon="thumbtack" />
          </span>

          <!-- Nút 3 chấm (...) -->
          <div class="menu-container">
            <span
              class="action-btn more-btn"
              title="Tùy chọn tin nhắn"
              @click.stop="showMenu = !showMenu"
            >
              <font-awesome-icon icon="ellipsis-vertical" />
            </span>

            <!-- Menu xổ xuống -->
            <div
              v-if="showMenu"
              class="action-dropdown"
              :class="`action-dropdown--${variant}`"
              @click.stop
            >
              <div v-if="variant === 'SendTo'" class="dropdown-opt" @click="handleStartEdit">
                <font-awesome-icon icon="pen" class="opt-icon" />
                <span>Chỉnh sửa</span>
              </div>
              <div v-if="isEdited" class="dropdown-opt" @click="handleViewHistory">
                <font-awesome-icon icon="clock-rotate-left" class="opt-icon" />
                <span>Xem lịch sử</span>
              </div>
              <div
                v-if="variant === 'SendTo'"
                class="dropdown-opt opt-danger"
                @click="handleRevoke"
              >
                <font-awesome-icon icon="trash" class="opt-icon" />
                <span>Thu hồi</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Dòng thời gian phía dưới (kèm nhãn 'đã chỉnh sửa') -->
      <div
        v-if="showTime || (isEdited && !isRevoked)"
        class="time-row"
        :class="`time-row--${variant}`"
      >
        <div v-if="showTime" :class="['time', `time--${variant}`]">{{ Time }}</div>
        <span
          v-if="isEdited && !isRevoked"
          class="edited-badge"
          title="Bấm để xem lịch sử chỉnh sửa"
          @click="handleViewHistory"
        >
          Đã chỉnh sửa
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.container {
  display: flex;
  width: 100%;
  flex-direction: row;
}

.container--SendTo {
  flex-direction: row-reverse;
}

.container--consecutive {
  margin-top: 0;
}

.container--consecutive .wrapper {
  margin-top: 0 !important;
}

.container--consecutive .message {
  margin-top: 2px !important;
}

.container--consecutive .bubble-spacer {
  height: 0;
}

.wrapper {
  display: flex;
  flex-direction: column;
  margin-top: 10px;
  max-width: 75%;
}

.wrapper--SendTo {
  display: flex;
  align-items: flex-end;
  max-width: 75%;
}

.wrapper--consecutive {
  margin-top: 0 !important;
}

.wrapper--editing {
  width: 75% !important;
  max-width: 75% !important;
}

.bubble-spacer {
  width: 37px;
  height: 1px;
  margin-left: 10px;
  margin-right: 6px;
  flex-shrink: 0;
}

/* Hàng ngang chứa bong bóng chat và các nút thao tác bên cạnh */
.bubble-row {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  max-width: 100%;
}

.bubble-row--editing {
  width: 100% !important;
}

.bubble-row--SendTo {
  flex-direction: row-reverse;
}

.message {
  display: inline-block;
  font-family: 'Quicksand', sans-serif;
  background-color: white;
  width: fit-content;
  max-width: 100%;
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: pre-wrap;
  height: fit-content;
  border: 1px solid #e6eaf2;
  border-radius: 0.2rem 1.2rem 1.2rem;
  margin-top: 5px;
  padding: 12px 16px;
  color: #1e293b;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.message--SendTo {
  color: white;
  background-color: #4b7bec;
  border-radius: 18px 4px 18px 18px;
}

/* --- Bo góc bong bóng chuẩn Facebook Messenger theo vị trí trong cụm tin nhắn --- */
/* 1. Tin nhắn của đối phương (bên trái) */
.message--pos-first {
  border-radius: 18px 18px 18px 4px !important;
}

.message--pos-middle {
  border-radius: 4px 18px 18px 4px !important;
}

.message--pos-last {
  border-radius: 4px 18px 18px 18px !important;
}

/* 2. Tin nhắn của chính mình (bên phải) */
.message--SendTo.message--pos-first {
  border-radius: 18px 18px 4px 18px !important;
}

.message--SendTo.message--pos-middle {
  border-radius: 18px 4px 4px 18px !important;
}

.message--SendTo.message--pos-last {
  border-radius: 18px 4px 18px 18px !important;
}

/* Giao diện chỉnh sửa tin nhắn inline: kéo dài bằng chiều rộng tin nhắn đầy đủ */
.message--editing {
  width: 100% !important;
  min-width: 280px;
  max-width: 100% !important;
  padding: 10px 14px;
  box-shadow: 0 0 0 2px rgba(75, 123, 236, 0.4);
}

.inline-edit-textarea {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: inherit;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.4;
  resize: none;
  box-sizing: border-box;
  display: block;
}

.edit-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  padding-top: 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.25);
  gap: 8px;
}

.message:not(.message--SendTo) .edit-toolbar {
  border-top-color: rgba(0, 0, 0, 0.08);
}

.edit-hint {
  font-size: 10.5px;
  opacity: 0.8;
  user-select: none;
}

.edit-btn-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.edit-mini-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 11px;
  line-height: 1;
  padding: 0;
  transition: all 0.15s;
  vertical-align: middle;
}

.btn-save {
  background-color: rgba(255, 255, 255, 0.95);
  color: #2563eb;
}

.btn-save:hover {
  background-color: #ffffff;
  transform: scale(1.1);
}

.btn-cancel {
  background-color: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

.message:not(.message--SendTo) .btn-cancel {
  background-color: rgba(0, 0, 0, 0.06);
  color: #64748b;
}

.btn-cancel:hover {
  background-color: rgba(255, 255, 255, 0.45);
  transform: scale(1.1);
}

/* Tin nhắn đã bị thu hồi */
.message--revoked {
  background-color: #f1f5f9 !important;
  color: #94a3b8 !important;
  border: 1px dashed #cbd5e1 !important;
  font-size: 13px;
  border-radius: 12px !important;
}

/* Các nút thao tác (Ghim, ...) nằm ngang hàng cạnh bong bóng */
.message-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s ease;
  flex-shrink: 0;
}

.bubble-row:hover .message-actions {
  opacity: 1;
}

.action-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 11.5px;
  color: #94a3b8;
  border-radius: 50%;
  transition: all 0.15s ease;
}

.action-btn:hover {
  background-color: #e2e8f0;
  color: #1e293b;
}

.pin-btn.is-pinned {
  color: #4b7bec;
  opacity: 1;
}

/* Menu xổ xuống của nút 3 chấm (...) */
.menu-container {
  position: relative;
  display: inline-flex;
}

.action-dropdown {
  position: absolute;
  top: 100%;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  min-width: 140px;
  z-index: 50;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
}

.action-dropdown--SendTo {
  right: 0;
}

.action-dropdown--default {
  left: 0;
}

.dropdown-opt {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  font-size: 12px;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  color: #334155;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
  white-space: nowrap;
}

.dropdown-opt:hover {
  background-color: #f1f5f9;
}

.opt-danger {
  color: #ef4444;
}

.opt-danger:hover {
  background-color: #fee2e2;
}

.opt-icon {
  font-size: 11px;
}

/* Dòng thời gian phía dưới */
.time-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 3px;
}

.time-row--SendTo {
  flex-direction: row-reverse;
}

.time {
  display: flex;
  font-family: 'Quicksand', sans-serif;
  margin-left: 10px;
  font-size: 11px;
  color: #a0aec0;
}

.time--SendTo {
  margin-left: auto;
  margin-right: 10px;
}

.edited-badge {
  font-size: 11px;
  font-family: 'Quicksand', sans-serif;
  color: #94a3b8;
  cursor: pointer;
  transition: color 0.15s;
  user-select: none;
}

.edited-badge:hover {
  color: #4b7bec;
  text-decoration: underline;
}

.sender {
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  margin-left: 7px;
  margin-top: 10px;
}

.highlight-msg .message {
  animation: flashMessage 1.5s ease-out;
}

@keyframes flashMessage {
  0%,
  100% {
    filter: brightness(1);
  }
  30% {
    filter: brightness(1.25);
    box-shadow: 0 0 12px rgba(75, 123, 236, 0.5);
  }
}
</style>
