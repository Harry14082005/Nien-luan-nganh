<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { searchMessages } from '@/api/message.api';

const props = defineProps({
  roomId: {
    type: [String, Number],
    required: true,
  },
});

const emit = defineEmits(['close', 'jumpToMessage', 'updateKeyword']);

const searchInputRef = ref(null);
const query = ref('');
const matchIds = ref([]);
const totalMatches = ref(0);
const currentIndex = ref(-1);
const isLoading = ref(false);
const hasSearched = ref(false);
let debounceTimer = null;

// Tự động focus ô tìm kiếm khi component xuất hiện
onMounted(() => {
  nextTick(() => {
    searchInputRef.value?.focus();
  });
});

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer);
  emit('updateKeyword', '');
});

// Xử lý tìm kiếm với cơ chế Debounce 300ms
const handleInput = () => {
  const trimmed = query.value.trim();
  emit('updateKeyword', trimmed);

  if (debounceTimer) clearTimeout(debounceTimer);

  if (!trimmed) {
    matchIds.value = [];
    totalMatches.value = 0;
    currentIndex.value = -1;
    hasSearched.value = false;
    isLoading.value = false;
    return;
  }

  isLoading.value = true;
  debounceTimer = setTimeout(async () => {
    await performSearch(trimmed);
  }, 300);
};

// Gọi API tìm kiếm tin nhắn (Lightweight Metadata: totalMatches, matchIds)
const performSearch = async (searchTerm) => {
  if (!props.roomId || !searchTerm) {
    isLoading.value = false;
    return;
  }

  try {
    const res = await searchMessages(props.roomId, searchTerm);
    // Hỗ trợ linh hoạt cấu trúc phản hồi từ Axios/Backend (data.data hoặc data)
    const payload = res?.data?.matchIds ? res.data : (res?.matchIds ? res : (res?.data || res || {}));
    const rawIds = Array.isArray(payload.matchIds) ? payload.matchIds : [];
    // Backend trả về DESC (mới nhất ở index 0, cũ nhất ở cuối).
    // Đảo ngược mảng để sắp xếp theo dòng thời gian tự nhiên từ trên xuống dưới (cũ nhất = 1/N -> mới nhất = N/N)
    const ids = [...rawIds].reverse();
    const total = payload.totalMatches !== undefined ? payload.totalMatches : ids.length;

    matchIds.value = ids;
    totalMatches.value = total;
    hasSearched.value = true;

    if (ids.length > 0) {
      // Mặc định nhảy tới tin nhắn mới nhất / gần nhất đầu tiên (kết quả cuối dòng thời gian, ví dụ 3/3: "Đẹp nè")
      currentIndex.value = ids.length - 1;
      emit('jumpToMessage', ids[currentIndex.value]);
    } else {
      currentIndex.value = -1;
    }
  } catch (err) {
    console.error('Lỗi tìm kiếm tin nhắn:', err);
    if (err?.response?.data) {
      console.error('Chi tiết lỗi phản hồi từ Server:', err.response.data);
    }
    matchIds.value = [];
    totalMatches.value = 0;
    currentIndex.value = -1;
  } finally {
    isLoading.value = false;
  }
};

// Chuyển tới tin nhắn cũ hơn (lên trên trong lịch sử chat: 3/3 -> 2/3 -> 1/3)
const handlePrev = () => {
  if (matchIds.value.length === 0) return;
  currentIndex.value = (currentIndex.value - 1 + matchIds.value.length) % matchIds.value.length;
  emit('jumpToMessage', matchIds.value[currentIndex.value]);
};

// Chuyển tới tin nhắn mới hơn (xuống dưới trong lịch sử chat: 1/3 -> 2/3 -> 3/3)
const handleNext = () => {
  if (matchIds.value.length === 0) return;
  currentIndex.value = (currentIndex.value + 1) % matchIds.value.length;
  emit('jumpToMessage', matchIds.value[currentIndex.value]);
};

// Xử lý phím tắt trong ô tìm kiếm
const handleKeyDown = (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    if (e.shiftKey) {
      // Shift + Enter: Xuôi xuống tin mới hơn
      handleNext();
    } else {
      // Enter: Ngược lên tin cũ hơn (3/3 -> 2/3 -> 1/3)
      handlePrev();
    }
  } else if (e.key === 'Escape') {
    e.preventDefault();
    handleClose();
  }
};

const handleClose = () => {
  query.value = '';
  matchIds.value = [];
  totalMatches.value = 0;
  currentIndex.value = -1;
  emit('updateKeyword', '');
  emit('close');
};

// Reset khi chuyển phòng khác
watch(
  () => props.roomId,
  () => {
    handleClose();
  }
);

// Nhãn hiển thị số lượng (vd: "1 / 12", "0 kết quả")
const counterText = computed(() => {
  if (isLoading.value) return 'Đang tìm...';
  if (!hasSearched.value || !query.value.trim()) return '';
  if (totalMatches.value === 0) return '0 kết quả';
  return `${currentIndex.value + 1} / ${totalMatches.value}`;
});
</script>

<template>
  <div class="message-search-bar">
    <div class="search-input-wrapper">
      <font-awesome-icon icon="magnifying-glass" class="search-icon" />
      <input
        ref="searchInputRef"
        v-model="query"
        type="text"
        class="search-input"
        placeholder="Tìm kiếm trong đoạn chat..."
        @input="handleInput"
        @keydown="handleKeyDown"
      />
      <div v-if="isLoading" class="search-spinner"></div>
      <span v-else-if="counterText" class="search-counter" :class="{ 'no-match': totalMatches === 0 }">
        {{ counterText }}
      </span>
    </div>

    <!-- Cụm nút điều hướng lên/xuống -->
    <div class="search-actions">
      <button
        class="nav-btn"
        :disabled="totalMatches <= 1"
        title="Tin cũ hơn (Enter hoặc ▲)"
        @click="handlePrev"
      >
        <font-awesome-icon icon="chevron-up" />
      </button>

      <button
        class="nav-btn"
        :disabled="totalMatches <= 1"
        title="Tin mới hơn (Shift+Enter hoặc ▼)"
        @click="handleNext"
      >
        <font-awesome-icon icon="chevron-down" />
      </button>

      <div class="divider"></div>

      <!-- Nút đóng tìm kiếm -->
      <button class="nav-btn close-btn" title="Đóng tìm kiếm (Esc)" @click="handleClose">
        <font-awesome-icon icon="x" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.message-search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 24px;
  min-height: 48px;
  box-sizing: border-box;
  animation: slideDown 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: 'Quicksand', sans-serif;
  z-index: 25;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 20px;
  padding: 4px 12px;
  flex: 1;
  max-width: 480px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-input-wrapper:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.search-icon {
  color: #3b82f6;
  font-size: 13px;
  flex-shrink: 0;
}

.search-input {
  border: none;
  outline: none;
  font-family: inherit;
  font-size: 13.5px;
  color: #1e293b;
  width: 100%;
  background: transparent;
}

.search-input::placeholder {
  color: #94a3b8;
}

.search-counter {
  font-size: 12px;
  font-weight: 700;
  color: #3b82f6;
  white-space: nowrap;
  user-select: none;
  padding: 2px 6px;
  background: #eff6ff;
  border-radius: 10px;
}

.search-counter.no-match {
  color: #ef4444;
  background: #fef2f2;
}

.search-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.divider {
  width: 1px;
  height: 18px;
  background-color: #cbd5e1;
  margin: 0 4px;
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.15s ease;
}

.nav-btn:hover:not(:disabled) {
  background-color: #e2e8f0;
  color: #1e293b;
}

.nav-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.nav-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.close-btn:hover {
  background-color: #fee2e2;
  color: #ef4444;
}
</style>

