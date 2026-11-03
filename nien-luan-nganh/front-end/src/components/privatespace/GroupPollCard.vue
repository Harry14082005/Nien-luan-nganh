<script setup>
import { ref } from 'vue'

const props = defineProps({
  badgeText: {
    type: String,
    default: 'BÌNH CHỌN TRONG NHÓM',
  },
  title: {
    type: String,
    default: 'Chọn thời gian team building?',
  },
  subtitle: {
    type: String,
    default: 'Team Marketing · Kết thúc sau 2 ngày',
  },
  options: {
    type: Array,
    default: () => [
      { id: 1, text: 'Thứ Sáu tuần này' },
      { id: 2, text: 'Thứ Bảy tuần này' },
    ],
  },
  totalVotes: {
    type: Number,
    default: 12,
  },
})

const emit = defineEmits(['vote', 'viewGroup'])

const selectedOption = ref(null)

function selectOption(optId) {
  selectedOption.value = optId
  emit('vote', optId)
}
</script>

<template>
  <div class="poll-card">
    <!-- Header -->
    <div class="card-header">
      <div class="badge-tag">
        <svg
          class="badge-icon"
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="4" y1="21" x2="4" y2="14" />
          <line x1="4" y1="10" x2="4" y2="3" />
          <line x1="12" y1="21" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12" y2="3" />
          <line x1="20" y1="21" x2="20" y2="16" />
          <line x1="20" y1="12" x2="20" y2="3" />
        </svg>
        <span>{{ badgeText }}</span>
      </div>

      <button class="btn-more" title="Tùy chọn" @click.stop>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <circle cx="5" cy="12" r="2" />
          <circle cx="12" cy="12" r="2" />
          <circle cx="19" cy="12" r="2" />
        </svg>
      </button>
    </div>

    <!-- Content -->
    <div class="card-body">
      <h3 class="poll-title">{{ title }}</h3>
      <p class="poll-subtitle">{{ subtitle }}</p>

      <!-- Poll Options -->
      <div class="options-list">
        <div
          v-for="opt in options"
          :key="opt.id"
          class="poll-option-item"
          :class="{ selected: selectedOption === opt.id }"
          role="button"
          tabindex="0"
          @click="selectOption(opt.id)"
          @keydown.enter="selectOption(opt.id)"
          @keydown.space.prevent="selectOption(opt.id)"
        >
          <div class="radio-circle">
            <span v-if="selectedOption === opt.id" class="radio-inner"></span>
          </div>
          <span class="option-text">{{ opt.text }}</span>
        </div>
      </div>
    </div>

    <div class="divider"></div>

    <!-- Footer -->
    <div class="card-footer">
      <span class="votes-count">{{ totalVotes }} người đã bình chọn</span>
      <button class="btn-view-group" @click="emit('viewGroup')">
        <span>Xem nhóm</span>
        <svg
          class="arrow-icon"
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="5" y1="12" x2="19" y2="12" />
          <polyline points="12 5 19 12 12 19" />
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.poll-card {
  background-color: #ffffff;
  border-radius: 20px;
  padding: 24px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.025);
  font-family: 'Quicksand', sans-serif;
  display: flex;
  flex-direction: column;
  transition: all 0.2s ease;
}

.poll-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  border-color: #e2e8f0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.badge-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background-color: #f0fdfa;
  color: #0d9488;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.btn-more {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}

.btn-more:hover {
  color: #1e293b;
}

.card-body {
  flex: 1;
}

.poll-title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.poll-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 16px 0;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.poll-option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
}

.poll-option-item:hover {
  background-color: #f8fafc;
  border-color: #cbd5e1;
}

.poll-option-item.selected {
  border-color: #2563eb;
  background-color: #eff6ff;
}

.radio-circle {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.poll-option-item.selected .radio-circle {
  border-color: #2563eb;
}

.radio-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #2563eb;
}

.option-text {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.poll-option-item.selected .option-text {
  color: #1e293b;
}

.divider {
  height: 1px;
  background-color: #f1f5f9;
  margin: 20px 0 16px 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.votes-count {
  font-size: 12px;
  color: #64748b;
}

.btn-view-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #2563eb;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: gap 0.15s ease;
}

.btn-view-group:hover {
  color: #1d4ed8;
  gap: 6px;
}

.arrow-icon {
  transition: transform 0.15s ease;
}

.btn-view-group:hover .arrow-icon {
  transform: translateX(2px);
}
</style>

