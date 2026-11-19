<script setup>
import { ref, computed } from 'vue'
import { useChatStore } from '@/stores/chat'
import { resolveFileUrl } from '@/api/storage.api'

// =============================================================================
// ████  PHẦN 1: KHỞI TẠO PROPS, STORE & TRẠNG THÁI THẺ LỊCH HẸN (REMINDER)  ███
// =============================================================================

const props = defineProps({
  message: {
    type: Object,
    required: true,
  },
  reminder: {
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
})

const chatStore = useChatStore()
const rsvpLoading = ref(false)
const cancelLoading = ref(false)
const showDetailModal = ref(false)
const detailTab = ref('ACCEPTED') // 'ACCEPTED' | 'DECLINED' | 'PENDING'

const effectiveTitle = computed(() => {
  return props.reminder?.title || props.message?.content || 'Lịch hẹn'
})

const effectiveStatus = computed(() => {
  return props.reminder?.reminderStatus || 'PENDING'
})

const effectiveType = computed(() => {
  return props.reminder?.reminderType || 'GROUP'
})

const creatorName = computed(() => {
  return (
    props.reminder?.createdByName ||
    props.reminder?.createdByUsername ||
    props.message?.sender?.username ||
    'Thành viên'
  )
})

const isPersonalOfOther = computed(() => {
  if (effectiveType.value !== 'PERSONAL') return false
  if (!props.reminder?.createdById || !props.currentUserId) return false
  return String(props.reminder.createdById) !== String(props.currentUserId)
})

// =============================================================================
// ██████████  PHẦN 2: KIỂM TRA HẾT HẠN, QUYỀN HỦY & ĐỊNH DẠNG TỜ LỊCH  ████████
// =============================================================================

// Kiểm tra xem thời điểm hiện tại đã trôi qua mốc hết hạn của lịch hẹn hay chưa
// (Ưu tiên kiểm tra endAt nếu có, nếu không có endAt thì so khớp startAt)
const isPastTime = computed(() => {
  const deadlineRaw = props.reminder?.endAt || props.reminder?.startAt
  if (!deadlineRaw) return false
  const time = new Date(deadlineRaw).getTime()
  return !isNaN(time) && time <= Date.now()
})

// Xác định lịch hẹn đã kết thúc (status = DONE hoặc thời gian đã qua)
const isExpiredOrDone = computed(() => {
  return effectiveStatus.value === 'DONE' || (effectiveStatus.value === 'PENDING' && isPastTime.value)
})

// Khóa quyền hủy nếu lịch hẹn đã hết hạn hoặc đã xong (chỉ PENDING và chưa hết hạn mới được hủy)
const canCancel = computed(() => {
  if (!props.reminder || isExpiredOrDone.value || effectiveStatus.value !== 'PENDING') return false
  const isCreator =
    props.currentUserId &&
    props.reminder.createdById &&
    String(props.reminder.createdById) === String(props.currentUserId)
  return Boolean(isCreator || props.canManageRoom)
})

// Tách thông tin tờ lịch từ startAt
const calendarSheet = computed(() => {
  const raw = props.reminder?.startAt || props.message?.createdAt
  const d = raw ? new Date(raw) : new Date()
  if (Number.isNaN(d.getTime())) {
    return { month: 'THÁNG --', day: '--', weekday: '---' }
  }
  const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
  return {
    month: `THÁNG ${d.getMonth() + 1}`,
    day: String(d.getDate()).padStart(2, '0'),
    weekday: weekdays[d.getDay()],
  }
})

// Định dạng chuỗi thời gian bắt đầu - kết thúc dễ đọc
const formattedTimeRange = computed(() => {
  const startRaw = props.reminder?.startAt
  if (!startRaw) return 'Đang cập nhật thời gian...'
  const start = new Date(startRaw)
  if (Number.isNaN(start.getTime())) return ''

  const pad = (n) => String(n).padStart(2, '0')
  const formatTime = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const formatDate = (d) => `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`

  const now = new Date()
  const isToday =
    start.getDate() === now.getDate() &&
    start.getMonth() === now.getMonth() &&
    start.getFullYear() === now.getFullYear()

  const tomorrow = new Date(now)
  tomorrow.setDate(now.getDate() + 1)
  const isTomorrow =
    start.getDate() === tomorrow.getDate() &&
    start.getMonth() === tomorrow.getMonth() &&
    start.getFullYear() === tomorrow.getFullYear()

  const dayLabel = isToday ? 'Hôm nay' : isTomorrow ? 'Ngày mai' : formatDate(start)
  let base = `${formatTime(start)} · ${dayLabel}`

  if (props.reminder?.endAt) {
    const end = new Date(props.reminder.endAt)
    if (!Number.isNaN(end.getTime())) {
      const sameDay =
        start.getDate() === end.getDate() &&
        start.getMonth() === end.getMonth() &&
        start.getFullYear() === end.getFullYear()
      if (sameDay) {
        base = `${formatTime(start)} - ${formatTime(end)} · ${dayLabel}`
      } else {
        base = `${formatTime(start)} ${formatDate(start)} → ${formatTime(end)} ${formatDate(end)}`
      }
    }
  }
  return base
})

// =============================================================================
// ████  PHẦN 3: THỐNG KÊ NGƯỜI THAM GIA & XỬ LÝ PHẢN HỒI RSVP / HỦY LỊCH  █████
// =============================================================================

const participants = computed(() => {
  return Array.isArray(props.reminder?.participants) ? props.reminder.participants : []
})

const acceptedParticipants = computed(() =>
  participants.value.filter((p) => p.response === 'ACCEPTED'),
)
const declinedParticipants = computed(() =>
  participants.value.filter((p) => p.response === 'DECLINED'),
)
const pendingParticipants = computed(() =>
  participants.value.filter((p) => p.response === 'PENDING'),
)

const filteredModalParticipants = computed(() => {
  if (detailTab.value === 'ACCEPTED') return acceptedParticipants.value
  if (detailTab.value === 'DECLINED') return declinedParticipants.value
  return pendingParticipants.value
})

const getAvatarSrc = (p) => {
  if (p?.avatarUrl) return resolveFileUrl(p.avatarUrl)
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(p?.username || 'U')}&background=random`
}

async function handleRsvp(status) {
  // Chặn người dùng tương tác nếu lịch hẹn đã hết hạn hoặc không còn ở trạng thái PENDING
  if (!props.reminder || rsvpLoading.value || isExpiredOrDone.value || effectiveStatus.value !== 'PENDING') return
  if (props.reminder.myRsvp === status) return
  rsvpLoading.value = true
  try {
    await chatStore.respondReminderRsvp(
      props.reminder.roomId || props.message.roomId,
      props.reminder.reminderId,
      status,
    )
  } catch (err) {
    alert(err?.response?.data?.message || 'Không thể cập nhật phản hồi lịch hẹn.')
  } finally {
    rsvpLoading.value = false
  }
}

async function handleCancel() {
  // Chặn hủy lịch hẹn nếu sự kiện đã kết thúc
  if (!props.reminder || cancelLoading.value || isExpiredOrDone.value || effectiveStatus.value !== 'PENDING') return
  if (!confirm(`Bạn có chắc chắn muốn hủy lịch hẹn "${effectiveTitle.value}" không?`)) return
  cancelLoading.value = true
  try {
    await chatStore.cancelRoomReminder(
      props.reminder.roomId || props.message.roomId,
      props.reminder.reminderId,
    )
    showDetailModal.value = false
  } catch (err) {
    alert(err?.response?.data?.message || 'Không thể hủy lịch hẹn này.')
  } finally {
    cancelLoading.value = false
  }
}
</script>

<template>
  <div
    class="rem-card"
    :class="[
      `status-${effectiveStatus === 'CANCELLED' ? 'cancelled' : isExpiredOrDone ? 'done' : 'pending'}`,
      `type-${effectiveType.toLowerCase()}`,
    ]"
  >
    <!-- Top Banner -->
    <div class="rem-card-banner">
      <div class="rem-banner-left">
        <font-awesome-icon :icon="effectiveType === 'PERSONAL' ? 'lock' : 'calendar-days'" />
        <span>{{ effectiveType === 'PERSONAL' ? 'Nhắc hẹn cá nhân' : 'Lịch hẹn nhóm' }}</span>
      </div>
      <span class="rem-status-pill">
        {{
          effectiveStatus === 'CANCELLED'
            ? 'Đã hủy'
            : isExpiredOrDone
              ? 'Đã kết thúc'
              : 'Sắp tới'
        }}
      </span>
    </div>

    <!-- Body: Tờ lịch + Thông tin -->
    <div class="rem-card-body">
      <div class="rem-leaf">
        <div class="rem-leaf-month">{{ calendarSheet.month }}</div>
        <div class="rem-leaf-center">
          <span class="rem-leaf-day">{{ calendarSheet.day }}</span>
          <span class="rem-leaf-weekday">{{ calendarSheet.weekday }}</span>
        </div>
      </div>

      <div class="rem-info-col">
        <h4 class="rem-title">{{ effectiveTitle }}</h4>

        <div class="rem-time-row">
          <font-awesome-icon icon="clock" class="rem-clock-icon" />
          <span>{{ formattedTimeRange }}</span>
        </div>

        <p v-if="reminder?.description" class="rem-desc-box">
          {{ reminder.description }}
        </p>

        <!-- Người tham gia (GROUP) -->
        <div
          v-if="reminder && effectiveType === 'GROUP'"
          class="rem-rsvp-summary"
          title="Bấm để xem danh sách phản hồi"
          @click.stop="showDetailModal = true"
        >
          <div class="rem-avatar-stack">
            <img
              v-for="p in acceptedParticipants.slice(0, 4)"
              :key="p.userId"
              :src="getAvatarSrc(p)"
              :title="`${p.username} (Tham gia)`"
              class="rem-stack-img"
              alt="avatar"
            />
            <span v-if="acceptedParticipants.length > 4" class="rem-stack-more">
              +{{ acceptedParticipants.length - 4 }}
            </span>
          </div>

          <div class="rem-rsvp-counts">
            <strong class="count-ok">{{ reminder.acceptedCount || 0 }}</strong> tham gia
            <template v-if="reminder.declinedCount > 0">
              · <strong class="count-no">{{ reminder.declinedCount }}</strong> từ chối
            </template>
            <font-awesome-icon icon="chevron-right" class="rem-chevron" />
          </div>
        </div>
      </div>
    </div>

    <!-- Footer: Nút Tham gia / Từ chối / Chi tiết -->
    <div v-if="reminder && !isPersonalOfOther" class="rem-card-footer">
      <!-- 1. Khi đang còn hạn (PENDING): Hiện nút Tham gia & Từ chối -->
      <template v-if="!isExpiredOrDone && effectiveStatus === 'PENDING' && effectiveType === 'GROUP'">
        <button
          type="button"
          :disabled="rsvpLoading"
          class="rem-rsvp-btn btn-accept"
          :class="{ active: reminder.myRsvp === 'ACCEPTED' }"
          @click.stop="handleRsvp('ACCEPTED')"
        >
          <font-awesome-icon icon="circle-check" />
          <span>{{ reminder.myRsvp === 'ACCEPTED' ? 'Đã tham gia' : 'Tham gia' }}</span>
        </button>

        <button
          type="button"
          :disabled="rsvpLoading"
          class="rem-rsvp-btn btn-decline"
          :class="{ active: reminder.myRsvp === 'DECLINED' }"
          @click.stop="handleRsvp('DECLINED')"
        >
          <font-awesome-icon icon="circle-xmark" />
          <span>{{ reminder.myRsvp === 'DECLINED' ? 'Đã từ chối' : 'Từ chối' }}</span>
        </button>
      </template>

      <!-- 2. Khi đã hết hạn: Thông báo nhẹ nhàng theo lựa chọn của người dùng -->
      <div v-else-if="effectiveStatus !== 'CANCELLED' && isExpiredOrDone" class="rem-finished-notice">
        <font-awesome-icon icon="clock-rotate-left" />
        <span>Lịch hẹn đã kết thúc</span>
      </div>

      <!-- 3. Khi đã bị hủy: Thông báo đã hủy -->
      <div v-else-if="effectiveStatus === 'CANCELLED'" class="rem-cancelled-notice">
        <font-awesome-icon icon="ban" />
        <span>Lịch hẹn này đã bị hủy</span>
      </div>

      <button
        type="button"
        class="rem-detail-btn"
        :class="{ 'compact-mode': isExpiredOrDone || effectiveStatus === 'CANCELLED' || effectiveType === 'PERSONAL' }"
        @click.stop="showDetailModal = true"
      >
        Chi tiết
      </button>
    </div>

    <!-- Modal Chi tiết lịch hẹn & Danh sách người tham gia -->
    <Teleport to="body">
      <div
        v-if="showDetailModal && reminder"
        class="rem-detail-backdrop"
        @click.self="showDetailModal = false"
      >
        <div class="rem-detail-modal">
          <div class="rem-detail-header">
            <div class="rem-detail-header-left">
              <font-awesome-icon icon="calendar-days" class="detail-header-icon" />
              <h3>Chi tiết lịch hẹn</h3>
            </div>
            <button type="button" class="detail-close-btn" @click="showDetailModal = false">
              <font-awesome-icon icon="x" />
            </button>
          </div>

          <div class="rem-detail-body">
            <div class="rem-detail-top">
              <div class="rem-leaf">
                <div class="rem-leaf-month">{{ calendarSheet.month }}</div>
                <div class="rem-leaf-center">
                  <span class="rem-leaf-day">{{ calendarSheet.day }}</span>
                  <span class="rem-leaf-weekday">{{ calendarSheet.weekday }}</span>
                </div>
              </div>

              <div class="rem-detail-meta">
                <h4 class="rem-detail-title">{{ effectiveTitle }}</h4>
                <p class="rem-detail-time">{{ formattedTimeRange }}</p>
                <p class="rem-detail-creator">
                  Tạo bởi <strong>{{ creatorName }}</strong>
                </p>
              </div>
            </div>

            <div v-if="reminder.description" class="rem-detail-desc">
              {{ reminder.description }}
            </div>

            <!-- Tabs danh sách người tham gia -->
            <div v-if="effectiveType === 'GROUP'" class="rem-detail-participants">
              <div class="rem-detail-tabs">
                <button
                  type="button"
                  class="rem-dtab"
                  :class="{ 'active-ok': detailTab === 'ACCEPTED' }"
                  @click="detailTab = 'ACCEPTED'"
                >
                  Tham gia ({{ acceptedParticipants.length }})
                </button>
                <button
                  type="button"
                  class="rem-dtab"
                  :class="{ 'active-no': detailTab === 'DECLINED' }"
                  @click="detailTab = 'DECLINED'"
                >
                  Từ chối ({{ declinedParticipants.length }})
                </button>
                <button
                  type="button"
                  class="rem-dtab"
                  :class="{ 'active-wait': detailTab === 'PENDING' }"
                  @click="detailTab = 'PENDING'"
                >
                  Chờ ({{ pendingParticipants.length }})
                </button>
              </div>

              <div class="rem-dlist">
                <div v-for="p in filteredModalParticipants" :key="p.userId" class="rem-ditem">
                  <div class="rem-ditem-user">
                    <img :src="getAvatarSrc(p)" class="rem-ditem-avatar" alt="avatar" />
                    <span class="rem-ditem-name">{{ p.username }}</span>
                  </div>
                  <span
                    class="rem-ditem-badge"
                    :class="
                      p.response === 'ACCEPTED'
                        ? 'badge-ok'
                        : p.response === 'DECLINED'
                          ? 'badge-no'
                          : 'badge-wait'
                    "
                  >
                    {{
                      p.response === 'ACCEPTED'
                        ? 'Tham gia'
                        : p.response === 'DECLINED'
                          ? 'Từ chối'
                          : 'Chờ phản hồi'
                    }}
                  </span>
                </div>

                <p v-if="filteredModalParticipants.length === 0" class="rem-dempty">
                  Chưa có thành viên nào ở mục này.
                </p>
              </div>
            </div>
          </div>

          <div class="rem-detail-footer">
            <button
              v-if="canCancel"
              type="button"
              :disabled="cancelLoading"
              class="rem-cancel-event-btn"
              @click="handleCancel"
            >
              <font-awesome-icon icon="trash" />
              <span>{{ cancelLoading ? 'Đang hủy...' : 'Hủy lịch hẹn' }}</span>
            </button>
            <div v-else></div>

            <button type="button" class="rem-close-modal-btn" @click="showDetailModal = false">
              Đóng
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700&display=swap');

.rem-card {
  width: 335px;
  max-width: 100%;
  background: #ffffff;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  box-shadow: 0 3px 12px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
  text-align: left;
  transition: box-shadow 0.2s ease;
}

.rem-card:hover {
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.12);
}

.rem-card.status-cancelled {
  border-color: #e2e8f0;
}

.rem-card.type-personal {
  border-color: #e0e7ff;
}

.rem-card-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: linear-gradient(90deg, #eff6ff 0%, #f8fafc 100%);
  border-bottom: 1px solid #dbeafe;
  color: #1d4ed8;
}

.rem-card.type-personal .rem-card-banner {
  background: linear-gradient(90deg, #eef2ff 0%, #f8fafc 100%);
  border-bottom-color: #e0e7ff;
  color: #4338ca;
}

.rem-card.status-cancelled .rem-card-banner {
  background: #f1f5f9;
  border-bottom-color: #e2e8f0;
  color: #64748b;
}

.rem-card.status-done .rem-card-banner {
  background: #f8fafc;
  border-bottom-color: #e2e8f0;
  color: #64748b;
}

.rem-banner-left {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.rem-status-pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;
  text-transform: uppercase;
  background: #dbeafe;
  color: #1d4ed8;
}

.rem-card.status-cancelled .rem-status-pill {
  background: #fee2e2;
  color: #dc2626;
}

.rem-card.status-done .rem-status-pill {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.rem-card-body {
  padding: 13px 14px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.rem-leaf {
  width: 66px;
  flex-shrink: 0;
  border-radius: 8px;
  border: 1px solid #dbeafe;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
  overflow: hidden;
  text-align: center;
  box-shadow: 0 2px 5px rgba(37, 99, 235, 0.06);
}

.rem-card.type-personal .rem-leaf {
  border-color: #e0e7ff;
  background: linear-gradient(180deg, #eef2ff 0%, #ffffff 100%);
}

.rem-card.status-cancelled .rem-leaf {
  border-color: #e2e8f0;
  background: #f8fafc;
  opacity: 0.65;
}

.rem-leaf-month {
  background: #2563eb;
  color: #ffffff;
  font-size: 9px;
  font-weight: 800;
  padding: 3px 2px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.rem-card.type-personal .rem-leaf-month {
  background: #4f46e5;
}

.rem-card.status-cancelled .rem-leaf-month {
  background: #94a3b8;
}

.rem-leaf-center {
  padding: 6px 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rem-leaf-day {
  font-size: 22px;
  font-weight: 800;
  color: #2563eb;
  line-height: 1.05;
}

.rem-card.type-personal .rem-leaf-day {
  color: #4f46e5;
}

.rem-card.status-cancelled .rem-leaf-day {
  color: #64748b;
  text-decoration: line-through;
}

.rem-leaf-weekday {
  font-size: 10px;
  font-weight: 600;
  color: #64748b;
  margin-top: 2px;
}

.rem-info-col {
  flex: 1;
  min-width: 0;
}

.rem-title {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  word-break: break-word;
}

.rem-card.status-cancelled .rem-title {
  color: #94a3b8;
  text-decoration: line-through;
}

.rem-time-row {
  margin-top: 5px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
}

.rem-clock-icon {
  font-size: 11px;
  flex-shrink: 0;
}

.rem-desc-box {
  margin: 7px 0 0;
  padding: 6px 9px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.rem-rsvp-summary {
  margin-top: 9px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
}

.rem-rsvp-summary:hover .rem-rsvp-counts {
  color: #2563eb;
}

.rem-avatar-stack {
  display: flex;
  align-items: center;
}

.rem-stack-img {
  width: 21px;
  height: 21px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ffffff;
  margin-left: -6px;
}

.rem-stack-img:first-child {
  margin-left: 0;
}

.rem-stack-more {
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

.rem-rsvp-counts {
  font-size: 11.5px;
  color: #64748b;
  transition: color 0.15s;
}

.count-ok {
  color: #059669;
}

.count-no {
  color: #e11d48;
}

.rem-chevron {
  font-size: 9px;
  margin-left: 4px;
}

.rem-card-footer {
  padding: 9px 12px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  gap: 7px;
}

.rem-rsvp-btn {
  flex: 1;
  padding: 6px 8px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  transition: all 0.15s;
}

.rem-rsvp-btn.btn-accept:hover {
  border-color: #10b981;
  color: #059669;
  background: #ecfdf5;
}

.rem-rsvp-btn.btn-accept.active {
  background: #059669;
  border-color: #059669;
  color: #ffffff;
}

.rem-rsvp-btn.btn-decline:hover {
  border-color: #f43f5e;
  color: #e11d48;
  background: #fff1f2;
}

.rem-rsvp-btn.btn-decline.active {
  background: #e11d48;
  border-color: #e11d48;
  color: #ffffff;
}

.rem-detail-btn {
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  color: #64748b;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.rem-detail-btn:hover {
  background: #ffffff;
  border-color: #cbd5e1;
  color: #2563eb;
}

.rem-detail-btn.full-width {
  width: 100%;
  border-color: #e2e8f0;
  background: #ffffff;
}

.rem-detail-btn.compact-mode {
  flex-shrink: 0;
  min-width: 80px;
  background: #ffffff;
  border-color: #e2e8f0;
}

.rem-finished-notice {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: #64748b;
  background: #ffffff;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px dashed #cbd5e1;
}

.rem-cancelled-notice {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: #ef4444;
  background: #fef2f2;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px dashed #fca5a5;
}

/* Detail Modal */
.rem-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  font-family: 'Quicksand', sans-serif;
}

.rem-detail-modal {
  width: 100%;
  max-width: 440px;
  max-height: 85vh;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.22);
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.rem-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.rem-detail-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-header-icon {
  color: #2563eb;
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
}

.detail-close-btn:hover {
  background: #e2e8f0;
}

.rem-detail-body {
  padding: 18px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rem-detail-top {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.rem-detail-meta {
  flex: 1;
  min-width: 0;
}

.rem-detail-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.rem-detail-time {
  margin: 4px 0 0;
  font-size: 12.5px;
  font-weight: 700;
  color: #2563eb;
}

.rem-detail-creator {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}

.rem-detail-desc {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

.rem-detail-tabs {
  display: flex;
  border-bottom: 1px solid #e2e8f0;
}

.rem-dtab {
  flex: 1;
  padding: 8px 4px;
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
}

.rem-dtab.active-ok {
  border-bottom-color: #059669;
  color: #059669;
}

.rem-dtab.active-no {
  border-bottom-color: #e11d48;
  color: #e11d48;
}

.rem-dtab.active-wait {
  border-bottom-color: #d97706;
  color: #d97706;
}

.rem-dlist {
  max-height: 180px;
  overflow-y: auto;
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rem-ditem {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 8px;
}

.rem-ditem:hover {
  background: #f8fafc;
}

.rem-ditem-user {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.rem-ditem-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.rem-ditem-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.rem-ditem-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
}

.badge-ok {
  background: #ecfdf5;
  color: #059669;
}

.badge-no {
  background: #fff1f2;
  color: #e11d48;
}

.badge-wait {
  background: #fffbeb;
  color: #d97706;
}

.rem-dempty {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  padding: 14px 0;
  margin: 0;
}

.rem-detail-footer {
  padding: 12px 18px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rem-cancel-event-btn {
  padding: 7px 13px;
  border-radius: 10px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}

.rem-cancel-event-btn:hover {
  background: #fee2e2;
}

.rem-close-modal-btn {
  padding: 7px 16px;
  border-radius: 10px;
  border: none;
  background: #e2e8f0;
  color: #334155;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.rem-close-modal-btn:hover {
  background: #cbd5e1;
}
</style>
