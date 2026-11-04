<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  getPendingFriendships,
  acceptedFriendship,
  declineFriendship,
  removeFriendship,
} from '@/api/contacts.api'

const emit = defineEmits(['close', 'accepted', 'changed'])

const activeTab = ref('incoming') // 'incoming' | 'outgoing'
const requests = ref([])
const loading = ref(false)
const errorMsg = ref('')
const actionStatus = ref({}) // { [friendshipId]: 'accepting' | 'declining' | 'canceling' | 'accepted' | 'declined' | 'canceled' }

const incoming = computed(() => requests.value.filter((r) => r.direction === 'INCOMING'))
const outgoing = computed(() => requests.value.filter((r) => r.direction === 'OUTGOING'))
const currentList = computed(() =>
  activeTab.value === 'incoming' ? incoming.value : outgoing.value,
)

const loadRequests = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    // API: GET /api/v1/friendships?status=PENDING  (gồm cả INCOMING + OUTGOING)
    const res = await getPendingFriendships({ page: 0, size: 50 })
    requests.value = Array.isArray(res) ? res : res?.content || []
  } catch (err) {
    console.error('Lỗi tải lời mời kết bạn:', err)
    requests.value = []
    errorMsg.value = err?.response?.data?.message || 'Không thể tải danh sách lời mời'
  } finally {
    loading.value = false
  }
}

onMounted(loadRequests)

const setStatus = (id, value) => {
  const next = { ...actionStatus.value }
  if (value) next[id] = value
  else delete next[id]
  actionStatus.value = next
}

const runAction = async (item, pending, done, fn, afterEvent) => {
  const id = item.friendshipId
  if (!id || actionStatus.value[id]) return
  errorMsg.value = ''
  setStatus(id, pending)
  try {
    await fn(id)
    setStatus(id, done)
    if (afterEvent) emit(afterEvent, item)
    emit('changed', item)
  } catch (err) {
    setStatus(id, null)
    errorMsg.value = err?.response?.data?.message || 'Thao tác thất bại, vui lòng thử lại'
  }
}

const handleAccept = (item) =>
  runAction(item, 'accepting', 'accepted', acceptedFriendship, 'accepted') // PUT /{id}/accept
const handleDecline = (item) => runAction(item, 'declining', 'declined', declineFriendship) // PUT /{id}/decline
const handleCancel = (item) => runAction(item, 'canceling', 'canceled', removeFriendship) // DELETE /{id}

const getInitials = (name) => {
  if (!name) return 'U'
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
}

const formatTimeAgo = (iso) => {
  if (!iso) return ''
  const diff = Date.now() - new Date(iso).getTime()
  if (Number.isNaN(diff)) return ''
  const min = Math.floor(diff / 60000)
  if (min < 1) return 'Vừa xong'
  if (min < 60) return `${min} phút trước`
  const hour = Math.floor(min / 60)
  if (hour < 24) return `${hour} giờ trước`
  const day = Math.floor(hour / 24)
  if (day < 30) return `${day} ngày trước`
  return new Date(iso).toLocaleDateString('vi-VN')
}
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card">
      <!-- Header -->
      <div class="modal-header">
        <h3>Lời mời kết bạn</h3>
        <button class="close-btn" @click="emit('close')">✕</button>
      </div>

      <!-- Tabs -->
      <div class="tab-section">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'incoming' }"
          @click="activeTab = 'incoming'"
        >
          Đã nhận
          <span class="tab-count">{{ incoming.length }}</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'outgoing' }"
          @click="activeTab = 'outgoing'"
        >
          Đã gửi
          <span class="tab-count">{{ outgoing.length }}</span>
        </button>
      </div>

      <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

      <!-- Danh sách lời mời -->
      <div class="user-list">
        <div v-if="loading" class="empty-state">Đang tải lời mời kết bạn...</div>
        <div v-else-if="currentList.length === 0" class="empty-state">
          {{
            activeTab === 'incoming'
              ? 'Bạn chưa có lời mời kết bạn nào.'
              : 'Bạn chưa gửi lời mời nào đang chờ.'
          }}
        </div>

        <template v-else>
          <div v-for="item in currentList" :key="item.friendshipId" class="user-row">
            <div class="user-avatar-wrap">
              <img
                v-if="item.friend?.avatarUrl"
                :src="item.friend.avatarUrl"
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
                {{ item.friend?.email || item.friend?.phoneNumber || '' }}
              </div>
              <div class="user-time">{{ formatTimeAgo(item.createdAt) }}</div>
            </div>

            <!-- ĐÃ NHẬN: chấp nhận / từ chối -->
            <template v-if="item.direction === 'INCOMING'">
              <span v-if="actionStatus[item.friendshipId] === 'accepted'" class="tag-done">
                Đã chấp nhận
              </span>
              <span
                v-else-if="actionStatus[item.friendshipId] === 'declined'"
                class="tag-done is-muted"
              >
                Đã từ chối
              </span>
              <div v-else class="btn-group">
                <button
                  class="action-btn"
                  :disabled="!!actionStatus[item.friendshipId]"
                  @click="handleAccept(item)"
                >
                  {{
                    actionStatus[item.friendshipId] === 'accepting' ? 'Đang xử lý...' : 'Chấp nhận'
                  }}
                </button>
                <button
                  class="action-btn is-ghost"
                  :disabled="!!actionStatus[item.friendshipId]"
                  @click="handleDecline(item)"
                >
                  {{ actionStatus[item.friendshipId] === 'declining' ? '...' : 'Từ chối' }}
                </button>
              </div>
            </template>

            <!-- ĐÃ GỬI: thu hồi -->
            <template v-else>
              <span v-if="actionStatus[item.friendshipId] === 'canceled'" class="tag-done is-muted">
                Đã thu hồi
              </span>
              <button
                v-else
                class="action-btn is-ghost"
                :disabled="!!actionStatus[item.friendshipId]"
                @click="handleCancel(item)"
              >
                {{
                  actionStatus[item.friendshipId] === 'canceling' ? 'Đang thu hồi...' : 'Thu hồi'
                }}
              </button>
            </template>
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

/* Tabs */
.tab-section {
  display: flex;
  gap: 8px;
  padding: 12px 20px 4px;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #1c2540;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.tab-btn:hover {
  background: #f3f6fd;
}

.tab-btn.active {
  background: #e6edfd;
  color: #3b6fe0;
}

.tab-count {
  min-width: 20px;
  padding: 1px 6px;
  border-radius: 999px;
  background: #ffffff;
  color: #3b6fe0;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.tab-btn:not(.active) .tab-count {
  background: #eef1f8;
  color: #8f98ab;
}

.error-banner {
  margin: 8px 20px 0;
  padding: 8px 12px;
  border-radius: 8px;
  background: #fee2e2;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
}

/* List */
.user-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px 12px;
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

.user-avatar,
.user-avatar-img {
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.user-avatar {
  background: #ece9ff;
  color: #6b5bd2;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
}

.user-avatar-img {
  object-fit: cover;
  display: block;
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

.user-time {
  margin-top: 1px;
  font-size: 11px;
  color: #a9b1c3;
}

/* Buttons */
.btn-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
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
  white-space: nowrap;
}

.action-btn:hover {
  background: #4b7bec;
  color: #ffffff;
}

.action-btn.is-ghost {
  background: #f3f4f8;
  color: #6b7488;
}

.action-btn.is-ghost:hover {
  background: #fee2e2;
  color: #dc2626;
}

.action-btn:disabled {
  cursor: default;
  opacity: 0.7;
}

.tag-done {
  padding: 6px 12px;
  border-radius: 8px;
  background: #ecfdf5;
  color: #059669;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.tag-done.is-muted {
  background: #f3f4f8;
  color: #8f98ab;
}
</style>
