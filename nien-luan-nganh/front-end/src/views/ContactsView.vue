<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import {
  getFriends,
  toContact,
  getFriendshipStatus,
  sendFriendRequest,
  cancelFriendRequest,
  unblockUser
} from '@/api/contacts.api'
import { getMe } from '@/api/auth.api'
import { getUserById } from '@/api/user.api'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
const startingChat = ref(false) // chặn bấm liên tục tạo phòng 2 lần
const router = useRouter()
const route = useRoute()

import NavigationSidebar from '@/components/common/NavigationSidebar.vue'
import ContactList from '@/components/contacts/ContactList.vue'
import ContactProfileCard from '@/components/contacts/ContactProfileCard.vue'

const contacts = ref([])
const standaloneContact = ref(null)
const selectedContactId = ref(null)
const me = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const selectedContact = computed(() => {
  if (standaloneContact.value && standaloneContact.value.id === selectedContactId.value) {
    return standaloneContact.value
  }
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

    const queryUserId = route.query.userId
    if (queryUserId) {
      const found = contacts.value.find((c) => c.id === queryUserId)
      if (found) {
        selectedContactId.value = found.id
      } else {
        try {
          const [u, rel] = await Promise.all([
            getUserById(queryUserId),
            getFriendshipStatus(queryUserId).catch(() => null)
          ])
          if (u) {
            standaloneContact.value = {
              id: u.userId,
              friendshipId: rel?.friendshipId || null,
              name: u.username,
              role: u.email || u.phoneNumber || 'Người dùng',
              avatar: u.avatarUrl || '',
              online: false,
              statusText: 'Ngoại tuyến',
              bio: u.description || '',
              isFriend: rel?.status === 'ACCEPTED',
              isPending: rel?.status === 'PENDING',
              isBlocked: rel?.status === 'BLOCKED' && rel?.direction === 'OUTGOING'
            }
            selectedContactId.value = u.userId
          }
        } catch (e) {
          console.error('Không tìm thấy thông tin người dùng query:', e)
        }
      }
    } else {
      // Giữ nguyên người đang chọn khi tải lại (ví dụ sau khi chấp nhận kết bạn)
      const stillExists = contacts.value.some((c) => c.id === selectedContactId.value)
      if (!stillExists) selectedContactId.value = contacts.value[0]?.id ?? null
    }
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

//Xly them/huy loi moi/bLock/unblock ban be
async function handleAddFriend(contact) {
  if (!contact?.id) return
  try {
    const res = await sendFriendRequest(contact.id)
    if (standaloneContact.value && standaloneContact.value.id === contact.id) {
      standaloneContact.value.isPending = true
      standaloneContact.value.friendshipId = res?.friendshipId
    }
  } catch (err) {
    console.error('Lỗi kết bạn:', err)
  }
}

async function handleCancelFriendRequest(contact) {
  if (!contact?.id) return
  try {
    if (contact.friendshipId) {
      await cancelFriendRequest(contact.friendshipId)
    }
    if (standaloneContact.value && standaloneContact.value.id === contact.id) {
      standaloneContact.value.isPending = false
      standaloneContact.value.friendshipId = null
    }
  } catch (err) {
    console.error('Lỗi thu hồi lời mời:', err)
  }
}

async function handleUnblock(contact) {
  if (!contact?.id) return
  try {
    await unblockUser(contact.id)
    if (standaloneContact.value && standaloneContact.value.id === contact.id) {
      standaloneContact.value.isBlocked = false
    }
  } catch (err) {
    console.error('Lỗi bỏ chặn:', err)
  }
}

async function handleMessageContact(contact) {
  if (startingChat.value) return
  const userId = contact?.userId ?? contact?.id
  if (!userId) return

  startingChat.value = true
  errorMsg.value = ''
  try {
    // Backend tự lấy phòng 1-1 đã có hoặc tạo mới, store tự chọn phòng đó làm phòng đang mở
    const room = await chatStore.startDirectChat(userId)
    if (room?.roomId) {
      router.push('/chat')
    } else {
      errorMsg.value = 'Không mở được cuộc trò chuyện'
    }
  } catch (e) {
    console.error('Lỗi mở cuộc trò chuyện:', e)
    errorMsg.value = e.response?.data?.message || 'Không thể mở cuộc trò chuyện'
  } finally {
    startingChat.value = false
  }
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
            @add-friend="handleAddFriend"
            @cancel-friend-request="handleCancelFriendRequest"
            @unblock="handleUnblock"
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
  grid-template-columns: 80px 370px 1fr;
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
  justify-content: center;
  margin-top: 20px;
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
