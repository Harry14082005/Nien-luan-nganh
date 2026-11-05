<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import NavigationSidebar from '@/components/common/NavigationSidebar.vue'
import StatCard from '@/components/privatespace/StatCard.vue'
import UpcomingEventCard from '@/components/privatespace/UpcomingEventCard.vue'
import GroupPollCard from '@/components/privatespace/GroupPollCard.vue'

const router = useRouter()

// Tab khám phá đang chọn trong Sidebar: 'overview' | 'calendar' | 'polls'
const activeSubTab = ref('overview')

function goToChat() {
  router.push('/chat')
}
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
        <font-awesome-icon icon="bars" class="menu-icon" />
      </section>

      <!-- Section Title -->
      <div class="sidebar-header">
        <span class="sub-label">HÔM NAY</span>
        <div class="title-row">
          <h1 class="page-title">Trang chủ</h1>
          <div class="home-icon-box" title="Trang chủ">
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
              <path
                d="M12 2l2.4 7.4H22l-6 4.6 2.3 7.4-6.3-4.6-6.3 4.6 2.3-7.4-6-4.6h7.6z"
              />
            </svg>
            <span class="item-text">Tổng quan</span>
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
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
            <span class="item-text">Lịch sắp tới</span>
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
              <line x1="4" y1="21" x2="4" y2="14" />
              <line x1="4" y1="10" x2="4" y2="3" />
              <line x1="12" y1="21" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12" y2="3" />
              <line x1="20" y1="21" x2="20" y2="16" />
              <line x1="20" y1="12" x2="20" y2="3" />
            </svg>
            <span class="item-text">Bình chọn</span>
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
      <!-- Đã loại bỏ khung màu xanh "Mọi thứ trong tầm tay..." và phần user ở dưới theo yêu cầu -->
    </div>

    <!-- Cột 3: Nội dung chính của Trang chủ -->
    <div class="private-space-main">
      <!-- Topbar -->
      <div class="topbar">
        <div class="breadcrumb-area">
          <span class="live-dot"></span>
          <span class="breadcrumb-text">Không gian làm việc của bạn</span>
        </div>

        <div class="topbar-actions">
          <button class="icon-btn" title="Tìm kiếm">
            <svg
              width="19"
              height="19"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </button>

          <button class="icon-btn" title="Thông báo">
            <svg
              width="19"
              height="19"
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
          </button>

          <div class="user-badge" title="Tài khoản">HN</div>
        </div>
      </div>

      <!-- Main Body Container -->
      <div class="main-body-scroll">
        <div class="content-wrapper">
          <!-- 1. Hero Banner -->
          <div class="hero-banner">
            <div class="hero-text-side">
              <div class="hero-tag">
                <svg
                  width="13"
                  height="13"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path d="M12 2l2.4 7.4H22l-6 4.6 2.3 7.4-6.3-4.6-6.3 4.6 2.3-7.4-6-4.6h7.6z" />
                </svg>
                <span>NGÀY MỚI, KẾT NỐI MỚI</span>
              </div>
              <h1 class="hero-title">
                Chào bạn, Nam
                <span class="sparkle-icon">✨</span>
              </h1>
              <p class="hero-subtitle">
                Mọi điều quan trọng từ các cuộc trò chuyện của bạn, ngay tại đây.
              </p>
              <button class="btn-to-chat" @click="goToChat">
                <span>Đến tin nhắn</span>
                <svg
                  width="15"
                  height="15"
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

            <!-- Floating 3D Cards Illustration -->
            <div class="hero-visual-side">
              <div class="floating-bubble-card small">
                <svg
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#3b82f6"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                  <line x1="16" y1="2" x2="16" y2="6" />
                  <line x1="8" y1="2" x2="8" y2="6" />
                  <line x1="3" y1="10" x2="21" y2="10" />
                </svg>
              </div>

              <div class="floating-bubble-card large">
                <svg
                  width="28"
                  height="28"
                  viewBox="0 0 24 24"
                  fill="#2563eb"
                  stroke="#2563eb"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
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

            <!-- 3 Stat Cards Grid -->
            <div class="stat-cards-grid">
              <StatCard
                count="02"
                label="Tin nhắn chưa đọc"
                action-text="Xem tin nhắn"
                variant="blue"
                @action="goToChat"
              />

              <StatCard
                count="01"
                label="Lịch hẹn sắp tới"
                action-text="Xem lịch hẹn"
                variant="purple"
              />

              <StatCard
                count="01"
                label="Bình chọn đang mở"
                action-text="Tham gia ngay"
                variant="teal"
              />
            </div>
          </div>

          <!-- 3. Interactive Cards Grid (Lịch sắp tới & Bình chọn) -->
          <div class="interactive-grid">
            <UpcomingEventCard />
            <GroupPollCard />
          </div>

          <!-- Đã loại bỏ phần banner dưới "Ý tưởng và tệp quan trọng..." theo yêu cầu -->
        </div>
      </div>
    </div>
  </div>
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
  justify-content: space-between;
  padding: 0 40px;
  height: 70px;
  background-color: #ffffff;
  border-bottom: 1px solid #e6eaf2;
  flex-shrink: 0;
}

.breadcrumb-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.live-dot {
  width: 8px;
  height: 8px;
  background-color: #10b981;
  border-radius: 50%;
}

.breadcrumb-text {
  font-size: 15px;
  font-weight: 600;
  color: #475569;
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
  0% {
    transform: translateY(0) rotate(-6deg);
  }
  100% {
    transform: translateY(-8px) rotate(-3deg);
  }
}

.decorative-star {
  position: absolute;
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

.star-1 {
  top: 0;
  right: 140px;
}

.star-2 {
  bottom: 10px;
  right: 110px;
  font-size: 12px;
}

.star-3 {
  top: 15px;
  right: 5px;
  font-size: 20px;
}

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

.stat-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* 3. Interactive Cards Grid */
.interactive-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}
</style>
