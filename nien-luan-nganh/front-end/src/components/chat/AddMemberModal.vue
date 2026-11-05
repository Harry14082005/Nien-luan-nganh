<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import * as userApi from '@/api/user.api'
import { addRoomMembers } from '@/api/room.api'
import UserAvatar from '@/components/common/UserAvatar.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  roomId: {
    type: String,
    required: true
  },
  existingMembers: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'member-added'])

const searchKeyword = ref('')
const users = ref([])
const loadingUsers = ref(false)
const submitting = ref(false)
const errorMsg = ref('')
const selectedUsers = ref([])

let debounceTimer = null

// Tập hợp UUID của các thành viên đã có trong phòng
const existingMemberIdsSet = computed(() => {
  const set = new Set()
  props.existingMembers.forEach((m) => {
    const id = m.userId || m.user?.userId
    if (id) set.add(id)
  })
  return set
})

const isAlreadyMember = (userId) => {
  return existingMemberIdsSet.value.has(userId)
}

const loadInitialUsers = async () => {
  loadingUsers.value = true
  errorMsg.value = ''
  try {
    users.value = await userApi.getSuggestedUsers()
  } catch {
    users.value = []
  } finally {
    loadingUsers.value = false
  }
}

const handleSearch = async (q) => {
  const trimmed = (q || '').trim()
  if (!trimmed) {
    await loadInitialUsers()
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
  debounceTimer = setTimeout(() => {
    handleSearch(newVal)
  }, 250)
})

watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      searchKeyword.value = ''
      selectedUsers.value = []
      errorMsg.value = ''
      loadInitialUsers()
    }
  }
)

onMounted(() => {
  if (props.show) {
    loadInitialUsers()
  }
})

const isUserSelected = (userId) => {
  return selectedUsers.value.some((u) => u.userId === userId)
}

const toggleSelectUser = (user) => {
  if (isAlreadyMember(user.userId)) return

  const idx = selectedUsers.value.findIndex((u) => u.userId === user.userId)
  if (idx !== -1) {
    selectedUsers.value.splice(idx, 1)
  } else {
    selectedUsers.value.push(user)
  }
}

const removeSelectedUser = (userId) => {
  selectedUsers.value = selectedUsers.value.filter((u) => u.userId !== userId)
}

const handleAddMembers = async () => {
  if (selectedUsers.value.length === 0) {
    errorMsg.value = 'Vui lòng chọn ít nhất 1 người để thêm vào nhóm'
    return
  }
  if (!props.roomId) {
    errorMsg.value = 'Không tìm thấy ID phòng trò chuyện'
    return
  }

  submitting.value = true
  errorMsg.value = ''
  try {
    const memberIds = selectedUsers.value.map((u) => u.userId)
    await addRoomMembers(props.roomId, memberIds)
    emit('member-added', memberIds)
    emit('close')
  } catch (err) {
    console.error('Lỗi thêm thành viên:', err)
    errorMsg.value = err?.response?.data?.message || 'Không thể thêm thành viên vào nhóm'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="show" class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card">
      <!-- Header -->
      <div class="modal-header">
        <div class="header-left">
          <div class="icon-wrap">
            <font-awesome-icon icon="user-plus" />
          </div>
          <div class="header-text">
            <h3 class="modal-title">Thêm thành viên</h3>
            <span class="modal-subtitle">Mời thêm bạn bè vào cuộc trò chuyện</span>
          </div>
        </div>
        <button class="close-btn" title="Đóng" @click="emit('close')">
          <font-awesome-icon icon="x" />
        </button>
      </div>

      <!-- Ô tìm kiếm -->
      <div class="search-box">
        <font-awesome-icon icon="magnifying-glass" class="search-icon" />
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="Tìm theo tên hoặc email..."
        />
        <button
          v-if="searchKeyword"
          type="button"
          class="clear-search-btn"
          @click="searchKeyword = ''"
        >
          ✕
        </button>
      </div>

      <!-- Danh sách chip đã chọn -->
      <div v-if="selectedUsers.length > 0" class="selected-chips-bar">
        <div
          v-for="u in selectedUsers"
          :key="u.userId"
          class="user-chip"
        >
          <UserAvatar
            :name="u.username || 'U'"
            :src="u.avatarUrl || u.avatar"
            :size="22"
          />
          <span class="chip-name">{{ u.username }}</span>
          <button type="button" class="chip-remove-btn" @click.stop="removeSelectedUser(u.userId)">
            ✕
          </button>
        </div>
      </div>

      <!-- Thông báo lỗi -->
      <div v-if="errorMsg" class="error-banner">
        {{ errorMsg }}
      </div>

      <!-- Danh sách người dùng -->
      <div class="users-list">
        <div v-if="loadingUsers" class="loading-state">
          <div class="spinner-small"></div>
          <span>Đang tìm kiếm...</span>
        </div>

        <div v-else-if="users.length === 0" class="empty-state">
          Không tìm thấy người dùng phù hợp
        </div>

        <div
          v-for="user in users"
          v-else
          :key="user.userId"
          class="user-row"
          :class="{
            'user-row--disabled': isAlreadyMember(user.userId),
            'user-row--selected': isUserSelected(user.userId)
          }"
          @click="toggleSelectUser(user)"
        >
          <div class="user-avatar-wrap">
            <UserAvatar
              :name="user.username || 'User'"
              :src="user.avatarUrl || user.avatar"
              :size="38"
            />
            <span v-if="user.isOnline" class="online-dot"></span>
          </div>

          <div class="user-info">
            <div class="user-name">{{ user.username || 'Người dùng' }}</div>
            <div class="user-sub">{{ user.email || user.phoneNumber || '' }}</div>
          </div>

          <div class="user-action">
            <span v-if="isAlreadyMember(user.userId)" class="already-badge">
              Đã tham gia
            </span>
            <input
              v-else
              type="checkbox"
              class="user-checkbox"
              :checked="isUserSelected(user.userId)"
              @click.stop="toggleSelectUser(user)"
            />
          </div>
        </div>
      </div>

      <!-- Footer Modal -->
      <div class="modal-footer">
        <button type="button" class="cancel-btn" @click="emit('close')">
          Hủy
        </button>
        <button
          type="button"
          class="submit-btn"
          :disabled="submitting || selectedUsers.length === 0"
          @click="handleAddMembers"
        >
          {{ submitting ? 'Đang thêm...' : `Xác nhận thêm (${selectedUsers.length})` }}
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
  backdrop-filter: blur(2px);
  animation: fadeIn 0.18s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-card {
  width: 440px;
  max-width: 92vw;
  max-height: 84vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from {
    transform: translateY(12px) scale(0.97);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #eff6ff;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}

.header-text {
  display: flex;
  flex-direction: column;
}

.modal-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.modal-subtitle {
  font-size: 12px;
  color: #64748b;
  margin-top: 1px;
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 15px;
  cursor: pointer;
  color: #94a3b8;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  margin: 12px 20px 8px;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #94a3b8;
  font-size: 13px;
}

.search-input {
  width: 100%;
  padding: 9px 34px 9px 34px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 13px;
  font-family: 'Quicksand', sans-serif;
  color: #1e293b;
  outline: none;
  transition: all 0.18s;
  box-sizing: border-box;
}

.search-input:focus {
  background: #ffffff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.clear-search-btn {
  position: absolute;
  right: 10px;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 11px;
  padding: 4px;
}

.selected-chips-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px 20px 8px;
  max-height: 80px;
  overflow-y: auto;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px 3px 4px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  font-size: 12px;
  color: #1d4ed8;
  font-weight: 600;
}

.chip-name {
  max-width: 100px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chip-remove-btn {
  background: transparent;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 11px;
  line-height: 1;
  padding: 0 2px;
}

.chip-remove-btn:hover {
  color: #ef4444;
}

.error-banner {
  margin: 4px 20px 8px;
  padding: 8px 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #b91c1c;
  font-size: 12px;
}

.users-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px;
  min-height: 200px;
  max-height: 320px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 40px 16px;
  color: #94a3b8;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  user-select: none;
}

.user-row:hover:not(.user-row--disabled) {
  background-color: #f8fafc;
}

.user-row--selected {
  background-color: #f0fdf4;
}

.user-row--disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #f8fafc;
}

.user-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background-color: #22c55e;
  border: 2px solid #ffffff;
}

.user-info {
  flex: 1;
  overflow: hidden;
}

.user-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-sub {
  font-size: 11.5px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.already-badge {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 500;
}

.user-checkbox {
  width: 17px;
  height: 17px;
  accent-color: #3b82f6;
  cursor: pointer;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #f1f5f9;
  background: #fafafa;
}

.cancel-btn {
  padding: 8px 16px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.cancel-btn:hover {
  background: #f1f5f9;
}

.submit-btn {
  padding: 8px 18px;
  background: #3b82f6;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.15s;
}

.submit-btn:hover:not(:disabled) {
  background: #2563eb;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>

