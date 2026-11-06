<script setup>
import { ref, watch, onMounted } from 'vue'
import { getFriends } from '@/api/contacts.api'
import { useChatStore } from '@/stores/chat'

const emit = defineEmits(['close'])
const chatStore = useChatStore()

const activeTab = ref('direct') // 'direct' | 'group'
const searchKeyword = ref('')
const allFriends = ref([])
const users = ref([])
const loadingUsers = ref(false)
const submitting = ref(false)
const errorMsg = ref('')

// Dành cho Tạo nhóm
const groupName = ref('')
const selectedUsers = ref([]) // mảng các user object đã chọn

let debounceTimer = null

const loadInitialFriends = async () => {
  loadingUsers.value = true
  errorMsg.value = ''
  try {
    const res = await getFriends({ page: 0, size: 500 })
    const list = Array.isArray(res) ? res : res?.content || []
    allFriends.value = list
      .map((item) => {
        const u = item.friend || item
        if (!u || !u.userId) return null
        const online =
          u.isOnline ||
          (u.lastSeenAt ? Date.now() - new Date(u.lastSeenAt).getTime() < 5 * 60 * 1000 : false)
        return {
          userId: u.userId,
          username: u.username || 'Người dùng',
          email: u.email || '',
          phoneNumber: u.phoneNumber || '',
          avatarUrl: u.avatarUrl || '',
          isOnline: Boolean(online),
        }
      })
      .filter(Boolean)

    applySearchFilter(searchKeyword.value)
  } catch (err) {
    console.error('Lỗi khi tải danh sách bạn bè:', err)
    errorMsg.value = 'Không thể tải danh sách bạn bè'
    allFriends.value = []
    users.value = []
  } finally {
    loadingUsers.value = false
  }
}

const applySearchFilter = (q) => {
  const trimmed = (q || '').trim().toLowerCase()
  if (!trimmed) {
    users.value = [...allFriends.value]
    return
  }
  users.value = allFriends.value.filter((u) => {
    const nameMatch = u.username && u.username.toLowerCase().includes(trimmed)
    const emailMatch = u.email && u.email.toLowerCase().includes(trimmed)
    const phoneMatch = u.phoneNumber && u.phoneNumber.toLowerCase().includes(trimmed)
    return nameMatch || emailMatch || phoneMatch
  })
}

watch(searchKeyword, (newVal) => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    applySearchFilter(newVal)
  }, 150)
})

onMounted(() => {
  loadInitialFriends()
})

const getInitials = (name) => {
  if (!name) return 'U'
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
}

// Bắt đầu chat 1-1
const handleStartDirect = async (user) => {
  if (submitting.value || !user?.userId) return
  submitting.value = true
  errorMsg.value = ''
  try {
    await chatStore.startDirectChat(user.userId)
    emit('close')
  } catch (err) {
    errorMsg.value = err?.response?.data?.message || 'Không thể tạo cuộc trò chuyện'
  } finally {
    submitting.value = false
  }
}

// Chọn / Bỏ chọn thành viên cho Nhóm
const isUserSelected = (userId) => {
  return selectedUsers.value.some((u) => u.userId === userId)
}

const toggleSelectUser = (user) => {
  const idx = selectedUsers.value.findIndex((u) => u.userId === user.userId)
  if (idx !== -1) {
    selectedUsers.value.splice(idx, 1)
  } else {
    selectedUsers.value.push(user)
  }
}

// Tạo nhóm chat
const handleCreateGroup = async () => {
  const name = groupName.value.trim()
  if (!name) {
    errorMsg.value = 'Vui lòng nhập tên nhóm chat'
    return
  }
  if (selectedUsers.value.length === 0) {
    errorMsg.value = 'Vui lòng chọn ít nhất 1 thành viên vào nhóm'
    return
  }

  submitting.value = true
  errorMsg.value = ''
  try {
    const memberIds = selectedUsers.value.map((u) => u.userId)
    await chatStore.createGroupChat({
      roomName: name,
      memberIds,
    })
    emit('close')
  } catch (err) {
    errorMsg.value = err?.response?.data?.message || 'Không thể tạo nhóm chat'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card">
      <!-- Header -->
      <div class="modal-header">
        <h3>Cuộc trò chuyện mới</h3>
        <button class="close-btn" @click="emit('close')">✕</button>
      </div>

      <!-- Tabs -->
      <div class="modal-tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'direct' }"
          @click="
            activeTab = 'direct';
            errorMsg = '';
          "
        >
          Nhắn tin 1-1
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'group' }"
          @click="
            activeTab = 'group';
            errorMsg = '';
          "
        >
          Tạo nhóm chat
        </button>
      </div>

      <!-- Nếu là Tạo nhóm: Ô nhập tên nhóm & danh sách đã chọn -->
      <div v-if="activeTab === 'group'" class="group-setup">
        <input
          v-model="groupName"
          type="text"
          class="text-input"
          placeholder="Nhập tên nhóm (VD: Nhóm Đồ Án KTPM)..."
        />
        <div v-if="selectedUsers.length > 0" class="selected-chips">
          <span v-for="u in selectedUsers" :key="u.userId" class="chip">
            {{ u.username || u.email }}
            <button type="button" class="chip-remove" @click="toggleSelectUser(u)">×</button>
          </span>
        </div>
      </div>

      <!-- Ô tìm kiếm người dùng -->
      <div class="search-section">
        <input
          v-model="searchKeyword"
          type="text"
          class="text-input"
          placeholder="Tìm kiếm bạn bè theo tên, email hoặc số điện thoại..."
        />
      </div>

      <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

      <!-- Danh sách kết quả người dùng (Bạn bè) -->
      <div class="user-list">
        <div v-if="loadingUsers" class="empty-state">Đang tải danh sách bạn bè...</div>
        <div v-else-if="allFriends.length === 0" class="empty-state">
          Bạn chưa có bạn bè nào trong danh bạ.
        </div>
        <div v-else-if="users.length === 0" class="empty-state">
          Không tìm thấy bạn bè nào phù hợp.
        </div>

        <template v-else>
          <div
            v-for="user in users"
            :key="user.userId"
            class="user-row"
            @click="activeTab === 'direct' ? handleStartDirect(user) : toggleSelectUser(user)"
          >
            <div class="user-avatar-wrap">
              <img
                v-if="user.avatarUrl"
                :src="user.avatarUrl"
                class="user-avatar-img"
                alt=""
              />
              <div v-else class="user-avatar">{{ getInitials(user.username || user.email) }}</div>
              <span v-if="user.isOnline" class="online-dot"></span>
            </div>

            <div class="user-info">
              <div class="user-name">{{ user.username || 'Người dùng' }}</div>
              <div class="user-sub">{{ user.email || user.phoneNumber || '' }}</div>
            </div>

            <!-- Tab Nhắn tin 1-1 -->
            <div v-if="activeTab === 'direct'">
              <button
                class="action-btn"
                :disabled="submitting"
                @click.stop="handleStartDirect(user)"
              >
                {{ submitting ? 'Đang mở...' : 'Nhắn tin' }}
              </button>
            </div>

            <!-- Tab Tạo nhóm (Chọn nhiều) -->
            <div v-else>
              <input
                type="checkbox"
                class="user-checkbox"
                :checked="isUserSelected(user.userId)"
                @click.stop="toggleSelectUser(user)"
              />
            </div>
          </div>
        </template>
      </div>

      <!-- Footer cho Tạo nhóm -->
      <div v-if="activeTab === 'group'" class="modal-footer">
        <button class="cancel-btn" @click="emit('close')">Hủy</button>
        <button class="create-btn" :disabled="submitting" @click="handleCreateGroup">
          {{ submitting ? 'Đang tạo...' : `Tạo nhóm (${selectedUsers.length})` }}
        </button>
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

.modal-tabs {
  display: flex;
  gap: 8px;
  padding: 12px 20px 0;
}

.tab-btn {
  flex: 1;
  padding: 9px 12px;
  border: none;
  border-radius: 10px;
  background: #f3f6fd;
  color: #5b6785;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn.active {
  background: #4b7bec;
  color: #ffffff;
}

.group-setup {
  padding: 12px 20px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
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
}

.text-input:focus {
  border-color: #4b7bec;
  background: #ffffff;
}

.selected-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 70px;
  overflow-y: auto;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #e6edfd;
  color: #3b6fe0;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.chip-remove {
  border: none;
  background: none;
  color: #3b6fe0;
  font-size: 14px;
  cursor: pointer;
  padding: 0 2px;
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
  cursor: pointer;
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
  padding: 6px 12px;
  border: none;
  border-radius: 8px;
  background: #e6edfd;
  color: #3b6fe0;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
}

.action-btn:hover {
  background: #4b7bec;
  color: #ffffff;
}

.user-checkbox {
  width: 18px;
  height: 18px;
  accent-color: #4b7bec;
  cursor: pointer;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #e6eaf2;
}

.cancel-btn {
  padding: 8px 16px;
  border: 1px solid #dce2ef;
  border-radius: 10px;
  background: #ffffff;
  color: #5b6785;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  cursor: pointer;
}

.create-btn {
  padding: 8px 18px;
  border: none;
  border-radius: 10px;
  background: #4b7bec;
  color: #ffffff;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  cursor: pointer;
}

.create-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

