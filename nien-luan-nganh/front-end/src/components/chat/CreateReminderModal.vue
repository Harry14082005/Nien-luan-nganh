<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import { resolveFileUrl } from '@/api/storage.api'

// =============================================================================
// ████  PHẦN 1: KHỞI TẠO PROPS, EMITS, STORE & STATE FORM TẠO LỊCH HẸN  ███████
// =============================================================================

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false,
  },
  roomId: {
    type: String,
    default: '',
  },
  roomType: {
    type: String,
    default: 'DIRECT',
  },
  roomName: {
    type: String,
    default: '',
  },
  members: {
    type: Array,
    default: () => [],
  },
  currentUserId: {
    type: [String, Number],
    default: null,
  },
})

const emit = defineEmits(['close', 'created'])

const chatStore = useChatStore()

const title = ref('')
const description = ref('')
const reminderType = ref('GROUP') // 'GROUP' | 'PERSONAL'
const startAtInput = ref('')
const hasEndAt = ref(false)
const endAtInput = ref('')
const participantScope = ref('ALL') // 'ALL' | 'CUSTOM'
const selectedUserIds = ref([])
const activePreset = ref('30m')
const submitting = ref(false)
const errorMessage = ref('')

// =============================================================================
// ████  PHẦN 2: BỘ CHỌN NGÀY GIỜ TÙY CHỈNH (CUSTOM DATE-TIME PICKER)  █████████
// =============================================================================

const activePickerField = ref(null) // null | 'START' | 'END'
const pickerViewYear = ref(new Date().getFullYear())
const pickerViewMonth = ref(new Date().getMonth()) // 0 - 11
const hourInputText = ref('09')
const minuteInputText = ref('00')
const hourListRef = ref(null)
const minuteListRef = ref(null)

const HOURS_LIST = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'))
const MINUTES_STEP_5 = Array.from({ length: 12 }, (_, i) => String(i * 5).padStart(2, '0'))
const WEEKDAY_HEADERS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']

// Chuyển đối tượng Date sang chuỗi 'YYYY-MM-DDTHH:mm'
function toDatetimeLocalValue(date) {
  const d = new Date(date)
  const pad = (n) => String(n).padStart(2, '0')
  const yyyy = d.getFullYear()
  const mm = pad(d.getMonth() + 1)
  const dd = pad(d.getDate())
  const hh = pad(d.getHours())
  const min = pad(d.getMinutes())
  return `${yyyy}-${mm}-${dd}T${hh}:${min}`
}

// Chuyển chuỗi sang chuẩn LocalDateTime của Spring Boot: 'YYYY-MM-DDTHH:mm:ss'
function toSpringLocalDateTime(localStr) {
  if (!localStr) return null
  return localStr.length === 16 ? `${localStr}:00` : localStr
}

// Định dạng hiển thị trên nút bấm chọn ngày giờ: "20:00 · Chủ Nhật, 08/11/2026"
function formatDisplayDateTime(localStr) {
  if (!localStr) return 'Chọn ngày & giờ...'
  const d = new Date(localStr)
  if (Number.isNaN(d.getTime())) return 'Chọn ngày & giờ...'
  const pad = (n) => String(n).padStart(2, '0')
  const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
  const hh = pad(d.getHours())
  const mm = pad(d.getMinutes())
  const dd = pad(d.getDate())
  const month = pad(d.getMonth() + 1)
  const yyyy = d.getFullYear()
  return `${hh}:${mm} · ${weekdays[d.getDay()]}, ${dd}/${month}/${yyyy}`
}

// Ngày giờ đang được chỉnh trong Popover hiện tại
const currentEditingValue = computed({
  get() {
    return activePickerField.value === 'END' ? endAtInput.value : startAtInput.value
  },
  set(val) {
    if (activePickerField.value === 'END') {
      endAtInput.value = val
    } else {
      startAtInput.value = val
      activePreset.value = 'custom'
    }
  },
})

const currentEditingDateObj = computed(() => {
  const raw = currentEditingValue.value
  const d = raw ? new Date(raw) : new Date()
  return Number.isNaN(d.getTime()) ? new Date() : d
})

// Mở Popover chọn ngày giờ cho START hoặc END
function toggleDateTimePicker(field) {
  if (activePickerField.value === field) {
    activePickerField.value = null
    return
  }
  activePickerField.value = field
  const raw = field === 'END' ? endAtInput.value : startAtInput.value
  const d = raw ? new Date(raw) : new Date(Date.now() + 30 * 60 * 1000)
  const validDate = Number.isNaN(d.getTime()) ? new Date() : d

  pickerViewYear.value = validDate.getFullYear()
  pickerViewMonth.value = validDate.getMonth()
  hourInputText.value = String(validDate.getHours()).padStart(2, '0')
  minuteInputText.value = String(validDate.getMinutes()).padStart(2, '0')

  nextTick(() => {
    scrollActiveTimeIntoView()
  })
}

function scrollActiveTimeIntoView() {
  if (hourListRef.value) {
    const activeHourEl = hourListRef.value.querySelector('.rem-time-opt.active')
    if (activeHourEl) {
      activeHourEl.scrollIntoView({ block: 'center', behavior: 'smooth' })
    }
  }
  if (minuteListRef.value) {
    const activeMinEl = minuteListRef.value.querySelector('.rem-time-opt.active')
    if (activeMinEl) {
      activeMinEl.scrollIntoView({ block: 'center', behavior: 'smooth' })
    }
  }
}

// Chuyển tháng trước / tháng sau trên bảng lịch
function changePickerMonth(delta) {
  let m = pickerViewMonth.value + delta
  let y = pickerViewYear.value
  if (m < 0) {
    m = 11
    y -= 1
  } else if (m > 11) {
    m = 0
    y += 1
  }
  pickerViewMonth.value = m
  pickerViewYear.value = y
}

// Nhảy nhanh về tháng hôm nay
function goToTodayDate() {
  const now = new Date()
  pickerViewYear.value = now.getFullYear()
  pickerViewMonth.value = now.getMonth()
  selectCalendarDay({
    year: now.getFullYear(),
    month: now.getMonth(),
    day: now.getDate(),
    isPast: false,
  })
}

// Tạo lưới 42 ô ngày (6 hàng x 7 cột, bắt đầu từ Thứ Hai)
const calendarDaysGrid = computed(() => {
  const year = pickerViewYear.value
  const month = pickerViewMonth.value
  const firstDayOfMonth = new Date(year, month, 1)
  // Chuyển Chủ Nhật (0) thành 6, Thứ Hai (1) thành 0...
  const startOffset = (firstDayOfMonth.getDay() + 6) % 7
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const daysInPrevMonth = new Date(year, month, 0).getDate()

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  // Nếu đang chọn END thì không cho chọn ngày trước ngày START
  let minAllowedDay = today
  if (activePickerField.value === 'END' && startAtInput.value) {
    const sDate = new Date(startAtInput.value)
    if (!Number.isNaN(sDate.getTime())) {
      minAllowedDay = new Date(sDate.getFullYear(), sDate.getMonth(), sDate.getDate())
    }
  }

  const sel = currentEditingDateObj.value
  const cells = []

  // 1. Các ngày của tháng trước
  for (let i = startOffset - 1; i >= 0; i--) {
    const d = daysInPrevMonth - i
    const prevM = month === 0 ? 11 : month - 1
    const prevY = month === 0 ? year - 1 : year
    const cellDate = new Date(prevY, prevM, d)
    cells.push({
      key: `prev-${d}`,
      year: prevY,
      month: prevM,
      day: d,
      isCurrentMonth: false,
      isToday: cellDate.getTime() === today.getTime(),
      isSelected:
        sel.getFullYear() === prevY && sel.getMonth() === prevM && sel.getDate() === d,
      isPast: cellDate < minAllowedDay,
    })
  }

  // 2. Các ngày trong tháng hiện tại
  for (let d = 1; d <= daysInMonth; d++) {
    const cellDate = new Date(year, month, d)
    cells.push({
      key: `curr-${d}`,
      year,
      month,
      day: d,
      isCurrentMonth: true,
      isToday: cellDate.getTime() === today.getTime(),
      isSelected:
        sel.getFullYear() === year && sel.getMonth() === month && sel.getDate() === d,
      isPast: cellDate < minAllowedDay,
    })
  }

  // 3. Các ngày đầu tháng sau cho đủ 42 ô
  const remaining = 42 - cells.length
  for (let d = 1; d <= remaining; d++) {
    const nextM = month === 11 ? 0 : month + 1
    const nextY = month === 11 ? year + 1 : year
    const cellDate = new Date(nextY, nextM, d)
    cells.push({
      key: `next-${d}`,
      year: nextY,
      month: nextM,
      day: d,
      isCurrentMonth: false,
      isToday: cellDate.getTime() === today.getTime(),
      isSelected:
        sel.getFullYear() === nextY && sel.getMonth() === nextM && sel.getDate() === d,
      isPast: cellDate < minAllowedDay,
    })
  }

  return cells
})

// Chọn 1 ngày trên lưới lịch
function selectCalendarDay(cell) {
  if (cell.isPast) return
  const current = new Date(currentEditingDateObj.value)
  current.setFullYear(cell.year, cell.month, cell.day)

  // Nếu chọn ngày hôm nay mà giờ đang ở quá khứ -> tự đẩy lên +15 phút
  const now = new Date()
  if (current <= now && activePickerField.value === 'START') {
    const future = new Date(now.getTime() + 15 * 60 * 1000)
    current.setHours(future.getHours(), future.getMinutes(), 0, 0)
    hourInputText.value = String(current.getHours()).padStart(2, '0')
    minuteInputText.value = String(current.getMinutes()).padStart(2, '0')
  }

  pickerViewYear.value = cell.year
  pickerViewMonth.value = cell.month
  currentEditingValue.value = toDatetimeLocalValue(current)
}

// Chọn giờ từ danh sách cuộn hoặc nhập tay
function selectHour(hhStr) {
  const hh = Math.min(23, Math.max(0, parseInt(hhStr, 10) || 0))
  hourInputText.value = String(hh).padStart(2, '0')
  const current = new Date(currentEditingDateObj.value)
  current.setHours(hh)
  currentEditingValue.value = toDatetimeLocalValue(current)
}

// Chọn phút từ danh sách bước nhảy 5 phút hoặc nhập tay
function selectMinute(mmStr) {
  const mm = Math.min(59, Math.max(0, parseInt(mmStr, 10) || 0))
  minuteInputText.value = String(mm).padStart(2, '0')
  const current = new Date(currentEditingDateObj.value)
  current.setMinutes(mm)
  currentEditingValue.value = toDatetimeLocalValue(current)
}

function handleManualHourBlur() {
  selectHour(hourInputText.value)
}

function handleManualMinuteBlur() {
  selectMinute(minuteInputText.value)
}

// =============================================================================
// ████  PHẦN 3: XEM TRƯỚC TỜ LỊCH, MỐC CHỌN NHANH (PRESETS) & WATCHERS  ███████
// =============================================================================

// Xem trước tờ lịch (Tháng, Ngày, Thứ) theo thời gian bắt đầu đang chọn
const calendarPreview = computed(() => {
  const d = startAtInput.value ? new Date(startAtInput.value) : new Date()
  if (Number.isNaN(d.getTime())) {
    return { month: 'THÁNG --', day: '--', weekday: '---', time: '--:--' }
  }
  const month = `THÁNG ${d.getMonth() + 1}`
  const day = String(d.getDate()).padStart(2, '0')
  const weekdays = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
  const weekday = weekdays[d.getDay()]
  const time = d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  return { month, day, weekday, time }
})

// Áp dụng các mốc chọn nhanh thời gian giống Zalo
function applyPreset(presetKey) {
  activePreset.value = presetKey
  activePickerField.value = null
  errorMessage.value = ''
  const now = new Date()
  let target = new Date(now)

  if (presetKey === '15m') {
    target = new Date(now.getTime() + 15 * 60 * 1000)
  } else if (presetKey === '30m') {
    target = new Date(now.getTime() + 30 * 60 * 1000)
  } else if (presetKey === '1h') {
    target = new Date(now.getTime() + 60 * 60 * 1000)
  } else if (presetKey === 'tonight') {
    target.setHours(20, 0, 0, 0)
    if (target <= now) {
      target.setDate(target.getDate() + 1)
    }
  } else if (presetKey === 'tomorrow9am') {
    target.setDate(target.getDate() + 1)
    target.setHours(9, 0, 0, 0)
  }

  startAtInput.value = toDatetimeLocalValue(target)

  if (hasEndAt.value) {
    const endTarget = new Date(target.getTime() + 60 * 60 * 1000)
    endAtInput.value = toDatetimeLocalValue(endTarget)
  }
}

// Khởi tạo lại form mỗi khi mở Modal
watch(
  () => props.isOpen,
  (open) => {
    if (open) {
      title.value = ''
      description.value = ''
      reminderType.value = 'GROUP'
      hasEndAt.value = false
      endAtInput.value = ''
      participantScope.value = 'ALL'
      selectedUserIds.value = (props.members || []).map((m) => m.userId)
      errorMessage.value = ''
      submitting.value = false
      activePickerField.value = null
      applyPreset('30m')
    }
  },
)

watch(hasEndAt, (enabled) => {
  if (enabled && startAtInput.value) {
    const start = new Date(startAtInput.value)
    if (!Number.isNaN(start.getTime())) {
      const end = new Date(start.getTime() + 60 * 60 * 1000)
      endAtInput.value = toDatetimeLocalValue(end)
    }
  } else if (!enabled) {
    endAtInput.value = ''
    if (activePickerField.value === 'END') {
      activePickerField.value = null
    }
  }
})

// =============================================================================
// ████  PHẦN 4: CHỌN THÀNH VIÊN THAM GIA & GỬI YÊU CẦU TẠO LỊCH HẸN  ██████████
// =============================================================================

const otherMembers = computed(() => {
  return (props.members || []).filter(
    (m) => !props.currentUserId || String(m.userId) !== String(props.currentUserId),
  )
})

function toggleMemberSelection(userId) {
  const idStr = String(userId)
  const idx = selectedUserIds.value.findIndex((id) => String(id) === idStr)
  if (idx !== -1) {
    selectedUserIds.value.splice(idx, 1)
  } else {
    selectedUserIds.value.push(userId)
  }
}

function isMemberSelected(userId) {
  return selectedUserIds.value.some((id) => String(id) === String(userId))
}

async function handleSubmit() {
  errorMessage.value = ''
  const trimmedTitle = title.value.trim()
  if (!trimmedTitle) {
    errorMessage.value = 'Vui lòng nhập tiêu đề nội dung nhắc hẹn.'
    return
  }
  if (!startAtInput.value) {
    errorMessage.value = 'Vui lòng chọn thời gian bắt đầu.'
    return
  }

  const startDate = new Date(startAtInput.value)
  if (Number.isNaN(startDate.getTime()) || startDate <= new Date()) {
    errorMessage.value = 'Thời gian nhắc hẹn phải lớn hơn thời điểm hiện tại.'
    return
  }

  if (hasEndAt.value && endAtInput.value) {
    const endDate = new Date(endAtInput.value)
    if (Number.isNaN(endDate.getTime()) || endDate <= startDate) {
      errorMessage.value = 'Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.'
      return
    }
  }

  let participantIds = null
  if (
    reminderType.value === 'GROUP' &&
    props.roomType === 'GROUP' &&
    participantScope.value === 'CUSTOM'
  ) {
    const ids = new Set(selectedUserIds.value.map((id) => String(id)))
    if (props.currentUserId) {
      ids.add(String(props.currentUserId))
    }
    participantIds = Array.from(ids)
  }

  const payload = {
    title: trimmedTitle,
    description: description.value.trim() || null,
    startAt: toSpringLocalDateTime(startAtInput.value),
    endAt: hasEndAt.value && endAtInput.value ? toSpringLocalDateTime(endAtInput.value) : null,
    reminderType: reminderType.value,
    participantIds,
  }

  submitting.value = true
  try {
    const created = await chatStore.createRoomReminder(props.roomId, payload)
    emit('created', created)
    emit('close')
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message ||
      err?.message ||
      'Không thể tạo lịch hẹn lúc này. Vui lòng kiểm tra lại thời gian.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="rem-fade">
      <div v-if="isOpen" class="rem-modal-backdrop" @click.self="emit('close')">
        <div class="rem-modal-card">
          <!-- Header -->
          <div class="rem-modal-header">
            <div class="rem-header-left">
              <div class="rem-header-icon">
                <font-awesome-icon icon="calendar-plus" />
              </div>
              <div class="rem-header-titles">
                <h3 class="rem-modal-title">Tạo nhắc hẹn mới</h3>
                <p class="rem-modal-subtitle">{{ roomName || 'Phòng trò chuyện' }}</p>
              </div>
            </div>
            <button type="button" class="rem-close-btn" title="Đóng" @click="emit('close')">
              <font-awesome-icon icon="x" />
            </button>
          </div>

          <!-- Body -->
          <div class="rem-modal-body">
            <!-- Thông báo lỗi -->
            <div v-if="errorMessage" class="rem-error-box">
              <font-awesome-icon icon="circle-xmark" />
              <span>{{ errorMessage }}</span>
            </div>

            <!-- Hàng trên: Tờ lịch xem trước + Nhập tiêu đề & ghi chú -->
            <div class="rem-top-section">
              <div
                class="rem-calendar-preview"
                :class="{ 'is-personal': reminderType === 'PERSONAL' }"
              >
                <div class="rem-cal-month">{{ calendarPreview.month }}</div>
                <div class="rem-cal-center">
                  <span class="rem-cal-day">{{ calendarPreview.day }}</span>
                  <span class="rem-cal-weekday">{{ calendarPreview.weekday }}</span>
                </div>
                <div class="rem-cal-time">{{ calendarPreview.time }}</div>
              </div>

              <div class="rem-inputs-col">
                <div class="rem-field">
                  <label class="rem-label">
                    Nội dung nhắc hẹn <span class="rem-required">*</span>
                  </label>
                  <input
                    v-model="title"
                    type="text"
                    maxlength="255"
                    class="rem-input"
                    placeholder="Nhập nội dung cần nhắc hẹn (VD: Họp nhóm đồ án lúc 20h)..."
                    @keyup.enter="handleSubmit"
                  />
                </div>
                <div class="rem-field">
                  <textarea
                    v-model="description"
                    rows="2"
                    maxlength="1000"
                    class="rem-textarea"
                    placeholder="Ghi chú thêm, link họp Google Meet hoặc địa điểm (tùy chọn)..."
                  ></textarea>
                </div>
              </div>
            </div>

            <!-- Chọn đối tượng nhận nhắc hẹn -->
            <div class="rem-field">
              <label class="rem-label">Đối tượng nhận nhắc hẹn</label>
              <div class="rem-segmented">
                <button
                  type="button"
                  class="rem-seg-btn"
                  :class="{ active: reminderType === 'GROUP' }"
                  @click="reminderType = 'GROUP'"
                >
                  <font-awesome-icon icon="user-group" />
                  <span>{{ roomType === 'GROUP' ? 'Tất cả' : 'Cả hai người' }}</span>
                </button>
                <button
                  type="button"
                  class="rem-seg-btn seg-personal"
                  :class="{ active: reminderType === 'PERSONAL' }"
                  @click="reminderType = 'PERSONAL'"
                >
                  <font-awesome-icon icon="lock" />
                  <span>Chỉ mình tôi</span>
                </button>
              </div>
            </div>

            <!-- Chọn nhanh thời gian -->
            <div class="rem-field">
              <label class="rem-label">Chọn nhanh thời gian</label>
              <div class="rem-presets-row">
                <button
                  v-for="preset in [
                    { key: '15m', label: '15 phút nữa' },
                    { key: '30m', label: '30 phút nữa' },
                    { key: '1h', label: '1 tiếng nữa' },
                    { key: 'tonight', label: 'Tối nay 20:00' },
                    { key: 'tomorrow9am', label: 'Sáng mai 09:00' },
                  ]"
                  :key="preset.key"
                  type="button"
                  class="rem-preset-chip"
                  :class="{ active: activePreset === preset.key }"
                  @click="applyPreset(preset.key)"
                >
                  {{ preset.label }}
                </button>
              </div>
            </div>

            <!-- Chọn ngày & giờ cụ thể (Nút mở Popover Lịch + Giờ) -->
            <div class="rem-datetime-panel">
              <div class="rem-datetime-row">
                <label class="rem-dt-label">
                  <font-awesome-icon icon="clock" class="dt-icon-blue" />
                  <span>Thời gian bắt đầu</span>
                </label>
                <button
                  type="button"
                  class="rem-dt-trigger"
                  :class="{ active: activePickerField === 'START' }"
                  @click="toggleDateTimePicker('START')"
                >
                  <font-awesome-icon icon="calendar-days" class="trigger-cal-icon" />
                  <span class="trigger-text">{{ formatDisplayDateTime(startAtInput) }}</span>
                  <font-awesome-icon
                    :icon="activePickerField === 'START' ? 'chevron-up' : 'chevron-down'"
                    class="trigger-chevron"
                  />
                </button>
              </div>

              <div class="rem-end-toggle-wrap">
                <button type="button" class="rem-end-toggle-btn" @click="hasEndAt = !hasEndAt">
                  <font-awesome-icon :icon="hasEndAt ? 'x' : 'plus'" />
                  <span>
                    {{
                      hasEndAt
                        ? 'Bỏ thời gian kết thúc'
                        : 'Thêm thời gian kết thúc (sự kiện dài ngày)'
                    }}
                  </span>
                </button>

                <div v-if="hasEndAt" class="rem-datetime-row rem-end-row">
                  <label class="rem-dt-label">
                    <font-awesome-icon icon="calendar-check" class="dt-icon-green" />
                    <span>Thời gian kết thúc</span>
                  </label>
                  <button
                    type="button"
                    class="rem-dt-trigger"
                    :class="{ active: activePickerField === 'END' }"
                    @click="toggleDateTimePicker('END')"
                  >
                    <font-awesome-icon icon="calendar-days" class="trigger-cal-icon is-end" />
                    <span class="trigger-text">{{ formatDisplayDateTime(endAtInput) }}</span>
                    <font-awesome-icon
                      :icon="activePickerField === 'END' ? 'chevron-up' : 'chevron-down'"
                      class="trigger-chevron"
                    />
                  </button>
                </div>
              </div>

              <!-- POPOVER BẢNG LỊCH THÁNG (TRÁI) + CỘT CHỌN GIỜ:PHÚT (PHẢI) -->
              <Transition name="picker-slide">
                <div v-if="activePickerField" class="rem-picker-popover">
                  <div class="rem-picker-top-banner">
                    <span class="picker-editing-tag">
                      {{
                        activePickerField === 'START'
                          ? 'Đang chọn: Thời gian bắt đầu'
                          : 'Đang chọn: Thời gian kết thúc'
                      }}
                    </span>
                    <button type="button" class="picker-today-btn" @click="goToTodayDate">
                      Hôm nay
                    </button>
                  </div>

                  <div class="rem-picker-grid-layout">
                    <!-- CỘT TRÁI: LỊCH THÁNG (T2 -> CN) -->
                    <div class="rem-cal-box">
                      <div class="rem-cal-nav">
                        <button
                          type="button"
                          class="cal-nav-btn"
                          title="Tháng trước"
                          @click="changePickerMonth(-1)"
                        >
                          <font-awesome-icon icon="chevron-left" />
                        </button>
                        <span class="cal-month-title">
                          Tháng {{ pickerViewMonth + 1 }}, {{ pickerViewYear }}
                        </span>
                        <button
                          type="button"
                          class="cal-nav-btn"
                          title="Tháng sau"
                          @click="changePickerMonth(1)"
                        >
                          <font-awesome-icon icon="chevron-right" />
                        </button>
                      </div>

                      <!-- Hàng thứ trong tuần -->
                      <div class="rem-cal-weekdays">
                        <span
                          v-for="(wd, idx) in WEEKDAY_HEADERS"
                          :key="wd"
                          class="cal-wd-cell"
                          :class="{ 'is-weekend': idx >= 5 }"
                        >
                          {{ wd }}
                        </span>
                      </div>

                      <!-- Lưới 42 ngày -->
                      <div class="rem-cal-days-grid">
                        <button
                          v-for="cell in calendarDaysGrid"
                          :key="cell.key"
                          type="button"
                          class="cal-day-btn"
                          :class="{
                            'is-other-month': !cell.isCurrentMonth,
                            'is-today': cell.isToday,
                            'is-selected': cell.isSelected,
                            'is-disabled': cell.isPast,
                          }"
                          :disabled="cell.isPast"
                          @click="selectCalendarDay(cell)"
                        >
                          <span>{{ cell.day }}</span>
                          <i v-if="cell.isToday && !cell.isSelected" class="today-dot"></i>
                        </button>
                      </div>
                    </div>

                    <!-- CỘT PHẢI: CHỌN GIỜ : PHÚT (NHẬP TAY + CUỘN BƯỚC NHẢY 5 PHÚT) -->
                    <div class="rem-time-box">
                      <div class="rem-time-header">Giờ : Phút</div>

                      <!-- Ô nhập trực tiếp HH : mm -->
                      <div class="rem-manual-time-inputs">
                        <input
                          v-model="hourInputText"
                          type="number"
                          min="0"
                          max="23"
                          class="manual-time-field"
                          title="Nhập giờ (00 - 23)"
                          @blur="handleManualHourBlur"
                          @keyup.enter="handleManualHourBlur"
                        />
                        <span class="manual-colon">:</span>
                        <input
                          v-model="minuteInputText"
                          type="number"
                          min="0"
                          max="59"
                          class="manual-time-field"
                          title="Nhập phút chính xác (00 - 59)"
                          @blur="handleManualMinuteBlur"
                          @keyup.enter="handleManualMinuteBlur"
                        />
                      </div>

                      <!-- 2 Cột cuộn chọn nhanh Giờ (00-23) & Phút (bước nhảy 5p) -->
                      <div class="rem-time-columns">
                        <div ref="hourListRef" class="rem-time-col">
                          <div class="time-col-label">Giờ</div>
                          <button
                            v-for="hh in HOURS_LIST"
                            :key="hh"
                            type="button"
                            class="rem-time-opt"
                            :class="{ active: hourInputText === hh }"
                            @click="selectHour(hh)"
                          >
                            {{ hh }}
                          </button>
                        </div>

                        <div ref="minuteListRef" class="rem-time-col">
                          <div class="time-col-label">Phút</div>
                          <button
                            v-for="mm in MINUTES_STEP_5"
                            :key="mm"
                            type="button"
                            class="rem-time-opt"
                            :class="{ active: minuteInputText === mm }"
                            @click="selectMinute(mm)"
                          >
                            {{ mm }}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Chân Popover -->
                  <div class="rem-picker-footer">
                    <span class="picker-selected-summary">
                      Đã chọn: <strong>{{ formatDisplayDateTime(currentEditingValue) }}</strong>
                    </span>
                    <button
                      type="button"
                      class="picker-done-btn"
                      @click="activePickerField = null"
                    >
                      <font-awesome-icon icon="check" />
                      <span>Xong</span>
                    </button>
                  </div>
                </div>
              </Transition>
            </div>

            <!-- Tùy chọn thành viên tham gia trong phòng nhóm -->
            <div
              v-if="reminderType === 'GROUP' && roomType === 'GROUP' && otherMembers.length > 0"
              class="rem-members-section"
            >
              <div class="rem-members-header">
                <label class="rem-label" style="margin: 0">Thành viên tham gia</label>
                <div class="rem-scope-pills">
                  <button
                    type="button"
                    class="rem-scope-pill"
                    :class="{ active: participantScope === 'ALL' }"
                    @click="participantScope = 'ALL'"
                  >
                    Tất cả ({{ members.length }})
                  </button>
                  <button
                    type="button"
                    class="rem-scope-pill"
                    :class="{ active: participantScope === 'CUSTOM' }"
                    @click="participantScope = 'CUSTOM'"
                  >
                    Tùy chọn
                  </button>
                </div>
              </div>

              <div v-if="participantScope === 'CUSTOM'" class="rem-members-list">
                <label v-for="m in otherMembers" :key="m.userId" class="rem-member-item">
                  <div class="rem-member-left">
                    <img
                      :src="
                        resolveFileUrl(m.avatarUrl) ||
                        `https://ui-avatars.com/api/?name=${encodeURIComponent(m.username || 'U')}&background=random`
                      "
                      class="rem-member-avatar"
                      alt="avatar"
                    />
                    <span class="rem-member-name">{{ m.username }}</span>
                  </div>
                  <input
                    type="checkbox"
                    :checked="isMemberSelected(m.userId)"
                    class="rem-member-checkbox"
                    @change="toggleMemberSelection(m.userId)"
                  />
                </label>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="rem-modal-footer">
            <button type="button" class="rem-btn-cancel" @click="emit('close')">Hủy</button>
            <button
              type="button"
              class="rem-btn-submit"
              :disabled="submitting || !title.trim()"
              @click="handleSubmit"
            >
              <font-awesome-icon icon="calendar-check" />
              <span>{{ submitting ? 'Đang tạo...' : 'Tạo nhắc hẹn' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700&display=swap');

.rem-modal-backdrop {
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

.rem-modal-card {
  width: 100%;
  max-width: 530px;
  max-height: 92vh;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.22);
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: remPop 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes remPop {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.rem-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 60%, #eef2ff 100%);
  border-bottom: 1px solid #e2e8f0;
}

.rem-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.rem-header-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
  flex-shrink: 0;
}

.rem-header-titles {
  min-width: 0;
}

.rem-modal-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.rem-modal-subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
}

.rem-close-btn {
  width: 32px;
  height: 32px;
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

.rem-close-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.rem-modal-body {
  padding: 16px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rem-error-box {
  padding: 10px 14px;
  border-radius: 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.rem-top-section {
  display: flex;
  gap: 14px;
  align-items: stretch;
}

.rem-calendar-preview {
  width: 88px;
  flex-shrink: 0;
  border-radius: 14px;
  border: 1px solid #dbeafe;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  text-align: center;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08);
}

.rem-calendar-preview.is-personal {
  border-color: #e0e7ff;
  background: linear-gradient(180deg, #eef2ff 0%, #ffffff 100%);
}

.rem-cal-month {
  background: #2563eb;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  padding: 5px 4px;
  letter-spacing: 0.5px;
}

.rem-calendar-preview.is-personal .rem-cal-month {
  background: #4f46e5;
}

.rem-cal-center {
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rem-cal-day {
  font-size: 28px;
  font-weight: 800;
  color: #2563eb;
  line-height: 1;
}

.rem-calendar-preview.is-personal .rem-cal-day {
  color: #4f46e5;
}

.rem-cal-weekday {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  margin-top: 4px;
}

.rem-cal-time {
  background: #f1f5f9;
  border-top: 1px solid #e2e8f0;
  font-size: 11px;
  font-weight: 700;
  color: #334155;
  padding: 4px;
}

.rem-inputs-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.rem-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rem-label {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
}

.rem-required {
  color: #ef4444;
}

.rem-input,
.rem-textarea {
  width: 100%;
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  font-family: 'Quicksand', sans-serif;
  font-size: 13.5px;
  color: #0f172a;
  outline: none;
  transition: all 0.15s;
  box-sizing: border-box;
}

.rem-textarea {
  resize: none;
  font-size: 13px;
}

.rem-input:focus,
.rem-textarea:focus {
  background: #ffffff;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.rem-segmented {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 12px;
}

.rem-seg-btn {
  border: none;
  background: transparent;
  padding: 8px 10px;
  border-radius: 9px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.15s;
}

.rem-seg-btn.active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.rem-seg-btn.seg-personal.active {
  color: #4f46e5;
}

.rem-presets-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.rem-preset-chip {
  padding: 6px 11px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.rem-preset-chip:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.rem-preset-chip.active {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #2563eb;
  font-weight: 700;
}

/* =========================================================================
   CUSTOM DATE & TIME TRIGGER + POPOVER (ZALO PC / GOOGLE CALENDAR STYLE)
   ========================================================================= */
.rem-datetime-panel {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rem-datetime-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.rem-dt-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.dt-icon-blue {
  color: #2563eb;
}

.dt-icon-green {
  color: #10b981;
}

.rem-dt-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 12px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  cursor: pointer;
  transition: all 0.16s ease;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.rem-dt-trigger:hover {
  border-color: #3b82f6;
  background: #f8fafc;
}

.rem-dt-trigger.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}

.trigger-cal-icon {
  color: #2563eb;
  font-size: 13px;
}

.trigger-cal-icon.is-end {
  color: #10b981;
}

.trigger-text {
  letter-spacing: 0.1px;
}

.trigger-chevron {
  font-size: 10px;
  color: #64748b;
  margin-left: 2px;
}

/* Popover Khung Lịch + Giờ */
.rem-picker-popover {
  margin-top: 4px;
  background: #ffffff;
  border: 1px solid #bfdbfe;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.12);
  overflow: hidden;
}

.picker-slide-enter-active,
.picker-slide-leave-active {
  transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}

.picker-slide-enter-from,
.picker-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.rem-picker-top-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: linear-gradient(90deg, #eff6ff 0%, #f8fafc 100%);
  border-bottom: 1px solid #e2e8f0;
}

.picker-editing-tag {
  font-size: 11.5px;
  font-weight: 700;
  color: #1d4ed8;
}

.picker-today-btn {
  border: 1px solid #bfdbfe;
  background: #ffffff;
  color: #2563eb;
  font-family: 'Quicksand', sans-serif;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s;
}

.picker-today-btn:hover {
  background: #2563eb;
  color: #ffffff;
}

.rem-picker-grid-layout {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  divide-x: 1px solid #f1f5f9;
}

/* Cột trái: Lịch tháng */
.rem-cal-box {
  padding: 12px 14px;
  border-right: 1px solid #f1f5f9;
}

.rem-cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.cal-month-title {
  font-size: 13.5px;
  font-weight: 800;
  color: #0f172a;
}

.cal-nav-btn {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  font-size: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.cal-nav-btn:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
}

.rem-cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 4px;
}

.cal-wd-cell {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  padding: 4px 0;
}

.cal-wd-cell.is-weekend {
  color: #ef4444;
}

.rem-cal-days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.cal-day-btn {
  position: relative;
  height: 31px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-family: 'Quicksand', sans-serif;
  font-size: 12.5px;
  font-weight: 700;
  color: #1e293b;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all 0.12s;
}

.cal-day-btn:hover:not(:disabled) {
  background: #eff6ff;
  color: #2563eb;
}

.cal-day-btn.is-other-month {
  color: #94a3b8;
  font-weight: 500;
}

.cal-day-btn.is-today {
  color: #2563eb;
  font-weight: 800;
}

.today-dot {
  position: absolute;
  bottom: 3px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #2563eb;
}

.cal-day-btn.is-selected {
  background: linear-gradient(135deg, #2563eb, #3b82f6) !important;
  color: #ffffff !important;
  font-weight: 800;
  box-shadow: 0 3px 8px rgba(37, 99, 235, 0.3);
}

.cal-day-btn.is-disabled {
  color: #cbd5e1 !important;
  cursor: not-allowed;
  opacity: 0.55;
}

/* Cột phải: Chọn Giờ : Phút */
.rem-time-box {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rem-time-header {
  font-size: 12px;
  font-weight: 800;
  color: #475569;
  text-align: center;
}

.rem-manual-time-inputs {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #f1f5f9;
  padding: 5px 8px;
  border-radius: 10px;
}

.manual-time-field {
  width: 46px;
  padding: 4px;
  border-radius: 7px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  font-weight: 800;
  color: #1d4ed8;
  text-align: center;
  outline: none;
}

.manual-time-field:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.manual-colon {
  font-size: 15px;
  font-weight: 800;
  color: #334155;
}

.rem-time-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.rem-time-col {
  max-height: 158px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  padding: 4px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rem-time-col::-webkit-scrollbar {
  width: 4px;
}

.rem-time-col::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.time-col-label {
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  text-align: center;
  padding: 2px 0 4px;
  text-transform: uppercase;
}

.rem-time-opt {
  padding: 5px 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-family: 'Quicksand', sans-serif;
  font-size: 12.5px;
  font-weight: 700;
  color: #334155;
  cursor: pointer;
  transition: all 0.12s;
}

.rem-time-opt:hover {
  background: #e0e7ff;
  color: #1d4ed8;
}

.rem-time-opt.active {
  background: #2563eb;
  color: #ffffff;
}

.rem-picker-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.picker-selected-summary {
  font-size: 12px;
  color: #475569;
}

.picker-selected-summary strong {
  color: #0f172a;
}

.picker-done-btn {
  padding: 5px 14px;
  border-radius: 8px;
  border: none;
  background: #2563eb;
  color: #ffffff;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: background 0.15s;
}

.picker-done-btn:hover {
  background: #1d4ed8;
}

.rem-end-toggle-wrap {
  border-top: 1px dashed #e2e8f0;
  padding-top: 8px;
}

.rem-end-toggle-btn {
  border: none;
  background: transparent;
  color: #2563eb;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0;
}

.rem-end-toggle-btn:hover {
  text-decoration: underline;
}

.rem-end-row {
  margin-top: 10px;
}

.rem-members-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rem-members-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rem-scope-pills {
  display: flex;
  gap: 6px;
}

.rem-scope-pill {
  border: none;
  background: #f1f5f9;
  color: #64748b;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.rem-scope-pill.active {
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 700;
}

.rem-members-list {
  max-height: 135px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 6px;
  background: #f8fafc;
}

.rem-member-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 8px;
  cursor: pointer;
}

.rem-member-item:hover {
  background: #ffffff;
}

.rem-member-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.rem-member-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
}

.rem-member-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.rem-member-checkbox {
  width: 15px;
  height: 15px;
  accent-color: #2563eb;
}

.rem-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.rem-btn-cancel {
  padding: 8px 16px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.rem-btn-cancel:hover {
  background: #f1f5f9;
}

.rem-btn-submit {
  padding: 8px 18px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  transition: all 0.15s;
}

.rem-btn-submit:hover:not(:disabled) {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
}

.rem-btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
