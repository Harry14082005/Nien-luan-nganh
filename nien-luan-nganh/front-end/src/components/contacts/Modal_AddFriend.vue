<script setup>
import { ref, watch, onMounted } from 'vue'
import * as userApi from '@/api/user.api'
import { getFriends, getPendingFriendships, sendFriendRequest } from '@/api/contacts.api'

const props = defineProps({
  friendIds: { type: Array, default: () => [] }, // userId những người đã là bạn
})
const emit = defineEmits(['close', 'sent'])

const searchInputRef = ref(null)
const searchKeyword = ref('')
const users = ref([])
const loadingUsers = ref(false)
const errorMsg = ref('')
const sendStatus = ref({}) // { [userId]: 'sending' | 'sent' }

const internalFriendIds = ref(new Set())
const pendingOutgoingIds = ref(new Set())

let debounceTimer = null

// Đồng bộ friendIds từ props nếu có
watch(
  () => props.friendIds,
  (newIds) => {
    if (Array.isArray(newIds)) {
      newIds.forEach((id) => internalFriendIds.value.add(id))
    }
  },
  { immediate: true },
)

// Tải thông tin bạn bè và lời mời đã gửi để hiển thị đúng trạng thái
const loadFriendshipStatus = async () => {
  try {
    const [friendsRes, pendingRes] = await Promise.all([
      getFriends().catch(() => []),
      getPendingFriendships({ page: 0, size: 100 }).catch(() => []),
    ])

    const friendsList = Array.isArray(friendsRes) ? friendsRes : friendsRes?.content || []
    friendsList.forEach((item) => {
      const uId = item?.friend?.userId || item?.friendId || item?.id
      if (uId) internalFriendIds.value.add(uId)
    })

    const pendingList = Array.isArray(pendingRes) ? pendingRes : pendingRes?.content || []
    pendingList.forEach((item) => {
      const uId = item?.friend?.userId || item?.friendId
      if (item?.direction === 'OUTGOING' && uId) {
        pendingOutgoingIds.value.add(uId)
        sendStatus.value[uId] = 'sent'
      }
    })
  } catch (err) {
    console.error('Lỗi khi tải trạng thái quan hệ:', err)
  }
}

const handleSearch = async (q) => {
  const trimmed = (q || '').trim()
  if (!trimmed) {
    users.value = []
    loadingUsers.value = false
    return
  }
  loadingUsers.value = true
  errorMsg.value = ''
  try {
    const res = await userApi.searchUsers(trimmed, 0, 20)
    users.value = res?.content || []
  } catch (err) {
    console.error('Lỗi tìm kiếm người dùng:', err)
    users.value = []
  } finally {
    loadingUsers.value = false
  }
}

watch(searchKeyword, (newVal) => {
  clearTimeout(debounceTimer)
  const trimmed = (newVal || '').trim()
  if (!trimmed) {
    users.value = []
    loadingUsers.value = false
    return
  }
  loadingUsers.value = true
  debounceTimer = setTimeout(() => {
    handleSearch(trimmed)
  }, 300)
})

onMounted(() => {
  loadFriendshipStatus()
  searchInputRef.value?.focus?.()
})

const getInitials = (name) => {
  if (!name) return 'U'
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
}

const isFriend = (user) => {
  if (!user?.userId) return false
  return internalFriendIds.value.has(user.userId)
}

const isSent = (user) => {
  if (!user?.userId) return false
  return sendStatus.value[user.userId] === 'sent' || pendingOutgoingIds.value.has(user.userId)
}

const handleAddFriend = async (user) => {
  if (!user?.userId || sendStatus.value[user.userId] || isFriend(user)) return
  errorMsg.value = ''
  sendStatus.value = { ...sendStatus.value, [user.userId]: 'sending' }
  try {
    await sendFriendRequest(user.userId)
    sendStatus.value = { ...sendStatus.value, [user.userId]: 'sent' }
    pendingOutgoingIds.value.add(user.userId)
    emit('sent', user)
  } catch (err) {
    const next = { ...sendStatus.value }
    delete next[user.userId]
    sendStatus.value = next

    const msg = err?.response?.data?.message || ''
    if (msg.includes('đã là bạn') || msg.includes('ALREADY_FRIENDS')) {
      internalFriendIds.value.add(user.userId)
    } else if (msg.includes('đã tồn tại') || msg.includes('FRIEND_REQUEST_EXISTED')) {
      pendingOutgoingIds.value.add(user.userId)
      sendStatus.value = { ...sendStatus.value, [user.userId]: 'sent' }
    } else {
      errorMsg.value = msg || 'Không thể gửi lời mời kết bạn'
    }
  }
}
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card">
      <!-- Header -->
      <div class="modal-header">
        <h3>Thêm bạn bè mới</h3>
        <button class="close-btn" @click="emit('close')">✕</button>
      </div>

      <!-- Ô tìm kiếm người dùng -->
      <div class="search-section">
        <input
          ref="searchInputRef"
          v-model="searchKeyword"
          type="text"
          class="text-input"
          placeholder="Tìm theo tên, email hoặc số điện thoại..."
        />
      </div>

      <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

      <!-- Danh sách kết quả người dùng -->
      <div class="user-list">
        <div v-if="loadingUsers" class="empty-state">Đang tìm kiếm người dùng...</div>
        <div v-else-if="!searchKeyword.trim()" class="empty-state">
          Nhập tên, email hoặc số điện thoại để tìm kiếm bạn bè.
        </div>
        <div v-else-if="users.length === 0" class="empty-state">
          Không tìm thấy người dùng nào phù hợp.
        </div>

        <template v-else>
          <div v-for="user in users" :key="user.userId" class="user-row">
            <div class="user-avatar-wrap">
              <img
                v-if="user.avatarUrl"
                :src="user.avatarUrl"
                class="user-avatar-img"
                alt=""
              />
              <div v-else class="user-avatar">{{ getInitials(user.username || user.email) }}</div>
            </div>

            <div class="user-info">
              <div class="user-name">{{ user.username || 'Người dùng' }}</div>
              <div class="user-sub">{{ user.email || user.phoneNumber || '' }}</div>
            </div>

            <!-- Nếu đã là bạn bè: hiển thị "Bạn bè" màu xanh lá -->
            <span v-if="isFriend(user)" class="tag-friend">Bạn bè</span>

            <!-- Nếu đã gửi lời mời: hiển thị "Đã gửi lời mời" màu xanh dương -->
            <button
              v-else-if="isSent(user)"
              class="action-btn is-sent"
              disabled
            >
              Đã gửi lời mời
            </button>

            <!-- Nếu chưa gửi: nút kết bạn -->
            <button
              v-else
              class="action-btn"
              :disabled="sendStatus[user.userId] === 'sending'"
              @click="handleAddFriend(user)"
            >
              {{ sendStatus[user.userId] === 'sending' ? 'Đang gửi...' : 'Kết bạn' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  font-family: 'Quicksand', sans-serif;
}

.modal-card {
  width: 440px;
  max-width: 92vw;
  max-height: 82vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e6eaf2;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1c2540;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #8f98ab;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}

.close-btn:hover {
  background: #f3f6fd;
  color: #1c2540;
}

.search-section {
  padding: 12px 20px;
}

.text-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 14px;
  border: 1px solid #dce2ef;
  border-radius: 10px;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  color: #1c2540;
  background: #f8f9fd;
  outline: none;
  transition: all 0.2s ease;
}

.text-input:focus {
  border-color: #4b7bec;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(75, 123, 236, 0.12);
}

.error-banner {
  margin: 0 20px 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #fee2e2;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
}

.user-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px 12px;
  min-height: 220px;
  max-height: 320px;
}

.empty-state {
  text-align: center;
  color: #8f98ab;
  padding: 36px 12px;
  font-size: 14px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  transition: background 0.15s;
}

.user-row:hover {
  background: #f3f6fd;
}

.user-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #ece9ff;
  color: #6b5bd2;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
}

.user-avatar-img {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  object-fit: cover;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #1c2540;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-sub {
  font-size: 12px;
  color: #8f98ab;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-btn {
  padding: 6px 14px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #e6edfd;
  color: #3b6fe0;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.action-btn:hover:not(:disabled) {
  background: #3b6fe0;
  color: #ffffff;
}

.action-btn:disabled {
  cursor: default;
  opacity: 0.7;
}

/* Đã gửi lời mời: màu xanh dương chủ đạo của web */
.action-btn.is-sent {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  opacity: 1;
  cursor: default;
  font-weight: 600;
  white-space: nowrap;
}

/* Bạn bè: màu xanh lá */
.tag-friend {
  padding: 6px 14px;
  border-radius: 8px;
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}
</style>
