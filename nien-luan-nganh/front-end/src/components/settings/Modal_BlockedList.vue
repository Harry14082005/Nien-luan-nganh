<script setup>
import { ref, onMounted } from 'vue'
import { getBlockedUsers, unblockUser } from '@/api/contacts.api'
import { getImageUrl } from '@/api/user.api'

const emit = defineEmits(['close', 'unblocked'])

const blockedList = ref([])
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const unblockingId = ref(null)

const loadBlocked = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await getBlockedUsers()
    blockedList.value = Array.isArray(res) ? res : res?.content || []
  } catch (err) {
    console.error('Lỗi khi tải danh sách chặn:', err)
    errorMsg.value = err?.response?.data?.message || 'Không thể tải danh sách người dùng đã chặn.'
    blockedList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadBlocked()
})

const getInitials = (name) => {
  if (!name) return 'U'
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
}

const handleUnblock = async (item) => {
  const targetId = item.friend?.userId
  if (!targetId || unblockingId.value) return

  unblockingId.value = targetId
  errorMsg.value = ''
  successMsg.value = ''

  try {
    await unblockUser(targetId)
    blockedList.value = blockedList.value.filter(
      (b) => (b.friend?.userId || b.friendshipId) !== (targetId || item.friendshipId),
    )
    successMsg.value = `Đã bỏ chặn ${item.friend?.username || 'người dùng'}.`
    emit('unblocked', item.friend)
    setTimeout(() => {
      successMsg.value = ''
    }, 3000)
  } catch (err) {
    console.error('Lỗi khi bỏ chặn:', err)
    errorMsg.value = err?.response?.data?.message || 'Không thể bỏ chặn người dùng này.'
  } finally {
    unblockingId.value = null
  }
}
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card">
      <!-- Header -->
      <div class="modal-header">
        <div class="header-left">
          <div class="icon-wrap">
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
              <circle cx="12" cy="12" r="10" />
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
            </svg>
          </div>
          <div>
            <h3>Danh sách chặn</h3>
            <p class="header-desc">Những người này sẽ không thể liên hệ với bạn trên BlueSpace</p>
          </div>
        </div>
        <button class="close-btn" @click="emit('close')" title="Đóng">✕</button>
      </div>

      <!-- Banners -->
      <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>
      <div v-if="successMsg" class="success-banner">{{ successMsg }}</div>

      <!-- Danh sách người dùng bị chặn -->
      <div class="blocked-list-scroll">
        <div v-if="loading" class="empty-state">
          <div class="spinner"></div>
          <span>Đang tải danh sách chặn...</span>
        </div>

        <div v-else-if="blockedList.length === 0" class="empty-state">
          <svg
            class="empty-icon"
            width="44"
            height="44"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <polyline points="9 12 11 14 15 10" />
          </svg>
          <span class="empty-title">Danh sách chặn trống</span>
          <span class="empty-desc">Bạn chưa chặn người dùng nào.</span>
        </div>

        <template v-else>
          <div
            v-for="item in blockedList"
            :key="item.friendshipId || item.friend?.userId"
            class="blocked-row"
          >
            <div class="user-avatar-wrap">
              <img
                v-if="item.friend?.avatarUrl"
                :src="getImageUrl(item.friend.avatarUrl)"
                class="user-avatar-img"
                alt=""
              />
              <div v-else class="user-avatar">
                {{ getInitials(item.friend?.username || item.friend?.email) }}
              </div>
            </div>

            <div class="user-info">
              <div class="user-name">{{ item.friend?.username || 'Người dùng' }}</div>
              <div class="user-sub">
                {{ item.friend?.email || item.friend?.phoneNumber || 'Không có thông tin liên hệ' }}
              </div>
            </div>

            <button
              class="btn-unblock"
              :disabled="unblockingId === item.friend?.userId"
              @click="handleUnblock(item)"
            >
              {{ unblockingId === item.friend?.userId ? 'Đang gỡ...' : 'Bỏ chặn' }}
            </button>
          </div>
        </template>
      </div>

      <!-- Footer -->
      <div class="modal-footer">
        <span class="count-badge" v-if="blockedList.length > 0">
          Tổng cộng: {{ blockedList.length }} người
        </span>
        <button class="btn-close-modal" @click="emit('close')">Đóng</button>
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
  backdrop-filter: blur(3px);
}

.modal-card {
  width: 480px;
  max-width: 92vw;
  max-height: 85vh;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalFadeIn 0.2s ease-out;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background-color: #fef2f2;
  color: #dc2626;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.header-desc {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #64748b;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.15s ease;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.error-banner {
  margin: 12px 24px 4px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #fef2f2;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #fecaca;
}

.success-banner {
  margin: 12px 24px 4px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #ecfdf5;
  color: #059669;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #a7f3d0;
}

.blocked-list-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 14px 24px;
  min-height: 200px;
  max-height: 360px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Custom scrollbar */
.blocked-list-scroll::-webkit-scrollbar {
  width: 6px;
}
.blocked-list-scroll::-webkit-scrollbar-track {
  background: #f8fafc;
}
.blocked-list-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}
.blocked-list-scroll::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #94a3b8;
  padding: 44px 16px;
  text-align: center;
}

.empty-icon {
  color: #cbd5e1;
  margin-bottom: 4px;
}

.empty-title {
  font-size: 15px;
  font-weight: 700;
  color: #475569;
}

.empty-desc {
  font-size: 13px;
  color: #94a3b8;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.blocked-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  background-color: #ffffff;
  transition: all 0.15s ease;
}

.blocked-row:hover {
  background-color: #f8fafc;
  border-color: #e2e8f0;
}

.user-avatar-wrap {
  flex-shrink: 0;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #fee2e2;
  color: #dc2626;
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
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-sub {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.btn-unblock {
  padding: 7px 14px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #dc2626;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-unblock:hover:not(:disabled) {
  background: #dc2626;
  color: #ffffff;
  border-color: #dc2626;
}

.btn-unblock:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background-color: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.count-badge {
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}

.btn-close-modal {
  margin-left: auto;
  padding: 8px 18px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-close-modal:hover {
  background-color: #f1f5f9;
}
</style>

