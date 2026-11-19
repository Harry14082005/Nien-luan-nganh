<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllMyReminders } from '@/api/reminder.api'
import { resolveFileUrl } from '@/api/storage.api'

const router = useRouter()
const reminders = ref([])
const isLoading = ref(true)
const error = ref(null)
const avatarErrorMap = ref({})

onMounted(async () => {
  try {
    reminders.value = await getAllMyReminders()
  } catch (e) {
    error.value = 'Không thể tải lịch hẹn. Vui lòng thử lại.'
    console.error('UpcomingEventList load error:', e)
  } finally {
    isLoading.value = false
  }
})

function formatDateTime(isoString) {
  if (!isoString) return '—'
  const d = new Date(isoString)
  const days = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
  const dow = days[d.getDay()]
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const mo = String(d.getMonth() + 1).padStart(2, '0')
  const yyyy = d.getFullYear()
  return `${dow}, ${dd}/${mo}/${yyyy} · ${hh}:${mm}`
}

function getStatusLabel(status) {
  if (status === 'PENDING') return 'Sắp diễn ra'
  if (status === 'DONE') return 'Đã xong'
  return status
}

function getStatusClass(status) {
  if (status === 'PENDING') return 'status-pending'
  if (status === 'DONE') return 'status-done'
  return ''
}

function getReminderTypeLabel(type) {
  if (type === 'GROUP') return 'Nhóm'
  if (type === 'PERSONAL') return 'Cá nhân'
  return type ?? ''
}

function getInitials(name = '') {
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (words.length >= 2) return (words[0][0] + words[words.length - 1][0]).toUpperCase()
  return name.slice(0, 2).toUpperCase()
}

const bgPalette = ['#ffe8d6', '#dcfce7', '#e0f2fe', '#fef9c3', '#f5f3ff']
const colorPalette = ['#d97736', '#0d9488', '#0284c7', '#ca8a04', '#7c3aed']
function avatarBg(idx) { return bgPalette[idx % bgPalette.length] }
function avatarColor(idx) { return colorPalette[idx % colorPalette.length] }

function goToChatWithReminder(item) {
  if (!item?.roomId) return
  router.push({
    path: '/chat',
    query: {
      roomId: item.roomId,
      messageId: item.messageId || undefined,
      reminderId: item.reminderId || undefined,
    },
  })
}
</script>

<template>
  <div class="event-list-container">
    <!-- Header -->
    <div class="list-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
        </div>
        <div>
          <h2 class="list-title">Lịch hẹn sắp tới</h2>
          <p class="list-subtitle">Danh sách các lịch hẹn sắp tới từ tất cả nhóm của bạn </p>
        </div>
      </div>
      <span v-if="!isLoading && !error" class="count-badge">{{ reminders.length }} lịch hẹn</span>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="state-box">
      <div class="spinner"></div>
      <p>Đang tải lịch hẹn...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="state-box error-box">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <p>{{ error }}</p>
    </div>

    <!-- Empty -->
    <div v-else-if="reminders.length === 0" class="state-box">
      <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
        <line x1="16" y1="2" x2="16" y2="6"/>
        <line x1="8" y1="2" x2="8" y2="6"/>
        <line x1="3" y1="10" x2="21" y2="10"/>
        <line x1="8" y1="15" x2="16" y2="15"/>
      </svg>
      <p class="empty-title">Không có lịch hẹn nào</p>
      <p class="empty-hint">Các lịch hẹn từ nhóm sẽ xuất hiện ở đây.</p>
    </div>

    <!-- List (Bố cục 1 hàng 2 card) -->
    <div v-else class="reminders-list">
      <div
        v-for="(item, idx) in reminders"
        :key="item.reminderId ?? idx"
        class="reminder-card"
        title="Nhấn để nhảy tới tin nhắn thông báo nhắc hẹn trong phòng chat"
        @click="goToChatWithReminder(item)"
      >
        <!-- Badge & Status -->
        <div class="card-top">
          <span class="type-badge">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            {{ getReminderTypeLabel(item.reminderType) }}
          </span>
          <span class="status-pill" :class="getStatusClass(item.status || item.reminderStatus)">{{ getStatusLabel(item.status || item.reminderStatus) }}</span>
        </div>

        <!-- Title & Description -->
        <h3 class="reminder-title">{{ item.title ?? '(Không có tiêu đề)' }}</h3>
        <p v-if="item.description" class="reminder-desc">{{ item.description }}</p>

        <!-- Time -->
        <div class="time-row">
          <svg class="time-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
          <span>{{ formatDateTime(item.startAt) }}</span>
          <span v-if="item.endAt" class="time-separator">→</span>
          <span v-if="item.endAt">{{ formatDateTime(item.endAt) }}</span>
        </div>

        <div class="card-divider"></div>

        <!-- Footer: nhóm & participants -->
        <div class="card-footer">
          <div class="group-row">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            <span class="group-name">{{ item.roomName ?? 'Nhóm' }}</span>
          </div>

          <!-- Participant avatars -->
          <div v-if="item.participants && item.participants.length" class="avatar-stack">
            <div
              v-for="(p, pi) in item.participants.slice(0, 4)"
              :key="p.userId ?? pi"
              class="stacked-avatar"
              :style="{ backgroundColor: avatarBg(pi), color: avatarColor(pi), zIndex: 10 - pi }"
              :title="p.username ?? ''"
            >
              <img
                v-if="p.avatarUrl && !avatarErrorMap[p.userId || pi]"
                :src="resolveFileUrl(p.avatarUrl)"
                class="avatar-img"
                alt=""
                @error="avatarErrorMap[p.userId || pi] = true"
              />
              <span v-else>{{ getInitials(p.username ?? '') }}</span>
            </div>
            <div v-if="item.participants.length > 4" class="stacked-avatar more-avatar">+{{ item.participants.length - 4 }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@300..700&display=swap');

.event-list-container {
  font-family: 'Quicksand', sans-serif;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Header */
.list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
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
  background: #f5f3ff;
  color: #7c3aed;
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

.count-badge {
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px;
  white-space: nowrap;
  margin-top: 4px;
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
  border-top-color: #7c3aed;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* List: 1 HÀNG 2 CARD */
.reminders-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

@media (max-width: 860px) {
  .reminders-list {
    grid-template-columns: 1fr;
  }
}

/* Card */
.reminder-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 20px 22px;
  border: 1px solid #eef2f7;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
  transition: all 0.2s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.reminder-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
  border-color: #cbd5e1;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #f5f3ff;
  color: #7c3aed;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.4px;
  padding: 3px 9px;
  border-radius: 7px;
}

.status-pill {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
}

.status-pending {
  background: #eff6ff;
  color: #2563eb;
}

.status-done {
  background: #f0fdf4;
  color: #16a34a;
}

.reminder-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 5px 0;
}

.reminder-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.time-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  flex-wrap: wrap;
}

.time-icon { color: #94a3b8; flex-shrink: 0; }
.time-separator { color: #94a3b8; }

.card-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 14px 0 12px 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.group-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.group-name {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

/* Avatar stack */
.avatar-stack {
  display: flex;
  align-items: center;
}

.stacked-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 700;
  margin-left: -6px;
  overflow: hidden;
  flex-shrink: 0;
}

.stacked-avatar:first-child { margin-left: 0; }

.more-avatar {
  background: #e2e8f0;
  color: #64748b;
  font-size: 9px;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  display: block;
}
</style>
