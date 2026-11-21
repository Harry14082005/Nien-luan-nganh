<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import NavigationSidebar from '@/components/common/NavigationSidebar.vue'
import StatCard from '@/components/privatespace/StatCard.vue'
import UpcomingEventCard from '@/components/privatespace/UpcomingEventCard.vue'
import UpcomingEventList from '@/components/privatespace/UpcomingEventList.vue'
import GroupPollCard from '@/components/privatespace/GroupPollCard.vue'
import PollList from '@/components/privatespace/PollList.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { getAllMyReminders, isReminderEnded } from '@/api/reminder.api'
import { getAllMyPolls } from '@/api/poll.api'
import { useNotificationStore } from '@/stores/notification'
import NotificationPopover from '@/components/common/NotificationPopover.vue'
import { resolveFileUrl } from '@/api/storage.api'

const chatStore = useChatStore()
const notificationStore = useNotificationStore()
const unreadNotifCount = computed(() => notificationStore.unreadCount)

const toggleNotifications = () => {
  notificationStore.togglePanel()
}

const formattedTotalUnreadCount = computed(() =>
  String(chatStore.totalUnreadCount).padStart(2, '0'),
)

const router = useRouter()
const authStore = useAuthStore()

const myInitials = computed(() => {
  const w = (authStore.user?.username || '').trim().split(/\s+/).filter(Boolean)
  if (!w.length) return 'U'
  return (w.length === 1 ? w[0].slice(0, 2) : w[0][0] + w[w.length - 1][0]).toUpperCase()
})

// Tab khám phá: 'overview' | 'calendar'
// 'polls' bị ẩn tạm thời (chức năng tích hợp sau)
const activeSubTab = ref('overview')

const userProfile = ref({ displayName: '' })
const currentUser = ref({
  fullName: '',
  displayName: '',
  initials: 'HN',
  avatarUrl: null,
})

// Lịch hẹn hôm nay dùng cho StatCard và preview
const todayReminders = ref([])
const isLoadingToday = ref(false)
const todayReminderCount = computed(() =>
  String(todayReminders.value.length).padStart(2, '0'),
)

// Bình chọn đang mở dùng cho StatCard và preview tổng quan
const openPolls = ref([])
const isLoadingPolls = ref(false)
const openPollsCount = computed(() =>
  String(openPolls.value.length).padStart(2, '0'),
)

function getShortName(name = '') {
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (!words.length) return ''
  return words[words.length - 1]
}

function getInitials(name = '') {
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (!words.length) return 'U'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
}

async function loadName() {
  try {
    const me = (await authStore.initUser()) || authStore.user
    if (me) {
      const full = me.fullName || me.username || ''
      currentUser.value = {
        fullName: full,
        displayName: getShortName(full),
        initials: getInitials(full),
        avatarUrl: me.avatarUrl ? resolveFileUrl(me.avatarUrl) : null,
      }
      userProfile.value = { displayName: getShortName(full) }
    }
  } catch (e) {
    console.warn('Không tải tên người dùng', e)
  }
}

async function loadTodayReminders() {
  isLoadingToday.value = true
  try {
    const list = await getAllMyReminders({ filterToday: true })
    todayReminders.value = (list || []).filter((r) => !isReminderEnded(r))
  } catch (e) {
    console.warn('Không tải được lịch hẹn hôm nay:', e)
  } finally {
    isLoadingToday.value = false
  }
}

async function loadOpenPolls() {
  isLoadingPolls.value = true
  try {
    openPolls.value = await getAllMyPolls({ openOnly: true })
  } catch (e) {
    console.warn('Không tải được bình chọn đang mở:', e)
  } finally {
    isLoadingPolls.value = false
  }
}

function formatTimeRange(startAt, endAt) {
  if (!startAt) return '—'
  const s = new Date(startAt)
  const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
  const dow = days[s.getDay()]
  const hh1 = String(s.getHours()).padStart(2, '0')
  const mm1 = String(s.getMinutes()).padStart(2, '0')
  if (!endAt) return `${dow} · ${hh1}:${mm1}`
  const e = new Date(endAt)
  const hh2 = String(e.getHours()).padStart(2, '0')
  const mm2 = String(e.getMinutes()).padStart(2, '0')
  return `${dow} · ${hh1}:${mm1} - ${hh2}:${mm2}`
}

function getParticipantMembers(reminder) {
  if (!reminder?.participants?.length) return []
  return reminder.participants.slice(0, 4).map((p, i) => {
    const bgPalette = ['#ffe8d6', '#dcfce7', '#e0f2fe', '#fef9c3', '#f5f3ff']
    const colorPalette = ['#d97736', '#0d9488', '#0284c7', '#ca8a04', '#7c3aed']
    const name = p.username ?? ''
    const words = name.trim().split(/\s+/).filter(Boolean)
    const initials =
      words.length >= 2
        ? (words[0][0] + words[words.length - 1][0]).toUpperCase()
        : name.slice(0, 2).toUpperCase()
    return {
      userId: p.userId,
      username: name,
      avatarUrl: p.avatarUrl ? resolveFileUrl(p.avatarUrl) : null,
      initials,
      bg: bgPalette[i % bgPalette.length],
      color: colorPalette[i % colorPalette.length],
    }
  })
}

function formatPollSubtitle(poll) {
  const room = poll.roomName || 'Nhóm'
  if (poll.isActive) {
    return `${room} · Đang diễn ra`
  }
  return `${room} · Đã đóng`
}

function formatPollOptions(options = []) {
  if (!Array.isArray(options)) return []
  return options.slice(0, 3).map((o) => ({
    id: o.optionId ?? o.id,
    text: o.optionText ?? o.text ?? '(Lựa chọn)',
  }))
}

function goToChat() {
  router.push('/chat')
}

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

function goToCalendar() {
  activeSubTab.value = 'calendar'
}

function goToPolls() {
  activeSubTab.value = 'polls'
}

onMounted(async () => {
  await loadName()
  loadTodayReminders()
  loadOpenPolls()
  notificationStore.fetchUnreadCount()
})
</script>

<template>
  <div class="app-container">
    <!-- Cột 1: Navigation Sidebar chính -->
    <NavigationSidebar current-page="home" />

    <!-- Cột 2: Sidebar menu Trang chủ / Không gian của tôi -->
    <div class="private-space-sidebar">
      <!-- App Header -->
      <section class="nameApp">
        <div class="logo-title">bluespace.</div>
      </section>

      <!-- Section Title -->
      <div class="sidebar-header">
        <div class="title-row">
          <h1 class="page-title">Trang chủ</h1>
          <div class="home-icon-box" title="Trang chủ">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Category Label -->
      <div class="nav-category">
        <span class="category-title">KHÁM PHÁ</span>
      </div>

      <!-- Menu Items -->
      <div class="sidebar-menu-list">
        <!-- Mục 1: Tổng quan -->
        <div
          class="menu-item"
          :class="{ active: activeSubTab === 'overview' }"
          role="button"
          tabindex="0"
          @click="activeSubTab = 'overview'"
          @keydown.enter="activeSubTab = 'overview'"
        >
          <div class="item-left">
            <svg class="item-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2l2.4 7.4H22l-6 4.6 2.3 7.4-6.3-4.6-6.3 4.6 2.3-7.4-6-4.6h7.6z" />
            </svg>
            <span class="item-text">Tổng quan</span>
          </div>
          <svg class="chevron-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>

        <!-- Mục 2: Lịch sắp tới -->
        <div
          class="menu-item"
          :class="{ active: activeSubTab === 'calendar' }"
          role="button"
          tabindex="0"
          @click="activeSubTab = 'calendar'"
          @keydown.enter="activeSubTab = 'calendar'"
        >
          <div class="item-left">
            <svg class="item-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
            <span class="item-text">Lịch sắp tới</span>
          </div>
          <svg class="chevron-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>

        <!-- Mục 3: Bình chọn -->
        <div
          class="menu-item"
          :class="{ active: activeSubTab === 'polls' }"
          role="button"
          tabindex="0"
          @click="activeSubTab = 'polls'"
          @keydown.enter="activeSubTab = 'polls'"
        >
          <div class="item-left">
            <svg class="item-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="4" y1="21" x2="4" y2="14" />
              <line x1="4" y1="10" x2="4" y2="3" />
              <line x1="12" y1="21" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12" y2="3" />
              <line x1="20" y1="21" x2="20" y2="16" />
              <line x1="20" y1="12" x2="20" y2="3" />
            </svg>
            <span class="item-text">Bình chọn</span>
          </div>
          <svg class="chevron-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>
      </div>

      <!-- Promo banner card at sidebar bottom -->
      <!-- User footer profile -->
    </div>

    <!-- Cột 3: Nội dung chính -->
    <div class="private-space-main">
      <!-- Topbar -->
      <div class="topbar">
        <div class="topbar-actions">
          <button class="icon-btn" title="Tìm kiếm">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </button>

          <button
            class="icon-btn notif-icon-btn"
            title="Thông báo"
            :class="{ active: notificationStore.isPanelOpen }"
            @click="toggleNotifications"
          >
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
            <span v-if="unreadNotifCount > 0" class="topbar-notif-badge">
              {{ unreadNotifCount > 99 ? '99+' : unreadNotifCount }}
            </span>
          </button>

          <div class="user-badge" title="Tài khoản">{{ myInitials }}</div>
        </div>
      </div>

      <!-- Main Body Scroll -->
      <div class="main-body-scroll">
        <div class="content-wrapper">

          <!-- ==================== TAB: TỔNG QUAN ==================== -->
          <template v-if="activeSubTab === 'overview'">
            <!-- 1. Hero Banner -->
            <div class="hero-banner">
              <div class="hero-text-side">
                <div class="hero-tag">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2l2.4 7.4H22l-6 4.6 2.3 7.4-6.3-4.6-6.3 4.6 2.3-7.4-6-4.6h7.6z" />
                  </svg>
                  <span>NGÀY MỚI, KẾT NỐI MỚI</span>
                </div>
                <h1 class="hero-title">
                  Chào bạn, {{ userProfile.displayName }}
                  <span class="sparkle-icon">✨</span>
                </h1>
                <p class="hero-subtitle">
                  Mọi điều quan trọng từ các cuộc trò chuyện của bạn, ngay tại đây.
                </p>
                <button class="btn-to-chat" @click="goToChat">
                  <span>Đến tin nhắn</span>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="5" y1="12" x2="19" y2="12" />
                    <polyline points="12 5 19 12 12 19" />
                  </svg>
                </button>
              </div>

              <!-- Floating 3D Cards Illustration -->
              <div class="hero-visual-side">
                <div class="floating-bubble-card small">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </svg>
                </div>
                <div class="floating-bubble-card large">
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="#2563eb" stroke="#2563eb" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                </div>
                <div class="decorative-star star-1">✦</div>
                <div class="decorative-star star-2">✦</div>
                <div class="decorative-star star-3">✦</div>
              </div>
            </div>

            <!-- 2. Section BẢNG TIN CỦA BẠN -->
            <div class="section-container">
              <span class="sub-section-tag">BẢNG TIN CỦA BẠN</span>
              <div class="section-title-row">
                <h2 class="section-title">Cập nhật gần đây</h2>
                <span class="date-tag">Hôm nay</span>
              </div>

              <!-- 3 Stat Cards: Tin nhắn + Lịch hẹn + Bình chọn -->
              <div class="stat-cards-grid">
                <StatCard
                  :count="formattedTotalUnreadCount"
                  label="Tin nhắn chưa đọc"
                  action-text="Xem tin nhắn"
                  variant="blue"
                  @action="goToChat"
                />

                <StatCard
                  :count="isLoadingToday ? '...' : todayReminderCount"
                  label="Lịch hẹn sắp tới"
                  action-text="Xem lịch hẹn"
                  variant="purple"
                  @action="goToCalendar"
                />

                <StatCard
                  :count="isLoadingPolls ? '...' : openPollsCount"
                  label="Bình chọn đang mở"
                  action-text="Tham gia ngay"
                  variant="teal"
                  @action="goToPolls"
                />
              </div>
            </div>

            <!-- 3. Dual Section: Lịch hẹn & Bình chọn trong nhóm -->
            <div class="overview-content-grid">
              <!-- Cột trái: Lịch hẹn sắp tới / hôm nay -->
              <div class="overview-column">
                <div v-if="todayReminders.length > 0" class="column-cards-stack">
                  <UpcomingEventCard
                    v-for="(item, idx) in todayReminders"
                    :key="item.reminderId ?? idx"
                    badge-text="LỊCH SẮP TỚI"
                    :title="item.title ?? '(Không có tiêu đề)'"
                    :description="item.description ?? ''"
                    :time-text="formatTimeRange(item.startAt, item.endAt)"
                    :group-name="item.roomName ?? 'Nhóm'"
                    :members="getParticipantMembers(item)"
                    @click="goToChatWithReminder(item)"
                  />
                </div>
                <div v-else-if="!isLoadingToday" class="no-event-hint">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                  <span>Hôm nay không có lịch hẹn nào sắp tới.</span>
                  <button class="link-btn" @click="goToCalendar">Xem tất cả lịch →</button>
                </div>
              </div>

              <!-- Cột phải: Bình chọn trong nhóm (đang mở) -->
              <div class="overview-column">
                <div v-if="openPolls.length > 0" class="column-cards-stack">
                  <GroupPollCard
                    v-for="(poll, pIdx) in openPolls"
                    :key="poll.pollId ?? pIdx"
                    badge-text="BÌNH CHỌN TRONG NHÓM"
                    :title="poll.question"
                    :subtitle="formatPollSubtitle(poll)"
                    :options="formatPollOptions(poll.options)"
                    :total-votes="poll.totalVoters || 0"
                    @click="goToChatWithPoll(poll)"
                  />
                </div>
                <div v-else-if="!isLoadingPolls" class="no-event-hint">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="4" y1="21" x2="4" y2="14" />
                    <line x1="4" y1="10" x2="4" y2="3" />
                    <line x1="12" y1="21" x2="12" y2="12" />
                    <line x1="12" y1="8" x2="12" y2="3" />
                    <line x1="20" y1="21" x2="20" y2="16" />
                    <line x1="20" y1="12" x2="20" y2="3" />
                  </svg>
                  <span>Hiện chưa có bình chọn nào đang mở.</span>
                  <button class="link-btn" @click="goToPolls">Xem tất cả bình chọn →</button>
                </div>
              </div>
            </div>
          </template>

          <!-- ==================== TAB: LỊCH SẮP TỚI ==================== -->
          <template v-else-if="activeSubTab === 'calendar'">
            <UpcomingEventList />
          </template>

          <!-- ==================== TAB: BÌNH CHỌN ==================== -->
          <template v-else-if="activeSubTab === 'polls'">
            <PollList />
          </template>

        </div>
      </div>
    </div>
  </div>

  <!-- Bảng thông báo xổ xuống góc trên bên phải (Kiểu A) -->
  <NotificationPopover />
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.app-container {
  display: grid;
  grid-template-columns: 80px 370px 1fr;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
}

/* Cột 2: Sidebar Trang chủ */
.private-space-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #ffffff;
  border-right: 1px solid #e6eaf2;
  box-sizing: border-box;
}

.nameApp {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 8px 24px;
}

.logo-title {
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(to right, #0a192f 0%, #1e3a8a 25%, #2563eb 50%, #3b82f6 75%, #60a5fa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

.menu-icon {
  font-size: 18px;
  color: #94a3b8;
  cursor: pointer;
  transition: color 0.2s ease;
}

.menu-icon:hover {
  color: #3b82f6;
}

.sidebar-header {
  padding: 10px 24px 14px 24px;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.home-icon-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: #e6edfd;
  border-radius: 10px;
  color: #2563eb;
}

.nav-category {
  padding: 8px 24px 6px 24px;
}

.category-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.6px;
  color: #94a3b8;
}

.sidebar-menu-list {
  flex: 1;
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 12px;
  cursor: pointer;
  color: #64748b;
  font-weight: 600;
  transition: all 0.15s ease;
  user-select: none;
}

.menu-item:hover {
  background-color: #f8fafd;
  color: #1e293b;
}

.menu-item.active {
  background-color: #eff6ff;
  color: #2563eb;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-icon {
  flex-shrink: 0;
}

.item-text {
  font-size: 14px;
}

.chevron-icon {
  color: #94a3b8;
  transition: transform 0.15s;
}

.menu-item:hover .chevron-icon {
  transform: translateX(2px);
}

.menu-item.active .chevron-icon {
  color: #2563eb;
}

/* Cột 3: Main Dashboard */
.private-space-main {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fd;
  overflow: hidden;
}

/* Topbar */
.topbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 40px;
  height: 70px;
  background-color: #ffffff;
  border-bottom: 1px solid #e6eaf2;
  flex-shrink: 0;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background-color: #f1f5f9;
  color: #1e293b;
}

.notif-icon-btn {
  position: relative;
}

.notif-icon-btn.active {
  background-color: #e6edfd;
  color: #2563eb;
}

.topbar-notif-badge {
  position: absolute;
  top: 1px;
  right: 1px;
  background-color: #ef4444;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ffffff;
  line-height: 1;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
  animation: pulse-badge 2s infinite ease-in-out;
}

@keyframes pulse-badge {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.user-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  background-color: #4b88f6;
  color: #ffffff;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.user-badge:hover {
  transform: scale(1.05);
}

/* Scroll Area */
.main-body-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 32px 48px;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 32px;
  max-width: 1080px;
  align-self: center;
  justify-self: center;
}

/* 1. Hero Banner */
.hero-banner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 50%, #60a5fa 100%);
  border-radius: 24px;
  padding: 36px 44px;
  color: #ffffff;
  overflow: hidden;
  box-shadow: 0 10px 25px -5px rgba(37, 99, 235, 0.25);
}

.hero-text-side {
  max-width: 60%;
  z-index: 2;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 8px;
}

.hero-title {
  font-size: 30px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sparkle-icon {
  font-size: 24px;
}

.hero-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 20px 0;
  line-height: 1.5;
}

.btn-to-chat {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: #ffffff;
  color: #2563eb;
  border: none;
  border-radius: 12px;
  padding: 10px 20px;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.btn-to-chat:hover {
  background-color: #f8fafc;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

/* Hero Visual Graphic (Floating 3D Blocks) */
.hero-visual-side {
  position: relative;
  width: 200px;
  height: 140px;
  z-index: 1;
}

.floating-bubble-card {
  position: absolute;
  background-color: #ffffff;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
  animation: floatAnim 4s ease-in-out infinite alternate;
}

.floating-bubble-card.large {
  width: 76px;
  height: 76px;
  right: 20px;
  top: 30px;
  transform: rotate(-6deg);
}

.floating-bubble-card.small {
  width: 52px;
  height: 52px;
  right: 90px;
  top: 10px;
  transform: rotate(8deg);
  animation-delay: -2s;
}

@keyframes floatAnim {
  0% { transform: translateY(0) rotate(-6deg); }
  100% { transform: translateY(-8px) rotate(-3deg); }
}

.decorative-star {
  position: absolute;
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

.star-1 { top: 0; right: 140px; }
.star-2 { bottom: 10px; right: 110px; font-size: 12px; }
.star-3 { top: 15px; right: 5px; font-size: 20px; }

/* 2. Section BẢNG TIN */
.section-container {
  display: flex;
  flex-direction: column;
}

.sub-section-tag {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: #64748b;
  text-transform: uppercase;
  margin-bottom: 4px;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.date-tag {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
}

/* Stat cards: 3 cột (Tin nhắn, Lịch hẹn, Bình chọn) */
.stat-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 900px) {
  .stat-cards-grid {
    grid-template-columns: 1fr;
  }
}

/* 3. Section 2 cột: Lịch hẹn & Bình chọn */
.overview-content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  align-items: start;
}

.overview-column {
  min-width: 0;
}

.column-cards-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

@media (max-width: 900px) {
  .overview-content-grid {
    grid-template-columns: 1fr;
  }
}

/* Hint khi không có lịch hẹn hoặc bình chọn */
.no-event-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
  background: #ffffff;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.link-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: #2563eb;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s;
}

.link-btn:hover {
  color: #1d4ed8;
}
</style>
