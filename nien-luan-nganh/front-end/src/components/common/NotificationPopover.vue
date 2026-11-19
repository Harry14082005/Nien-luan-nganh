<script setup>
import { computed } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { useNotificationStore } from '@/stores/notification'
import { getImageUrl } from '@/api/user.api'
import { handleNotificationClick } from '@/utils/notificationNavigation'

const notificationStore = useNotificationStore()

const isOpen = computed(() => notificationStore.isPanelOpen)
const currentFilter = computed(() => notificationStore.filter)
const notifications = computed(() => notificationStore.filteredNotifications)
const unreadCount = computed(() => notificationStore.unreadCount)
const loading = computed(() => notificationStore.loading)
const loadingMore = computed(() => notificationStore.loadingMore)
const hasMore = computed(() => notificationStore.hasMore)

const onClose = () => {
  notificationStore.closePanel()
}

const onSetFilter = (f) => {
  notificationStore.setFilter(f)
}

const onMarkAllAsRead = async () => {
  await notificationStore.markAllAsRead()
}

const onItemClick = async (notif) => {
  await handleNotificationClick(notif)
}

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

/**
 * Định dạng thời gian tương đối
 */
const formatTimeAgo = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now - date
  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)
  const diffHour = Math.floor(diffMin / 60)
  const diffDay = Math.floor(diffHour / 24)

  if (diffSec < 60) return 'Vừa xong'
  if (diffMin < 60) return `${diffMin} phút trước`
  if (diffHour < 24) return `${diffHour} giờ trước`
  if (diffDay === 1) return 'Hôm qua'
  if (diffDay < 7) return `${diffDay} ngày trước`

  const d = String(date.getDate()).padStart(2, '0')
  const m = String(date.getMonth() + 1).padStart(2, '0')
  return `${d}/${m}`
}

const handleScroll = (e) => {
  const target = e.target
  if (!target) return
  // Cuộn gần đáy (còn 60px) thì tải thêm trang tiếp theo
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 60) {
    if (hasMore.value && !loadingMore.value) {
      notificationStore.fetchMore()
    }
  }
}
</script>

<template>
  <div>
    <!-- Backdrop mờ khi click ra ngoài -->
    <transition name="fade">
      <div
        v-if="isOpen"
        class="notification-backdrop"
        @click="onClose"
      ></div>
    </transition>

    <!-- Drawer Panel thông báo gắn cạnh Navigation Sidebar -->
    <transition name="panel-slide">
      <aside
        v-if="isOpen"
        class="notification-panel"
        aria-label="Bảng thông báo"
      >
        <!-- Header -->
        <div class="panel-header">
          <div class="header-left">
            <h2 class="header-title">Thông báo</h2>
            <span v-if="unreadCount > 0" class="unread-pill">{{ unreadCount }} mới</span>
          </div>

          <div class="header-actions">
            <button
              v-if="unreadCount > 0"
              class="action-btn"
              title="Đánh dấu tất cả là đã đọc"
              @click="onMarkAllAsRead"
            >
              <font-awesome-icon icon="check-double" />
              <span>Đã đọc hết</span>
            </button>
            <button
              class="close-btn"
              title="Đóng bảng thông báo"
              @click="onClose"
            >
              <font-awesome-icon icon="xmark" />
            </button>
          </div>
        </div>

        <!-- Filter tabs -->
        <div class="panel-tabs">
          <button
            class="tab-btn"
            :class="{ active: currentFilter === 'ALL' }"
            @click="onSetFilter('ALL')"
          >
            Tất cả
          </button>
          <button
            class="tab-btn"
            :class="{ active: currentFilter === 'UNREAD' }"
            @click="onSetFilter('UNREAD')"
          >
            Chưa đọc
            <span v-if="unreadCount > 0" class="tab-badge">{{ unreadCount }}</span>
          </button>
        </div>

        <!-- Body / List -->
        <div class="panel-body" @scroll="handleScroll">
          <!-- Loading state lần đầu -->
          <div v-if="loading" class="empty-state">
            <font-awesome-icon icon="spinner" spin class="spinner-icon" />
            <span>Đang tải thông báo...</span>
          </div>

          <!-- Empty state -->
          <div v-else-if="notifications.length === 0" class="empty-state">
            <div class="empty-icon-box">
              <font-awesome-icon icon="bell-slash" />
            </div>
            <p class="empty-text">
              {{ currentFilter === 'UNREAD' ? 'Bạn không có thông báo chưa đọc nào' : 'Bạn chưa có thông báo nào' }}
            </p>
          </div>

          <!-- Notification Items -->
          <div v-else class="notif-list">
            <div
              v-for="item in notifications"
              :key="item.notificationId"
              class="notif-item"
              :class="{ unread: !item.isRead }"
              @click="onItemClick(item)"
            >
              <!-- Avatar & icon badge -->
              <div class="item-avatar-box">
                <img
                  v-if="item.actorAvatarUrl"
                  :src="getImageUrl(item.actorAvatarUrl)"
                  :alt="item.actorUsername || 'User'"
                  class="item-avatar"
                />
                <div v-else class="item-avatar-placeholder">
                  {{ (item.actorUsername || 'U').charAt(0).toUpperCase() }}
                </div>
                <div class="item-type-badge" :class="getTypeColorClass(item.notificationType)">
                  <font-awesome-icon :icon="getTypeIcon(item.notificationType)" />
                </div>
              </div>

              <!-- Nội dung -->
              <div class="item-content">
                <div class="item-header-row">
                  <h4 class="item-title">{{ item.title || 'Thông báo' }}</h4>
                  <span class="item-time">{{ formatTimeAgo(item.createdAt) }}</span>
                </div>
                <p class="item-desc">{{ item.content }}</p>
              </div>

              <!-- Chấm xanh chưa đọc -->
              <div v-if="!item.isRead" class="unread-dot" title="Chưa đọc"></div>
            </div>

            <!-- Loading more spinner -->
            <div v-if="loadingMore" class="loading-more">
              <font-awesome-icon icon="spinner" spin />
              <span>Đang tải thêm...</span>
            </div>
          </div>
        </div>
      </aside>
    </transition>
  </div>
</template>

<style scoped>
/* Backdrop */
.notification-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.25);
  z-index: 998;
  backdrop-filter: blur(2px);
}

/* Panel Drawer */
.notification-panel {
  position: fixed;
  top: 0;
  left: 80px; /* Bắt đầu ngay sau NavigationSidebar (80px) */
  bottom: 0;
  width: 380px;
  max-width: calc(100vw - 80px);
  background: #ffffff;
  z-index: 999;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 24px rgba(15, 23, 42, 0.12);
  border-right: 1px solid #e2e8f0;
  font-family: 'Quicksand', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* Header */
.panel-header {
  padding: 16px 18px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.unread-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: transparent;
  border: none;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  cursor: pointer;
  padding: 6px 9px;
  border-radius: 8px;
  transition: all 0.15s;
}

.action-btn:hover {
  background: #eff6ff;
}

.close-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 14px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

/* Tabs */
.panel-tabs {
  display: flex;
  padding: 8px 18px;
  gap: 8px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid transparent;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn:hover {
  color: #1e293b;
  background: #f1f5f9;
}

.tab-btn.active {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
}

.tab-badge {
  font-size: 10px;
  background: #ef4444;
  color: #ffffff;
  padding: 1px 6px;
  border-radius: 10px;
}

/* Body / List */
.panel-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #94a3b8;
  gap: 12px;
  text-align: center;
}

.empty-icon-box {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #cbd5e1;
}

.empty-text {
  margin: 0;
  font-size: 13.5px;
  color: #64748b;
}

.spinner-icon {
  font-size: 22px;
  color: #3b82f6;
}

/* Notif Item */
.notif-list {
  display: flex;
  flex-direction: column;
}

.notif-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 18px;
  cursor: pointer;
  border-bottom: 1px solid #f8fafc;
  transition: background-color 0.15s ease;
}

.notif-item:hover {
  background-color: #f8fafc;
}

.notif-item.unread {
  background-color: #f0f7ff;
}

.notif-item.unread:hover {
  background-color: #e0f0fe;
}

.item-avatar-box {
  position: relative;
  flex-shrink: 0;
}

.item-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 1.5px solid #e2e8f0;
}

.item-avatar-placeholder {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
}

.item-type-badge {
  position: absolute;
  bottom: -2px;
  right: -3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
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

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 2px;
}

.item-title {
  margin: 0;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-time {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.item-desc {
  margin: 0;
  font-size: 12.5px;
  color: #475569;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #2563eb;
  flex-shrink: 0;
  align-self: center;
  margin-left: 4px;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  font-size: 12px;
  color: #64748b;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(-20px);
  opacity: 0;
}
</style>
