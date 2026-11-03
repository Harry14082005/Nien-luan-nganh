<script setup>
/**
 * =========================================================================
 * Component: MessageHistoryModal.vue
 * =========================================================================
 * - Hiển thị popup lịch sử các lần chỉnh sửa của tin nhắn.
 * - Danh sách bao gồm phiên bản hiện tại và toàn bộ các phiên bản trước đó kèm thời gian.
 */
defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  historyList: {
    type: Array,
    default: () => [],
  },
  currentContent: {
    type: String,
    default: '',
  },
  senderName: {
    type: String,
    default: 'Thành viên',
  },
});

const emit = defineEmits(['close']);

const formatDateTime = (isoString) => {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '';
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    return `${hours}:${minutes} - ${day}/${month}/${year}`;
  } catch {
    return '';
  }
};
</script>

<template>
  <div v-if="show" class="modal-backdrop" @click="emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <div class="modal-title">
          <font-awesome-icon icon="clock-rotate-left" class="title-icon" />
          <span>Lịch sử chỉnh sửa</span>
        </div>
        <button class="btn-close" @click="emit('close')" title="Đóng">✕</button>
      </div>

      <div class="modal-body">
        <!-- Phiên bản hiện tại -->
        <div class="history-item current-version">
          <div class="item-header">
            <span class="version-tag tag-current">Phiên bản hiện tại</span>
          </div>
          <div class="item-text">{{ currentContent }}</div>
        </div>

        <div class="divider">
          <span>Các lần sửa trước</span>
        </div>

        <!-- Danh sách phiên bản cũ -->
        <div v-if="historyList.length === 0" class="empty-history">
          Không có dữ liệu các lần sửa cũ.
        </div>
        <div 
          v-else 
          v-for="(hist, idx) in historyList" 
          :key="hist.messageHistoryId || idx" 
          class="history-item past-version"
        >
          <div class="item-header">
            <span class="version-tag">Lần sửa #{{ historyList.length - idx }}</span>
            <span class="edited-time">{{ formatDateTime(hist.editedAt) }}</span>
          </div>
          <div class="item-text">{{ hist.content }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
  font-family: 'Quicksand', sans-serif;
}

.modal-content {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 480px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 16px 36px rgba(15, 37, 71, 0.16);
  overflow: hidden;
  animation: modalFadeIn 0.2s ease-out;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #eef2f7;
  background-color: #f8fafc;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.title-icon {
  color: #4b7bec;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #94a3b8;
  padding: 4px;
  border-radius: 4px;
  transition: color 0.15s;
}

.btn-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 16px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  padding: 12px 14px;
  border-radius: 10px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
}

.current-version {
  background-color: #eff6ff;
  border-color: #bfdbfe;
}

.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.version-tag {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  background: #e2e8f0;
  padding: 2px 7px;
  border-radius: 4px;
}

.tag-current {
  background: #3b82f6;
  color: #ffffff;
}

.edited-time {
  font-size: 11px;
  color: #94a3b8;
}

.item-text {
  font-size: 13.5px;
  color: #1e293b;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}

.divider {
  display: flex;
  align-items: center;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
  margin: 4px 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px dashed #cbd5e1;
}

.divider span {
  padding: 0 10px;
}

.empty-history {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  padding: 16px 0;
}
</style>

