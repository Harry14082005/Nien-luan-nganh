<script setup>
import { computed } from 'vue'
import UserAvatar from '@/components/common/UserAvatar.vue'

const props = defineProps({
  name: { type: String, required: true },
  avatar: { type: String, default: '' },
  initials: { type: String, default: '' },
  lastMessage: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  time: { type: String, default: '' },
  unread: { type: Number, default: 0 },
  hasUnread: { type: Boolean, default: false },
  online: { type: Boolean, default: false },
  active: { type: Boolean, default: false },
  avatarSize: { type: Number, default: 48 },
  avatarShape: { type: String, default: 'squircle' },
  avatarRadius: { type: String, default: '14px' },
})

const emit = defineEmits(['select'])

// Trạng thái in đậm:
// - Chỉ in đậm khi phòng CHƯA ĐỌC (unread > 0 hoặc hasUnread = true) và phòng KHÔNG active
// - Khi người dùng bấm vào phòng (active hoặc đã đọc): TỰ ĐỘNG MẤT IN ĐẬM
// - Nếu là hành động do chính mình thực hiện ("Bạn..."): KHÔNG in đậm
const isBold = computed(() => {
  if (props.active) return false
  const text = props.subtitle || props.lastMessage || ''
  if (text.startsWith('Bạn')) return false
  return Boolean(props.hasUnread || props.unread > 0)
})

const displayText = computed(() => props.subtitle || props.lastMessage || '')
</script>

<template>
  <div
    class="chat-item"
    :class="{ active, 'has-unread': isBold }"
    role="button"
    tabindex="0"
    @click="emit('select')"
    @keydown.enter="emit('select')"
  >
    <!-- Avatar -->
    <slot name="avatar">
      <div class="avatar-wrap">
        <UserAvatar
          :name="name"
          :src="avatar"
          :size="avatarSize"
          :shape="avatarShape"
          :radius="avatarRadius"
          :initials="initials"
        />
        <span v-if="online" class="online-dot"></span>
      </div>
    </slot>

    <!-- Nội dung -->
    <div class="content">
      <div class="top-row">
        <slot name="title">
          <span class="name">{{ name }}</span>
        </slot>
        <slot name="time">
          <span v-if="time" class="time">{{ time }}</span>
        </slot>
      </div>
      <div class="bottom-row">
        <slot name="subtitle" :is-bold="isBold">
          <span 
            v-if="displayText"
            class="last-message" 
            :class="{ 'last-message--bold': isBold }"
          >
            {{ displayText }}
          </span>
        </slot>
        <slot name="badge">
          <span v-if="unread > 0" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
        </slot>
      </div>
    </div>

    <!-- Tùy biến vùng thao tác bên phải (vd: menu 3 chấm, button) -->
    <slot name="right"></slot>
  </div>
</template>

<style scoped>
.chat-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
  border-radius: 0.8rem;
}
.chat-item:hover {
  background-color: #f3f6fd;
}
.chat-item.active {
  background-color: #eaf0fd;
}
/* Thanh xanh bên trái khi đang chọn */
.chat-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  height: 70%;
  align-self: center;
  border-radius: 1.25rem;
}

/* Avatar */
.avatar-wrap {
  position: relative;
  flex-shrink: 0;
  display: inline-flex;
}
.online-dot {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background-color: #22c55e;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.25);
  border: 1px solid #fff;
}

/* Nội dung */
.content {
  flex: 1;
  min-width: 0; /* cần để text-overflow hoạt động trong flex */
}
.top-row,
.bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.name {
  font-size: 15px;
  font-weight: 600;
  color: #1c2540;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time {
  flex-shrink: 0;
  font-size: 12px;
  color: #9aa3b5;
}
.bottom-row {
  margin-top: 4px;
}
.last-message {
  font-size: 13px;
  color: #8f98ab;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.last-message--bold,
.has-unread .last-message {
  font-weight: 700 !important;
  color: #1e293b !important;
}
.has-unread .last-message {
  color: #4a5470;
  font-weight: 500;
}

/* Badge số tin chưa đọc */
.badge {
  flex-shrink: 0;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 8px;
  background-color: #3b6fe0;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
