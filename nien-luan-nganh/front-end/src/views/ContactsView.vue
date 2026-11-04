<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { getFriends, toContact } from '@/api/contacts.api'
import { getMe } from '@/api/auth.api'
const router = useRouter()

import NavigationSidebar from '@/components/common/NavigationSidebar.vue'
import ContactList from '@/components/contacts/ContactList.vue'
import ContactProfileCard from '@/components/contacts/ContactProfileCard.vue'

const contacts = ref([])
const selectedContactId = ref(null)
const me = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const selectedContact = computed(() => {
  return contacts.value.find((c) => c.id === selectedContactId.value) || contacts.value[0]
})

const myInitials = computed(() => {
  const w = (me.value?.username || '').trim().split(/\s+/).filter(Boolean)
  if (!w.length) return '?'
  return (w.length === 1 ? w[0].slice(0, 2) : w[0][0] + w[w.length - 1][0]).toUpperCase()
})

// Tải danh bạ (bạn bè đã ACCEPTED) + thông tin cá nhân
async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [res, profile] = await Promise.all([getFriends(), getMe()])

    const list = Array.isArray(res) ? res : res?.content || []
    contacts.value = list.map(toContact)
    me.value = profile

    // Giữ nguyên người đang chọn khi tải lại (ví dụ sau khi chấp nhận kết bạn)
    const stillExists = contacts.value.some((c) => c.id === selectedContactId.value)
    if (!stillExists) selectedContactId.value = contacts.value[0]?.id ?? null
  } catch (e) {
    console.error('Lỗi tải danh bạ:', e)
    errorMsg.value = e.response?.data?.message || 'Không tải được danh bạ'
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

function onSelectContact(contact) {
  selectedContactId.value = contact.id
}

function handleMessageContact() {
  router.push('/chat')
}
</script>

<template>
  <div class="app-container">
    <!-- Cột 1: Navigation Sidebar -->
    <NavigationSidebar current-page="contactList" />

    <!-- Cột 2: Danh sách liên hệ -->
    <div class="ContactListSidebar">
      <ContactList
        :contacts="contacts"
        :selected-id="selectedContactId"
        @select="onSelectContact"
        @refresh="loadData"
      />
    </div>

    <!-- Cột 3: Chi tiết liên hệ -->
    <div class="contacts-view">
      <!-- Header / Topbar -->
      <div class="contacts-topbar">
        <div class="topbar-actions">
          <button class="topbar-btn" title="Thông báo">
            <svg
              width="20"
              height="20"
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
          <div class="user-avatar-badge">{{ myInitials }}</div>
        </div>
      </div>

      <!-- Main Body Content -->
      <div class="contacts-body">
        <!-- Title & Introduction Section -->
        <p v-if="loading" class="state-msg">Đang tải danh bạ...</p>
        <p v-if="errorMsg" class="state-msg state-error">{{ errorMsg }}</p>

        <!-- Contact Profile Card -->
        <div class="card-display-area">
          <ContactProfileCard
            v-if="selectedContact"
            :contact="selectedContact"
            @message="handleMessageContact"
          />
          <p v-else-if="!loading && !errorMsg" class="state-msg">
            Bạn chưa có liên hệ nào. Bấm dấu + để thêm bạn bè.
          </p>
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

.ContactListSidebar {
  height: 100%;
  overflow: hidden;
}

.contacts-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f8f9fd;
  border-left: 1px solid #e6eaf2;
  overflow-y: auto;
}

/* Topbar */
.contacts-topbar {
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

.topbar-btn {
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

.topbar-btn:hover {
  background-color: #f1f5f9;
  color: #4b88f6;
}

.user-avatar-badge {
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

.user-avatar-badge:hover {
  transform: scale(1.05);
}

/* Body Content */
.contacts-body {
  flex: 1;
  padding: 36px 48px;
  display: flex;
  flex-direction: column;
}

.card-display-area {
  display: flex;
  justify-content: flex-start;
}

.state-msg {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px 0;
}

.state-error {
  color: #dc2626;
}
</style>

Claude đang trả lời Beta AI có thể mắc lỗi, hãy kiểm tra lại. 0 / 10 lượt đã dùng 1
