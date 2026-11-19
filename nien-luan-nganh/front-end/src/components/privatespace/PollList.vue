<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllMyPolls } from '@/api/poll.api'

const router = useRouter()
const polls = ref([])
const isLoading = ref(true)
const error = ref(null)
const currentFilter = ref('ALL') // 'ALL' | 'ACTIVE' | 'CLOSED'

onMounted(async () => {
  try {
    polls.value = await getAllMyPolls()
  } catch (e) {
    error.value = 'Không thể tải danh sách bình chọn. Vui lòng thử lại sau.'
    console.error('PollList load error:', e)
  } finally {
    isLoading.value = false
  }
})

const filteredPolls = computed(() => {
  if (currentFilter.value === 'ACTIVE') {
    return polls.value.filter((p) => p.isActive === true)
  }
  if (currentFilter.value === 'CLOSED') {
    return polls.value.filter((p) => p.isActive === false)
  }
  return polls.value
})

function formatDateTime(isoString) {
  if (!isoString) return ''
  try {
    const d = new Date(isoString)
    const dd = String(d.getDate()).padStart(2, '0')
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const yyyy = d.getFullYear()
    const hh = String(d.getHours()).padStart(2, '0')
    const min = String(d.getMinutes()).padStart(2, '0')
    return `${hh}:${min} · ${dd}/${mm}/${yyyy}`
  } catch {
    return ''
  }
}

function calculatePercentage(voteCount, totalVoters) {
  if (!totalVoters || totalVoters <= 0) return 0
  return Math.round(((voteCount || 0) / totalVoters) * 100)
}

function goToChatWithPoll(poll) {
  if (!poll?.roomId) return
  router.push({
    path: '/chat',
    query: {
      roomId: poll.roomId,
      messageId: poll.messageId || undefined,
      pollId: poll.pollId || undefined,
    },
  })
}
</script>

<template>
  <div class="poll-list-container">
    <!-- Header -->
    <div class="list-header">
      <div class="header-left">
        <div class="header-icon">
          <svg
            width="20"
            height="20"
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
        </div>
        <div>
          <h2 class="list-title">Bình chọn trong các nhóm</h2>
          <p class="list-subtitle">Tổng hợp tất cả các cuộc bình chọn từ các nhóm chat của bạn</p>
        </div>
      </div>

      <!-- Filters & Count -->
      <div class="header-actions">
        <div class="filter-pills">
          <button
            type="button"
            class="filter-pill"
            :class="{ active: currentFilter === 'ALL' }"
            @click="currentFilter = 'ALL'"
          >
            Tất cả ({{ polls.length }})
          </button>
          <button
            type="button"
            class="filter-pill"
            :class="{ active: currentFilter === 'ACTIVE' }"
            @click="currentFilter = 'ACTIVE'"
          >
            Đang mở ({{ polls.filter((p) => p.isActive).length }})
          </button>
          <button
            type="button"
            class="filter-pill"
            :class="{ active: currentFilter === 'CLOSED' }"
            @click="currentFilter = 'CLOSED'"
          >
            Đã đóng ({{ polls.filter((p) => !p.isActive).length }})
          </button>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="state-box">
      <div class="spinner"></div>
      <p>Đang tải danh sách bình chọn...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="state-box error-box">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <p>{{ error }}</p>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredPolls.length === 0" class="state-box">
      <svg
        width="56"
        height="56"
        viewBox="0 0 24 24"
        fill="none"
        stroke="#94a3b8"
        stroke-width="1.2"
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
      <p class="empty-title">Không tìm thấy cuộc bình chọn nào</p>
      <p class="empty-hint">Các cuộc bình chọn được tạo trong nhóm sẽ xuất hiện ở đây.</p>
    </div>

    <!-- Polls Grid (1 hàng 2 card) -->
    <div v-else class="polls-grid">
      <div
        v-for="poll in filteredPolls"
        :key="poll.pollId"
        class="poll-card"
        title="Nhấn để nhảy tới tin nhắn bình chọn trong phòng chat"
        @click="goToChatWithPoll(poll)"
      >
        <!-- Card Header -->
        <div class="card-header">
          <div class="badge-tag" :class="{ 'is-active': poll.isActive, 'is-closed': !poll.isActive }">
            <svg
              width="13"
              height="13"
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
            <span>{{ poll.isActive ? 'ĐANG MỞ' : 'ĐÃ ĐÓNG' }}</span>
          </div>

          <div class="group-tag">
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            <span>{{ poll.roomName || 'Nhóm' }}</span>
          </div>
        </div>

        <!-- Question & Info -->
        <div class="card-body">
          <h3 class="poll-question">{{ poll.question }}</h3>
          <p class="poll-meta">
            Tạo bởi <strong>{{ poll.createdByName || 'Thành viên' }}</strong> · {{ formatDateTime(poll.createdAt) }}
          </p>

          <!-- Options Preview -->
          <div class="options-container">
            <div
              v-for="opt in (poll.options || []).slice(0, 3)"
              :key="opt.pollOptionId"
              class="option-row"
              :class="{ 'my-vote': opt.votedByMe }"
            >
              <div class="option-progress-bg" :style="{ width: `${calculatePercentage(opt.voteCount, poll.totalVoters)}%` }"></div>
              <div class="option-content">
                <span class="option-text">
                  <span v-if="opt.votedByMe" class="voted-dot">✓</span>
                  {{ opt.optionText }}
                </span>
                <span class="option-votes">{{ opt.voteCount || 0 }} phiếu</span>
              </div>
            </div>

            <div v-if="(poll.options || []).length > 3" class="more-options-hint">
              +{{ poll.options.length - 3 }} lựa chọn khác
            </div>
          </div>
        </div>

        <div class="card-divider"></div>

        <!-- Footer -->
        <div class="card-footer">
          <span class="total-voters">
            <strong>{{ poll.totalVoters || 0 }}</strong> người đã tham gia
          </span>

          <div class="action-link">
            <span>Vào bình chọn</span>
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
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@300..700&display=swap');

.poll-list-container {
  font-family: 'Quicksand', sans-serif;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Header */
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #f0fdfa;
  color: #0d9488;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.list-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 2px 0;
}

.list-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

/* Filter pills */
.filter-pills {
  display: flex;
  align-items: center;
  background: #ffffff;
  padding: 4px;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  gap: 4px;
}

.filter-pill {
  border: none;
  background: none;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-pill:hover {
  color: #0d9488;
  background: #f0fdfa;
}

.filter-pill.active {
  background: #0d9488;
  color: #ffffff;
}

/* State boxes */
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 56px 24px;
  color: #94a3b8;
  font-size: 14px;
  text-align: center;
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #eef2f7;
}

.error-box { color: #ef4444; }

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
  margin: 0;
}

.empty-hint {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

/* Spinner */
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e2e8f0;
  border-top-color: #0d9488;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* Grid: 1 hàng 2 card */
.polls-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

@media (max-width: 860px) {
  .polls-grid {
    grid-template-columns: 1fr;
  }
}

/* Card */
.poll-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 22px;
  border: 1px solid #eef2f7;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
  transition: all 0.2s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.poll-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(13, 148, 136, 0.1);
  border-color: #99f6e4;
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
  gap: 5px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.badge-tag.is-active {
  background: #f0fdfa;
  color: #0d9488;
}

.badge-tag.is-closed {
  background: #f1f5f9;
  color: #64748b;
}

.group-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.poll-question {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.poll-meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0 0 16px 0;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-row {
  position: relative;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 14px;
  overflow: hidden;
  transition: all 0.15s ease;
}

.option-row.my-vote {
  border-color: #0d9488;
  background: #f0fdfa;
}

.option-progress-bg {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  background: rgba(13, 148, 136, 0.12);
  transition: width 0.3s ease;
  z-index: 1;
}

.option-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
}

.option-text {
  color: #334155;
  display: flex;
  align-items: center;
  gap: 6px;
}

.voted-dot {
  color: #0d9488;
  font-weight: 800;
}

.option-votes {
  color: #64748b;
  font-size: 12px;
}

.more-options-hint {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  padding: 4px 0;
}

.card-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 16px 0 14px 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.total-voters {
  font-size: 12px;
  color: #64748b;
}

.action-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #0d9488;
  transition: gap 0.15s ease;
}

.poll-card:hover .action-link {
  gap: 7px;
}

.arrow-icon {
  transition: transform 0.15s ease;
}

.poll-card:hover .arrow-icon {
  transform: translateX(2px);
}
</style>
