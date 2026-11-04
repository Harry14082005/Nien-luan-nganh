<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NavigationSidebar from '@/components/common/NavigationSidebar.vue'
import SettingToggle from '@/components/settings/SettingToggle.vue'
import UserProfileEdit from '@/components/settings/UserProfileEdit.vue'
import { getImageUrl } from '@/api/user.api'
import { http } from '@/api/http'

const router = useRouter()
const authStore = useAuthStore()

// Tab hiện tại: 'profile' (Hồ sơ cá nhân) hoặc 'general' (Tùy chọn chung)
const activeTab = ref('profile')

// Thông tin người dùng — load từ API
const userProfile = ref({
  displayName: '',
  username: '',
  initials: '',
  statusText: 'Đang hoạt động',
  bio: '',
  avatar: '',
  online: true,
})

const avatarSrc = computed(() => getImageUrl(userProfile.value.avatar))

function buildInitials(name = '') {
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (!words.length) return '??'
  return (words.length === 1 ? words[0].slice(0, 2) : words[0][0] + words[words.length - 1][0]).toUpperCase()
}

async function loadProfile() {
  try {
    const me = authStore.user || await authStore.initUser()
    if (me) {
      userProfile.value = {
        displayName: me.username || me.displayName || '',
        username: me.username || '',
        initials: buildInitials(me.username),
        statusText: 'Đang hoạt động',
        bio: me.description || '',
        avatar: me.avatarUrl || '',
        online: me.isOnline ?? true,
      }
    }
  } catch (e) {
    console.warn('Không tải được hồ sơ:', e)
  }
}

onMounted(loadProfile)

async function handleProfileSave(updatedUser) {
  userProfile.value = updatedUser
  // Nếu avatar thay đổi, cập nhật auth store
  if (authStore.user) {
    authStore.user = { ...authStore.user, avatarUrl: updatedUser.avatar }
    localStorage.setItem('currentUser', JSON.stringify(authStore.user))
  }
}

// Cài đặt tùy chọn chung
const settings = ref({
  messageNotifications: true,
  soundNotifications: false,
  readReceipts: true,
  darkMode: false,
})

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-container">
    <!-- Cột 1: Navigation Sidebar -->
    <NavigationSidebar current-page="setting" />

    <!-- Cột 2: Settings Sidebar Menu -->
    <div class="settings-sidebar">
      <!-- App Header -->
      <section class="nameApp">
        <div class="logo-title">bluespace.</div>
        <font-awesome-icon icon="bars" class="menu-icon" />
      </section>

      <!-- Sidebar Section Header -->
      <div class="sidebar-header">
        <span class="sub-label">TÀI KHOẢN CÁ NHÂN</span>
        <div class="title-row">
          <h1 class="page-title">{{ activeTab === 'profile' ? 'Hồ sơ' : 'Cài đặt' }}</h1>
          <button class="settings-action-btn" title="Tùy chọn">
            <svg
              width="18"
              height="18"
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
              <line x1="1" y1="14" x2="7" y2="14" />
              <line x1="9" y1="8" x2="15" y2="8" />
              <line x1="17" y1="16" x2="23" y2="16" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Navigation Category -->
      <div class="nav-category">
        <span class="category-title">TÙY CHỈNH</span>
      </div>

      <!-- Settings Menu Items -->
      <div class="settings-menu-list">
        <!-- Mục 1: Hồ sơ cá nhân -->
        <div
          class="menu-item"
          :class="{ active: activeTab === 'profile' }"
          role="button"
          tabindex="0"
          @click="activeTab = 'profile'"
          @keydown.enter="activeTab = 'profile'"
        >
          <div class="item-left">
            <svg
              class="item-icon"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <span class="item-text">Hồ sơ cá nhân</span>
          </div>
          <svg
            class="chevron-icon"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>

        <!-- Mục 2: Tùy chọn chung -->
        <div
          class="menu-item"
          :class="{ active: activeTab === 'general' }"
          role="button"
          tabindex="0"
          @click="activeTab = 'general'"
          @keydown.enter="activeTab = 'general'"
        >
          <div class="item-left">
            <svg
              class="item-icon"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="3" />
              <path
                d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"
              />
            </svg>
            <span class="item-text">Tùy chọn chung</span>
          </div>
          <svg
            class="chevron-icon"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </div>
      </div>

      <!-- Bottom Mini Profile -->
      <div class="sidebar-user-footer">
        <div class="footer-avatar">
          <img v-if="avatarSrc" :src="avatarSrc" :alt="userProfile.displayName" class="footer-avatar-img" />
          <span v-else>{{ userProfile.initials || 'HN' }}</span>
        </div>
        <div class="footer-info">
          <div class="footer-name">{{ userProfile.displayName }}</div>
          <div class="footer-status">{{ userProfile.statusText || 'Đang hoạt động' }}</div>
        </div>
        <span class="footer-dot"></span>
      </div>
    </div>

    <!-- Cột 3: Nội dung chính của Cài đặt -->
    <div class="settings-content-view">
      <!-- 1. Giao diện Hồ sơ cá nhân -->
      <div v-if="activeTab === 'profile'" class="tab-pane">
        <!-- Header -->
        <div class="content-header">
          <span class="tag-label">HỒ SƠ CÁ NHÂN</span>
          <h1 class="heading-title">Xin chào, {{ userProfile.displayName || 'bạn' }}.</h1>
          <p class="heading-desc">Một chút về bạn, để mọi người hiểu nhau hơn.</p>
        </div>

        <!-- Component chỉnh sửa & hiển thị hồ sơ -->
        <div class="profile-component-wrap">
          <UserProfileEdit
            :user="userProfile"
            @save="handleProfileSave"
          />
        </div>
      </div>

      <!-- 2. Giao diện Tùy chọn chung -->
      <div v-else-if="activeTab === 'general'" class="tab-pane">
        <!-- Header -->
        <div class="content-header">
          <span class="tag-label">CÀI ĐẶT</span>
          <h1 class="heading-title">Theo cách của bạn.</h1>
          <p class="heading-desc">Tùy chỉnh BlueSpace để mỗi kết nối đều thoải mái hơn.</p>
        </div>

        <div class="settings-cards-wrap">
          <!-- Card 1: Thông báo -->
          <div class="settings-card">
            <div class="card-title-group">
              <div class="card-icon-badge blue">
                <svg
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                  <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                </svg>
              </div>
              <div class="card-title-text">
                <h3>Thông báo</h3>
                <p>Chọn cách bạn nhận cập nhật</p>
              </div>
            </div>

            <div class="toggle-list">
              <!-- Thông báo tin nhắn -->
              <SettingToggle
                v-model="settings.messageNotifications"
                title="Thông báo tin nhắn"
                description="Nhận thông báo khi có tin nhắn mới"
              />

              <!-- Âm thanh thông báo (giữ trong code nhưng ẩn theo yêu cầu) -->
              <SettingToggle
                v-if="false"
                v-model="settings.soundNotifications"
                title="Âm thanh thông báo"
                description="Phát âm thanh cho các cập nhật mới"
              />
            </div>
          </div>

          <!-- Card 2: Quyền riêng tư & giao diện -->
          <div class="settings-card">
            <div class="card-title-group">
              <div class="card-icon-badge purple">
                <svg
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                </svg>
              </div>
              <div class="card-title-text">
                <h3>Quyền riêng tư & giao diện</h3>
                <p>Một không gian an toàn, đúng ý bạn</p>
              </div>
            </div>

            <div class="toggle-list">
              <!-- Hiển thị đã xem -->
              <SettingToggle
                v-model="settings.readReceipts"
                title="Hiển thị đã xem"
                description="Cho người khác biết khi bạn đã đọc tin nhắn"
              />

              <div class="card-inner-divider"></div>

              <!-- Chế độ tối -->
              <SettingToggle
                v-model="settings.darkMode"
                title="Chế độ tối"
                description="Chuyển sang giao diện dịu mắt hơn"
              />
            </div>
          </div>

          <!-- Quick Navigation Actions -->
          <div class="action-links-group">
            <!-- Xem và chỉnh sửa hồ sơ -->
            <div
              class="action-item"
              role="button"
              tabindex="0"
              @click="activeTab = 'profile'"
              @keydown.enter="activeTab = 'profile'"
            >
              <div class="action-left">
                <svg
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                <span>Xem và chỉnh sửa hồ sơ của bạn</span>
              </div>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>

            <!-- Đăng xuất khỏi BlueSpace -->
            <div
              class="action-item danger"
              role="button"
              tabindex="0"
              @click="handleLogout"
              @keydown.enter="handleLogout"
            >
              <div class="action-left">
                <svg
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                <span>Đăng xuất khỏi BlueSpace</span>
              </div>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.app-container {
  display: grid;
  grid-template-columns: 80px 430px 1fr;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
}

/* Cột 2: Settings Sidebar */
.settings-sidebar {
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
  background: linear-gradient(
    to right,
    #0a192f 0%,
    #1e3a8a 25%,
    #2563eb 50%,
    #3b82f6 75%,
    #60a5fa 100%
  );
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

.sub-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: #94a3b8;
  text-transform: uppercase;
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

.settings-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: #e6edfd;
  border: none;
  border-radius: 10px;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.2s ease;
}

.settings-action-btn:hover {
  background-color: #dbeafe;
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

.settings-menu-list {
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

/* Sidebar Bottom Mini Profile */
.sidebar-user-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
  background-color: #ffffff;
}

.footer-avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background-color: #3b82f6;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.footer-info {
  flex: 1;
  min-width: 0;
}

.footer-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.footer-status {
  font-size: 12px;
  color: #64748b;
}

.footer-dot {
  width: 8px;
  height: 8px;
  background-color: #10b981;
  border-radius: 50%;
  flex-shrink: 0;
}

.footer-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  object-fit: cover;
  display: block;
}

/* Cột 3: Main Settings View */
.settings-content-view {
  flex: 1;
  height: 100%;
  background-color: #f8f9fd;
  overflow-y: auto;
  padding: 40px 48px;
  box-sizing: border-box;
}

.tab-pane {
  display: flex;
  flex-direction: column;
}

.content-header {
  margin-bottom: 30px;
}

.tag-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: #64748b;
  text-transform: uppercase;
}

.heading-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 6px 0 8px 0;
}

.heading-desc {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.profile-component-wrap {
  display: flex;
  flex-direction: column;
}

/* General Options Cards */
.settings-cards-wrap {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 780px;
}

.settings-card {
  background-color: #ffffff;
  border-radius: 20px;
  padding: 24px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.card-title-group {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.card-icon-badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-icon-badge.blue {
  background-color: #eff6ff;
  color: #2563eb;
}

.card-icon-badge.purple {
  background-color: #f5f3ff;
  color: #7c3aed;
}

.card-title-text h3 {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 2px 0;
}

.card-title-text p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.toggle-list {
  display: flex;
  flex-direction: column;
}

.card-inner-divider {
  height: 1px;
  background-color: #f1f5f9;
  margin: 4px 20px;
}

/* Action Links */
.action-links-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background-color: #ffffff;
  border-radius: 16px;
  border: 1px solid #eef2f7;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  cursor: pointer;
  color: #334155;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.15s ease;
  user-select: none;
}

.action-item:hover {
  background-color: #f8fafc;
  transform: translateY(-1px);
}

.action-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-item.danger {
  color: #dc2626;
}

.action-item.danger:hover {
  background-color: #fef2f2;
}
</style>
