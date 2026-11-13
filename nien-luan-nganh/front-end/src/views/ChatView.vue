<script setup>
/**
 * =========================================================================
 * View: ChatView.vue
 * =========================================================================
 * - Màn hình chính của ứng dụng Chat (Bố cục 3 cột: NavigationSidebar | ChatListSidebar | ChatView).
 * - Kết nối dữ liệu phòng đang mở (activeRoom), danh sách tin nhắn (messages), và tin ghim (pinnedMessages).
 * - Tự động cuộn xuống đáy khi có tin nhắn mới hoặc khi chuyển phòng (scrollToBottom).
 * - Tự động cuộn mượt và highlight tới tin nhắn khi bấm vào thanh ghim (handleScrollToMessage).
 * - Lắng nghe sự kiện ghim/bỏ ghim tin nhắn bất kỳ (handleTogglePin).
 * - Hiển thị trạng thái đang gõ phím của đối phương với hiệu ứng chấm nhảy (Typing indicator).
 */
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import ChatHeader from '@/components/chat/ChatHeader.vue'
import PinnedMessage from '@/components/chat/PinnedMessage.vue'
import MessageList from '@/components/chat/MessageList.vue'
import MessageSearchSidebar from '@/components/chat/MessageSearchSidebar.vue'
import ChatInfoSidebar from '@/components/chat/ChatInfoSidebar.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import NavigationSidebar from '@/components/common/NavigationSidebar.vue'
import ChatListSidebar from '@/components/chat/ChatListSidebar.vue'
import MessageHistoryModal from '@/components/chat/MessageHistoryModal.vue'
import AddMemberModal from '@/components/chat/AddMemberModal.vue'
import CreateReminderModal from '@/components/chat/CreateReminderModal.vue'
import MediaLightboxModal from '@/components/chat/MediaLightboxModal.vue'
import { initGlobalWebSocket } from '@/services/websocket'
import { getRoomMembers } from '@/api/room.api'
import { resolveFileUrl } from '@/api/storage.api';

const chatStore = useChatStore()
const authStore = useAuthStore()
const chatBodyRef = ref(null) // Tham chiếu tới vùng cuộn tin nhắn (.chat-body)
const isLoadingOlder = ref(false) // Cờ chặn scrollToBottom khi tải tin nhắn cũ
const showScrollBottomBtn = ref(false) // Trạng thái hiển thị nút cuộn xuống đáy
const isSmoothScrollingToBottom = ref(false) // Cờ hiệu ứng cuộn mượt về đáy tin nhắn mới nhất
const ticker = ref(Date.now()) // Bộ đếm thời gian cập nhật lại thời gian offline mỗi 30 giây
let tickerTimer = null
let previousLastMessageId = null // Lưu id của tin nhắn cuối cùng để phân biệt tải tin cũ vs tin nhắn mới

// Quản lý popup lịch sử chỉnh sửa tin nhắn
const showHistoryModal = ref(false)
const selectedHistoryList = ref([])
const historyCurrentContent = ref('')
const historySenderName = ref('')

// Quản lý tìm kiếm tin nhắn trong phòng (In-Room Message Search)
const showSearchBar = ref(false)
const currentSearchKeyword = ref('')

// Quản lý thanh thông tin phòng chat bên phải (Chat Info Sidebar)
const isInfoSidebarOpen = ref(false)
const currentRoomMembers = ref([])
const isNotificationMuted = ref(false)
const showAddMemberModal = ref(false)
const showCreateReminderModal = ref(false)

// Kiểm tra quyền Admin/Owner của người dùng hiện tại trong phòng đang mở
const canManageCurrentRoom = computed(() => {
  const myId = authStore.user?.userId
  if (!myId || !Array.isArray(currentRoomMembers.value)) return false
  const me = currentRoomMembers.value.find((m) => String(m.userId) === String(myId))
  const role = me?.role || me?.roomMemberRole
  return role === 'ADMIN' || role === 'OWNER'
})

const handleOpenCreateReminder = async () => {
  if (!chatStore.activeRoomId) return
  if (chatStore.activeRoom?.roomType === 'GROUP' && currentRoomMembers.value.length === 0) {
    await fetchCurrentRoomMembers()
  }
  showCreateReminderModal.value = true
}

// Quản lý trình xem ảnh / video toàn màn hình (Media Lightbox)
const showMediaLightbox = ref(false)
const lightboxMediaList = ref([])
const lightboxInitialIndex = ref(0)

/**
 * Mở xem ảnh / video toàn màn hình
 * @param {object} targetMedia { type: 'IMAGE'|'VIDEO', url: string }
 */
const handleOpenMediaPreview = (targetMedia) => {
  if (!targetMedia || !targetMedia.url) return

  // Gom toàn bộ ảnh và video trong phòng thành danh sách xem liên tục
  const allMedia = [
    ...(roomMediaData.value?.images || []),
    ...(roomMediaData.value?.videos || []),
  ]

  // Tìm index của file được click
  const targetUrl = targetMedia.url
  let foundIdx = allMedia.findIndex(
    (m) => m.url === targetUrl || m.content === targetUrl
  )

  if (foundIdx === -1) {
    // Nếu chưa có trong danh sách tổng hợp (ví dụ vừa gửi xong), tạo 1 item tạm
    allMedia.unshift({
      url: targetUrl,
      type: targetMedia.type || 'IMAGE',
      name: 'Phương tiện',
    })
    foundIdx = 0
  }

  lightboxMediaList.value = allMedia
  lightboxInitialIndex.value = foundIdx
  showMediaLightbox.value = true
}

const handleMemberAdded = () => {
  fetchCurrentRoomMembers()
}

const fetchCurrentRoomMembers = async () => {
  if (!chatStore.activeRoomId) {
    currentRoomMembers.value = []
    return
  }
  try {
    const list = await getRoomMembers(chatStore.activeRoomId)
    currentRoomMembers.value = Array.isArray(list) ? list : []
  } catch (err) {
    console.error('Lỗi lấy danh sách thành viên phòng:', err)
    currentRoomMembers.value = []
  }
}

const handleToggleSearch = () => {
  showSearchBar.value = !showSearchBar.value
  if (showSearchBar.value) {
    isInfoSidebarOpen.value = false
  } else {
    currentSearchKeyword.value = ''
  }
}

const handleCloseSearch = () => {
  showSearchBar.value = false
  currentSearchKeyword.value = ''
}

const handleToggleInfo = () => {
  isInfoSidebarOpen.value = !isInfoSidebarOpen.value
  if (isInfoSidebarOpen.value) {
    showSearchBar.value = false
    currentSearchKeyword.value = ''
    fetchCurrentRoomMembers()
    if (chatStore.activeRoomId) {
      chatStore.fetchRoomMedia(chatStore.activeRoomId)
    }
  }
}

const handleCloseInfo = () => {
  isInfoSidebarOpen.value = false
}

// Cờ xác định có sidebar bên phải nào đang mở hay không (Tìm kiếm hoặc Thông tin)
const isRightDrawerOpen = computed(() => Boolean(showSearchBar.value || isInfoSidebarOpen.value))

const handleMuteNotification = (isMuted) => {
  isNotificationMuted.value = isMuted
}

// Tổng hợp danh sách File phương tiện (Ảnh, Video, Tài liệu) từ Pinia Store (Toàn bộ trong phòng)
const roomMediaData = computed(() => {
  const storeMedia = chatStore.roomMedia
  if (
    storeMedia &&
    (storeMedia.images?.length > 0 || storeMedia.videos?.length > 0 || storeMedia.files?.length > 0)
  ) {
    return storeMedia
  }

  // Fallback từ các tin nhắn hiện có
  const images = []
  const videos = []
  const files = []

  ;(chatStore.messages || []).forEach((m) => {
    if (m.isRevoked) return
    const content = m.content || ''
    const isImg = m.messageType === 'IMAGE' || content.match(/\.(jpg|jpeg|png|gif|webp)$/i)
    const isVid = m.messageType === 'VIDEO' || content.match(/\.(mp4|mov|webm|avi)$/i)

    if (isImg) {
      images.push({
        id: m.messageId,
        url: m.content,
        name: 'Hình ảnh',
        type: 'IMAGE',
        createdAt: m.createdAt,
      })
    } else if (isVid) {
      videos.push({
        id: m.messageId,
        url: m.content,
        name: 'Video',
        type: 'VIDEO',
        createdAt: m.createdAt,
      })
    } else if (m.messageType === 'FILE' || m.messageType === 'DOCUMENT') {
      files.push({
        id: m.messageId,
        url: m.content,
        name: m.content?.split('/').pop() || 'Tài liệu',
        size: 'Đính kèm',
        type: 'FILE',
        createdAt: m.createdAt,
      })
    }
  })

  const sortByDateDesc = (a, b) => {
    const tA = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const tB = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return tB - tA
  }
  images.sort(sortByDateDesc)
  videos.sort(sortByDateDesc)
  files.sort(sortByDateDesc)

  return { images, videos, files }
})

const handleUpdateSearchKeyword = (keyword) => {
  currentSearchKeyword.value = keyword
}

const handleGlobalKeyDown = (e) => {
  if ((e.ctrlKey || e.metaKey) && (e.key === 'f' || e.key === 'F')) {
    e.preventDefault()
    isInfoSidebarOpen.value = false
    showSearchBar.value = true
  } else if (e.key === 'Escape') {
    if (showSearchBar.value) {
      handleCloseSearch()
    } else if (isInfoSidebarOpen.value) {
      handleCloseInfo()
    }
  }
}

/**
 * Định dạng trạng thái hiện diện (Online / Offline / Thời gian offline)
 * - Nếu đang online: "Đang hoạt động"
 * - Nếu là nhóm: "Nhóm trò chuyện"
 * - Nếu offline dưới 1 phút: "Vừa mới truy cập"
 * - Nếu offline trên 1 phút: "Đã offline X phút trước" / "X giờ trước" / "X ngày trước"
 */
const formatPresenceStatus = (room) => {
  const _now = ticker.value // Kích hoạt tính phản ứng khi ticker nhảy
  if (!room) return ''
  if (room.roomType === 'GROUP') {
    return 'Nhóm trò chuyện'
  }
  if (room.isOnline) {
    return 'Đang hoạt động'
  }
  if (!room.lastSeenAt) {
    return 'Offline'
  }

  const lastSeen = new Date(room.lastSeenAt)
  if (isNaN(lastSeen.getTime())) {
    return 'Offline'
  }

  const diffMs = _now - lastSeen.getTime()
  const diffMinutes = Math.floor(diffMs / 60000)

  if (diffMinutes < 1) {
    return 'Vừa mới truy cập'
  }
  if (diffMinutes < 60) {
    return `Đã offline ${diffMinutes} phút trước`
  }
  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) {
    return `Đã offline ${diffHours} giờ trước`
  }
  const diffDays = Math.floor(diffHours / 24)
  return `Đã offline ${diffDays} ngày trước`
}

let scrollToBottomTimer1 = null
let scrollToBottomTimer2 = null
const isJumpingToMessage = ref(false) // Khóa cuộn tự động khi đang nhảy tới tin nhắn ghim/reply/reminder
const isJumpingContextBadge = ref(false) // Hiển thị badge nhỏ khi đang tải ngữ cảnh tin nhắn ở xa
let jumpLockTimer = null
let jumpRecenterTimer = null

/**
 * Cuộn khung chat xuống sát đáy mượt mà và chuẩn xác sau khi DOM cập nhật
 * (Có nhịp chờ dự phòng 60ms và 180ms để đảm bảo các badge thông báo hệ thống và avatar render xong vẫn chạm sát đáy)
 */
const scrollToBottom = async () => {
  //Chan ngay neu dang trong qua trinh tai tin nhan cu hoac dang nhay toi tin nhan ghim
  if (
    isLoadingOlder.value ||
    chatStore.isLoadingOlder ||
    isJumpingToMessage.value ||
    chatStore.isJumpingToContext
  ) {
    return
  }
  
  //uy timer cu con dang xep hang cho chay => Ngan xung dot cac lan goi cuon
  if (scrollToBottomTimer1) clearTimeout(scrollToBottomTimer1)
  if (scrollToBottomTimer2) clearTimeout(scrollToBottomTimer2)

  await nextTick()
  if (
    chatBodyRef.value &&
    !isLoadingOlder.value &&
    !chatStore.isLoadingOlder &&
    !isJumpingToMessage.value &&
    !chatStore.isJumpingToContext
  ) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
  scrollToBottomTimer1 = setTimeout(() => {
    if (
      chatBodyRef.value &&
      !isLoadingOlder.value &&
      !chatStore.isLoadingOlder &&
      !isJumpingToMessage.value &&
      !chatStore.isJumpingToContext
    ) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  }, 60)
  scrollToBottomTimer2 = setTimeout(() => {
    if (
      chatBodyRef.value &&
      !isLoadingOlder.value &&
      !chatStore.isLoadingOlder &&
      !isJumpingToMessage.value &&
      !chatStore.isJumpingToContext
    ) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  }, 180)
}

/**
 * Kiểm tra xem tin nhắn có phải do chính người dùng hiện tại gửi đi hay không
 * @param {object} msg Đối tượng tin nhắn (MessageResponse)
 */
const isMyMessage = (msg) => {
  if (!msg || !msg.sender) return false
  const currentUserId = authStore.user?.userId
  if (currentUserId && msg.sender.userId) {
    return String(msg.sender.userId) === String(currentUserId)
  }
  if (authStore.user?.username && msg.sender.username) {
    return msg.sender.username === authStore.user.username
  }
  return msg.sender.userId === 'me'
}

/**
 * Định dạng chuỗi thời gian tin nhắn dạng HH:mm (vd: 15:45)
 */
const formatTime = (isoString) => {
  if (!isoString) return ''
  try {
    const d = new Date(isoString)
    if (isNaN(d.getTime())) return ''
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes}`
  } catch {
    return ''
  }
}

/**
 * Nhận diện tin nhắn SYSTEM dạng mốc thời gian cũ trong DB (vd: "14:35 05/11/2026")
 * để ẩn đi, tránh hiển thị trùng lặp với mốc thời gian tự động của Frontend.
 */
const isLegacyTimeLogMessage = (msg) => {
  if (!msg || msg.messageType !== 'SYSTEM') return false;
  const content = String(msg.content || '').trim();
  return /^\d{1,2}:\d{2}(\s+\d{1,2}\/\d{1,2}(\/\d{2,4})?)?$/.test(content);
};

/**
 * Định dạng mốc phân chia thời gian giữa các cuộc trò chuyện (Time Divider):
 * - Hôm nay: "Hôm nay, 14:35"
 * - Hôm qua: "Hôm qua, 14:35"
 * - Cùng năm: "14:35, 14/11"
 * - Khác năm: "14:35, 14/11/2025"
 */
const formatTimeDivider = (isoString) => {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '';

    const now = new Date();
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const timeStr = `${hours}:${minutes}`;

    const isToday = d.toDateString() === now.toDateString();
    if (isToday) {
      return `Hôm nay, ${timeStr}`;
    }

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const isYesterday = d.toDateString() === yesterday.toDateString();
    if (isYesterday) {
      return `Hôm qua, ${timeStr}`;
    }

    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();

    if (year === now.getFullYear()) {
      return `${timeStr}, ${day}/${month}`;
    }

    return `${timeStr}, ${day}/${month}/${year}`;
  } catch {
    return '';
  }
};

/**
 * Kiểm tra xem có nên hiển thị mốc phân chia thời gian (Time Divider) trước tin nhắn thứ idx hay không
 * Hiển thị khi:
 * 1. Là tin nhắn đầu tiên trong danh sách (idx === 0)
 * 2. Khoảng cách thời gian giữa tin hiện tại và tin hợp lệ liền trước >= 5 phút (300,000 ms)
 * 3. Hoặc chuyển sang ngày mới (khác ngày)
 */
const shouldShowTimeDivider = (idx) => {
  const current = chatStore.messages[idx];
  if (!current || !current.createdAt) return false;
  if (isLegacyTimeLogMessage(current)) return false;

  // Tìm tin nhắn hợp lệ liền trước (bỏ qua các tin nhắn time log cũ nếu có trong DB)
  let prev = null;
  for (let i = idx - 1; i >= 0; i--) {
    const msg = chatStore.messages[i];
    if (!isLegacyTimeLogMessage(msg)) {
      prev = msg;
      break;
    }
  }

  // Nếu không có tin nhắn hợp lệ nào trước đó -> đây là tin đầu tiên -> hiển thị mốc giờ
  if (!prev || !prev.createdAt) return true;

  const prevDate = new Date(prev.createdAt);
  const currDate = new Date(current.createdAt);
  const prevTime = prevDate.getTime();
  const currTime = currDate.getTime();

  if (isNaN(prevTime) || isNaN(currTime)) return false;

  // Nếu khác ngày -> luôn hiện mốc ngày mới
  if (prevDate.toDateString() !== currDate.toDateString()) {
    return true;
  }

  // Cùng ngày -> hiển thị nếu cách nhau >= 5 phút (5 * 60 * 1000 ms)
  return Math.abs(currTime - prevTime) >= 5 * 60 * 1000;
};

/**
 * Kiểm tra xem 2 tin nhắn có cùng người gửi hay không
 */
const isSameSender = (msg1, msg2) => {
  if (!msg1 || !msg2) return false
  const isMsg1Mine = isMyMessage(msg1)
  const isMsg2Mine = isMyMessage(msg2)
  if (isMsg1Mine !== isMsg2Mine) return false
  if (!isMsg1Mine) {
    const id1 = msg1.sender?.userId || msg1.sender?.username
    const id2 = msg2.sender?.userId || msg2.sender?.username
    return id1 === id2
  }
  return true
}

/**
 * Kiểm tra xem khoảng cách thời gian giữa 2 tin nhắn có trong vòng 2 phút hay không
 */
const isCloseInTime = (msg1, msg2) => {
  if (!msg1?.createdAt || !msg2?.createdAt) return false
  const t1 = new Date(msg1.createdAt).getTime()
  const t2 = new Date(msg2.createdAt).getTime()
  if (isNaN(t1) || isNaN(t2)) return false
  return Math.abs(t2 - t1) <= 2 * 60 * 1000
}

/**
 * Danh sách các loại tin nhắn thông thường cho phép gộp bubble bo góc (Whitelist)
 * Các loại Card tương tác độc lập (POLL, REMINDER, CALL_LOG...) và SYSTEM sẽ không nằm trong nhóm này.
 */
const MERGEABLE_TYPES = new Set(['TEXT', 'IMAGE', 'FILE', 'LINK'])

/**
 * Kiểm tra xem một tin nhắn có thuộc nhóm cho phép gộp bubble bo góc hay không
 */
const isMergeable = (msg) => {
  if (!msg || msg.isEvent) return false
  const type = msg.messageType || 'TEXT'
  return MERGEABLE_TYPES.has(type)
}

/**
 * Tin nhắn thứ idx có phải là tin nhắn liên tiếp (consecutive) tiếp nối tin trước đó không?
 */
const isConsecutiveMessage = (idx) => {
  if (idx <= 0) return false
  const current = chatStore.messages[idx]
  const prev = chatStore.messages[idx - 1]
  if (!isMergeable(current) || !isMergeable(prev)) return false
  return isSameSender(current, prev) && isCloseInTime(current, prev)
}

/**
 * Có hiển thị mốc thời gian ở tin nhắn thứ idx không?
 * Chỉ hiển thị ở tin nhắn cuối cùng của chuỗi tin nhắn liên tiếp (hoặc khi tin sau khác người gửi / cách > 2 phút / là thẻ Card hoặc SYSTEM)
 */
const shouldShowTime = (idx) => {
  const current = chatStore.messages[idx]
  if (!isMergeable(current)) return true
  if (idx >= chatStore.messages.length - 1) return true
  const next = chatStore.messages[idx + 1]
  if (!isMergeable(next)) return true
  return !isSameSender(current, next) || !isCloseInTime(current, next)
}

/**
 * Xác định vị trí của tin nhắn trong cụm tin nhắn liên tiếp (Messenger style bo góc):
 * - 'single': Tin nhắn đơn lẻ (hoặc tin nhắn dạng Card / SYSTEM không merge)
 * - 'first': Tin nhắn đầu tiên của cụm
 * - 'middle': Tin nhắn ở giữa cụm
 * - 'last': Tin nhắn cuối cùng của cụm (bao gồm khi tin kế tiếp là SYSTEM hoặc Card)
 */
const getGroupPosition = (idx) => {
  const current = chatStore.messages[idx]
  if (!isMergeable(current)) return 'single'

  const prev = idx > 0 ? chatStore.messages[idx - 1] : null
  const next = idx < chatStore.messages.length - 1 ? chatStore.messages[idx + 1] : null

  const hasPrev = isMergeable(prev) && isSameSender(current, prev) && isCloseInTime(current, prev)
  const hasNext = isMergeable(next) && isSameSender(current, next) && isCloseInTime(current, next)

  if (hasPrev && hasNext) return 'middle'
  if (hasPrev && !hasNext) return 'last'
  if (!hasPrev && hasNext) return 'first'
  return 'single'
}



onMounted(async () => {
  // 1. Tải thông tin tài khoản nếu chưa có
  if (!authStore.user) {
    await authStore.initUser()
  }
  // 2. Kích hoạt kết nối WebSocket STOMP
  const token = authStore.accessToken || localStorage.getItem('accessToken')
  if (token) {
    initGlobalWebSocket(token)
  }
  // 3. Tải tin nhắn, tin ghim, lịch hẹn và toàn bộ file phương tiện của phòng đang mở
  if (chatStore.activeRoomId) {
    if (chatStore.messages.length === 0) {
      await chatStore.fetchMessages(chatStore.activeRoomId)
    }
    await Promise.all([
      chatStore.fetchPinnedMessages(chatStore.activeRoomId),
      chatStore.fetchRoomMedia(chatStore.activeRoomId),
      chatStore.fetchRoomReminders(chatStore.activeRoomId),
    ])
    await chatStore.markRoomAsRead(chatStore.activeRoomId)
  }
  await scrollOrJumpToLatestActivity()

  // 4. Kích hoạt timer cập nhật trạng thái offline mỗi 30s
  tickerTimer = setInterval(() => {
    ticker.value = Date.now()
  }, 30000)

  // 5. Lắng nghe phím tắt Ctrl+F / Cmd+F mở thanh tìm kiếm
  window.addEventListener('keydown', handleGlobalKeyDown)
})

onUnmounted(() => {
  if (tickerTimer) clearInterval(tickerTimer)
  window.removeEventListener('keydown', handleGlobalKeyDown)
})

// Đóng và xóa từ khóa tìm kiếm khi chuyển phòng chat, tải lại thành viên nếu đang mở Info
watch(
  () => chatStore.activeRoomId,
  (newId) => {
    handleCloseSearch()
    if (isInfoSidebarOpen.value && newId) {
      fetchCurrentRoomMembers()
    }
  },
)

/**
 * Xử lý sự kiện Ghim hoặc Bỏ ghim tin nhắn bất kỳ từ danh sách tin nhắn
 * @param {string} messageId UUID của tin nhắn
 */
const handleTogglePin = async (messageId) => {
  if (!messageId) return
  const msg = chatStore.messages.find((m) => String(m.messageId) === String(messageId))
  if (msg?.isPinned) {
    await chatStore.unpinMessage(messageId)
  } else {
    await chatStore.pinMessage(messageId)
  }
}

/**
 * Định dạng nội dung tin nhắn hệ thống (SYSTEM):
 * Nếu là do chính mình thực hiện -> hiển thị "Bạn đã ghim/bỏ ghim một tin nhắn"
 * Nếu là thành viên khác -> hiển thị đúng tên thành viên đó
 */
const formatSystemMessageContent = (msg) => {
  if (!msg) return ''
  const myUsername = authStore.user?.username
  const myUserId = authStore.user?.userId
  const content = msg.content || ''

  const isMe =
    (myUserId && msg.sender?.userId && String(msg.sender.userId) === String(myUserId)) ||
    (myUsername && msg.sender?.username === myUsername) ||
    msg.actorUsername === 'Bạn' ||
    (myUsername && typeof content === 'string' && content.startsWith(myUsername))

  if (isMe) {
    if (content.includes('bỏ') || content.includes('gỡ')) {
      return 'Bạn đã bỏ ghim một tin nhắn'
    }
    if (content.includes('ghim')) {
      return 'Bạn đã ghim một tin nhắn'
    }
    if (content.includes('đã đổi tên nhóm thành')) {
      return content.replace(new RegExp('^' + (myUsername || '')), 'Bạn')
    }
    if (content.includes('đã thay đổi ảnh đại diện nhóm')) {
      return 'Bạn đã thay đổi ảnh đại diện nhóm'
    }
  }
  return content
}

/**
 * Xử lý chỉnh sửa nội dung tin nhắn
 * @param {object} param0 { messageId, content }
 */
const handleEditMessage = async ({ messageId, content }) => {
  if (!messageId || !content) return
  await chatStore.editMessage(messageId, content)
}

/**
 * Xử lý thu hồi tin nhắn
 * @param {string} messageId UUID của tin nhắn
 */
const handleRevokeMessage = async (messageId) => {
  if (!messageId) return
  if (confirm('Bạn có chắc chắn muốn thu hồi tin nhắn này không?')) {
    await chatStore.revokeMessage(messageId)
  }
}

/**
 * Xử lý mở popup xem lịch sử chỉnh sửa tin nhắn
 * @param {object} param0 { messageId, content, sender }
 */
const handleViewHistory = async ({ messageId, content, sender }) => {
  if (!messageId) return
  historyCurrentContent.value = content || ''
  historySenderName.value = sender || 'Thành viên'
  selectedHistoryList.value = await chatStore.fetchMessageHistory(messageId)
  showHistoryModal.value = true
}

/**
 * Xử lý khi người dùng chọn Trả lời một tin nhắn
 * @param {object} message Đối tượng tin nhắn cần trả lời
 */
const handleReplyMessage = (message) => {
  chatStore.setReplyingMessage(message)
}

const highlightedMessageId = ref(null) // ID tin nhắn đang được nhấp nháy sáng
let highlightTimer = null

/**
 * Căn giữa phần tử tin nhắn trong vùng cuộn .chat-body
 */
const centerElementInChatBody = (el, smooth = false) => {
  const container = chatBodyRef.value
  if (!container || !el) return
  const containerRect = container.getBoundingClientRect()
  const elRect = el.getBoundingClientRect()
  const currentScrollTop = container.scrollTop
  const offsetWithinContainer = elRect.top - containerRect.top + currentScrollTop
  const targetScrollTop = Math.max(
    0,
    offsetWithinContainer - container.clientHeight / 2 + elRect.height / 2,
  )

  if (smooth) {
    container.scrollTo({
      top: targetScrollTop,
      behavior: 'smooth',
    })
  } else {
    container.scrollTop = targetScrollTop
  }
}

/**
 * Cuộn mượt tới phần tử tin nhắn trong DOM và tạo hiệu ứng tô sáng hàng + nhún bong bóng (2.6 giây)
 */
const scrollToElement = (messageId, { instantCenter = false } = {}) => {
  const el = document.getElementById(`msg-${messageId}`)
  if (el) {
    if (instantCenter) {
      centerElementInChatBody(el, false)
      requestAnimationFrame(() => {
        const currentEl = document.getElementById(`msg-${messageId}`)
        if (currentEl) centerElementInChatBody(currentEl, false)
      })
      if (jumpRecenterTimer) clearTimeout(jumpRecenterTimer)
      jumpRecenterTimer = setTimeout(() => {
        const currentEl = document.getElementById(`msg-${messageId}`)
        if (currentEl) centerElementInChatBody(currentEl, false)
      }, 70)
    } else {
      centerElementInChatBody(el, true)
    }

    highlightedMessageId.value = String(messageId)

    // Reset animation nếu bấm liên tiếp vào cùng 1 tin nhắn
    el.classList.remove('highlight-msg')
    void el.offsetWidth
    el.classList.add('highlight-msg')

    if (highlightTimer) clearTimeout(highlightTimer)
    highlightTimer = setTimeout(() => {
      highlightedMessageId.value = null
      const currentEl = document.getElementById(`msg-${messageId}`)
      if (currentEl) currentEl.classList.remove('highlight-msg')
    }, 1500)
  }
}

/**
 * Nhảy tới tin nhắn ghim / reply / lịch hẹn:
 * 1. Nếu đã có trong danh sách hiển thị -> cuộn mượt tới vị trí chính giữa và nháy sáng
 * 2. Nếu chưa có (>20 tin đã tải) -> Gọi API lấy Cửa sổ ngữ cảnh 20 tin bao quanh tin đích,
 *    khóa sự kiện cuộn tự động để không bị khựng, căn giữa ngay lập tức và nháy sáng
 */
const handleScrollToMessage = async (pinnedId) => {
  if (!pinnedId) return

  // Hủy mọi timer cuộn xuống đáy đang chờ để tránh kéo ngược khung chat
  if (scrollToBottomTimer1) clearTimeout(scrollToBottomTimer1)
  if (scrollToBottomTimer2) clearTimeout(scrollToBottomTimer2)
  if (jumpLockTimer) clearTimeout(jumpLockTimer)
  if (jumpRecenterTimer) clearTimeout(jumpRecenterTimer)

  isJumpingToMessage.value = true
  const exists = chatStore.messages.some((m) => String(m.messageId) === String(pinnedId))

  if (exists) {
    await nextTick()
    scrollToElement(pinnedId, { instantCenter: false })
    jumpLockTimer = setTimeout(() => {
      isJumpingToMessage.value = false
    }, 550)
  } else {
    isJumpingContextBadge.value = true
    try {
      const ok = await chatStore.jumpToPinnedMessageContext(pinnedId)
      if (ok) {
        await nextTick()
        scrollToElement(pinnedId, { instantCenter: true })
        showScrollBottomBtn.value = true
      }
    } finally {
      isJumpingContextBadge.value = false
      jumpLockTimer = setTimeout(() => {
        isJumpingToMessage.value = false
      }, 450)
    }
  }
}

/**
 * Tự động cuộn khung chat xuống đáy khi vào phòng hoặc chuyển phòng
 * để luôn hiển thị thông tin / tin nhắn / thông báo sự kiện ghim mới nhất
 */
const scrollOrJumpToLatestActivity = async () => {
  await scrollToBottom()
}

const isLoadingNewer = ref(false)

/**
 * Xử lý cuộn tin nhắn hai chiều (Bi-directional Infinite Scroll):
 * - Cuộn sát đỉnh (<= 60px): Tải thêm tin cũ hơn (Prepend)
 * - Cuộn sát đáy (distanceFromBottom <= 60px) & còn tin mới hơn (hasMoreNewer): Tải thêm tin mới hơn (Append)
 * - Hiện nút mũi tên cuộn xuống khi đang xem đoạn tin ghim (isDetachedFromBottom / hasMoreNewer) HOẶC cách đáy > 150px
 */
const handleChatBodyScroll = async () => {
  const el = chatBodyRef.value
  if (!el) return

  const { scrollTop, scrollHeight, clientHeight } = el
  const distanceFromBottom = scrollHeight - (scrollTop + clientHeight)

  // 1. Chỉ cập nhật nút cuộn nếu không đang trong quá trình cuộn mượt về đáy (tránh nhấp nháy nút)
  if (!isSmoothScrollingToBottom.value) {
    showScrollBottomBtn.value = Boolean(
      chatStore.hasMoreNewer || chatStore.isDetachedFromBottom || distanceFromBottom > 150,
    )
  }

  // 2. Cuộn sát đỉnh (còn cách mép trên <= 120px) -> Tải tin cũ hơn (không chạy khi đang lướt về đáy hoặc đang nhảy tới tin ghim)
  if (
    !isSmoothScrollingToBottom.value &&
    !isJumpingToMessage.value &&
    !chatStore.isJumpingToContext &&
    scrollTop <= 120 &&
    chatStore.hasMoreOlder &&
    !chatStore.isLoadingOlder &&
    !chatStore.loadingMessages &&
    !isLoadingOlder.value
  ) {
    await loadMoreOlder()
  }

  // 3. Cuộn sát đáy (còn cách mép dưới <= 60px) VÀ đang còn tin mới hơn -> Tải tiếp tin mới
  if (
    !isSmoothScrollingToBottom.value &&
    !isJumpingToMessage.value &&
    !chatStore.isJumpingToContext &&
    distanceFromBottom <= 60 &&
    chatStore.hasMoreNewer &&
    !chatStore.isLoadingNewer &&
    !chatStore.loadingMessages &&
    !isLoadingNewer.value
  ) {
    await loadMoreNewer()
  }
}

/**
 * Tải thêm tin nhắn cũ hơn và giữ nguyên vị trí cuộn không bị giật
 */
const loadMoreOlder = async () => {
  const el = chatBodyRef.value
  if (!el || chatStore.isLoadingOlder || !chatStore.hasMoreOlder) return

  const prevScrollHeight = el.scrollHeight // chieu cao khun chat truoc khi nap tin cu
  const prevScrollTop = el.scrollTop // Vi tri cuon hien tai cua nguoi dung
  isLoadingOlder.value = true

  const success = await chatStore.loadOlderMessages()
  if (success) {
    await nextTick()
    // Giữ nguyên vị trí cuộn hiện tại của người dùng sau khi prepend dữ liệu
    const scrollDiff = el.scrollHeight - prevScrollHeight
    el.scrollTop = prevScrollTop + scrollDiff
    requestAnimationFrame(() => {
      if (el) {
        el.scrollTop = prevScrollTop + (el.scrollHeight - prevScrollHeight)
      }
    })
  }

  setTimeout(() => {
    isLoadingOlder.value = false
  }, 250)
}

/**
 * Tải thêm tin nhắn mới hơn khi cuộn xuống đáy
 */
const loadMoreNewer = async () => {
  const el = chatBodyRef.value
  if (!el || chatStore.isLoadingNewer || !chatStore.hasMoreNewer) return

  isLoadingNewer.value = true
  await chatStore.loadNewerMessages()
  await nextTick()

  setTimeout(() => {
    isLoadingNewer.value = false
  }, 150)
}

/**
 * Bấm nút cuộn về mới nhất:
 * - Nếu đang ở khúc giữa lịch sử (isDetachedFromBottom hoặc hasMoreNewer) -> Tải lại 20 tin nhắn mới nhất
 * - Thực hiện cuộn lướt mượt mà (Smooth scroll), không bị nhảy bùm/đột ngột
 */
const scrollToLatestMessages = async () => {
  const el = chatBodyRef.value
  if (!el) return

  // 1. Kích hoạt cờ cuộn mượt và ẩn nút ngay để giao diện phản hồi tức thì
  isSmoothScrollingToBottom.value = true
  showScrollBottomBtn.value = false

  if (scrollToBottomTimer1) clearTimeout(scrollToBottomTimer1)
  if (scrollToBottomTimer2) clearTimeout(scrollToBottomTimer2)

  // 2. Nếu đang xem đoạn ngắt quãng (tin ghim từ xa) -> Tải lại ngữ cảnh mới nhất
  if (chatStore.isDetachedFromBottom || chatStore.hasMoreNewer) {
    chatStore.isDetachedFromBottom = false
    chatStore.hasMoreNewer = false
    await chatStore.fetchMessages(chatStore.activeRoomId)
    await nextTick()
  }

  // 3. Nếu khoảng cách cuộn quá xa (> 1800px), nhảy nhanh một nhịp lại gần đáy (cách đáy ~700px)
  // rồi cuộn lướt mượt mà nốt đoạn cuối để hiệu ứng cực kỳ êm ái, thanh thoát mà không phải chờ lâu
  const currentDistance = el.scrollHeight - (el.scrollTop + el.clientHeight)
  if (currentDistance > 1800) {
    el.scrollTop = el.scrollHeight - el.clientHeight - 700
  }

  // 4. Lướt mượt mà xuống chạm đáy
  el.scrollTo({
    top: el.scrollHeight,
    behavior: 'smooth',
  })

  // 5. Kết thúc hiệu ứng cuộn mượt sau 450ms và kiểm tra lại vị trí
  setTimeout(() => {
    isSmoothScrollingToBottom.value = false
    if (el) {
      const dist = el.scrollHeight - (el.scrollTop + el.clientHeight)
      showScrollBottomBtn.value = dist > 150
      // Đảm bảo chạm sát đáy 100% nếu có ảnh hoặc badge hệ thống nở ra
      if (dist > 0 && dist <= 150) {
        el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
      }
    }
  }, 450)
}

/**
 * Khi người dùng nhấn Enter hoặc click nút gửi tin nhắn:
 * - Nếu đang ở khúc giữa lịch sử (do lướt lên, tìm kiếm, bấm tin ghim) -> nạp lại đoạn tin nhắn mới nhất.
 * - Cuộn ngay lập tức xuống đáy cùng của khung chat.
 */
const handleMessageSent = async () => {
  if (chatStore.isDetachedFromBottom || chatStore.hasMoreNewer) {
    chatStore.isDetachedFromBottom = false
    chatStore.hasMoreNewer = false
    await chatStore.fetchMessages(chatStore.activeRoomId)
  }
  await nextTick()
  scrollToBottom()
  showScrollBottomBtn.value = false

  // Dự phòng khi tin nhắn được gắn vào DOM hoặc có độ trễ kết xuất
  setTimeout(() => {
    scrollToBottom()
  }, 80)
  setTimeout(() => {
    scrollToBottom()
  }, 250)
}

// Tự động cuộn xuống đáy khi có tin nhắn mới được thêm vào (bỏ qua nếu đang tải tin cũ, tải tin mới hoặc đang xem khúc giữa)
watch(
  () => chatStore.messages,
  async (newMessages) => {
    if (!newMessages || newMessages.length === 0) {
      previousLastMessageId = null
      return
    }

    const currentLastMsg = newMessages[newMessages.length - 1]
    const currentLastMsgId = currentLastMsg?.messageId ? String(currentLastMsg.messageId) : null

    // 1. Lần đầu nạp danh sách tin nhắn phòng (từ 0 -> 20 tin dau tien) -> cuon xuong đáy
    if (!previousLastMessageId) {
      previousLastMessageId = currentLastMsgId
      await scrollOrJumpToLatestActivity()
      return
    }

    // 2. Kiểm tra xem tin nhắn ở đáy mảng có thực sự thay đổi không (có tin nhắn MỚI được thêm vào cuối)
    const isNewMessageAtBottom = currentLastMsgId && currentLastMsgId !== previousLastMessageId
    previousLastMessageId = currentLastMsgId

    // Nếu đang nhảy tới ngữ cảnh tin nhắn ghim/reply ở xa hoặc đang xem khúc giữa lịch sử -> TUYỆT ĐỐI KHÔNG tự cuộn xuống đáy!
    if (
      isJumpingToMessage.value ||
      chatStore.isJumpingToContext ||
      chatStore.isDetachedFromBottom ||
      chatStore.isViewingOlderSegment ||
      isLoadingNewer.value ||
      chatStore.isLoadingNewer
    ) {
      return
    }

    // Nếu KHÔNG có tin nhắn mới ở đáy (tức là tin cũ được nạp thêm ở đỉnh hoặc cập nhật tin nhắn) HOẶC đang tải tin cũ -> TUYỆT ĐỐI KHÔNG cuộn xuống đáy!
    if (!isNewMessageAtBottom || isLoadingOlder.value || chatStore.isLoadingOlder) {
      return
    }

    // 3. Nếu tin nhắn mới nhất là do chính mình gửi đi -> LUÔN LUÔN cuộn xuống sát đáy
    if (currentLastMsg && isMyMessage(currentLastMsg)) {
      scrollToBottom()
      showScrollBottomBtn.value = false
      return
    }

    // 4. Nếu là tin nhắn mới do người khác gửi đến:
    const el = chatBodyRef.value
    const isUserNearBottom = el ? el.scrollHeight - el.scrollTop - el.clientHeight <= 150 : true

    //neu dang o đáy (cách <=150px) thì cuộn xuống
    if (
      isUserNearBottom &&
      !isLoadingNewer.value &&
      !chatStore.isLoadingNewer &&
      !chatStore.isDetachedFromBottom &&
      !chatStore.hasMoreNewer
    ) {
      scrollToBottom()
    } else {
      showScrollBottomBtn.value = true
    }
  },
  { deep: false },
)

/**
 * Chuẩn hóa URL hiển thị ảnh (Avatar hoặc hình ảnh đính kèm)
 */
const getImageUrl = (path) => {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('blob:')) {
    return path;
  }
  return resolveFileUrl(path);
};

// Tự động tải tin ghim và điều hướng phù hợp khi chuyển đổi phòng chat
watch(
  () => chatStore.activeRoomId,
  async (newRoomId) => {
    previousLastMessageId = null
    showScrollBottomBtn.value = false
    if (newRoomId) {
      await Promise.all([
        chatStore.fetchPinnedMessages(newRoomId),
        chatStore.fetchRoomMedia(newRoomId),
        chatStore.fetchRoomReminders(newRoomId),
      ])
    }
    await scrollOrJumpToLatestActivity()
  },
)

// Không tự động cuộn khi người khác đang soạn tin nhắn để giữ nguyên vị trí đọc
</script>

<template>
  <div class="app-container">
    <NavigationSidebar></NavigationSidebar>
    <div class="ChatListSidebar">
      <ChatListSidebar></ChatListSidebar>
    </div>
    <div class="chat-view">
      <div class="chat-main-area">
        <ChatHeader
          :RoomName="chatStore.activeRoom?.roomName || 'Cuộc trò chuyện'"
          :avatarUrl="chatStore.activeRoom?.roomAvatarURL || chatStore.activeRoom?.avatarUrl"
          :StatusRoom="formatPresenceStatus(chatStore.activeRoom)"
          :isOnline="chatStore.activeRoom?.roomType !== 'GROUP' && Boolean(chatStore.activeRoom?.isOnline)"
          :isSearching="showSearchBar"
          :isInfoOpen="isInfoSidebarOpen"
          @toggleSearch="handleToggleSearch"
          @toggleInfo="handleToggleInfo"
        />
        <PinnedMessage @scrollToMessage="handleScrollToMessage" />

        <!-- Badge nổi thông báo khi đang tải ngữ cảnh tin nhắn ở xa (>20 tin) -->
        <Transition name="fade">
          <div
            v-if="isJumpingContextBadge || chatStore.isJumpingToContext"
            class="jumping-context-badge"
          >
            <div class="spinner-small jumping-spinner"></div>
            <span>Đang di chuyển tới tin nhắn...</span>
          </div>
        </Transition>

        <div class="chat-body" ref="chatBodyRef" @scroll="handleChatBodyScroll">
          <!-- 0. Thanh tải thêm tin nhắn cũ (Cursor-based Pagination limit: 20) -->
          <div v-if="chatStore.hasMoreOlder" class="load-more-container">
            <button
              v-if="!chatStore.isLoadingOlder"
              type="button"
              class="load-more-btn"
              @click="loadMoreOlder"
            >
              <font-awesome-icon icon="clock-rotate-left" class="load-more-icon" />
              <span>Tải thêm tin nhắn cũ hơn</span>
            </button>
            <div v-else class="loading-more-indicator">
              <div class="spinner-small"></div>
              <span>Đang tải tin nhắn cũ...</span>
            </div>
          </div>

          <div
            v-if="chatStore.loadingMessages"
            style="
              text-align: center;
              color: #a0aec0;
              margin-top: 20px;
              font-family: 'Quicksand', sans-serif;
            "
          >
            Đang tải tin nhắn...
          </div>
          <div
            v-else-if="chatStore.messages.length === 0"
            style="
              text-align: center;
              color: #a0aec0;
              margin-top: 40px;
              font-family: 'Quicksand', sans-serif;
            "
          >
            Chưa có tin nhắn nào. Hãy gửi lời chào đầu tiên! 👋
          </div>
          <template v-for="(msg, idx) in chatStore.messages" :key="msg.messageId">
            <!-- Mốc thời gian lớn giữa các đoạn hội thoại (>= 5 phút hoặc sang ngày mới) -->
            <div v-if="shouldShowTimeDivider(idx)" class="chat-time-divider-row">
              <span class="chat-time-divider-pill">{{ formatTimeDivider(msg.createdAt) }}</span>
            </div>

            <!-- 1. Sự kiện ghim tin nhắn hoặc thông báo hệ thống trong phòng chat (ẩn legacy time log nếu có) -->
            <div
              v-if="(msg.isEvent || msg.messageType === 'SYSTEM') && !isLegacyTimeLogMessage(msg)"
              :id="msg.messageId ? 'msg-' + msg.messageId : null"
              class="system-message-row chat-event-row"
            >
              <div class="system-message-badge chat-event-pill">
                <font-awesome-icon icon="thumbtack" class="event-pin-icon" />
                <span>{{ formatSystemMessageContent(msg) }}</span>
              </div>
            </div>

            <!-- 2. Dòng tin nhắn thông thường -->
            <MessageList
              v-else-if="!isLegacyTimeLogMessage(msg)"
              :messageType="msg.messageType"
              :messageId="msg.messageId"
              :isPinned="!!msg.isPinned"
              :isRevoked="!!msg.isRevoked"
              :isEdited="!!msg.isEdited"
              :repliedMessage="msg.repliedMessage"
              :rawMessage="msg"
              :reactions="msg.reactions || []"
              :isConsecutive="isConsecutiveMessage(idx)"
              :groupPosition="getGroupPosition(idx)"
              :showTime="shouldShowTime(idx)"
              :Content_Message="msg.content"
              :searchKeyword="currentSearchKeyword"
              :Time="formatTime(msg.createdAt)"
              :Sender="isMyMessage(msg) ? '' : msg.sender?.username || 'Thành viên'"
              :Avatar="
                isMyMessage(msg) ? '' : getImageUrl(msg.sender?.avatar || msg.sender?.avatarUrl)
              "
              :variant="isMyMessage(msg) ? 'SendTo' : 'default'"
              :canManageRoom="canManageCurrentRoom"
              @togglePin="handleTogglePin"
              @editMessage="handleEditMessage"
              @revokeMessage="handleRevokeMessage"
              @viewHistory="handleViewHistory"
              @replyMessage="handleReplyMessage"
              @scrollToMessage="handleScrollToMessage"
              @preview-media="handleOpenMediaPreview"
            />
          </template>

          <!-- Chỉ báo đang tải tin nhắn mới hơn khi cuộn xuống dưới (Bi-directional scroll) -->
          <div v-if="chatStore.isLoadingNewer" class="loading-newer-container">
            <div class="spinner-small"></div>
            <span>Đang tải tin nhắn mới hơn...</span>
          </div>

          <!-- Trạng thái đang soạn tin nhắn (Typing indicator) -->
          <div v-if="chatStore.activeTypingText" class="typing-indicator">
            <span class="typing-text">{{ chatStore.activeTypingText }}</span>
            <span class="dots">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </span>
          </div>
        </div>

        <!-- Nút mũi tên nổi cuộn tới tin nhắn mới nhất khi vượt lên trên -->
        <transition name="fade">
          <button
            v-if="showScrollBottomBtn"
            type="button"
            class="scroll-bottom-btn"
            title="Cuộn tới tin nhắn mới nhất"
            @click="scrollToLatestMessages"
          >
            <font-awesome-icon icon="chevron-down" />
          </button>
        </transition>

        <ChatInput
          @messageSent="handleMessageSent"
          @openCreateReminder="handleOpenCreateReminder"
        ></ChatInput>
      </div>

      <!-- Sidebar tìm kiếm tin nhắn bên phải (Search Drawer phong cách Telegram / Discord / Zalo) -->
      <Transition name="search-drawer">
        <MessageSearchSidebar
          v-if="showSearchBar"
          :room-id="chatStore.activeRoomId"
          @close="handleCloseSearch"
          @jump-to-message="handleScrollToMessage"
          @update-keyword="handleUpdateSearchKeyword"
        />
      </Transition>

      <!-- Sidebar Thông tin phòng chat bên phải (Chat Info Sidebar phong cách Zalo PC / Telegram) -->
      <Transition name="search-drawer">
        <ChatInfoSidebar
          v-if="isInfoSidebarOpen"
          :room="chatStore.activeRoom || {}"
          :members="currentRoomMembers"
          :media-data="roomMediaData"
          @close="isInfoSidebarOpen = false"
          @toggle-notification="handleMuteNotification"
          @add-member="showAddMemberModal = true"
          @create-reminder="handleOpenCreateReminder"
          @preview-media="handleOpenMediaPreview"
        />
      </Transition>
    </div>
  </div>

  <!-- Popup Modal tạo lịch hẹn / nhắc nhở mới -->
  <CreateReminderModal
    :is-open="showCreateReminderModal"
    :room-id="String(chatStore.activeRoomId || '')"
    :room-type="chatStore.activeRoom?.roomType || 'DIRECT'"
    :room-name="chatStore.activeRoom?.roomName || ''"
    :members="currentRoomMembers"
    :current-user-id="authStore.user?.userId"
    @close="showCreateReminderModal = false"
    @created="handleMessageSent"
  />

  <!-- Popup Modal thêm thành viên vào nhóm chat -->
  <AddMemberModal
    :show="showAddMemberModal"
    :roomId="chatStore.activeRoomId || ''"
    :existingMembers="currentRoomMembers"
    @close="showAddMemberModal = false"
    @member-added="handleMemberAdded"
  />

  <!-- Popup Modal lịch sử các lần chỉnh sửa tin nhắn -->
  <MessageHistoryModal
    :show="showHistoryModal"
    :historyList="selectedHistoryList"
    :currentContent="historyCurrentContent"
    :senderName="historySenderName"
    @close="showHistoryModal = false"
  />

  <!-- Trình xem ảnh / video toàn màn hình (Media Lightbox) -->
  <MediaLightboxModal
    :show="showMediaLightbox"
    :mediaList="lightboxMediaList"
    :initialIndex="lightboxInitialIndex"
    @close="showMediaLightbox = false"
  />
</template>
<style scoped>
.ChatListSidebar {
  height: 100vh;
  max-height: 100vh;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-view {
  position: relative;
  display: flex;
  flex-direction: row;
  height: 100vh;
  max-height: 100vh;
  min-height: 0;
  overflow: hidden;
  background-color: #f8f9fd;
  border-right: 1px solid #e6eaf2;
  border-left: 1px solid #e6eaf2;
}

.chat-main-area {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.chat-body {
  flex: 1 1 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px;
}

.chat-body::-webkit-scrollbar {
  width: 6px;
}

.chat-body::-webkit-scrollbar-track {
  background: transparent;
}

.chat-body::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 4px;
}

.chat-body::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}
.app-container {
  display: grid;
  grid-template-columns: 80px 370px 1fr;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
}
.input_text {
  flex-shrink: 0;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: white;
}

.chat-time-divider-row {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 16px 0 10px;
  width: 100%;
  user-select: none;
}

.chat-time-divider-pill {
  display: inline-block;
  padding: 4px 14px;
  background-color: rgba(226, 232, 240, 0.7);
  color: #64748b;
  border-radius: 12px;
  font-size: 11.5px;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.chat-event-row {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 12px 0 8px;
  width: 100%;
}

.chat-event-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 14px;
  /* background-color: #f1f5f9; */
  color: #64748b;
  /* border: 1px solid #e2e8f0; */
  /* border-radius: 20px; */
  font-size: 12px;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  /* box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); */
}

.event-pin-icon {
  color: #4b7bec;
  font-size: 11px;
}

.floating-typing-bar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 14px;
  margin: 0 0 4px 24px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  color: #4b7bec;
  font-weight: 600;
  width: fit-content;
  align-self: flex-start;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  margin: 8px 0 4px 6px;
  background-color: #ffffff;
  border: 1px solid #e6eaf2;
  border-radius: 14px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  color: #718096;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.typing-text {
  font-weight: 500;
}

.dots {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.dot {
  width: 5px;
  height: 5px;
  background-color: #4b7bec;
  border-radius: 50%;
  display: inline-block;
  animation: typingBounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typingBounce {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1.1);
    opacity: 1;
  }
}

.chat-sidebar-wrapper {
  height: 100%;
  overflow: hidden;
}

/* =========================================================================
   RIGHT DRAWER CONTAINER (PUSH DRAWER - CHUẨN ZALO PC)
   - Trượt đẩy mượt mà từ 0 -> 340px, khung chat co lại nhịp nhàng
   - Chuyển đổi nội dung giữa Tìm kiếm và Thông tin ngay tại chỗ (Cross-fade)
   ========================================================================= */
.right-drawer-container {
  display: flex;
  height: 100%;
  width: 0;
  min-width: 0;
  max-width: 340px;
  flex-shrink: 0;
  background-color: #ffffff;
  border-left: 1px solid transparent;
  overflow: hidden;
  transition:
    width 0.28s cubic-bezier(0.2, 0, 0, 1),
    border-color 0.2s ease;
  will-change: width;
  z-index: 25;
}

.right-drawer-container--open {
  width: 340px;
  min-width: 340px;
  border-left-color: #e2e8f0;
}

/* Cross-fade mượt mà khi hoán đổi giữa Tìm kiếm và Thông tin hội thoại */
.sidebar-fade-enter-active,
.sidebar-fade-leave-active {
  transition:
    opacity 0.18s cubic-bezier(0.2, 0, 0, 1),
    transform 0.18s cubic-bezier(0.2, 0, 0, 1);
  width: 340px;
  flex-shrink: 0;
}

.sidebar-fade-enter-from {
  opacity: 0;
  transform: translateX(14px);
}

.sidebar-fade-leave-to {
  opacity: 0;
  transform: translateX(-14px);
}

.chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 24px;
}

.load-more-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px 0 16px;
  width: 100%;
}

.load-more-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 16px;
  background-color: #ffffff;
  border: 1px solid #dbeafe;
  border-radius: 20px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #3b82f6;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(59, 130, 246, 0.08);
  transition: all 0.2s ease;
}

.load-more-btn:hover {
  background-color: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.15);
}

.load-more-icon {
  font-size: 12px;
}

.loading-more-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-family: 'Quicksand', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.loading-newer-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  width: 100%;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  font-family: 'Quicksand', sans-serif;
}

.spinner-small {
  width: 12px;
  height: 12px;
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

.chat-input-wrapper {
  flex-shrink: 0;
  background-color: #ffffff;
  border-top: 1px solid #e6eaf2;
  padding: 12px 10px;
}

.scroll-bottom-btn {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 92px;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background-color: #ffffff;
  color: #3b82f6;
  border: 1px solid #dbeafe;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 15px;
  z-index: 30;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.scroll-bottom-btn:hover {
  background-color: #3b82f6;
  color: #ffffff;
  border-color: #3b82f6;
  transform: translateX(-50%) translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.38);
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.22s ease,
    transform 0.22s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(10px);
}

.jumping-context-badge {
  position: absolute;
  top: 118px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 25;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 16px;
  border-radius: 999px;
  background: rgba(30, 64, 175, 0.92);
  color: #ffffff;
  font-family: 'Quicksand', sans-serif;
  font-size: 12.5px;
  font-weight: 700;
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.32);
  pointer-events: none;
}

.jumping-spinner {
  border-color: rgba(255, 255, 255, 0.35);
  border-top-color: #ffffff;
}
</style>
