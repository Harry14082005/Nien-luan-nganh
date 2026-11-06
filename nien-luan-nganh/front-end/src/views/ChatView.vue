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
import { initGlobalWebSocket } from '@/services/websocket'
import { getRoomMembers } from '@/api/room.api'
import { resolveFileUrl } from '@/api/storage.api';

const chatStore = useChatStore()
const authStore = useAuthStore()
const chatBodyRef = ref(null) // Tham chiếu tới vùng cuộn tin nhắn (.chat-body)
const isLoadingOlder = ref(false) // Cờ chặn scrollToBottom khi tải tin nhắn cũ
const showScrollBottomBtn = ref(false) // Trạng thái hiển thị nút cuộn xuống đáy
const ticker = ref(Date.now()) // Bộ đếm thời gian cập nhật lại thời gian offline mỗi 30 giây
let tickerTimer = null

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
  }
}

const handleMuteNotification = (isMuted) => {
  isNotificationMuted.value = isMuted
}

// Tổng hợp danh sách File phương tiện (Ảnh, Video, Tài liệu) từ tin nhắn trong phòng
const roomMediaData = computed(() => {
  const images = []
  const videos = []
  const files = []

  ;(chatStore.messages || []).forEach((m) => {
    if (m.isRevoked) return
    if (m.messageType === 'IMAGE') {
      images.push({
        id: m.messageId,
        url: m.content,
        name: 'Hình ảnh',
        type: 'IMAGE',
        createdAt: m.createdAt,
      })
    } else if (m.messageType === 'VIDEO') {
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

/**
 * Cuộn khung chat xuống đáy mượt mà sau khi DOM cập nhật
 */
const scrollToBottom = async () => {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
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
  // 3. Tải tin nhắn và tin ghim của phòng đang mở
  if (chatStore.activeRoomId) {
    if (chatStore.messages.length === 0) {
      await chatStore.fetchMessages(chatStore.activeRoomId)
    }
    await chatStore.fetchPinnedMessages(chatStore.activeRoomId)
    await chatStore.markRoomAsRead(chatStore.activeRoomId)
  }
  scrollToBottom()

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

const highlightedMessageId = ref(null) // ID tin nhắn đang được nhấp nháy sáng (2s)

/**
 * Cuộn mượt tới phần tử tin nhắn trong DOM và tạo hiệu ứng nhấp nháy sáng 2 giây
 */
const scrollToElement = (messageId) => {
  const el = document.getElementById(`msg-${messageId}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    highlightedMessageId.value = String(messageId)
    el.classList.add('highlight-msg')
    setTimeout(() => {
      highlightedMessageId.value = null
      el.classList.remove('highlight-msg')
    }, 2000)
  }
}

/**
 * Nhảy tới tin nhắn ghim:
 * 1. Nếu đã có trong danh sách hiển thị -> Cuộn mượt + Nhấp nháy 2s
 * 2. Nếu chưa có -> Gọi API lấy Cửa sổ ngữ cảnh 20 tin bao quanh tin ghim (10 trước + target + 10 sau)
 *    và thay thế danh sách hiển thị, bật cờ isDetachedFromBottom = true
 */
const handleScrollToMessage = async (pinnedId) => {
  if (!pinnedId) return
  const exists = chatStore.messages.some((m) => String(m.messageId) === String(pinnedId))

  if (exists) {
    scrollToElement(pinnedId)
  } else {
    const ok = await chatStore.jumpToPinnedMessageContext(pinnedId)
    if (ok) {
      await nextTick()
      scrollToElement(pinnedId)
      showScrollBottomBtn.value = true
    }
  }
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

  // Cách đáy hơn 150px HOẶC đang xem đoạn cắt khúc quanh tin ghim
  showScrollBottomBtn.value = Boolean(
    chatStore.hasMoreNewer || chatStore.isDetachedFromBottom || distanceFromBottom > 150,
  )

  // 1. Cuộn sát đỉnh (còn cách mép trên <= 120px) -> Tải tin cũ hơn
  if (
    scrollTop <= 120 &&
    chatStore.hasMoreOlder &&
    !chatStore.isLoadingOlder &&
    !chatStore.loadingMessages &&
    !isLoadingOlder.value
  ) {
    await loadMoreOlder()
  }

  // 2. Cuộn sát đáy (còn cách mép dưới <= 60px) VÀ đang còn tin mới hơn -> Tải tiếp tin mới
  if (
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

  const prevScrollHeight = el.scrollHeight
  const prevScrollTop = el.scrollTop
  isLoadingOlder.value = true

  const success = await chatStore.loadOlderMessages()
  if (success) {
    await nextTick()
    // Giữ nguyên vị trí cuộn hiện tại của người dùng sau khi prepend dữ liệu
    el.scrollTop = el.scrollHeight - prevScrollHeight + prevScrollTop
  }

  setTimeout(() => {
    isLoadingOlder.value = false
  }, 150)
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
 * Nếu đang ở khúc giữa lịch sử (isDetachedFromBottom hoặc hasMoreNewer) -> Tải lại 20 tin nhắn mới nhất rồi cuộn sát đáy
 */
const scrollToLatestMessages = async () => {
  if (chatStore.isDetachedFromBottom || chatStore.hasMoreNewer) {
    await chatStore.fetchMessages(chatStore.activeRoomId)
    await nextTick()
  }
  scrollToBottom()
  showScrollBottomBtn.value = false
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
  () => chatStore.messages.length,
  () => {
    // Nếu tin nhắn mới nhất là do chính mình gửi đi -> LUÔN LUÔN cuộn xuống sát đáy
    const lastMsg = chatStore.messages[chatStore.messages.length - 1]
    if (lastMsg && isMyMessage(lastMsg)) {
      scrollToBottom()
      showScrollBottomBtn.value = false
      return
    }

    if (
      !isLoadingOlder.value &&
      !isLoadingNewer.value &&
      !chatStore.isLoadingOlder &&
      !chatStore.isLoadingNewer &&
      !chatStore.isDetachedFromBottom &&
      !chatStore.hasMoreNewer
    ) {
      scrollToBottom()
    }
  },
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

// Tự động tải tin ghim và cuộn xuống đáy khi chuyển đổi phòng chat
watch(
  () => chatStore.activeRoomId,
  async (newRoomId) => {
    showScrollBottomBtn.value = false
    if (newRoomId) {
      await chatStore.fetchPinnedMessages(newRoomId)
    }
    scrollToBottom()
  },
)

// Tự động cuộn xuống đáy khi đối phương bắt đầu soạn tin nhắn
watch(
  () => chatStore.activeTypingText,
  (newVal) => {
    if (newVal) {
      scrollToBottom()
    }
  },
)
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
          :StatusRoom="formatPresenceStatus(chatStore.activeRoom)"
          :isSearching="showSearchBar"
          :isInfoOpen="isInfoSidebarOpen"
          @toggleSearch="handleToggleSearch"
          @toggleInfo="handleToggleInfo"
        />
        <PinnedMessage @scrollToMessage="handleScrollToMessage" />

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
              @togglePin="handleTogglePin"
              @editMessage="handleEditMessage"
              @revokeMessage="handleRevokeMessage"
              @viewHistory="handleViewHistory"
              @replyMessage="handleReplyMessage"
              @scrollToMessage="handleScrollToMessage"
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

        <ChatInput @messageSent="handleMessageSent"></ChatInput>
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
        />
      </Transition>
    </div>
  </div>

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

/* Animation mở/đóng mượt mà chuẩn Drawer Telegram / Slack Desktop */
.search-drawer-enter-active {
  transition:
    width 0.32s cubic-bezier(0.16, 1, 0.3, 1),
    opacity 0.25s ease;
  will-change: width, opacity;
  overflow: hidden !important;
}

.search-drawer-leave-active {
  transition:
    width 0.24s cubic-bezier(0.2, 0, 0, 1),
    opacity 0.2s ease;
  will-change: width, opacity;
  overflow: hidden !important;
}

.search-drawer-enter-from,
.search-drawer-leave-to {
  width: 0 !important;
  opacity: 0;
  border-left-color: transparent !important;
}

.search-drawer-enter-from :deep(.search-sidebar-inner),
.search-drawer-leave-to :deep(.search-sidebar-inner),
.search-drawer-enter-from :deep(.chat-info-sidebar-inner),
.search-drawer-leave-to :deep(.chat-info-sidebar-inner) {
  transform: translateX(40px);
  opacity: 0;
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
</style>
