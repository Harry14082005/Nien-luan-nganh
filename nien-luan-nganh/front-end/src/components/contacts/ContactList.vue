<script setup>
import { ref, computed } from 'vue'
import Modal_AddFriend from './Modal_AddFriend.vue'
import Modal_FriendRequests from './Modal_FriendRequests.vue'
import ContactMenu from './ContactMenu.vue'
import { removeFriendship } from '@/api/contacts.api.js'
import { blockFriendship } from '@/api/contacts.api.js'

const emit = defineEmits(['select', 'refresh'])

async function handleRemove(contact) {
  if (!confirm(`Xóa ${contact.name} khỏi danh sách bạn bè?`)) return
  try {
    await removeFriendship(contact.friendshipId)
    emit('refresh')
  } catch (e) {
    console.error('Lỗi xóa bạn:', e)
    alert(e.response?.data?.message || 'Không xóa được bạn')
  }
}

async function handleBlock(contact) {
  if (!confirm(`Chặn ${contact.name}?`)) return
  try {
    await blockFriendship(contact.friendshipId)
    emit('refresh')
  } catch (e) {
    console.error('Lỗi chặn người dùng:', e)
    alert(e.response?.data?.message || 'Không chặn được người này')
  }
}

const showNewAddFriendModal = ref(false)
const showNewFriendRequestsModal = ref(false)
const props = defineProps({
  contacts: {
    type: Array,
    default: () => [],
  },
  selectedId: {
    type: [Number, String],
    default: 0,
  },
})

const searchQuery = ref('')

const filteredContacts = computed(() => {
  if (!searchQuery.value.trim()) return props.contacts
  const q = searchQuery.value.toLowerCase().trim()
  return props.contacts.filter(
    (c) => c.name.toLowerCase().includes(q) || (c.role && c.role.toLowerCase().includes(q)),
  )
})

const palette = [
  { bg: '#FFE9DC', color: '#D2693B' },
  { bg: '#DDF5EA', color: '#2E9A6B' },
  { bg: '#FFF3D6', color: '#B8860B' },
  { bg: '#E3EDFF', color: '#3B6FD2' },
  { bg: '#ECE9FF', color: '#6B5BD2' },
  { bg: '#DCE7FE', color: '#2563EB' },
]

function getAvatarStyle(contact) {
  if (contact.bgColor && contact.color) {
    return { backgroundColor: contact.bgColor, color: contact.color }
  }
  let hash = 0
  for (const ch of contact.name || '') hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  const c = palette[hash % palette.length]
  return { backgroundColor: c.bg, color: c.color }
}

function getInitials(contact) {
  if (contact.initials) return contact.initials.slice(0, 2).toUpperCase()
  const words = (contact.name || '').trim().split(/\s+/).filter(Boolean)
  if (!words.length) return '?'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
}
</script>

<template>
  <div class="contact-list-sidebar">
    <!-- App Header -->
    <section class="nameApp">
      <div class="logo-title">bluespace.</div>
      <font-awesome-icon
        icon="user-plus"
        class="menu-icon"
        @click="showNewFriendRequestsModal = true"
      />
    </section>

    <!-- Page Header -->
    <section class="header-section">
      <div class="header-content">
        <div class="title-row">
          <div class="left-title">
            <h1 class="page-title">Danh bạ</h1>
            <span class="contact-count">{{ filteredContacts.length }}</span>
          </div>
          <button
            class="add-button"
            title="Thêm liên hệ"
            style="cursor: pointer"
            @click="showNewAddFriendModal = true"
          >
            <font-awesome-icon icon="plus" />
          </button>
        </div>
      </div>
    </section>

    <!-- Search Bar -->
    <div class="search-container">
      <div class="search-box">
        <font-awesome-icon icon="magnifying-glass" class="search-icon" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Tìm trong danh bạ"
          spellcheck="false"
        />
      </div>
    </div>

    <!-- Category Section -->
    <div class="category-header">
      <span>TẤT CẢ LIÊN HỆ</span>
    </div>

    <!-- Contact Items List -->
    <div class="contacts-scroll-area">
      <div
        v-for="contact in filteredContacts"
        :key="contact.id"
        class="contact-item"
        :class="{ active: contact.id === selectedId }"
        role="button"
        tabindex="0"
        @click="emit('select', contact)"
        @keydown.enter="emit('select', contact)"
      >
        <!-- Avatar -->
        <div class="avatar-wrap">
          <img
            v-if="contact.avatar"
            :src="contact.avatar"
            :alt="contact.name"
            class="avatar avatar-img"
          />
          <div v-else class="avatar avatar-text" :style="getAvatarStyle(contact)">
            {{ getInitials(contact) }}
          </div>
          <span v-if="contact.online" class="online-indicator"></span>
        </div>

        <!-- Contact Info -->
        <div class="contact-info">
          <span class="contact-name">{{ contact.name }}</span>
          <span class="contact-role">{{ contact.role }}</span>
        </div>

        <div class="right-icon-wrap">
          <ContactMenu @block="handleBlock(contact)" @remove="handleRemove(contact)" />
        </div>
      </div>

      <div v-if="filteredContacts.length === 0" class="empty-search">
        <span>Không tìm thấy liên hệ phù hợp</span>
      </div>
    </div>
    <Modal_AddFriend v-if="showNewAddFriendModal" @close="showNewAddFriendModal = false" />
    <Modal_FriendRequests
      v-if="showNewFriendRequestsModal"
      @close="showNewFriendRequestsModal = false"
      @accepted="emit('refresh')"
    />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.contact-list-sidebar {
  display: flex;
  flex-direction: column;
  font-family: 'Quicksand', sans-serif;
  width: 100%;
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

.header-section {
  padding: 10px 24px 14px 24px;
}

.header-content {
  display: flex;
  flex-direction: column;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.contact-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #3b82f6;
  background-color: #e6edfd;
  border-radius: 8px;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
}

.add-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: #e6edfd;
  border: none;
  border-radius: 10px;
  color: #2563eb;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.add-button:hover {
  background-color: #dbeafe;
  transform: translateY(-1px);
}

.search-container {
  padding: 0 24px 16px 24px;
}

.search-box {
  display: flex;
  align-items: center;
  background-color: #f8f9fd;
  border: 1px solid #e6eaf2;
  border-radius: 12px;
  padding: 8px 14px;
  transition: all 0.2s ease;
}

.search-box:focus-within {
  border-color: #3b82f6;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-icon {
  color: #94a3b8;
  font-size: 15px;
  margin-right: 10px;
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.search-box input::placeholder {
  color: #94a3b8;
}

/* Category Header */
.category-header {
  padding: 0 24px 10px 24px;
}

.category-header span {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.6px;
  color: #94a3b8;
}

/* Contacts Scroll Area */
.contacts-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* Contact Item */
.contact-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
}

.contact-item:hover {
  background-color: #f8fafd;
}

.contact-item.active {
  background-color: #edf3ff;
}

/* Avatar */
.avatar-wrap {
  position: relative;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  margin-right: 14px;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  object-fit: cover;
}

.avatar-text {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.online-indicator {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  background-color: #10b981;
  border-radius: 50%;
  border: 2px solid #ffffff;
}

/* Contact Info */
.contact-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.contact-name {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.contact-role {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Arrow Right */
.right-icon-wrap {
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
  transition: transform 0.15s ease;
}
.right-icon-wrap:hover {
  color: #2563eb;
}

.contact-item:hover .arrow-wrap {
  transform: translateX(2px);
  color: #64748b;
}

.empty-search {
  text-align: center;
  padding: 30px 10px;
  color: #94a3b8;
  font-size: 14px;
}
</style>
