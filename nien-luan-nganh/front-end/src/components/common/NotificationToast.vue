<script setup>
import { computed } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { useNotificationStore } from '@/stores/notification'
import { getImageUrl } from '@/api/user.api'
import { handleNotificationClick } from '@/utils/notificationNavigation'

const notificationStore = useNotificationStore()
const toasts = computed(() => notificationStore.toasts)

const getTypeIcon = (type) => {
  const t = String(type || '')
  if (t.startsWith('FRIEND_')) return 'user-plus'
  if (t === 'ROOM_ROLE_PROMOTED') return 'crown'
  if (t === 'ROOM_JOIN_REQUEST') return 'user-shield'
  if (t === 'ROOM_JOIN_REJECTED' || t === 'ROOM_REMOVED') return 'user-xmark'
  if (t.startsWith('ROOM_')) return 'users'
  if (t.startsWith('REMINDER_')) return 'calendar-days'
  if (t.startsWith('POLL_')) return 'chart-simple'
  return 'bell'
}

const getTypeColorClass = (type) => {
  const t = String(type || '')
  if (t.startsWith('FRIEND_')) return 'badge-green'
  if (t === 'ROOM_ROLE_PROMOTED') return 'badge-gold'
  if (t === 'ROOM_JOIN_REQUEST') return 'badge-amber'
  if (t === 'ROOM_JOIN_REJECTED' || t === 'ROOM_REMOVED') return 'badge-red'
  if (t.startsWith('ROOM_')) return 'badge-blue'
  if (t.startsWith('REMINDER_')) return 'badge-orange'
  if (t.startsWith('POLL_')) return 'badge-purple'
  return 'badge-blue'
}

const onToastClick = (toast) => {
  notificationStore.removeToast(toast.id)
  handleNotificationClick(toast)
}

const onCloseToast = (e, toastId) => {
  e.stopPropagation()
  notificationStore.removeToast(toastId)
}
</script>

<template>
  <div class="toast-container" aria-live="polite">
    <transition-group name="toast-slide">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast-card"
        @click="onToastClick(toast)"
      >
        <div class="toast-avatar-box">
          <img
            v-if="toast.actorAvatarUrl"
            :src="getImageUrl(toast.actorAvatarUrl)"
            :alt="toast.actorUsername || 'User'"
            class="toast-avatar"
          />
          <div v-else class="toast-avatar-placeholder">
            {{ (toast.actorUsername || 'U').charAt(0).toUpperCase() }}
          </div>
          <div class="toast-icon-badge" :class="getTypeColorClass(toast.notificationType)">
            <font-awesome-icon :icon="getTypeIcon(toast.notificationType)" />
          </div>
        </div>

        <div class="toast-content">
          <div class="toast-header">
            <span class="toast-title">{{ toast.title || 'Thông báo mới' }}</span>
            <button
              class="toast-close-btn"
              title="Đóng"
              @click="(e) => onCloseToast(e, toast.id)"
            >
              <font-awesome-icon icon="xmark" />
            </button>
          </div>
          <p class="toast-body">{{ toast.content }}</p>
          <span class="toast-time">Vừa xong</span>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@500;600;700&display=swap');

.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 99999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
  max-width: 360px;
  width: calc(100vw - 40px);
  font-family: 'Quicksand', sans-serif;
}

.toast-card {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background-color: #ffffff;
  border-radius: 0.9rem;
  box-shadow: 0 10px 25px -5px rgba(15, 37, 71, 0.15), 0 8px 10px -6px rgba(15, 37, 71, 0.08);
  border: 1px solid #e6eaf2;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
}

.toast-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px -5px rgba(15, 37, 71, 0.2);
  border-color: #cbd5e1;
}

.toast-avatar-box {
  position: relative;
  flex-shrink: 0;
}

.toast-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e6eaf2;
}

.toast-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
}

.toast-icon-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 8.5px;
  color: white;
  border: 2px solid #ffffff;
}

.badge-blue {
  background-color: #2563eb;
}
.badge-green {
  background-color: #10b981;
}
.badge-gold {
  background-color: #f59e0b;
}
.badge-amber {
  background-color: #d97706;
}
.badge-red {
  background-color: #ef4444;
}
.badge-orange {
  background-color: #f97316;
}
.badge-purple {
  background-color: #8b5cf6;
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 2px;
}

.toast-title {
  font-size: 14px;
  font-weight: 700;
  color: #1c2540;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toast-close-btn {
  background: transparent;
  border: none;
  color: #9aa3b5;
  cursor: pointer;
  padding: 3px 6px;
  font-size: 12px;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.toast-close-btn:hover {
  color: #2563eb;
  background-color: #e6edfd;
}

.toast-body {
  margin: 0;
  font-size: 12.5px;
  color: #4a5470;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.toast-time {
  display: inline-block;
  margin-top: 4px;
  font-size: 11.5px;
  color: #9aa3b5;
  font-weight: 500;
}

/* Transitions */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(50px) scale(0.95);
}

.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.9);
}
</style>
