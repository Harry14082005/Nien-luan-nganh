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
 * Định dạng thời gian tương đối theo style Bluespace
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
  if (diffMin < 60) return `${diffMin} phút`
  if (diffHour < 24) return `${diffHour} giờ`
  if (diffDay === 1) return 'Hôm qua'
  if (diffDay < 7) return `${diffDay} ngày`

  const d = String(date.getDate()).padStart(2, '0')
  const m = String(date.getMonth() + 1).padStart(2, '0')
  return `${d}/${m}`
}

const handleScroll = (e) => {
  const target = e.target
  if (!target) return
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 60) {
    if (hasMore.value && !loadingMore.value) {
      notificationStore.fetchMore()
    }
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="notification-popover-wrapper">
      <!-- Backdrop làm mờ khi click ra ngoài -->
      <transition name="fade">
        <div
          v-if="isOpen"
          class="notification-backdrop"
          @click="onClose"
        ></div>
      </transition>

      <!-- Dropdown Popover thông báo xổ xuống góc trên bên phải (Kiểu A - 360px) -->
      <transition name="dropdown-slide">
        <aside
          v-if="isOpen"
          class="notification-popover-dropdown"
          aria-label="Bảng thông báo"
        >
          <!-- Header đồng bộ theo namePage của ChatListSidebar -->
          <div class="namePage">
            <div class="left-namePage">
              <span>Thông báo</span>
              <div v-if="unreadCount > 0" class="NumberOfMessages">
                {{ unreadCount > 99 ? '99+' : unreadCount }}
              </div>
            </div>

            <div class="right-action-group">
              <button
                v-if="unreadCount > 0"
                class="btn-action-icon"
                title="Đánh dấu tất cả là đã đọc"
                @click="onMarkAllAsRead"
              >
                <font-awesome-icon icon="check-double" />
                <span>Đã đọc hết</span>
              </button>
              <button
                class="btn-close-icon"
                title="Đóng bảng thông báo"
                @click="onClose"
              >
                <font-awesome-icon icon="xmark" />
              </button>
            </div>
          </div>

          <!-- Filter tabs đồng bộ theo .filter của ChatListSidebar -->
          <div class="filter">
            <button
              class="SelectedFilter"
              :class="{ active: currentFilter === 'ALL' }"
              @click="onSetFilter('ALL')"
            >
              Tất cả
            </button>
            <button
              class="SelectedFilter"
              :class="{ active: currentFilter === 'UNREAD' }"
              @click="onSetFilter('UNREAD')"
            >
              Chưa đọc
              <span v-if="unreadCount > 0" class="filter-badge">{{ unreadCount }}</span>
            </button>
          </div>

          <!-- Danh sách thông báo -->
          <div class="panel-body" @scroll="handleScroll">
            <!-- Loading state -->
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

            <!-- Danh sách từng thông báo theo chuẩn ChatItem -->
            <div v-else class="notif-list">
              <div
                v-for="item in notifications"
                :key="item.notificationId"
                class="notification-item"
                :class="{ unread: !item.isRead }"
                @click="onItemClick(item)"
              >
                <!-- Avatar & icon badge -->
                <div class="avatar-wrap">
                  <img
                    v-if="item.actorAvatarUrl"
                    :src="getImageUrl(item.actorAvatarUrl)"
                    :alt="item.actorUsername || 'User'"
                    class="item-avatar-img"
                  />
                  <div v-else class="item-avatar-initials">
                    {{ (item.actorUsername || 'U').charAt(0).toUpperCase() }}
                  </div>
                  <div class="type-icon-badge" :class="getTypeColorClass(item.notificationType)">
                    <font-awesome-icon :icon="getTypeIcon(item.notificationType)" />
                  </div>
                </div>

                <!-- Nội dung thông báo -->
                <div class="item-content-wrap">
                  <div class="top-row">
                    <div class="item-name" :title="item.title || 'Thông báo'">
                      {{ item.title || 'Thông báo' }}
                    </div>
                    <div class="item-time">
                      {{ formatTimeAgo(item.createdAt) }}
                    </div>
                  </div>
                  <div class="bottom-row">
                    <p class="item-desc">
                      {{ item.content }}
                    </p>
                    <!-- Chấm tròn xanh chưa đọc (kết hợp với vạch mép trái) -->
                    <div v-if="!item.isRead" class="unread-dot" title="Chưa đọc"></div>
                  </div>
                </div>
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
  </Teleport>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700&display=swap');

/* Backdrop mờ nhẹ */
.notification-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 37, 71, 0.2);
  z-index: 999;
  backdrop-filter: blur(1.5px);
}

/* Dropdown Popover (Kiểu A: Xổ xuống góc trên bên phải - 360px) */
.notification-popover-dropdown {
  position: fixed;
  top: 72px;
  right: 28px;
  width: 360px;
  max-width: calc(100vw - 36px);
  max-height: min(620px, calc(100vh - 90px));
  background-color: #ffffff;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  box-shadow: 0 16px 40px -6px rgba(15, 37, 71, 0.2), 0 0 0 1px rgba(15, 37, 71, 0.08);
  border-radius: 1.1rem;
  border: 1px solid #e6eaf2;
  font-family: 'Quicksand', sans-serif;
  box-sizing: border-box;
  overflow: hidden;
}

/* Header chuẩn namePage từ ChatListSidebar */
.namePage {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 10px 20px;
  box-sizing: border-box;
}

.left-namePage {
  display: flex;
  align-items: center;
  font-size: 24px;
  color: #223451;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.NumberOfMessages {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #3b82f6;
  background-color: #e6edfd;
  border-radius: 8px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  margin-left: 8px;
}

.right-action-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-action-icon {
  background-color: #e6edfd;
  color: #2563eb;
  padding: 6px 10px;
  border-radius: 0.7rem;
  cursor: pointer;
  border: none;
  font-size: 12px;
  font-weight: 700;
  font-family: 'Quicksand', sans-serif;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: all 0.2s ease;
}

.btn-action-icon:hover {
  background-color: #d8e4fd;
  transform: scale(1.02);
}

.btn-close-icon {
  background-color: #f1f5f9;
  color: #64748b;
  width: 30px;
  height: 30px;
  border-radius: 0.7rem;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  transition: all 0.2s ease;
}

.btn-close-icon:hover {
  background-color: #e2e8f0;
  color: #1e293b;
}

/* Tabs chuẩn .filter & .SelectedFilter từ ChatListSidebar */
.filter {
  display: flex;
  gap: 8px;
  font-size: 15px;
  padding: 4px 20px 12px 20px;
  border-bottom: 1px solid #e6edfd;
  font-weight: 700;
  box-sizing: border-box;
}

.SelectedFilter {
  padding: 6px 14px;
  cursor: pointer;
  color: #4a5a78;
  border-radius: 0.8rem;
  border: none;
  background: transparent;
  font-family: 'Quicksand', sans-serif;
  font-weight: 700;
  font-size: 14px;
  transition: all 0.15s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.SelectedFilter:hover {
  color: #2f5fd0;
  background-color: #f3f6fd;
}

.SelectedFilter.active {
  border-radius: 0.8rem;
  background-color: #e6edfd;
  color: #2f5fd0;
}

.filter-badge {
  font-size: 10px;
  background-color: #ef4444;
  color: #ffffff;
  padding: 1px 5px;
  border-radius: 8px;
  font-weight: 700;
  line-height: 1.2;
}

/* Body / List */
.panel-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 6px;
  box-sizing: border-box;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9aa3b5;
  gap: 12px;
  text-align: center;
}

.empty-icon-box {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background-color: #f3f6fd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #9aa3b5;
}

.empty-text {
  margin: 0;
  font-size: 13.5px;
  font-weight: 600;
  color: #64748b;
}

.spinner-icon {
  font-size: 22px;
  color: #3b82f6;
}

/* Danh sách Item chuẩn ChatItem */
.notif-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.notification-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  border-radius: 0.8rem;
  box-sizing: border-box;
}

.notification-item:hover {
  background-color: #f3f6fd;
}

.notification-item.unread {
  background-color: #eaf0fd;
}

/* 1. VẠCH XANH Ở MÉP TRÁI KHI CHƯA ĐỌC */
.notification-item.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 15%;
  bottom: 15%;
  width: 4px;
  background-color: #2563eb;
  border-radius: 0 4px 4px 0;
}

/* Avatar chuẩn Bluespace */
.avatar-wrap {
  position: relative;
  flex-shrink: 0;
  display: inline-flex;
}

.item-avatar-img {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e6eaf2;
}

.item-avatar-initials {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
}

.type-icon-badge {
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
  color: #ffffff;
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

/* Nội dung thông báo */
.item-content-wrap {
  flex: 1;
  min-width: 0;
}

.top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.item-name {
  font-size: 14.5px;
  font-weight: 600;
  color: #1c2540;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-item.unread .item-name {
  font-weight: 700;
  color: #0f172a;
}

.item-time {
  flex-shrink: 0;
  font-size: 12px;
  color: #9aa3b5;
  font-weight: 500;
}

.bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 3px;
}

.item-desc {
  margin: 0;
  font-size: 12.5px;
  color: #8f98ab;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.notification-item.unread .item-desc {
  color: #4a5470;
  font-weight: 500;
}

/* 2. CHẤM TRÒN XANH Ở BÊN PHẢI KHI CHƯA ĐỌC */
.unread-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #2563eb;
  margin-left: 6px;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
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

.dropdown-slide-enter-active,
.dropdown-slide-leave-active {
  transition: all 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}
.dropdown-slide-enter-from,
.dropdown-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.97);
}
</style>
