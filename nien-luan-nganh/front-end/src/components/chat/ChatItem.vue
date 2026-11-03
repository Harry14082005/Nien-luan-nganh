<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  avatar: { type: String, default: '' },      // URL ảnh, để trống thì hiện chữ
  initials: { type: String, default: '' },    // tự đặt chữ viết tắt (vd: 'TK'), để trống thì tự tính
  lastMessage: { type: String, default: '' },
  time: { type: String, default: '' },
  unread: { type: Number, default: 0 },
  hasUnread: { type: Boolean, default: false },
  online: { type: Boolean, default: false },
  active: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

// Nếu ảnh lỗi (404...) thì quay về avatar chữ
const imgError = ref(false)
watch(() => props.avatar, () => (imgError.value = false))
const showImage = computed(() => !!props.avatar && !imgError.value)

// Chữ viết tắt: ưu tiên prop, không có thì lấy chữ cái đầu của từ đầu và từ cuối
const avatarText = computed(() => {
  if (props.initials) return props.initials.slice(0, 2).toUpperCase()
  const words = props.name.trim().split(/\s+/).filter(Boolean)
  if (!words.length) return '?'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
})

// Mỗi tên luôn cho ra cùng một màu (băm tên -> chọn trong bảng màu)
const palette = [
  { bg: '#ECE9FF', color: '#6B5BD2' }, // tím
  { bg: '#FFE9DC', color: '#D2693B' }, // cam
  { bg: '#E3EDFF', color: '#3B6FD2' }, // xanh dương
  { bg: '#DDF5EA', color: '#2E9A6B' }, // xanh lá
  { bg: '#FFE3EC', color: '#D23B73' }, // hồng
  { bg: '#FFF3D6', color: '#B8860B' }, // vàng
]
const avatarStyle = computed(() => {
  let hash = 0
  for (const ch of props.name) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  const c = palette[hash % palette.length]
  return { backgroundColor: c.bg, color: c.color }
})

// Trạng thái in đậm:
// - Chỉ in đậm khi phòng CHƯA ĐỌC (unread > 0 hoặc hasUnread = true) và phòng KHÔNG active
// - Khi người dùng bấm vào phòng (active hoặc đã đọc): TỰ ĐỘNG MẤT IN ĐẬM
// - Nếu là hành động do chính mình thực hiện ("Bạn..."): KHÔNG in đậm
const isBold = computed(() => {
  if (props.active) return false
  if (props.lastMessage && props.lastMessage.startsWith('Bạn')) return false
  return Boolean(props.hasUnread || props.unread > 0)
})
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
    <div class="avatar-wrap">
      <img
        v-if="showImage"
        class="avatar avatar-img"
        :src="avatar"
        :alt="name"
        @error="imgError = true"
      />
      <div v-else class="avatar avatar-text" :style="avatarStyle">
        {{ avatarText }}
      </div>
      <span v-if="online" class="online-dot"></span>
    </div>

    <!-- Nội dung -->
    <div class="content">
      <div class="top-row">
        <span class="name">{{ name }}</span>
        <span class="time">{{ time }}</span>
      </div>
      <div class="bottom-row">
        <span 
          class="last-message" 
          :class="{ 'last-message--bold': isBold }"
        >
          {{ lastMessage }}
        </span>
        <span v-if="unread > 0" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
      </div>
    </div>
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
  background-color: #3b6fe0;
  align-self: center;
  border-radius: 1.25rem;
}

/* Avatar */
.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}
.avatar {
  width: 48px;
  height: 48px;
  border-radius: 14px;
}
.avatar-img {
  object-fit: cover;
  display: block;
}
.avatar-text {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  user-select: none;
}
.online-dot {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background-color: #22c55e;
  border: 2px solid #fff;
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