<template>
  <aside class="search-sidebar">
    <div class="search-sidebar-inner">
      <!-- Header Sidebar -->
      <div class="sidebar-header">
        <div class="header-title">
          
          <h3>Tìm kiếm trong trò chuyện</h3>
        </div>
        <button class="btn-close" title="Đóng (Esc)" @click="$emit('close')">
          <font-awesome-icon icon="x" />
        </button>
      </div>

      <!-- Khung Input tìm kiếm -->
      <div class="search-box-wrapper">
        <div class="search-input-inner">
          <font-awesome-icon icon="magnifying-glass" class="input-icon" />
          <input
            ref="searchInputRef"
            v-model="keyword"
            type="text"
            placeholder="Nhập từ khóa tìm kiếm..."
            @input="onSearchInput"
            @keydown.esc="$emit('close')"
          />
          <button v-if="keyword" class="btn-clear" title="Xóa từ khóa" @click="clearSearch">
            <font-awesome-icon icon="x" />
          </button>
        </div>
      </div>

      <!-- Trạng thái số lượng kết quả -->
      <div class="search-status">
        <span v-if="isSearching" class="status-loading">
          <span class="mini-spinner"></span>
          Đang tìm kiếm...
        </span>
        <span v-else-if="hasSearched" class="status-count">
          Tìm thấy <strong>{{ results.length }}</strong> tin nhắn
        </span>
      </div>

      <!-- Danh sách tin nhắn khớp -->
      <div class="search-results-list">
        <div
          v-for="item in results"
          :key="item.messageId"
          class="search-item-card"
          @click="selectMessage(item.messageId)"
        >
          <!-- Avatar người gửi -->
          <UserAvatar
            :name="item.senderUsername"
            :src="item.senderAvatarUrl"
            :size="36"
          />

          <div class="item-body">
            <div class="item-header">
              <span class="sender-name">{{ item.senderUsername }}</span>
              <span class="send-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <!-- Bôi vàng từ khóa trùng khớp an toàn -->
            <p class="snippet-content" v-html="highlightKeyword(item.content, keyword)"></p>
          </div>
        </div>

        <!-- Trạng thái trống -->
        <div v-if="hasSearched && !isSearching && results.length === 0" class="empty-state">
          <font-awesome-icon icon="message" class="empty-icon" />
          <p>Không tìm thấy tin nhắn phù hợp</p>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { searchMessagesSidebar } from '@/api/message.api';
import UserAvatar from '@/components/common/UserAvatar.vue';

const props = defineProps({
  roomId: {
    type: [String, Number],
    required: true,
  },
});

const emit = defineEmits(['close', 'jump-to-message', 'update-keyword']);

const keyword = ref('');
const results = ref([]);
const isSearching = ref(false);
const hasSearched = ref(false);
const searchInputRef = ref(null);
let debounceTimer = null;

onMounted(() => {
  // Trì hoãn focus nhẹ (120ms) để CSS Transition mở hoàn toàn êm ái trên GPU trước khi nháy con trỏ
  setTimeout(() => {
    searchInputRef.value?.focus();
  }, 120);
});

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer);
  emit('update-keyword', '');
});

// Cơ chế Debounce 300ms thuần tránh phụ thuộc lodash
const onSearchInput = () => {
  const q = keyword.value.trim();
  emit('update-keyword', q);

  if (debounceTimer) clearTimeout(debounceTimer);

  if (!q) {
    results.value = [];
    hasSearched.value = false;
    isSearching.value = false;
    return;
  }

  isSearching.value = true;
  hasSearched.value = true;

  debounceTimer = setTimeout(async () => {
    try {
      const res = await searchMessagesSidebar(props.roomId, q);
      const list = res?.data || res || [];
      results.value = Array.isArray(list) ? list : [];
    } catch (err) {
      console.error('Lỗi tìm kiếm tin nhắn sidebar:', err);
      results.value = [];
    } finally {
      isSearching.value = false;
    }
  }, 300);
};

const clearSearch = () => {
  keyword.value = '';
  results.value = [];
  hasSearched.value = false;
  emit('update-keyword', '');
  searchInputRef.value?.focus();
};

// Chọn tin nhắn -> Gửi ID ra ngoài để khung chat chính nhảy tới và chớp sáng
const selectMessage = (messageId) => {
  if (!messageId) return;
  emit('jump-to-message', messageId);
};

// Hàm escape ký tự đặc biệt chống tấn công XSS
const escapeHtml = (text) => {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
};

// Hàm bôi vàng từ khóa an toàn
const highlightKeyword = (text, term) => {
  if (!text) return '';
  const escapedText = escapeHtml(text);
  if (!term || !term.trim()) return escapedText;
  const escapedTerm = escapeHtml(term.trim()).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const regex = new RegExp(`(${escapedTerm})`, 'gi');
  return escapedText.replace(regex, '<mark class="search-highlight">$1</mark>');
};

// Format thời gian hiển thị (ví dụ: 15:45 - 04/11)
const formatTime = (isoString) => {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '';
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    return `${hours}:${minutes} ${day}/${month}`;
  } catch {
    return '';
  }
};

// Đóng và reset khi chuyển phòng chat khác
watch(
  () => props.roomId,
  () => {
    clearSearch();
    emit('close');
  }
);
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700&display=swap');

.search-sidebar {
  width: 330px;
  min-width: 330px;
  max-width: 330px;
  height: 100%;
  background: #ffffff;
  border-left: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  z-index: 25;
  font-family: 'Quicksand', sans-serif;
  box-sizing: border-box;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #f1f5f9;
  min-height: 60px;
  box-sizing: border-box;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 15px;
  color: #3b82f6;
}

.header-title h3 {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 13px;
  cursor: pointer;
  color: #94a3b8;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.btn-close:hover {
  background-color: #fee2e2;
  color: #ef4444;
}

.search-box-wrapper {
  padding: 12px 16px 8px;
}

.search-input-inner {
  position: relative;
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 20px;
  padding: 6px 12px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.search-input-inner:focus-within {
  border-color: #3b82f6;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.input-icon {
  font-size: 13px;
  color: #94a3b8;
  margin-right: 8px;
  flex-shrink: 0;
}

.search-input-inner input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: #1e293b;
}

.search-input-inner input::placeholder {
  color: #94a3b8;
}

.btn-clear {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #94a3b8;
  padding: 2px 4px;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: color 0.15s ease;
}

.btn-clear:hover {
  color: #ef4444;
}

.search-status {
  padding: 4px 18px 8px;
  font-size: 12px;
  color: #64748b;
  min-height: 22px;
}

.status-loading {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #3b82f6;
  font-weight: 600;
}

.mini-spinner {
  width: 11px;
  height: 11px;
  border: 2px solid #bfdbfe;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.status-count strong {
  color: #2563eb;
  font-weight: 700;
}

.search-results-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px 16px;
}

.search-results-list::-webkit-scrollbar {
  width: 5px;
}

.search-results-list::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 4px;
}

.search-item-card {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  margin-bottom: 6px;
  border: 1px solid transparent;
}

.search-item-card:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
  transform: translateY(-1px);
}

.item-body {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3px;
}

.sender-name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.send-time {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
  font-weight: 500;
}

.snippet-content {
  font-size: 12.5px;
  color: #475569;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
  line-height: 1.4;
}

:deep(.search-highlight) {
  background-color: #fef08a;
  color: #854d0e;
  padding: 0 2px;
  border-radius: 3px;
  font-weight: 700;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
  margin-top: 60px;
  gap: 8px;
}

.empty-icon {
  font-size: 28px;
  color: #cbd5e1;
}
</style>

