<script setup>
import { ref, computed, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import { resolveFileUrl } from '@/api/storage.api'

const props = defineProps({
  message: {
    type: Object,
    required: true,
  },
  poll: {
    type: Object,
    default: null,
  },
  currentUserId: {
    type: [String, Number],
    default: null,
  },
  canManageRoom: {
    type: Boolean,
    default: false,
  },
  isHighlighted: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['open-detail'])

const chatStore = useChatStore()
const voting = ref(false)
const showAddOptionInput = ref(false)
const newOptionText = ref('')
const addingOption = ref(false)
const closingPoll = ref(false)
const limitAlertMessage = ref('')
const optionInputRef = ref(null)

const openAddOption = async () => {
  if (options.value.length >= 10) {
    limitAlertMessage.value = 'Cuộc bình chọn đã đạt giới hạn tối đa 10 phương án!'
    return
  }
  showAddOptionInput.value = true
  await nextTick()
  optionInputRef.value?.focus()
}

const effectiveQuestion = computed(() => {
  return props.poll?.question || props.message?.content || 'Cuộc bình chọn'
})

const effectiveIsActive = computed(() => {
  return props.poll?.isActive ?? true
})

const creatorName = computed(() => {
  return (
    props.poll?.createdByName ||
    props.message?.sender?.username ||
    'Thành viên'
  )
})

// Kiểm tra người tạo bình chọn
const isCreator = computed(() => {
  if (!props.poll?.createdById || !props.currentUserId) return false
  return String(props.poll.createdById) === String(props.currentUserId)
})

// Quyền kết thúc: người tạo poll HOẶC admin nhóm (canManageRoom)
const canClose = computed(() => {
  if (!effectiveIsActive.value) return false
  return Boolean(isCreator.value || props.canManageRoom)
})

const totalVoters = computed(() => {
  return props.poll?.totalVoters || 0
})

const options = computed(() => {
  return props.poll?.options || []
})

// Danh sách tất cả cử tri duy nhất trong poll (để hiển thị cụm avatar tròn giống Reminder)
const uniqueVoters = computed(() => {
  const map = new Map()
  for (const opt of options.value) {
    if (Array.isArray(opt.voters)) {
      for (const v of opt.voters) {
        if (v && v.userId && !map.has(String(v.userId))) {
          map.set(String(v.userId), v)
        }
      }
    }
  }
  return Array.from(map.values())
})

// Tính tổng số phiếu đã vote trong tất cả options để làm mẫu số %
const totalVotesCount = computed(() => {
  return options.value.reduce((sum, opt) => sum + (opt.voteCount || 0), 0)
})

// Sắp xếp các phương án: cái nào có nhiều vote nhất xếp lên đầu
const sortedOptions = computed(() => {
  return [...options.value].sort((a, b) => {
    const diff = (b.voteCount || 0) - (a.voteCount || 0)
    if (diff !== 0) return diff
    return 0
  })
})

// Ở ngoài thẻ chat: chỉ hiển thị tối đa 3 lựa chọn
const displayedOptions = computed(() => {
  return sortedOptions.value.slice(0, 3)
})

const remainingOptionsCount = computed(() => {
  return Math.max(0, sortedOptions.value.length - 3)
})

// Hàm tính phần trăm tiến độ
const getPercentage = (voteCount) => {
  if (!totalVotesCount.value || totalVotesCount.value === 0) return 0
  return Math.round(((voteCount || 0) / totalVotesCount.value) * 100)
}

const getAvatarUrl = (user) => {
  return resolveFileUrl(user?.avatarUrl)
}

// Xử lý chọn/bỏ chọn một option
const handleToggleOption = async (option) => {
  if (!effectiveIsActive.value || voting.value || !option) return
  const pollId = props.poll?.pollId
  const roomId = props.message?.roomId || chatStore.activeRoomId
  if (!pollId || !roomId) return

  let wantedOptionIds = []
  const currentVotedIds = options.value
    .filter((o) => o.votedByMe)
    .map((o) => o.pollOptionId)

  if (option.votedByMe) {
    // Bỏ chọn option này
    wantedOptionIds = currentVotedIds.filter((id) => id !== option.pollOptionId)
  } else {
    // Thêm option này
    wantedOptionIds = [...currentVotedIds, option.pollOptionId]
  }

  try {
    voting.value = true
    await chatStore.voteRoomPoll(roomId, pollId, wantedOptionIds)
  } catch (err) {
    console.error('Lỗi bỏ phiếu:', err)
  } finally {
    voting.value = false
  }
}

// Xử lý thêm lựa chọn mới
const handleAddOption = async (customText = null) => {
  const text = (customText !== null ? customText : newOptionText.value).trim()
  if (!text || addingOption.value) return

  if (options.value.length >= 10) {
    limitAlertMessage.value = 'Cuộc bình chọn đã đạt giới hạn tối đa 10 phương án!'
    return
  }

  const pollId = props.poll?.pollId
  const roomId = props.message?.roomId || chatStore.activeRoomId
  if (!pollId || !roomId) return

  try {
    addingOption.value = true
    await chatStore.addOptionToRoomPoll(roomId, pollId, text)
    newOptionText.value = ''
    showAddOptionInput.value = false
  } catch (err) {
    console.error('Lỗi thêm lựa chọn:', err)
    const errCode = err?.response?.data?.code
    const errMsg = err?.response?.data?.message
    if (errCode === 1026 || errMsg?.includes('đã tồn tại') || errMsg?.includes('tối đa')) {
      limitAlertMessage.value = errMsg || 'Phương án đã tồn tại hoặc đã đạt tối đa!'
    } else {
      limitAlertMessage.value = errMsg || 'Không thể thêm phương án, vui lòng thử lại!'
    }
  } finally {
    addingOption.value = false
  }
}

// Xử lý khóa / kết thúc poll
const handleClosePoll = async () => {
  if (!confirm('Bạn có chắc muốn kết thúc cuộc bình chọn này không?')) return
  const pollId = props.poll?.pollId
  const roomId = props.message?.roomId || chatStore.activeRoomId
  if (!pollId || !roomId) return

  try {
    closingPoll.value = true
    await chatStore.closeRoomPoll(roomId, pollId)
    showDetailModal.value = false
  } catch (err) {
    console.error('Lỗi đóng poll:', err)
  } finally {
    closingPoll.value = false
  }
}
</script>

<template>
  <div class="poll-card" :class="{ 'status-closed': !effectiveIsActive, 'highlight-msg': isHighlighted }">
    <!-- BANNER TRÊN CÙNG: Đồng bộ 100% với ReminderMessageCard -->
    <div class="poll-card-banner">
      <div class="poll-banner-left">
        <font-awesome-icon icon="chart-simple" />
        <span>BÌNH CHỌN NHÓM</span>
      </div>
      <div class="poll-status-pill" :class="{ 'pill-closed': !effectiveIsActive }">
        {{ effectiveIsActive ? 'ĐANG MỞ' : 'ĐÃ KẾT THÚC' }}
      </div>
    </div>

    <!-- NỘI DUNG CHÍNH CỦA THẺ BÌNH CHỌN -->
    <div class="poll-card-body">
      <!-- Tiêu đề câu hỏi -->
      <h3 class="poll-question" :class="{ 'text-closed': !effectiveIsActive }" @click="showDetailModal = true">
        {{ effectiveQuestion }}
      </h3>

      <!-- Hàng thông tin: Người tạo (trái) & Số người tham gia (phải) -->
      <div class="poll-meta-row">
        <span class="poll-creator-sub">
          Tạo bởi <strong>{{ creatorName }}</strong>
        </span>
        <div
          class="poll-voter-summary-inline"
          title="Bấm để xem chi tiết"
          @click="emit('open-detail', poll)"
        >
          <strong class="count-bold">{{ totalVoters }}</strong> người tham gia
        </div>
      </div>

      <!-- Danh sách Options: Hiển thị tối đa 3 lựa chọn dẫn đầu -->
      <div class="poll-options-list">
        <div
          v-for="opt in displayedOptions"
          :key="opt.pollOptionId"
          class="poll-option-row"
          :class="{
            'poll-option-row--selected': opt.votedByMe,
            'poll-option-row--disabled': !effectiveIsActive,
          }"
          role="button"
          tabindex="0"
          @click="handleToggleOption(opt)"
        >
          <!-- Thanh tiến độ nền phía sau -->
          <div
            class="poll-progress-bg"
            :style="{ width: `${getPercentage(opt.voteCount)}%` }"
            :class="{ 'poll-progress-bg--voted': opt.votedByMe }"
          ></div>

          <!-- Nội dung nổi phía trên -->
          <div class="poll-option-content">
            <div class="option-left">
              <div class="option-checkbox" :class="{ checked: opt.votedByMe }">
                <font-awesome-icon v-if="opt.votedByMe" icon="check" class="check-icon" />
              </div>
              <span class="option-text">{{ opt.optionText }}</span>
            </div>

            <div class="option-right">
              <span class="option-percent">{{ getPercentage(opt.voteCount) }}%</span>
              <span class="option-count">({{ opt.voteCount || 0 }})</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Nút xem thêm các phương án nếu có > 3 lựa chọn -->
      <button
        v-if="remainingOptionsCount > 0"
        type="button"
        class="btn-view-all-opts"
        @click="emit('open-detail', poll)"
      >
        <span>Xem tất cả (+{{ remainingOptionsCount }})</span>
        <font-awesome-icon icon="chevron-right" class="btn-arrow" />
      </button>

      <!-- Khối Thêm phương án mới (chỉ hiện khi poll còn mở) -->
      <div v-if="effectiveIsActive" class="poll-add-area">
        <div v-if="showAddOptionInput" class="add-option-box">
          <input
            ref="optionInputRef"
            v-model="newOptionText"
            type="text"
            class="add-option-input"
            placeholder="Nhập phương án mới..."
            maxlength="100"
            @keydown.enter.prevent="handleAddOption()"
          />
          <button
            type="button"
            class="btn-confirm-add"
            :disabled="!newOptionText.trim() || addingOption"
            title="Thêm"
            @click="handleAddOption()"
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
          class="btn-trigger-add"
          @click="openAddOption"
        >
          <font-awesome-icon icon="plus" class="btn-icon" />
          <span>Thêm phương án</span>
        </button>
      </div>


    </div>

    <!-- FOOTER: Nút kết thúc & Chi tiết (Đồng bộ với ReminderMessageCard) -->
    <div class="poll-card-footer">
      <button
        v-if="canClose"
        type="button"
        class="btn-close-poll"
        :disabled="closingPoll"
        @click="handleClosePoll"
      >
        <font-awesome-icon v-if="!closingPoll" icon="lock" class="btn-icon" />
        <font-awesome-icon v-else icon="spinner" spin class="btn-icon" />
        <span>Kết thúc</span>
      </button>
      <div v-else></div>

      <button
        type="button"
        class="btn-detail-link"
        @click="emit('open-detail', poll)"
      >
        <span>Chi tiết</span>
        <font-awesome-icon icon="chevron-right" />
      </button>
    </div>

    <!-- POPUP CẢNH BÁO GIỚI HẠN / THÔNG BÁO -->
    <Teleport to="body">
      <div v-if="limitAlertMessage" class="poll-alert-backdrop" @click.self="limitAlertMessage = ''">
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
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700;800&display=swap');

/* Khung thẻ cố định 335px đồng bộ 100% với ReminderMessageCard */
.poll-card {
  width: 335px;
  max-width: 100%;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  box-shadow: 0 3px 12px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
  text-align: left;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
  user-select: none;
}

.poll-card:hover {
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.12);
}

.poll-card.status-closed {
  border-color: #e2e8f0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.poll-card.highlight-msg {
  animation: pollCardBounceStandalone 1.5s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes pollCardBounceStandalone {
  0% {
    transform: scale(1);
  }
  22% {
    transform: scale(1.03) translateY(-3px);
    box-shadow:
      0 0 0 2.5px #ffffff,
      0 0 0 5px #2563eb,
      0 14px 30px rgba(37, 99, 235, 0.45);
  }
  60% {
    transform: scale(1);
    box-shadow:
      0 0 0 2px #ffffff,
      0 0 0 3.5px rgba(37, 99, 235, 0.35),
      0 6px 18px rgba(37, 99, 235, 0.2);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 3px 12px rgba(15, 23, 42, 0.06);
  }
}

/* Banner header phong cách Reminder */
.poll-card-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: linear-gradient(90deg, #eff6ff 0%, #f8fafc 100%);
  border-bottom: 1px solid #dbeafe;
  color: #1d4ed8;
  box-sizing: border-box;
}

.poll-card.status-closed .poll-card-banner {
  background: #f8fafc;
  border-bottom-color: #e2e8f0;
  color: #64748b;
}

.poll-banner-left {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.4px;
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

/* Thân card */
.poll-card-body {
  padding: 12px 14px;
  box-sizing: border-box;
}

.poll-question {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  word-break: break-word;
  cursor: pointer;
  transition: color 0.15s;
}

.poll-question:hover {
  color: #2563eb;
}

.poll-question.text-closed {
  color: #64748b;
}

.poll-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 3px 0 10px 0;
}

.poll-creator-sub {
  margin: 0;
  font-size: 11.5px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.poll-creator-sub strong {
  color: #334155;
  font-weight: 600;
}

.poll-voter-summary-inline {
  font-size: 11px;
  color: #64748b;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s;
}

.poll-voter-summary-inline:hover {
  color: #2563eb;
}

/* Danh sách Options: cố định min-height, chống dịch layout */
.poll-options-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  box-sizing: border-box;
}

.poll-option-row {
  position: relative;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 10px;
  min-height: 38px;
  box-sizing: border-box;
  background: #ffffff;
  cursor: pointer;
  transition: border-color 0.15s, background-color 0.15s;
}

.poll-option-row:hover:not(.poll-option-row--disabled) {
  border-color: #93c5fd;
  background: #f8fafc;
}

.poll-option-row--selected {
  border-color: #3b82f6 !important;
  background: #eff6ff;
}

.poll-option-row--disabled {
  cursor: default;
}

/* Thanh tiến độ nền */
.poll-progress-bg {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  background: #e2e8f0;
  opacity: 0.6;
  border-radius: 9px;
  transition: width 0.3s ease;
  pointer-events: none;
}

.poll-progress-bg--voted {
  background: #bfdbfe;
  opacity: 0.75;
}

/* Nội dung option */
.poll-option-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  box-sizing: border-box;
}

.option-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.option-checkbox {
  width: 17px;
  height: 17px;
  border-radius: 5px;
  border: 1.5px solid #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  flex-shrink: 0;
  transition: all 0.15s;
}

.option-checkbox.checked {
  background: #2563eb;
  border-color: #2563eb;
  color: #ffffff;
}

.check-icon {
  font-size: 10px;
}

.option-text {
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
  word-break: break-word;
  line-height: 1.3;
}

.option-right {
  display: flex;
  align-items: baseline;
  gap: 3px;
  font-size: 11.5px;
  font-weight: 700;
  color: #334155;
  flex-shrink: 0;
}

.option-count {
  font-weight: 500;
  color: #64748b;
  font-size: 10.5px;
}

/* Nút xem tất cả options */
.btn-view-all-opts {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  margin-top: 6px;
  background: transparent;
  border: none;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.15s;
}

.btn-view-all-opts:hover {
  background: #eff6ff;
}

.btn-arrow {
  font-size: 10px;
}

/* Thêm phương án */
.poll-add-area {
  margin-top: 8px;
}

.btn-trigger-add {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 7px;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-trigger-add:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.add-option-box {
  display: flex;
  align-items: center;
  gap: 6px;
}

.add-option-input {
  flex: 1;
  padding: 7px 10px;
  font-family: 'Quicksand', sans-serif;
  font-size: 12.5px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  outline: none;
  box-sizing: border-box;
}

.add-option-input:focus {
  border-color: #2563eb;
}

.btn-confirm-add,
.btn-cancel-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 12px;
  flex-shrink: 0;
}

.btn-confirm-add {
  background: #2563eb;
  color: #ffffff;
}

.btn-confirm-add:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-cancel-add {
  background: #f1f5f9;
  color: #64748b;
}

.btn-cancel-add:hover {
  background: #e2e8f0;
}

/* CỤM AVATAR TRÒN (ĐỒNG BỘ 100% REMINDER) */
.poll-voter-summary-bar {
  margin-top: 9px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
}

.poll-voter-summary-bar:hover .poll-voter-counts {
  color: #2563eb;
}

.poll-avatar-stack {
  display: flex;
  align-items: center;
}

.poll-stack-img {
  width: 21px;
  height: 21px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ffffff;
  margin-left: -6px;
}

.poll-stack-img:first-child {
  margin-left: 0;
}

.poll-stack-more {
  width: 21px;
  height: 21px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-size: 9px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ffffff;
  margin-left: -6px;
}

.poll-stack-empty {
  font-size: 13px;
  color: #94a3b8;
}

.poll-voter-counts {
  font-size: 11.5px;
  color: #64748b;
  transition: color 0.15s;
}

.count-bold {
  color: #059669;
}

.poll-chevron {
  font-size: 9px;
  margin-left: 4px;
}

/* Footer nút kết thúc & chi tiết */
.poll-card-footer {
  padding: 8px 14px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}

.btn-close-poll {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 11px;
  font-family: 'Quicksand', sans-serif;
  font-size: 11.5px;
  font-weight: 700;
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-close-poll:hover:not(:disabled) {
  background: #fee2e2;
  border-color: #fca5a5;
}

.btn-close-poll:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-detail-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 6px;
  transition: all 0.15s;
}

.btn-detail-link:hover {
  background: #eff6ff;
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

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
</style>
