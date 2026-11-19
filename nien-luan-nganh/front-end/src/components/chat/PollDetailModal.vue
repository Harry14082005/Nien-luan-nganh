<script setup>
import { ref, computed, nextTick } from 'vue'
import { resolveFileUrl } from '@/api/storage.api'

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false,
  },
  poll: {
    type: Object,
    default: null,
  },
  currentUserId: {
    type: [String, Number],
    default: null,
  },
  canClose: {
    type: Boolean,
    default: false,
  },
  closingPoll: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close', 'toggle-option', 'close-poll', 'add-option'])

// State mở popup xem người bình chọn cho một option cụ thể
const viewingVotersOption = ref(null)

const openVotersPopup = (opt) => {
  if (!opt.voters || opt.voters.length === 0) return
  viewingVotersOption.value = opt
}

const closeVotersPopup = () => {
  viewingVotersOption.value = null
}

// State thêm phương án bên trong modal
const showAddOptionInput = ref(false)
const newOptionText = ref('')
const addingOption = ref(false)
const modalOptionInputRef = ref(null)
const limitAlertMessage = ref('')

const openAddOptionInside = async () => {
  if (props.poll?.options?.length >= 10) {
    limitAlertMessage.value = 'Cuộc bình chọn đã đạt giới hạn tối đa 10 phương án!'
    return
  }
  showAddOptionInput.value = true
  await nextTick()
  modalOptionInputRef.value?.focus()
}

const handleAddOptionInside = async () => {
  const text = newOptionText.value.trim()
  if (!text || addingOption.value) return

  if (props.poll?.options?.length >= 10) {
    limitAlertMessage.value = 'Cuộc bình chọn đã đạt giới hạn tối đa 10 phương án!'
    return
  }

  try {
    addingOption.value = true
    emit('add-option', text)
    newOptionText.value = ''
    showAddOptionInput.value = false
  } finally {
    addingOption.value = false
  }
}

// Danh sách các phương án của poll (sắp xếp theo số vote nhiều nhất lên đầu)
const sortedOptions = computed(() => {
  if (!props.poll?.options) return []
  return [...props.poll.options].sort((a, b) => {
    const diff = (b.voteCount || 0) - (a.voteCount || 0)
    if (diff !== 0) return diff
    return (a.pollOptionId || 0) - (b.pollOptionId || 0)
  })
})

const totalVotesCount = computed(() => {
  if (!props.poll?.options) return 0
  return props.poll.options.reduce((sum, opt) => sum + (opt.voteCount || 0), 0)
})

const getPercentage = (voteCount) => {
  if (!totalVotesCount.value || totalVotesCount.value === 0) return 0
  return Math.round(((voteCount || 0) / totalVotesCount.value) * 100)
}

const getAvatar = (user) => {
  return resolveFileUrl(user?.avatarUrl)
}

const formatVotedTime = (timeStr) => {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) + ' ' + d.toLocaleDateString('vi-VN')
  } catch {
    return timeStr
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="isOpen && poll" class="rem-detail-backdrop" @click.self="$emit('close')">
      <div class="rem-detail-modal">
        <!-- Header modal chuẩn phong cách Reminder -->
        <div class="rem-detail-header">
          <div class="rem-detail-header-left">
            <font-awesome-icon icon="chart-simple" class="detail-header-icon" />
            <h3>Chi tiết cuộc bình chọn</h3>
          </div>
          <button type="button" class="detail-close-btn" title="Đóng" @click="$emit('close')">
            <font-awesome-icon icon="xmark" />
          </button>
        </div>

        <!-- Body modal -->
        <div class="rem-detail-body">
          <!-- Phần tóm tắt: Câu hỏi, người tạo, trạng thái -->
          <div class="poll-detail-summary">
            <div class="poll-summary-top">
              <span class="poll-status-pill" :class="{ 'pill-closed': !poll.isActive }">
                {{ poll.isActive ? 'ĐANG MỞ' : 'ĐÃ KẾT THÚC' }}
              </span>
              <span class="poll-meta-summary">
                <strong>{{ poll.totalVoters || 0 }}</strong> người tham gia · <strong>{{ totalVotesCount }}</strong> lượt bình chọn
              </span>
            </div>
            <h4 class="poll-modal-title">{{ poll.question }}</h4>
            <p class="poll-modal-creator">Tạo bởi <strong>{{ poll.createdByName || 'Thành viên' }}</strong></p>
          </div>

          <!-- Danh sách phương án: hiển thị vừa vặn 5 phương án, nhiều hơn 5 thì cuộn mượt -->
          <div class="poll-options-scroll-container">
            <div
              v-for="opt in sortedOptions"
              :key="opt.pollOptionId"
              class="poll-opt-row"
              :class="{
                'poll-opt-row--voted': opt.votedByMe,
                'poll-opt-row--disabled': !poll.isActive,
              }"
            >
              <!-- Thanh tiến độ % nền -->
              <div
                class="poll-opt-progress-bg"
                :style="{ width: `${getPercentage(opt.voteCount)}%` }"
                :class="{ 'poll-opt-progress-bg--voted': opt.votedByMe }"
              ></div>

              <!-- Nội dung của phương án -->
              <div class="poll-opt-content">
                <!-- Checkbox + Tên phương án (bấm để vote nếu poll mở) -->
                <div
                  class="poll-opt-info"
                  :title="poll.isActive ? (opt.votedByMe ? 'Bấm để bỏ chọn' : 'Bấm để bình chọn') : ''"
                  @click="poll.isActive && $emit('toggle-option', opt)"
                >
                  <div class="poll-opt-checkbox" :class="{ checked: opt.votedByMe }">
                    <font-awesome-icon v-if="opt.votedByMe" icon="check" class="opt-check-icon" />
                  </div>
                  <span class="poll-opt-text">{{ opt.optionText }}</span>
                </div>

                <!-- Cột phải: Cụm avatar cử tri (trái) + số vote căn phải nhất (phải) -->
                <div class="poll-opt-right">
                  <!-- Cụm avatar tròn: chỉ hiển thị khi có người chọn -->
                  <div
                    v-if="opt.voters && opt.voters.length > 0"
                    class="poll-opt-avatar-stack"
                    title="Bấm xem danh sách người chọn"
                    @click.stop="openVotersPopup(opt)"
                  >
                    <img
                      v-for="u in opt.voters.slice(0, 3)"
                      :key="u.userId"
                      :src="getAvatar(u)"
                      :title="u.username"
                      class="opt-stack-img"
                      alt="avatar"
                    />
                    <span v-if="opt.voters.length > 3" class="opt-stack-more">
                      +{{ opt.voters.length - 3 }}
                    </span>
                  </div>

                  <!-- Số vote căn lề phải nhất, dạng (N) -->
                  <!-- <span class="poll-opt-stat">
                    ({{ opt.voteCount || 0 }})
                  </span> -->
                </div>
              </div>
            </div>
          </div>

          <!-- Thêm phương án trong modal chi tiết (chỉ khi poll còn mở) -->
          <div v-if="poll.isActive" class="poll-modal-add-area">
            <div v-if="showAddOptionInput" class="modal-add-row">
              <input
                ref="modalOptionInputRef"
                v-model="newOptionText"
                type="text"
                class="modal-add-input"
                placeholder="Nhập phương án mới..."
                maxlength="100"
                @keydown.enter.prevent="handleAddOptionInside"
              />
              <button
                type="button"
                class="btn-confirm-add"
                :disabled="!newOptionText.trim() || addingOption"
                title="Lưu"
                @click="handleAddOptionInside"
              >
                <font-awesome-icon v-if="!addingOption" icon="check" />
                <font-awesome-icon v-else icon="spinner" spin />
              </button>
              <button
                type="button"
                class="btn-cancel-add"
                title="Hủy"
                @click="showAddOptionInput = false; newOptionText = ''"
              >
                <font-awesome-icon icon="xmark" />
              </button>
            </div>

            <button
              v-else
              type="button"
              class="btn-modal-trigger-add"
              @click="openAddOptionInside"
            >
              <font-awesome-icon icon="plus" class="btn-icon" />
              <span>Thêm phương án</span>
            </button>
          </div>
        </div>

        <!-- Footer modal chuẩn phong cách Reminder -->
        <div class="rem-detail-footer">
          <button
            v-if="canClose"
            type="button"
            :disabled="closingPoll"
            class="rem-cancel-event-btn"
            @click="$emit('close-poll')"
          >
            <font-awesome-icon icon="lock" />
            <span>{{ closingPoll ? 'Đang kết thúc...' : 'Kết thúc bình chọn' }}</span>
          </button>
          <div v-else></div>

          <button type="button" class="rem-close-modal-btn" @click="$emit('close')">
            Đóng
          </button>
        </div>
      </div>
    </div>

    <!-- POPUP XEM DANH SÁCH THÀNH VIÊN ĐÃ CHỌN PHƯƠNG ÁN (NHỎ GỌN TRÊN CÙNG) -->
    <div
      v-if="viewingVotersOption"
      class="voters-popup-backdrop"
      @click.self="closeVotersPopup"
    >
      <div class="voters-popup-card">
        <!-- Header Popup -->
        <div class="voters-popup-header">
          <div class="voters-popup-title-box">
            <h4 class="voters-popup-title">Người bình chọn</h4>
            <span class="voters-popup-opt-name">{{ viewingVotersOption.optionText }}</span>
          </div>
          <button type="button" class="voters-popup-close-btn" @click="closeVotersPopup">
            <font-awesome-icon icon="xmark" />
          </button>
        </div>

        <!-- Danh sách thành viên có thanh cuộn mượt -->
        <div class="voters-popup-list">
          <div
            v-for="u in viewingVotersOption.voters"
            :key="u.userId"
            class="voter-popup-item"
          >
            <div class="voter-item-user">
              <img :src="getAvatar(u)" class="voter-avatar" alt="avatar" />
              <div class="voter-info-col">
                <span class="voter-name">{{ u.username }}</span>
                <span v-if="u.votedAt" class="voter-time">{{ formatVotedTime(u.votedAt) }}</span>
              </div>
            </div>
            <span
              class="voter-tag"
              :class="String(u.userId) === String(currentUserId) ? 'voter-tag-me' : 'voter-tag-other'"
            >
              {{ String(u.userId) === String(currentUserId) ? 'Bạn' : 'Đã chọn' }}
            </span>
          </div>
        </div>

        <!-- Footer Popup -->
        <div class="voters-popup-footer">
          <span class="voters-count-total">Tổng cộng: {{ viewingVotersOption.voters?.length || 0 }} người</span>
          <button type="button" class="btn-close-subpopup" @click="closeVotersPopup">Đóng</button>
        </div>
      </div>
    </div>

    <!-- POPUP CẢNH BÁO GIỚI HẠN / THÔNG BÁO -->
    <div
      v-if="limitAlertMessage"
      class="poll-alert-backdrop"
      @click.self="limitAlertMessage = ''"
    >
      <div class="poll-alert-card">
        <div class="poll-alert-icon">
          <font-awesome-icon icon="circle-exclamation" />
        </div>
        <h4 class="poll-alert-title">Thông báo</h4>
        <p class="poll-alert-text">{{ limitAlertMessage }}</p>
        <button type="button" class="poll-alert-btn" @click="limitAlertMessage = ''">
          Đã hiểu
        </button>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700;800&display=swap');

/* Modal Backdrop & Container đồng bộ 100% với Reminder Modal */
.rem-detail-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  animation: fadeIn 0.15s ease-out;
}

.rem-detail-modal {
  width: 100%;
  max-width: 480px;
  max-height: 85vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
  animation: popIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.rem-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #e2e8f0;
}

.rem-detail-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-header-icon {
  color: #2563eb;
  font-size: 16px;
}

.rem-detail-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.detail-close-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.detail-close-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.rem-detail-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Tóm tắt câu hỏi */
.poll-detail-summary {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
}

.poll-summary-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.poll-status-pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;
  text-transform: uppercase;
  background: #dbeafe;
  color: #1d4ed8;
}

.poll-status-pill.pill-closed {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #cbd5e1;
}

.poll-meta-summary {
  font-size: 11.5px;
  color: #64748b;
}

.poll-meta-summary strong {
  color: #334155;
}

.poll-modal-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  word-break: break-word;
}

.poll-modal-creator {
  margin: 4px 0 0 0;
  font-size: 11.5px;
  color: #64748b;
}

.poll-modal-creator strong {
  color: #334155;
}

/* Danh sách phương án: hiển thị tối đa ~5 dòng (khoảng 270px), nhiều hơn thì cuộn */
.poll-options-scroll-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 275px;
  overflow-y: auto;
  padding-right: 2px;
  scrollbar-width: thin;
  -webkit-overflow-scrolling: touch;
  will-change: scroll-position;
}

.poll-opt-row {
  position: relative;
  min-height: 44px;
  display: flex;
  align-items: center;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  contain: paint;
}

.poll-opt-row--voted {
  background: #f0fdf4;
  border-color: #86efac;
}

/* Thanh % tiến độ nền */
.poll-opt-progress-bg {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  background: #eff6ff;
  z-index: 1;
  pointer-events: none;
}

.poll-opt-progress-bg--voted {
  background: #dcfce7;
}

/* Nội dung nổi bên trên thanh tiến độ */
.poll-opt-content {
  position: relative;
  z-index: 2;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  gap: 10px;
}

.poll-opt-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
  cursor: pointer;
}

.poll-opt-checkbox {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 1.5px solid #cbd5e1;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.poll-opt-checkbox.checked {
  background: #10b981;
  border-color: #10b981;
}

.opt-check-icon {
  color: #ffffff;
  font-size: 11px;
}

.poll-opt-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  word-break: break-word;
}

/* Cột bên phải: Cụm avatar bên trái, số liệu bên phải */
.poll-opt-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0px;
  flex-shrink: 0;
}

.poll-opt-stat {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  min-width: 24px;
  text-align: right;
  flex-shrink: 0;
}

/* Cụm avatar tròn cử tri (bỏ background xám, hiển thị sạch và tự nhiên) */
.poll-opt-avatar-stack {
  display: flex;
  align-items: center;
  padding: 0;
  background: transparent;
  cursor: pointer;
  user-select: none;
}

.opt-stack-img {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  border: 1.5px solid #ffffff;
  margin-left: -6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.opt-stack-img:first-child {
  margin-left: 0;
}

.opt-stack-more {
  margin-left: -6px;
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #cbd5e1;
  color: #334155;
  font-size: 9.5px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #ffffff;
  padding: 0 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

/* Khu vực thêm phương án trong modal */
.poll-modal-add-area {
  margin-top: 2px;
}

.btn-modal-trigger-add {
  width: 100%;
  padding: 8px 12px;
  border: 1.5px dashed #bfdbfe;
  border-radius: 10px;
  background: #f8fafc;
  color: #2563eb;
  font-family: 'Quicksand', sans-serif;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.15s ease;
}

.btn-modal-trigger-add:hover {
  background: #eff6ff;
  border-color: #3b82f6;
}

.modal-add-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.modal-add-input {
  flex: 1;
  padding: 7px 10px;
  font-family: 'Quicksand', sans-serif;
  font-size: 12.5px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  outline: none;
  box-sizing: border-box;
}

.modal-add-input:focus {
  border-color: #2563eb;
}

.btn-confirm-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: #2563eb;
  color: #ffffff;
  cursor: pointer;
  font-size: 12px;
  flex-shrink: 0;
}

.btn-confirm-add:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-cancel-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  flex-shrink: 0;
}

.btn-cancel-add:hover {
  background: #e2e8f0;
}

/* Footer modal */
.rem-detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.rem-cancel-event-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: 8px;
  border: 1px solid #fee2e2;
  background: #fff1f2;
  color: #e11d48;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.rem-cancel-event-btn:hover:not(:disabled) {
  background: #fee2e2;
}

.rem-close-modal-btn {
  padding: 7px 18px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  font-family: 'Quicksand', sans-serif;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.rem-close-modal-btn:hover {
  background: #f1f5f9;
}

/* POPUP XEM DANH SÁCH THÀNH VIÊN ĐÃ CHỌN */
.voters-popup-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(2px);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  animation: fadeIn 0.15s ease-out;
}

.voters-popup-card {
  width: 100%;
  max-width: 380px;
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
  animation: popIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.voters-popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.voters-popup-title-box {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.voters-popup-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.voters-popup-opt-name {
  font-size: 11.5px;
  color: #2563eb;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
}

.voters-popup-close-btn {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voters-popup-close-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.voters-popup-list {
  max-height: 240px;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  scrollbar-width: thin;
}

.voter-popup-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}

.voter-item-user {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.voter-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.voter-info-col {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.voter-name {
  font-size: 12.5px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.voter-time {
  font-size: 10px;
  color: #94a3b8;
}

.voter-tag {
  font-size: 10.5px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  flex-shrink: 0;
}

.voter-tag-me {
  background: #ecfdf5;
  color: #059669;
}

.voter-tag-other {
  background: #e2e8f0;
  color: #475569;
}

.voters-popup-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.voters-count-total {
  font-size: 11.5px;
  color: #64748b;
  font-weight: 600;
}

.btn-close-subpopup {
  padding: 5px 14px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  font-family: 'Quicksand', sans-serif;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}

.btn-close-subpopup:hover {
  background: #f1f5f9;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

/* POPUP THÔNG BÁO / CẢNH BÁO */
.poll-alert-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  z-index: 10001;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  animation: fadeIn 0.15s ease-out;
}

.poll-alert-card {
  width: 100%;
  max-width: 320px;
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);
  padding: 20px 18px 16px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  font-family: 'Quicksand', sans-serif;
  animation: popIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.poll-alert-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #fef2f2;
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-bottom: 12px;
}

.poll-alert-title {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.poll-alert-text {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}

.poll-alert-btn {
  width: 100%;
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  background: #2563eb;
  color: #ffffff;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s;
}

.poll-alert-btn:hover {
  background: #1d4ed8;
}
</style>
