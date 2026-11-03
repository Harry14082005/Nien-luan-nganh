<script setup>
import { computed } from 'vue'
import IconMessageSquare from '@/components/icons/IconMessageSquare.vue'

const props = defineProps({
  contact: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['message'])

const palette = [
  { bg: '#FFE9DC', color: '#D2693B' },
  { bg: '#DDF5EA', color: '#2E9A6B' },
  { bg: '#FFF3D6', color: '#B8860B' },
  { bg: '#E3EDFF', color: '#3B6FD2' },
  { bg: '#ECE9FF', color: '#6B5BD2' },
  { bg: '#DCE7FE', color: '#2563EB' },
]

const avatarStyle = computed(() => {
  if (props.contact?.bgColor && props.contact?.color) {
    return {
      backgroundColor: props.contact.bgColor,
      color: props.contact.color,
    }
  }
  let hash = 0
  for (const ch of props.contact?.name || '') hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  const c = palette[hash % palette.length]
  return { backgroundColor: c.bg, color: c.color }
})

const avatarText = computed(() => {
  if (props.contact?.initials) return props.contact.initials.slice(0, 2).toUpperCase()
  const words = (props.contact?.name || '').trim().split(/\s+/).filter(Boolean)
  if (!words.length) return '?'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
})

const isOnline = computed(() => !!props.contact?.online)

const statusLabel = computed(() => {
  if (props.contact?.statusText) return props.contact.statusText
  return isOnline.value ? 'Đang hoạt động' : 'Ngoại tuyến'
})

function onSendMessage() {
  emit('message', props.contact)
}
</script>

<template>
  <div class="profile-card">
    <!-- Header Banner with decorative curves -->
    <div class="banner">
      <svg
        class="banner-pattern"
        viewBox="0 0 600 160"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        preserveAspectRatio="none"
      >
        <circle cx="500" cy="80" r="140" stroke="rgba(255, 255, 255, 0.12)" stroke-width="1.5" />
        <circle cx="500" cy="80" r="90" stroke="rgba(255, 255, 255, 0.1)" stroke-width="1.5" />
        <circle cx="500" cy="80" r="40" stroke="rgba(255, 255, 255, 0.08)" stroke-width="1.5" />
      </svg>
    </div>

    <!-- Main Card Body -->
    <div class="card-content">
      <!-- Avatar Section (overlapping the banner) -->
      <div class="avatar-container">
        <div class="avatar-wrapper">
          <img
            v-if="contact.avatar"
            :src="contact.avatar"
            :alt="contact.name"
            class="avatar-image"
          />
          <div v-else class="avatar-text" :style="avatarStyle">
            {{ avatarText }}
          </div>
          <span v-if="isOnline" class="status-dot"></span>
        </div>
      </div>

      <!-- User Info & Status -->
      <div class="info-container">
        <div class="name-status-row">
          <div class="name-column">
            <h2 class="user-name">{{ contact.name }}</h2>
            <p class="user-role">{{ contact.role || 'Thành viên' }}</p>
          </div>

          <div class="status-pill" :class="{ 'is-online': isOnline }">
            <span class="pill-dot"></span>
            <span class="pill-text">{{ statusLabel }}</span>
          </div>
        </div>

        <!-- Bio Quote -->
        <p class="user-bio">
          {{ contact.bio || 'Những cuộc trò chuyện ý nghĩa luôn bắt đầu bằng một lời chào.' }}
        </p>

        <!-- Divider line -->
        <div class="card-divider"></div>

        <!-- Actions Row (Filtered Layout: Primary Message Action) -->
        <div class="actions-row">
          <button class="btn-message" @click="onSendMessage">
            <IconMessageSquare class="btn-icon" :size="18" />
            <span>Nhắn tin</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.profile-card {
  font-family: 'Quicksand', sans-serif;
  background-color: #ffffff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  border: 1px solid #eef2f7;
  width: 100%;
  max-width: 680px;
}

/* Banner */
.banner {
  position: relative;
  height: 140px;
  background: linear-gradient(135deg, #3275ea 0%, #4686f5 60%, #68a0ff 100%);
  overflow: hidden;
}

.banner-pattern {
  position: absolute;
  top: 0;
  right: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

/* Card Content */
.card-content {
  padding: 0 32px 32px 32px;
}

/* Avatar container with negative margin to overlap banner */
.avatar-container {
  display: flex;
  align-items: flex-end;
  margin-top: -42px;
  margin-bottom: 16px;
}

.avatar-wrapper {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: 18px;
  background-color: #ffffff;
  padding: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 14px;
  object-fit: cover;
}

.avatar-text {
  width: 100%;
  height: 100%;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.status-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 14px;
  height: 14px;
  background-color: #10b981;
  border-radius: 50%;
  border: 3px solid #ffffff;
}

/* Info container */
.info-container {
  display: flex;
  flex-direction: column;
}

.name-status-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.name-column {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.user-role {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  margin: 0;
}

/* Status Pill */
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background-color: #f1f5f9;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

.status-pill.is-online {
  background-color: #ecfdf5;
  color: #059669;
}

.pill-dot {
  width: 7px;
  height: 7px;
  background-color: #94a3b8;
  border-radius: 50%;
}

.status-pill.is-online .pill-dot {
  background-color: #10b981;
}

/* Bio */
.user-bio {
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
  margin: 18px 0 0 0;
}

/* Divider */
.card-divider {
  height: 1px;
  background-color: #f1f5f9;
  margin: 24px 0 20px 0;
}

/* Actions Row */
.actions-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-message {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background-color: #2f5fd0;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  padding: 10px 22px;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 14px rgba(47, 95, 208, 0.25);
}

.btn-message:hover {
  background-color: #254eb2;
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(47, 95, 208, 0.35);
}

.btn-message:active {
  transform: translateY(0);
}

.btn-icon {
  flex-shrink: 0;
}
</style>

