<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  currentPage: {
    type: String,
    default: '',
  },
})
const auth = useAuthStore()
const { user: me } = storeToRefs(auth)

const myInitials = computed(() => {
  const w = (me.value?.username || '').trim().split(/\s+/).filter(Boolean)
  if (!w.length) return '  '
  return (w.length === 1 ? w[0].slice(0, 2) : w[0][0] + w[w.length - 1][0]).toUpperCase()
})

onMounted(() => auth.initUser())

const router = useRouter()
const route = useRoute()

const activeTab = computed(() => {
  if (props.currentPage) return props.currentPage
  if (!route) return 'message'
  if (route.path.startsWith('/contacts')) return 'contactList'
  if (route.path.startsWith('/chat')) return 'message'
  if (route.path.startsWith('/private-space')) return 'home'
  if (route.path.startsWith('/settings')) return 'setting'
  return 'message'
})

function navigateTo(page, targetPath) {
  if (route && route.path === targetPath) return
  router.push(targetPath)
}
</script>

<template>
  <div class="sidebar">
    <font-awesome-icon
      class="logo"
      icon="comment"
      title="bluespace"
      @click="navigateTo('message', '/chat')"
    />

    <div class="navigation">
      <div
        class="nav-item"
        :class="{ active: activeTab === 'home' }"
        title="Không gian của tôi"
        role="button"
        tabindex="0"
        @click="navigateTo('home', '/private-space')"
        @keydown.enter="navigateTo('home', '/private-space')"
      >
        <div class="icon-box">
          <font-awesome-icon class="nav-icon" :icon="['far', 'house']" />
          <span class="dot"></span>
        </div>
      </div>

      <!-- Tin nhắn / Chat -->
      <div
        class="nav-item"
        :class="{ active: activeTab === 'message' }"
        title="Tin nhắn"
        role="button"
        tabindex="0"
        @click="navigateTo('message', '/chat')"
        @keydown.enter="navigateTo('message', '/chat')"
      >
        <div class="icon-box">
          <font-awesome-icon class="nav-icon" :icon="['far', 'message']" />
          <span class="dot"></span>
        </div>
      </div>

      <!-- Danh bạ / Contacts -->
      <div
        class="nav-item"
        :class="{ active: activeTab === 'contactList' }"
        title="Danh bạ"
        role="button"
        tabindex="0"
        @click="navigateTo('contactList', '/contacts')"
        @keydown.enter="navigateTo('contactList', '/contacts')"
      >
        <div class="icon-box">
          <font-awesome-icon class="nav-icon" icon="user-group" />
          <span class="dot"></span>
        </div>
      </div>

      <!-- Cài đặt / Settings -->
      <div
        class="nav-item"
        :class="{ active: activeTab === 'setting' }"
        title="Cài đặt"
        role="button"
        tabindex="0"
        @click="navigateTo('setting', '/settings')"
        @keydown.enter="navigateTo('setting', '/settings')"
      >
        <div class="icon-box">
          <font-awesome-icon class="nav-icon" icon="gear" />
          <span class="dot"></span>
        </div>
      </div>
    </div>

    <!-- Tài khoản -->
    <div
      class="account"
      title="Tài khoản"
      role="button"
      tabindex="0"
      @click="navigateTo('setting', '/settings')"
      @keydown.enter="navigateTo('setting', '/settings')"
    >
      {{ myInitials }}
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.sidebar {
  display: flex;
  flex-direction: column;
  font-family: 'Quicksand', sans-serif;
  font-size: 100%;
  font-weight: 600;
  width: 80px;
  height: 100%;
  background-color: #0f2547;
  color: #e6edfd;
  align-self: center;
  justify-content: space-between;
  align-items: center;
  box-sizing: border-box;
}

.logo {
  font-size: 20px;
  margin-top: 15px;
  color: white;
  padding: 12px;
  background-color: #2f5fd0;
  border-radius: 1rem;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    background-color 0.15s ease;
}

.logo:hover {
  background-color: #254eb2;
  transform: scale(1.05);
}

.navigation {
  display: flex;
  flex-direction: column;
  gap: 40px;
  font-size: 27px;
  margin-bottom: 25vh;
  margin-top: 40px;
  color: #6b81a2;
}

.nav-item {
  position: relative;
  width: 100%;
  display: flex;
  justify-content: center;
  cursor: pointer;
}

.icon-box {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 48px;
  height: 48px;
  border-radius: 0.7rem;
  color: #6b81a2;
  transition: all 0.2s ease-in;
}

.nav-icon {
  padding: 7px;
  transition: all 0.2s ease-in;
}

.nav-item:hover .icon-box {
  color: white;
  background-color: #294a86;
}

.nav-item.active .icon-box {
  background-color: #294a86;
  color: white;
}

/* === DẤU GẠCH TRẠNG THÁI ACTIVE === */
.nav-item.active .icon-box::before {
  content: '';
  position: absolute;
  left: -13.5px; /* Đẩy vạch ra sát mép trái của thanh sidebar */
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 28px;
  background-color: #799bf1;
  border-radius: 0 4px 4px 0;
}

/* === ĐỊNH DẠNG DẤU CHẤM === */
.dot {
  display: none; /* Ẩn mặc định ở tất cả các nút */
  position: absolute;
  top: 6px;
  right: 6px;
  width: 10px;
  height: 10px;
  background-color: #799bf1;
  border-radius: 50%;
  border: 2px solid #294a86;
}

/* CHỈ HIỂN THỊ DẤU CHẤM Ở NÚT ĐANG ACTIVE */
.nav-item.active .dot {
  display: block;
}

.account {
  margin-bottom: 20px;
  background-color: #6ba3f5;
  padding: 10px;
  border-radius: 0.9rem;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    background-color 0.15s ease;
  user-select: none;
}

.account:hover {
  background-color: #5392ee;
  transform: scale(1.05);
}
</style>
