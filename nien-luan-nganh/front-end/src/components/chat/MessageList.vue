<script setup>
/**
 * =========================================================================
 * Component: MessageList.vue
 * =========================================================================
 * - PHẦN 1: KHỞI TẠO PROPS, EMITS, STORE & TRẠNG THÁI MENU TƯƠNG TÁC
 * - PHẦN 2: DỊCH TIN NHẮN ĐA NGÔN NGỮ BẰNG AI (AI MESSAGE TRANSLATION)
 * - PHẦN 3: BIỂU CẢM TIN NHẮN (MESSAGE REACTIONS)
 * - PHẦN 4: FILE PHƯƠNG TIỆN, REMINDER (LỊCH HẸN) & HIGHLIGHT TỪ KHÓA
 * - PHẦN 5: CHỈNH SỬA INLINE, THU HỒI, LỊCH SỬ SỬA & TRẢ LỜI (REPLY)
 */
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useChatStore, getReplySnippet, highlightKeyword } from '@/stores/chat';
import { resolveFileUrl } from '@/api/storage.api'; 
import { translateMessage } from '@/api/message.api';
import ReminderMessageCard from './ReminderMessageCard.vue';

// =============================================================================
// ████  PHẦN 1: KHỞI TẠO PROPS, EMITS, STORE & TRẠNG THÁI MENU TƯƠNG TÁC  █████
// =============================================================================

const props = defineProps({
    // Mốc thời gian gửi tin nhắn (vd: 14:30)
    Time: {
        type: String,
        default: '12:35'
    },
    // Nội dung văn bản của tin nhắn
    Content_Message: {
        type: String,
        default: ''
    },
    // Tên người gửi (ẩn đi nếu là tin nhắn của chính mình)
    Sender: {
        type: String,
        default: 'Minh Anh'
    },
    // Kiểu hiển thị: 'default' (người khác) hoặc 'SendTo' (chính mình)
    variant: {
        type: String,
        default: 'default',
        validator: (v) => ['default', 'SendTo'].includes(v),
    },
    // Khóa chính UUID của tin nhắn
    messageId: {
        type: String,
        default: ''
    },
    // Trạng thái tin nhắn đã được ghim hay chưa
    isPinned: {
        type: Boolean,
        default: false
    },
    // Trạng thái tin nhắn đã bị thu hồi hay chưa
    isRevoked: {
        type: Boolean,
        default: false
    },
    // Danh sách cảm xúc đã thả trên tin nhắn
    reactions: {
        type: Array,
        default: () => []
    },
    // Tin nhắn liên tiếp từ cùng 1 người gửi (ẩn avatar, ẩn tên, thu hẹp khoảng cách)
    isConsecutive: {
        type: Boolean,
        default: false
    },
    // Hiển thị mốc thời gian (chỉ hiển thị ở tin nhắn cuối của cụm tin nhắn liên tiếp)
    showTime: {
        type: Boolean,
        default: true
    },
    // Vị trí của tin nhắn trong cụm tin nhắn liên tiếp ('single', 'first', 'middle', 'last')
    groupPosition: {
        type: String,
        default: 'single',
        validator: (v) => ['single', 'first', 'middle', 'last'].includes(v),
    },
    // Trạng thái tin nhắn đã từng được chỉnh sửa hay chưa
    isEdited: {
        type: Boolean,
        default: false
    },
    // Hiển thị Avatar
    Avatar: {
      type: String,
      default: ''
    },
    // Dữ liệu tin nhắn được phản hồi (nếu có)
    repliedMessage: {
        type: Object,
        default: null
    },
    // Đối tượng tin nhắn gốc đầy đủ để truyền khi bấm Reply
    rawMessage: {
        type: Object,
        default: null
    },
    // Từ khóa tìm kiếm đang kích hoạt để highlight
    searchKeyword: {
        type: String,
        default: ''
    },
    // Xử lý cái hiển thị ảnh file chỗ tin nhắn
    messageType: {
          type: String,
          default: 'TEXT', // TEXT, IMAGE, VIDEO, FILE, REMINDER
      },
      mediaUrl: {
          type: String,
          default: ''
      },
      fileName: {
          type: String,
          default: ''
      },
      fileSize: {
          type: String,
          default: ''
      },
      canManageRoom: {
          type: Boolean,
          default: false
      }
});

const emit = defineEmits([
    'togglePin',
    'editMessage',
    'revokeMessage',
    'viewHistory',
    'replyMessage',
    'scrollToMessage',
    'preview-media'
])

const authStore = useAuthStore();
const chatStore = useChatStore();

const showMenu = ref(false); // Trạng thái mở menu 3 chấm

// =============================================================================
// ██████████  PHẦN 2: DỊCH TIN NHẮN ĐA NGÔN NGỮ BẰNG AI (TRANSLATION)  ████████
// =============================================================================

// Lưu trữ trạng thái dịch của tin nhắn này
const translatedText = ref('');       // Nội dung đã dịch
const isTranslating = ref(false);     // Trạng thái đang gọi AI dịch (loading)
const showTranslation = ref(true);    // Trạng thái mở ra hay thu gọn bản dịch

// Danh sách ngôn ngữ thông dụng
const ALL_LANGUAGES = [
    { code: 'vi', label: 'Tiếng Việt' },
    { code: 'en', label: 'English' },
    { code: 'zh', label: '中文' },
    { code: 'ja', label: '日本語' },
    { code: 'ko', label: '한국어' },
    { code: 'fr', label: 'Français' },
    { code: 'de', label: 'Deutsch' },
    { code: 'es', label: 'Español' },
    { code: 'it', label: 'Italiano' },
    { code: 'pt', label: 'Português' },
    { code: 'ru', label: 'Русский' },
    { code: 'nl', label: 'Nederlands' },
    { code: 'pl', label: 'Polski' },
    { code: 'sv', label: 'Svenska' },
    { code: 'tr', label: 'Türkçe' },
    { code: 'uk', label: 'Українська' },
    { code: 'th', label: 'ไทย' },
    { code: 'id', label: 'Bahasa Indonesia' },
    { code: 'ms', label: 'Bahasa Melayu' },
    { code: 'fil', label: 'Filipino' },
    { code: 'lo', label: 'ພາສາລາວ' },
    { code: 'km', label: 'ភាសាខ្មែរ' },
    { code: 'my', label: 'မြန်မာ' },
    { code: 'hi', label: 'हिन्दी' },
    { code: 'ar', label: 'العربية' }
];

const langSearch = ref('');
const filteredLanguages = computed(() => {
    if (!langSearch.value.trim()) return ALL_LANGUAGES;
    const q = langSearch.value.toLowerCase().trim();
    return ALL_LANGUAGES.filter(item => 
        item.label.toLowerCase().includes(q) || 
        item.code.toLowerCase().includes(q)
    );
});

const currentTargetLang = ref(authStore.user?.preferredLanguage || 'vi'); // Khởi tạo bằng preferredLanguage của user
const showLangMenu = ref(false);     // Bật/tắt popup chọn ngôn ngữ
const dropdownPlacement = ref('bottom'); // Vị trí popup: 'bottom' (dưới) hoặc 'top' (trên)

// Bật/tắt popup chọn ngôn ngữ (tự động đảo vị trí nếu sát đáy)
const toggleLangMenu = (event) => {
    if (!showLangMenu.value) {
        const buttonEl = event?.currentTarget;
        if (buttonEl) {
            const rect = buttonEl.getBoundingClientRect();
            const spaceBelow = window.innerHeight - rect.bottom;
            // Nếu khoảng trống bên dưới < 220px thì lật lên trên, ngược lại mở xuống dưới
            dropdownPlacement.value = spaceBelow < 220 ? 'top' : 'bottom';
        }
        showLangMenu.value = true;
    } else {
        showLangMenu.value = false;
    }
};

// Key lưu trạng thái đóng/mở và nội dung dịch theo từng tin nhắn trong session/local
const getTranslationStorageKey = () => `bluespace_trans_${props.messageId}`;

const loadSavedTranslationState = () => {
    if (!props.messageId) return;
    try {
        const raw = sessionStorage.getItem(getTranslationStorageKey());
        if (raw) {
            const data = JSON.parse(raw);
            if (data.translatedText) translatedText.value = data.translatedText;
            if (typeof data.showTranslation === 'boolean') showTranslation.value = data.showTranslation;
            if (data.currentTargetLang) currentTargetLang.value = data.currentTargetLang;
        }
    } catch (e) {
        // bỏ qua nếu parse lỗi
    }
};

const saveTranslationState = () => {
    if (!props.messageId) return;
    try {
        if (!translatedText.value) {
            sessionStorage.removeItem(getTranslationStorageKey());
            return;
        }
        sessionStorage.setItem(getTranslationStorageKey(), JSON.stringify({
            translatedText: translatedText.value,
            showTranslation: showTranslation.value,
            currentTargetLang: currentTargetLang.value
        }));
    } catch (e) {
        // bỏ qua nếu storage đầy
    }
};

// Hàm khi người dùng bấm chọn một ngôn ngữ từ popup
const handleSelectLanguage = async (langCode) => {
    showLangMenu.value = false;
    currentTargetLang.value = langCode;
    try {
        isTranslating.value = true;
        showTranslation.value = true;
        const res = await translateMessage(props.messageId, langCode);
        translatedText.value = res.translatedText;
        if (res.targetLanguage) {
            currentTargetLang.value = res.targetLanguage;
        }
        saveTranslationState();
    } catch (error) {
        console.error('Lỗi khi dịch ngôn ngữ:', error);
    } finally {
        isTranslating.value = false;
    }
};

// Hàm xử lý khi bấm nút Dịch
const handleTranslate = async () => {
    // Nếu đã dịch rồi thì bấm là BẬT / TẮT 
    if (translatedText.value) {
        showTranslation.value = !showTranslation.value;
        saveTranslationState();
        return;
    }
    // Nếu chưa dịch thì gọi AI (không truyền targetLanguage để backend tự lấy preferredLanguage của User)
    try {
        isTranslating.value = true;
        showTranslation.value = true; // Mở sẵn khung hiển thị dòng loading
        const res = await translateMessage(props.messageId, null);
        translatedText.value = res.translatedText;
        // Cập nhật targetLanguage trả về từ server để đồng bộ với popup
        if (res.targetLanguage) {
            currentTargetLang.value = res.targetLanguage;
        }
        saveTranslationState();
    } catch (error) {
        console.error('Lỗi dịch tin nhắn:', error);
    } finally {
        isTranslating.value = false;
    }
};

// Nút bấm thu gọn mất tiêu bản dịch
const handleCloseTranslation = () => {
    showTranslation.value = false;
    saveTranslationState();
};

// Khi nội dung tin nhắn thay đổi (do chỉnh sửa), tự động xóa bản dịch cũ trên giao diện
watch(() => props.Content_Message, (newContent, oldContent) => {
    if (newContent !== oldContent) {
        translatedText.value = '';
        showTranslation.value = false;
        try {
            if (props.messageId) sessionStorage.removeItem(getTranslationStorageKey());
        } catch (e) {}
    }
});

// =============================================================================
// ██████████████  PHẦN 3: BIỂU CẢM TIN NHẮN (MESSAGE REACTIONS)  ██████████████
// =============================================================================

const showReactionPicker = ref(false); // Trạng thái hiển thị thanh chọn nhanh 6 icon
let closeReactionTimer = null; // Timer hoãn đóng thanh cảm xúc (250ms)

// 6 biểu cảm chuẩn: LIKE, LOVE, HAHA, WOW, SAD, ANGRY
const AVAILABLE_REACTIONS = [
    { type: 'LIKE', emoji: '👍', label: 'Thích' },
    { type: 'LOVE', emoji: '❤️', label: 'Yêu thích' },
    { type: 'HAHA', emoji: '😂', label: 'Haha' },
    { type: 'WOW', emoji: '😮', label: 'Wow' },
    { type: 'SAD', emoji: '😢', label: 'Buồn' },
    { type: 'ANGRY', emoji: '😡', label: 'Phẫn nộ' }
];

const getEmojiChar = (type) => {
    const found = AVAILABLE_REACTIONS.find((r) => r.type === type);
    return found ? found.emoji : '👍';
};

// Quản lý đóng/mở thanh cảm xúc có độ trễ 250ms
const handleMouseEnterReaction = () => {
    if (closeReactionTimer) {
        clearTimeout(closeReactionTimer);
        closeReactionTimer = null;
    }
    showReactionPicker.value = true;
};

const handleMouseLeaveReaction = () => {
    if (closeReactionTimer) {
        clearTimeout(closeReactionTimer);
    }
    closeReactionTimer = setTimeout(() => {
        showReactionPicker.value = false;
    }, 200);
};

// Gom nhóm reactions để render badge: { LOVE: { count: 1, reactedByMe: true, users: ['Harry'] } }
const groupedReactions = computed(() => {
    const groups = {};
    const myId = authStore.user?.userId;
    for (const r of (props.reactions || [])) {
        if (!groups[r.reactionType]) {
            groups[r.reactionType] = { count: 0, reactedByMe: false, users: [] };
        }
        groups[r.reactionType].count++;
        groups[r.reactionType].users.push(r.username || 'Thành viên');
        if (myId && String(r.userId) === String(myId)) {
            groups[r.reactionType].reactedByMe = true;
        }
    }
    return groups;
});

// Thả hoặc gỡ cảm xúc với Optimistic UI
const handleSelectReaction = async (type) => {
    if (closeReactionTimer) {
        clearTimeout(closeReactionTimer);
        closeReactionTimer = null;
    }
    showReactionPicker.value = false;
    if (props.messageId) {
        await chatStore.toggleReaction(props.messageId, type);
    }
};

// =============================================================================
// ████  PHẦN 4: FILE PHƯƠNG TIỆN, REMINDER (LỊCH HẸN) & HIGHLIGHT TỪ KHÓA  ████
// =============================================================================

const isMediaLoadError = ref(false); // Trạng thái ảnh/video load thất bại

// Xử lý hiển thị ảnh / video / file đính kèm trên tin nhắn
const fileStorageKey = computed(() => {
    // Nếu là tin nhắn media/file mà có Content_Message chứa đường dẫn
    if (['IMAGE', 'VIDEO', 'FILE'].includes(props.messageType)) {
        return props.mediaUrl || props.Content_Message || '';
    }
    return props.mediaUrl || '';
});

// Nhận diện kiểu tin nhắn thực tế (hỗ trợ trường hợp backend lưu video dưới dạng IMAGE hoặc FILE)
const isVideoContent = computed(() => {
    const key = fileStorageKey.value || '';
    return props.messageType === 'VIDEO' || /\.(mp4|mov|webm|avi|mkv)$/i.test(key);
});

const displayFileName = computed(() => {
    return props.fileName || fileStorageKey.value.split('/').pop() || 'Tài liệu đính kèm';
});

// Dữ liệu chi tiết Lịch hẹn tương ứng với tin nhắn REMINDER
const reminderData = computed(() => {
    if (props.messageType !== 'REMINDER') return null;
    return chatStore.getReminderForMessage(
        props.rawMessage || {
            messageId: props.messageId,
            roomId: chatStore.activeRoomId,
        }
    );
});

// Kiểm tra nội dung tin nhắn có chứa từ khóa tìm kiếm hay không
const hasKeywordMatch = computed(() => {
    if (!props.searchKeyword || !props.Content_Message) return false;
    return props.Content_Message.toLowerCase().includes(props.searchKeyword.toLowerCase());
});

// =============================================================================
// ████  PHẦN 5: CHỈNH SỬA INLINE, THU HỒI, LỊCH SỬ SỬA & TRẢ LỜI (REPLY)  █████
// =============================================================================

const isEditing = ref(false); // Trạng thái đang chỉnh sửa inline
const editContent = ref(''); // Nội dung văn bản đang sửa
const editTextareaRef = ref(null); // Ref tới ô textarea sửa
const editBoxRef = ref(null); // Ref tới khối wrapper chỉnh sửa

// Tự động điều chỉnh chiều cao textarea theo nội dung gõ
const autoResize = () => {
  nextTick(() => {
    if (editTextareaRef.value) {
      editTextareaRef.value.style.height = 'auto'
      editTextareaRef.value.style.height = editTextareaRef.value.scrollHeight + 'px'
    }
  })
}

// Bắt đầu chỉnh sửa
const handleStartEdit = () => {
  showMenu.value = false
  editContent.value = props.Content_Message
  isEditing.value = true
  nextTick(() => {
    if (editTextareaRef.value) {
      editTextareaRef.value.focus()
      autoResize()
    }
  })
}

// Lưu nội dung đã sửa
const handleSaveEdit = () => {
  const trimmed = editContent.value.trim()
  if (trimmed && trimmed !== props.Content_Message) {
    emit('editMessage', { messageId: props.messageId, content: trimmed })
  }
  isEditing.value = false
}

// Hủy chỉnh sửa
const handleCancelEdit = () => {
  isEditing.value = false
  editContent.value = ''
}

// Kích hoạt thu hồi tin nhắn
const handleRevoke = () => {
  showMenu.value = false
  emit('revokeMessage', props.messageId)
}

// Mở popup lịch sử chỉnh sửa
const handleViewHistory = () => {
  showMenu.value = false
  emit('viewHistory', {
    messageId: props.messageId,
    content: props.Content_Message,
    sender: props.Sender || 'Bạn',
  })
}

// Tên hiển thị của người được trả lời ("Bạn" nếu là chính mình, "@username" nếu là người khác)
const repliedSenderDisplayName = computed(() => {
    if (!props.repliedMessage) return '';
    const currentUserId = authStore.user?.userId;
    const currentUsername = authStore.user?.username;
    const senderId = props.repliedMessage.senderId || props.repliedMessage.sender?.userId;
    const senderUsername = props.repliedMessage.senderUsername || props.repliedMessage.sender?.username;
    const isMe = (currentUserId && senderId && String(senderId) === String(currentUserId)) ||
                 (currentUsername && senderUsername && currentUsername === senderUsername);
    return isMe ? 'Bạn' : (senderUsername ? `@${senderUsername}` : 'Thành viên');
});

// Xử lý khi bấm nút Trả lời (Reply)
const handleReplyClick = () => {
    showMenu.value = false;
    if (props.isRevoked) return;
    const msgPayload = props.rawMessage || {
        messageId: props.messageId,
        sender: {
            userId: props.variant === 'SendTo' ? authStore.user?.userId : null,
            username: props.variant === 'SendTo' ? (authStore.user?.username || 'Bạn') : props.Sender,
        },
        content: props.Content_Message,
        isRevoked: props.isRevoked,
        messageType: 'TEXT',
    };
    emit('replyMessage', msgPayload);
};

// Xử lý khi click vào khối trích dẫn để cuộn tới tin nhắn gốc
const handleClickRepliedQuote = () => {
    if (props.repliedMessage?.messageId) {
        emit('scrollToMessage', props.repliedMessage.messageId);
    }
};

// Đóng menu 3 chấm, thanh chọn cảm xúc và hủy chỉnh sửa khi click bên ngoài
const handleDocumentClick = (e) => {
    showMenu.value = false;
    showReactionPicker.value = false;
    showLangMenu.value = false;

    // Nếu đang chỉnh sửa inline mà click ra ngoài khung sửa -> tự động hủy bỏ
    if (isEditing.value && editBoxRef.value && !editBoxRef.value.contains(e.target)) {
        handleCancelEdit();
    }
};

onMounted(() => {
  loadSavedTranslationState()
  document.addEventListener('click', handleDocumentClick)
})

watch(() => props.messageId, () => {
  loadSavedTranslationState()
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<template>
  <div
    :id="messageId ? 'msg-' + messageId : null"
    :class="[
      'container', 
      `container--${variant}`, 
      { 
        'container--consecutive': isConsecutive,
        'container--has-reply': repliedMessage && !isRevoked
      }
    ]"
  >
    <div v-if="!isConsecutive && variant !== 'SendTo'" class="message-bubble-avatar-box">
      <img v-if="Avatar" :src="Avatar" alt="Avatar" class="custom-avatar-img" />
      <span v-else>{{ Sender ? Sender.charAt(0).toUpperCase() : '?' }}</span>
    </div>
    <div v-else-if="variant !== 'SendTo'" class="bubble-spacer"></div>

    <div
      :class="[
        'wrapper',
        `wrapper--${variant}`,
        { 'wrapper--consecutive': isConsecutive, 'wrapper--editing': isEditing },
      ]"
    >
      <div class="sender" v-if="Sender && !isConsecutive">{{ Sender }}</div>

      <!-- Khối trích dẫn đặt RIÊNG BIỆT phía trên bong bóng tin nhắn (không nằm chung khung) -->
      <div 
          v-if="repliedMessage && !isRevoked" 
          class="replied-quote-box"
          :class="`replied-quote-box--${variant}`"
          @click.stop="handleClickRepliedQuote"
          title="Bấm để cuộn tới tin nhắn gốc"
      >
          <div class="replied-quote-sender">
              {{ repliedSenderDisplayName }}
          </div>
          <div class="replied-quote-content">
              {{ getReplySnippet(repliedMessage) }}
          </div>
      </div>

        <!-- Hàng bong bóng tin nhắn kèm nút thao tác nằm ngang hàng ngay bên cạnh -->
        <div class="bubble-row" :class="[`bubble-row--${variant}`, { 'bubble-row--editing': isEditing }]">
            <!-- Khối chứa bong bóng chat và huy hiệu cảm xúc đính kèm sát mép dưới -->
            <div class="message-bubble-group" :class="`message-bubble-group--${variant}`">
                
                <!-- 1. Trạng thái tin nhắn đã bị thu hồi (Ưu tiên kiểm tra đầu tiên) -->
                <div v-if="isRevoked" :class="['message', 'message--revoked']">
                    <i>Tin nhắn đã được thu hồi</i>
                </div>

                <!-- 2. Trạng thái đang chỉnh sửa -->
                <div 
                    v-else-if="isEditing" 
                    ref="editBoxRef"
                    :class="['message', `message--${variant}`, 'message--editing']"
                >
                    <textarea 
                        ref="editTextareaRef"
                        v-model="editContent" 
                        class="inline-edit-textarea" 
                        rows="1" 
                        @input="autoResize"
                        @keydown.enter.exact.prevent="handleSaveEdit" 
                        @keydown.esc="handleCancelEdit"
                    ></textarea>
                    <div class="edit-toolbar">
                        <span class="edit-hint"></span>
                        <div class="edit-btn-group">
                            <button class="edit-mini-btn btn-save" title="Lưu" @click="handleSaveEdit">
                                <font-awesome-icon icon="check" />
                            </button>
                            <button class="edit-mini-btn btn-cancel" title="Hủy" @click="handleCancelEdit">
                                <font-awesome-icon icon="x" />
                            </button>
                        </div>
                    </div>
                </div>

                <!-- 3. Tin nhắn dạng Ảnh -->
                <div 
                    v-else-if="messageType === 'IMAGE' && !isVideoContent" 
                    class="message message--media message--img-box"
                    :class="[`message--${variant}`, `message--pos-${groupPosition}`]"
                    @click="emit('preview-media', { type: 'IMAGE', url: fileStorageKey || Content_Message })"
                >
                    <div class="message-media-wrapper">
                        <!-- Fallback khi ảnh không load được -->
                        <div v-if="isMediaLoadError" class="msg-media-fallback">
                            <font-awesome-icon icon="image" class="msg-fallback-icon" />
                            <span class="msg-fallback-text">Ảnh không tải được</span>
                        </div>
                        <img
                            v-else
                            :src="resolveFileUrl(fileStorageKey || Content_Message)"
                            alt="Hình ảnh đính kèm"
                            class="msg-media-img"
                            @error="isMediaLoadError = true"
                        />
                    </div>
                </div>

                <!-- 4. Tin nhắn dạng Video (hỗ trợ cả messageType === 'VIDEO' hoặc file/image có đuôi video) -->
                <div 
                    v-else-if="messageType === 'VIDEO' || (['IMAGE', 'FILE'].includes(messageType) && isVideoContent)" 
                    class="message message--media message--video-box"
                    :class="[`message--${variant}`, `message--pos-${groupPosition}`]"
                >
                    <div class="message-media-wrapper message-video-wrapper" @click="emit('preview-media', { type: 'VIDEO', url: fileStorageKey || Content_Message })">
                        <!-- Fallback khi video không load được -->
                        <div v-if="isMediaLoadError" class="msg-media-fallback">
                            <font-awesome-icon icon="video" class="msg-fallback-icon" />
                            <span class="msg-fallback-text">Video không tải được</span>
                        </div>
                        <template v-else>
                            <video
                                :src="resolveFileUrl(fileStorageKey || Content_Message)"
                                controls
                                preload="metadata"
                                class="msg-media-video"
                                @error="isMediaLoadError = true"
                            ></video>
                            <!-- Lớp bắt click trên thân video để mở modal, nhưng chừa khoảng thanh điều khiển 42px ở dưới cho controls -->
                            <div 
                                class="video-click-overlay" 
                                title="Bấm để phóng to video"
                                @click.stop="emit('preview-media', { type: 'VIDEO', url: fileStorageKey || Content_Message })"
                            ></div>
                        </template>
                    </div>
                </div>

                <!-- 5. Tin nhắn dạng Tệp đính kèm (File) -->
                <div 
                    v-else-if="messageType === 'FILE' && !isVideoContent" 
                    class="message-file-standalone"
                >
                    <a :href="resolveFileUrl(fileStorageKey)" target="_blank" rel="noopener noreferrer" class="message-file-card" @click.stop>
                        <div class="msg-file-icon-box">
                            <font-awesome-icon icon="file-lines" />
                        </div>
                        <div class="msg-file-meta">
                            <span class="msg-file-name" :title="displayFileName">{{ displayFileName }}</span>
                            <span class="msg-file-size">Tệp tải xuống</span>
                        </div>
                        <font-awesome-icon icon="arrow-down" class="msg-file-download-icon" />
                    </a>
                </div>

                <!-- 5.5. Tin nhắn dạng Thẻ Lịch hẹn / Nhắc nhở (REMINDER) -->
                <div
                    v-else-if="messageType === 'REMINDER'"
                    class="message-reminder-standalone"
                >
                    <ReminderMessageCard
                        :message="rawMessage || { messageId, content: Content_Message, roomId: chatStore.activeRoomId }"
                        :reminder="reminderData"
                        :current-user-id="authStore.user?.userId"
                        :can-manage-room="canManageRoom"
                    />
                </div>

                <!-- 6. Tin nhắn Văn bản (TEXT) thông thường -->
                <div 
                    v-else 
                    class="message"
                    :class="[`message--${variant}`, `message--pos-${groupPosition}`]"
                    @dblclick="variant === 'SendTo' ? handleStartEdit() : null"
                    :title="variant === 'SendTo' ? 'Bấm 2 lần để sửa nhanh' : ''"
                >
                    <!-- [1] Nội dung tin nhắn gốc -->
                    <div class="message-original-text">
                        <span v-if="searchKeyword && hasKeywordMatch" v-html="highlightKeyword(Content_Message, searchKeyword)"></span>
                        <template v-else>{{ Content_Message }}</template>
                    </div>

                    <!-- [2] Dòng hiệu ứng khi đang chờ AI dịch (không bị khựng) -->
                    <div v-if="isTranslating" class="message-translation-box">
                        <div class="translation-divider"></div>
                        <div class="translation-loading">
                            <font-awesome-icon icon="spinner" spin class="loading-spinner" />
                            <span>Đang dịch...</span>
                        </div>
                    </div>

                    <!-- [3] Bản dịch hoàn chỉnh (khi showTranslation = true) -->
                    <div v-else-if="translatedText && showTranslation" class="message-translation-box">
                        <!-- Vạch kẻ phân cách -->
                        <div class="translation-divider"></div>

                        <!-- Nội dung đã dịch -->
                        <div class="translation-content">
                            {{ translatedText }}
                        </div>

                                <!-- Chân bản dịch: 2 bên -->
                                <div class="translation-footer">
                                    <!-- BÊN TRÁI: Vùng thu gọn chiếm nửa trái -->
                                    <div 
                                        class="translation-toggle-zone" 
                                        title="Bấm để thu gọn bản dịch"
                                        @click.stop="handleCloseTranslation"
                                    >
                                        <font-awesome-icon icon="chevron-up" class="chevron-icon" />
                                    </div>

                                    <!-- BÊN PHẢI: Chỉ có icon chọn ngôn ngữ -->
                                    <div class="translation-lang-wrapper" @click.stop>
                                        <button 
                                            type="button" 
                                            class="translation-lang-btn" 
                                            title="Đổi ngôn ngữ dịch"
                                            @click.stop="toggleLangMenu"
                                        >
                                            <font-awesome-icon icon="language" class="lang-icon" />
                                        </button>

                                        <!-- POPUP CHỌN NGÔN NGỮ (Tự động lật lên hoặc xuống) -->
                                        <div 
                                            v-if="showLangMenu" 
                                            :class="['lang-dropdown', `lang-dropdown--${dropdownPlacement}`]" 
                                            @click.stop
                                        >
                                            <div class="lang-search-box">
                                                <input 
                                                    v-model="langSearch" 
                                                    type="text" 
                                                    placeholder="Tìm kiếm..." 
                                                    class="lang-search-input"
                                                    @click.stop
                                                />
                                            </div>

                                            <div class="lang-list-scroll">
                                                <div 
                                                    v-for="lang in filteredLanguages" 
                                                    :key="lang.code"
                                                    :class="['lang-opt', { 'lang-opt--active': currentTargetLang === lang.code }]"
                                                    @click.stop="handleSelectLanguage(lang.code)"
                                                >
                                                    <span>{{ lang.label }}</span>
                                                    <font-awesome-icon v-if="currentTargetLang === lang.code" icon="check" class="opt-check" />
                                                </div>

                                                <div v-if="filteredLanguages.length === 0" class="lang-empty">
                                                    Không tìm thấy
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                    </div>
                </div>

                <!-- Reaction Badges gắn trực tiếp ngay mép dưới bong bóng tin nhắn -->
                <div 
                    v-if="props.reactions && props.reactions.length > 0 && !isRevoked" 
                    class="reaction-badges" 
                    :class="`reaction-badges--${variant}`"
                >
                    <button 
                        v-for="(data, type) in groupedReactions" 
                        :key="type"
                        type="button"
                        :class="['reaction-badge-item', { 'reaction-badge-item--active': data.reactedByMe }]"
                        :title="data.users.join(', ')"
                        @click.stop="handleSelectReaction(type)"
                    >
                        <span class="badge-emoji">{{ getEmojiChar(type) }}</span>
                        <span class="badge-count">{{ data.count }}</span>
                    </button>
                </div>
            </div>

            <!-- Nút Thả cảm xúc, Trả lời, Ghim, ... để NGAY BÊN CẠNH / SAU TIN NHẮN (hiển thị khi hover) -->
            <div 
                v-if="!isRevoked" 
                class="message-actions" 
                :class="[`message-actions--${variant}`, { 'has-active-picker': showReactionPicker }]"
            >
                <!-- 1. Nút Thả cảm xúc -->
                <div 
                    class="reaction-trigger-container"
                    @mouseenter="handleMouseEnterReaction"
                    @mouseleave="handleMouseLeaveReaction"
                >
                    <span 
                        class="action-btn react-btn" 
                        title="Thả cảm xúc" 
                        @click.stop="showReactionPicker = !showReactionPicker"
                    >
                        <font-awesome-icon :icon="['far', 'face-smile']" />
                    </span>

                    <!-- Thanh chọn nhanh 6 icon: LIKE, LOVE, HAHA, WOW, SAD, ANGRY -->
                    <transition name="reaction-pop">
                        <div 
                            v-if="showReactionPicker" 
                            class="quick-reaction-bar"
                            :class="`quick-reaction-bar--${variant}`"
                            @click.stop
                        >
                            <button 
                                v-for="item in AVAILABLE_REACTIONS" 
                                :key="item.type"
                                type="button"
                                class="quick-reaction-btn"
                                :title="item.label"
                                @click="handleSelectReaction(item.type)"
                            >
                                {{ item.emoji }}
                            </button>
                        </div>
                    </transition>
                </div>

                <!-- 2. Nút Trả lời (Reply) -->
                <span 
                    class="action-btn reply-btn" 
                    title="Trả lời" 
                    @click="handleReplyClick"
                >
                    <font-awesome-icon icon="reply"/>
                </span>

                <!-- 3. Nút Ghim -->
                <span 
                    class="action-btn pin-btn" 
                    :class="{ 'is-pinned': isPinned }" 
                    :title="isPinned ? 'Bỏ ghim tin nhắn' : 'Ghim tin nhắn'"
                    @click="emit('togglePin', messageId)"
                >
                    <font-awesome-icon icon="thumbtack"/>
                </span>

                <!-- Nút Dịch (chỉ hiện cho tin nhắn văn bản thông thường) -->
                <span 
                    v-if="messageType === 'TEXT' && !isRevoked"
                    class="action-btn translate-btn" 
                    :class="{ 'is-loading': isTranslating }"
                    title="Dịch tin nhắn" 
                    @click.stop="handleTranslate"
                >
                    <!-- Có thể dùng icon 'language' của font-awesome có sẵn trong dự án -->
                    <font-awesome-icon v-if="!isTranslating" icon="language" />
                    <font-awesome-icon v-else icon="spinner" spin />
                </span>
                <!-- 4. Nút 3 chấm (...) -->
                <div class="menu-container">
                    <span 
                        class="action-btn more-btn" 
                        title="Tùy chọn tin nhắn" 
                        @click.stop="showMenu = !showMenu"
                    >
                        <font-awesome-icon icon="ellipsis-vertical"/>
                    </span>

                    <!-- Menu xổ xuống -->
                    <div v-if="showMenu" class="action-dropdown" :class="`action-dropdown--${variant}`" @click.stop>
                        <div class="dropdown-opt" @click="handleReplyClick">
                            <font-awesome-icon icon="reply" class="opt-icon"/>
                            <span>Trả lời</span>
                        </div>
                        <div v-if="variant === 'SendTo' && messageType === 'TEXT'" class="dropdown-opt" @click="handleStartEdit">
                            <font-awesome-icon icon="pen" class="opt-icon"/>
                            <span>Chỉnh sửa</span>
                        </div>
                        <div v-if="isEdited" class="dropdown-opt" @click="handleViewHistory">
                            <font-awesome-icon icon="clock-rotate-left" class="opt-icon"/>
                            <span>Xem lịch sử</span>
                        </div>
                        <div v-if="variant === 'SendTo'" class="dropdown-opt opt-danger" @click="handleRevoke">
                            <font-awesome-icon icon="trash" class="opt-icon"/>
                            <span>Thu hồi</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Dòng thời gian phía dưới (kèm nhãn 'đã chỉnh sửa') -->
        <div
          v-if="showTime || (isEdited && !isRevoked)"
          class="time-row"
          :class="`time-row--${variant}`"
        >
          <div v-if="showTime" :class="['time', `time--${variant}`]">{{ Time }}</div>
          <span
            v-if="isEdited && !isRevoked"
            class="edited-badge"
            title="Bấm để xem lịch sử chỉnh sửa"
            @click="handleViewHistory"
          >
            Đã chỉnh sửa
          </span>
        </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.container {
  display: flex;
  width: 100%;
  flex-direction: row;
  position: relative;
}

.container--SendTo {
  flex-direction: row-reverse;
}

.container--consecutive {
  margin-top: -0.5px;
}

.container--consecutive .wrapper {
  margin-top: 0 !important;
}

.container--consecutive .message {
  margin-top: -0.5px !important;
}

/* Tin nhắn có phần trích dẫn reply: tách xa ra 1 xíu so với tin nhắn trước đó */
.container--has-reply {
  margin-top: 8px;
}

.container--consecutive.container--has-reply {
  margin-top: 10px !important;
}

.container--consecutive .bubble-spacer {
  height: 1px;
}

.wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-top: 6px;
  max-width: 75%;
  position: relative;
  padding-left: 0;
}

/* Tin nhắn của mình: không chừa lề trái và căn toàn bộ nội dung về bên phải */
.wrapper--SendTo {
  padding-left: 0;
  align-items: flex-end;
}

.wrapper--consecutive {
  margin-top: 0 !important;
}

.wrapper--editing {
  width: 75% !important;
  max-width: 75% !important;
}

.bubble-spacer {
  width: 36px;
  height: 1px;
  margin-right: 8px;
  margin-left: 0;
  flex-shrink: 0;
}

/* Hàng ngang chứa bong bóng chat và các nút thao tác bên cạnh */
.bubble-row {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  max-width: 100%;
}

.bubble-row--editing {
  width: 100% !important;
}

.bubble-row--SendTo {
  flex-direction: row-reverse;
}

/* Khối bọc bong bóng chat và huy hiệu cảm xúc đính kèm */
.message-bubble-group {
  display: flex;
  flex-direction: column;
  position: relative;
  max-width: 100%;
}

.message-bubble-group--SendTo {
  align-items: flex-end;
}

.message-bubble-group--default {
  align-items: flex-start;
}

.message {
  display: inline-block;
  font-family: 'Quicksand', sans-serif;
  background-color: white;
  width: fit-content;
  max-width: 100%;
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: pre-wrap;
  height: fit-content;
  border: 1px solid #e6eaf2;
  border-radius: 0.2rem 1.2rem 1.2rem;
  margin-top: 5px;
  padding: 12px 16px;
  color: #1e293b;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.message--SendTo {
  color: white;
  background-color: #4b7bec;
  border-radius: 18px 4px 18px 18px;
}

/* =========================================================
   KHỐI TRÍCH DẪN REPLY - TÁCH KHUNG RIÊNG BIỆT (DETACHED STYLE)
   ========================================================= */
.replied-quote-box {
  position: relative;
  cursor: pointer;
  border-radius: 8px;
  padding: 4px 10px 4px 12px; /* Dành chỗ bên trái cho vạch accent */
  margin-top: 4px; /* Giúp khối quote cách xa 1 xíu với tin nhắn trước đó */
  margin-bottom: 2px; /* Dời sát lại gần bong bóng tin nhắn reply bên dưới */
  text-align: left;
  user-select: none;
  width: fit-content;
  max-width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
  transition: transform 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease;
}

/* Kéo bong bóng tin nhắn bên dưới sát lên ngay cạnh khối quote */
.replied-quote-box + .bubble-row .message {
  margin-top: 1px !important;
}

/* Hiệu ứng nhún nhẹ khi click/hover tạo cảm giác tương tác */
.replied-quote-box:hover {
  transform: translateY(-1px);
}
.replied-quote-box:active {
  transform: translateY(0);
}

/* Vạch accent dọc bên trái: Thẳng tắp, bo tròn 2 đầu độc lập, không bị méo góc */
.replied-quote-box::before {
  content: '';
  position: absolute;
  left: 0;
  top: 3px;
  bottom: 3px;
  width: 3.5px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

/* Hàng tiêu đề người gửi */
.replied-quote-sender {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 700;
  font-size: 11px;
  letter-spacing: 0.2px;
  line-height: 1.15;
}

/* Tự động chèn icon reply mini trước tên người gửi */
.replied-quote-sender::before {
  content: '↩';
  font-size: 10.5px;
  font-weight: 900;
  transform: translateY(-0.5px);
  opacity: 0.85;
}

/* Nội dung tin nhắn trích dẫn */
.replied-quote-content {
  font-size: 11.5px;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* =========================================================
   1. KHỐI REPLY CỦA ĐỐI PHƯƠNG (default - Bên trái)
   ========================================================= */
.replied-quote-box--default {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.replied-quote-box--default::before {
  background: linear-gradient(180deg, #3b82f6, #2563eb); /* Gradient xanh dương */
}

.replied-quote-box--default:hover {
  background: #e2e8f0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.replied-quote-box--default .replied-quote-sender {
  color: #2563eb;
}

.replied-quote-box--default .replied-quote-content {
  color: #475569;
}

/* =========================================================
   2. KHỐI REPLY CỦA CHÍNH MÌNH (SendTo - Bên phải)
   ========================================================= */
.replied-quote-box--SendTo {
  background: #eff6ff; /* Nền xanh pastel tách biệt hoàn toàn khỏi bong bóng xanh đậm */
  border: 1px solid #dbeafe;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.05);
}

.replied-quote-box--SendTo::before {
  background: linear-gradient(180deg, #4b7bec, #2563eb);
}

.replied-quote-box--SendTo:hover {
  background: #dbeafe;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.1);
}

.replied-quote-box--SendTo .replied-quote-sender {
  color: #2563eb;
}

.replied-quote-box--SendTo .replied-quote-content {
  color: #334155;
}

.message-content-text {
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: pre-wrap;
}

.reply-btn:hover {
  color: #4b7bec;
}

/* --- Bo góc bong bóng chuẩn Facebook Messenger theo vị trí trong cụm tin nhắn --- */
/* 1. Tin nhắn của đối phương (bên trái) */
.message--pos-first {
  border-radius: 18px 18px 18px 4px !important;
}

.message--pos-middle {
  border-radius: 4px 18px 18px 4px !important;
}

.message--pos-last {
  border-radius: 4px 18px 18px 18px !important;
}

/* 2. Tin nhắn của chính mình (bên phải) */
.message--SendTo.message--pos-first {
  border-radius: 18px 18px 4px 18px !important;
}

.message--SendTo.message--pos-middle {
  border-radius: 18px 4px 4px 18px !important;
}

.message--SendTo.message--pos-last {
  border-radius: 18px 4px 18px 18px !important;
}

/* Giao diện chỉnh sửa tin nhắn inline: kéo dài bằng chiều rộng tin nhắn đầy đủ */
.message--editing {
    width: 100% !important;
    min-width: 280px;
    max-width: 100% !important;
    padding: 10px 14px;
    box-shadow: 0 0 0 2px rgba(75, 123, 236, 0.4);
    overflow: hidden;
}

.inline-edit-textarea {
    width: 100%;
    background: transparent;
    border: none;
    outline: none;
    color: inherit;
    font-family: inherit;
    font-size: 14px;
    line-height: 1.4;
    resize: none;
    box-sizing: border-box;
    display: block;
    overflow: hidden;
    scrollbar-width: none;
    -ms-overflow-style: none;
}

.inline-edit-textarea::-webkit-scrollbar {
    display: none;
    width: 0;
    height: 0;
}

.edit-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  padding-top: 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.25);
  gap: 8px;
}

.message:not(.message--SendTo) .edit-toolbar {
  border-top-color: rgba(0, 0, 0, 0.08);
}

.edit-hint {
  font-size: 10.5px;
  opacity: 0.8;
  user-select: none;
}

.edit-btn-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.edit-mini-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 11px;
  line-height: 1;
  padding: 0;
  transition: all 0.15s;
  vertical-align: middle;
}

.btn-save {
  background-color: rgba(255, 255, 255, 0.95);
  color: #2563eb;
}

.btn-save:hover {
  background-color: #ffffff;
  transform: scale(1.1);
}

.btn-cancel {
  background-color: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

.message:not(.message--SendTo) .btn-cancel {
  background-color: rgba(0, 0, 0, 0.06);
  color: #64748b;
}

.btn-cancel:hover {
  background-color: rgba(255, 255, 255, 0.45);
  transform: scale(1.1);
}

/* Tin nhắn đã bị thu hồi */
.message--revoked {
  background-color: #f1f5f9 !important;
  color: #94a3b8 !important;
  border: 1px dashed #cbd5e1 !important;
  font-size: 13px;
  border-radius: 12px !important;
}

/* Các nút thao tác (Ghim, ...) nằm ngang hàng cạnh bong bóng */
.message-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s ease;
  flex-shrink: 0;
}

.bubble-row:hover .message-actions,
.message-actions.has-active-picker {
    opacity: 1;
}

/* Container kích hoạt thanh chọn cảm xúc */
.reaction-trigger-container {
    position: relative;
    display: inline-flex;
}

.react-btn:hover {
    color: #f59e0b;
}

/* Thanh chọn nhanh 6 icon cảm xúc nổi lên trên */
.quick-reaction-bar {
    position: absolute;
    bottom: calc(100% + 6px);
    display: inline-flex;
    align-items: center;
    gap: 2px;
    background: #ffffff;
    padding: 3px 6px;
    border-radius: 24px;
    box-shadow: 0 4px 18px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(0, 0, 0, 0.06);
    z-index: 60;
    white-space: nowrap;
}

.quick-reaction-bar--SendTo {
    right: 0;
}

.quick-reaction-bar--default {
    left: 0;
}

.quick-reaction-btn {
    background: transparent;
    border: none;
    cursor: pointer;
    font-size: 19px;
    line-height: 1;
    padding: 3px 4px;
    border-radius: 50%;
    transition: transform 0.15s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    user-select: none;
}

.quick-reaction-btn:hover {
    transform: scale(1.35) translateY(-2px);
}

.reaction-pop-enter-active,
.reaction-pop-leave-active {
    transition: all 0.18s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.reaction-pop-enter-from,
.reaction-pop-leave-to {
    opacity: 0;
    transform: translateY(6px) scale(0.85);
}

/* Huy hiệu gom nhóm cảm xúc (Reaction Badges) đính sát mép dưới bong bóng */
.reaction-badges {
    display: inline-flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 3px;
    margin-top: -8px; /* Đính ôm sát nhẹ vào góc dưới bong bóng tin nhắn chuẩn Messenger/Zalo */
    z-index: 2;
    position: relative;
}

.reaction-badges--SendTo {
    justify-content: flex-end;
    margin-right: 6px; /* Căn góc phải bong bóng tin nhắn mình */
}

.reaction-badges--default {
    justify-content: flex-start;
    margin-left: 6px; /* Căn góc trái bong bóng tin nhắn đối phương */
}

.reaction-badge-item {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    padding: 1.5px 6px;
    background-color: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    cursor: pointer;
    font-size: 11px;
    font-family: 'Quicksand', sans-serif;
    color: #475569;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    transition: all 0.15s ease;
    user-select: none;
}

.reaction-badge-item:hover {
    background-color: #f8fafc;
    border-color: #cbd5e1;
    transform: scale(1.05);
}

.reaction-badge-item--active {
    background-color: #eff6ff;
    border-color: #93c5fd;
    color: #1d4ed8;
    font-weight: 600;
}

.badge-emoji {
    font-size: 13px;
    line-height: 1;
}

.badge-count {
    font-size: 11px;
    font-weight: 600;
}
.quick-reaction-bar::after {
    content: '';
    position: absolute;
    top: 100%;             /* Nằm ngay mép dưới của thanh icon */
    left: 0;
    width: 100%;
    height: 10px;           /* Phủ kín khoảng hở 6px và chờ chờm nhẹ xuống nút dưới */
    background: transparent;
}

.action-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 11.5px;
  color: #94a3b8;
  border-radius: 50%;
  transition: all 0.15s ease;
}

.action-btn:hover {
  background-color: #e2e8f0;
  color: #1e293b;
}

.pin-btn.is-pinned {
  color: #4b7bec;
  opacity: 1;
}

/* Menu xổ xuống của nút 3 chấm (...) */
.menu-container {
  position: relative;
  display: inline-flex;
}

.action-dropdown {
  position: absolute;
  top: 100%;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  min-width: 140px;
  z-index: 50;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
}

.action-dropdown--SendTo {
  right: 0;
}

.action-dropdown--default {
  left: 0;
}

.dropdown-opt {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  font-size: 12px;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  color: #334155;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
  white-space: nowrap;
}

.dropdown-opt:hover {
  background-color: #f1f5f9;
}

.opt-danger {
  color: #ef4444;
}

.opt-danger:hover {
  background-color: #fee2e2;
}

.opt-icon {
  font-size: 11px;
}

/* Dòng thời gian phía dưới */
.time-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 3px;
}

.time-row--SendTo {
  flex-direction: row-reverse;
}

.time {
  display: flex;
  font-family: 'Quicksand', sans-serif;
  margin-left: 10px;
  font-size: 11px;
  color: #a0aec0;
}

.time--SendTo {
  margin-left: auto;
  margin-right: 10px;
}

.edited-badge {
  font-size: 11px;
  font-family: 'Quicksand', sans-serif;
  color: #94a3b8;
  cursor: pointer;
  transition: color 0.15s;
  user-select: none;
}

.edited-badge:hover {
  color: #4b7bec;
  text-decoration: underline;
}

.sender {
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  font-size: 12px;
  color: #64748b;
  margin-left: 4px;
  margin-top: 0;
  margin-bottom: 2px;
}

/* =========================================================================
   HIỆU ỨNG LÀM NỔI BẬT TIN NHẮN KHI NHẢY TỚI (GHIM / REPLY / TÌM KIẾM / LỊCH HẸN)
   - Kết hợp: Tô sáng toàn hàng ngang (Electric Blue) + Nhún nhẹ 1 nhịp êm ái duy nhất trong ~1.5s
   ========================================================================= */
.container.highlight-msg {
  position: relative;
  z-index: 15;
  border-radius: 16px;
  animation: rowElectricHighlight 1.5s cubic-bezier(0.22, 1, 0.36, 1);
}

/* Thanh vạch sáng chỉ điểm bên lề hàng tin nhắn */


.container--default.highlight-msg::after {
  left: -8px;
}

.container--SendTo.highlight-msg::after {
  right: -8px;
}

/* Nhún nhẹ 1 lần duy nhất + Viền đôi phát sáng cho mọi loại tin nhắn: Văn bản, Ảnh/Video, File và Thẻ Lịch Hẹn */
.highlight-msg .message,
.highlight-msg .message-file-card,
.highlight-msg :deep(.rem-card) {
  animation: bubbleElectricBounce 1.5s cubic-bezier(0.22, 1, 0.36, 1);
}

:deep(.chat-search-mark) {
  background-color: #fde047;
  color: #0f172a;
  border-radius: 3px;
  padding: 0 2px;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

@keyframes rowElectricHighlight {
  0% {
    background-color: rgba(37, 99, 235, 0);
  }
  20% {
    background-color: rgba(37, 99, 235, 0.16);
    box-shadow: 0 0 0 6px rgba(37, 99, 235, 0.16);
  }
  50% {
    background-color: rgba(37, 99, 235, 0.08);
    box-shadow: 0 0 0 6px rgba(37, 99, 235, 0.08);
  }
  100% {
    background-color: rgba(37, 99, 235, 0);
    box-shadow: 0 0 0 6px rgba(37, 99, 235, 0);
  }
}

@keyframes bubbleElectricBounce {
  0% {
    transform: scale(1);
  }
  22% {
    transform: scale(1.035) translateY(-2px);
    box-shadow:
      0 0 0 2.5px #ffffff,
      0 0 0 5px #2563eb,
      0 10px 24px rgba(37, 99, 235, 0.42);
  }
  60% {
    transform: scale(1);
    box-shadow:
      0 0 0 2px #ffffff,
      0 0 0 3.5px rgba(37, 99, 235, 0.35),
      0 4px 14px rgba(37, 99, 235, 0.18);
  }
  100% {
    transform: scale(1);
    box-shadow: none;
  }
}

@keyframes accentBarFade {
  0% {
    opacity: 0;
    transform: scaleY(0.4);
  }
  15%,
  70% {
    opacity: 1;
    transform: scaleY(1);
  }
  100% {
    opacity: 0;
    transform: scaleY(0.6);
  }
}

/* Khung chứa và bo vuông avatar (16px) đồng bộ giao diện profile */
/* Đồng bộ chuẩn khung hình vuông bo góc 16px giống hệt profile và khung TK cũ */
.msg-avatar-content {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
  font-weight: 700;
  font-size: 13px;
  background-color: #ede9fe; /* Màu nền tím nhạt giống chữ TK cũ */
  color: #7c3aed;            /* Màu chữ giống chữ TK cũ */
}

.msg-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.msg-avatar-fallback {
  width: 100%;
  height: 100%;
  border-radius: 14px;
  background-color: #3b82f6;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  font-family: 'Quicksand', sans-serif;
}

.msg-avatar-spacer {
  width: 36px;
  margin-right: 8px;
  flex-shrink: 0;
}

.custom-avatar-box {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
  font-weight: 700;
  font-size: 13px;
  background-color: #ede9fe; /* Màu nền dự phòng nếu chưa có ảnh */
  color: #7c3aed;
}

.message-bubble-avatar-box {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
  font-weight: 700;
  font-size: 13px;
  background-color: #ede9fe;
  color: #7c3aed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  margin-right: 8px;
  align-self: flex-start;
  margin-top: 4px;
}

.custom-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 14px;
}

/* Tinh chỉnh bong bóng khi chứa ảnh/video (bỏ padding thừa để ảnh tràn đẹp) */
.message.message--media {
    padding: 4px !important; /* Thu nhỏ khoảng đệm để khung ôm sát ảnh */
    background-color: transparent !important;
    border: none !important;
    box-shadow: none !important;
    overflow: hidden;
}

.message-media-wrapper {
    max-width: 260px;
    max-height: 820px;
    border-radius: 12px;
    overflow: hidden;
    cursor: pointer;
    border-radius: 12px;
    background-color: #f1f5f9;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #e2e8f0;
}

.message-video-wrapper {
    position: relative;
}

.video-click-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 44px; /* Chừa lại thanh controls ở đáy để người dùng bấm nút Play, Volume, Timeline */
    cursor: pointer;
    z-index: 2;
}

.msg-media-img, .msg-media-video {
    width: 100%;
    height: 100%;
    object-fit: contain; /* để hiển thị đầy đủ hình ảnh không bị cắt */
    display: block;
}

/* Fallback khi media load thất bại */
.msg-media-fallback {
    width: 180px;
    height: 120px;
    background-color: #f1f5f9;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: #94a3b8;
    user-select: none;
}

.msg-fallback-icon {
    font-size: 28px;
    color: #94a3b8;
}

.msg-fallback-text {
    font-size: 11px;
    font-weight: 600;
    color: #64748b;
}

.message-file-standalone {
    margin-top: 4px;
    margin-bottom: 2px;
    display: inline-block;
    max-width: 100%;
    background: transparent;
    border: none;
    padding: 0;
}

/* Khung hiển thị Tệp đính kèm (File Card) */
.message-file-card {
    display: flex;
    align-items: center;
    gap: 12px;
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 10px 14px;
    text-decoration: none;
    color: #1e293b;
    max-width: 280px;
    transition: all 0.2s ease;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.message-file-card:hover {
    background-color: #f1f5f9;
    border-color: #cbd5e1;
    transform: translateY(-1px);
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

/* Màu sắc file card tùy biến theo variant (của mình hay người khác) */
.message--SendTo .message-file-card {
    background: rgba(255, 255, 255, 0.15);
    color: #ffffff;
}

.message:not(.message--SendTo) .message-file-card {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    color: #1e293b;
}

.msg-file-icon-box {
    width: 38px;
    height: 38px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #3b82f6;
    color: white;
    border-radius: 8px;
    font-size: 16px;
    flex-shrink: 0;
}

.message--SendTo .msg-file-icon-box {
    background: rgba(255, 255, 255, 0.25);
    color: #ffffff;
}

.message:not(.message--SendTo) .msg-file-icon-box {
    background: #e2e8f0;
    color: #475569;
}

.msg-file-meta {
    display: flex;
    flex-direction: column;
    min-width: 0;
    flex: 1;
    gap: 2px;
}

.msg-file-name {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.msg-file-size {
    font-size: 11px;
    color: #64748b;
}

.msg-file-download-icon {
    color: #64748b;
    font-size: 13px;
    flex-shrink: 0;
    transition: transform 0.15s ease;
}

.message-file-card:hover .msg-file-download-icon {
    /* transform: translateY(2px); */
    color: #2563eb;
}

/* Vạch kẻ ngăn cách */
.translation-divider {
    height: 1px;
    background-color: rgba(0, 0, 0, 0.08);
    margin: 8px 0;
}
.message--SendTo .translation-divider {
    background-color: rgba(255, 255, 255, 0.2);
}

/* Dòng trạng thái đang dịch mượt mà */
.translation-loading {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12.5px;
    font-style: italic;
    opacity: 0.8;
    padding: 2px 0;
}
.loading-spinner {
    font-size: 12px;
}

/* Nội dung bản dịch */
.translation-content {
    font-size: 0.95em;
    line-height: 1.4;
    word-break: break-word;
}

/* Chân bản dịch */
.translation-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    padding-top: 6px;
    padding-bottom: 2px;
    user-select: none;
    box-sizing: border-box;
}

/* Khu vực mũi tên: chiếm nửa trái, click vào là thu gọn */
.translation-toggle-zone {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    padding: 2px 4px;
    cursor: pointer;
    background-color: transparent; 
    opacity: 0.45;              
    transition: opacity 0.2s ease, transform 0.2s ease;
}

.translation-toggle-zone:hover {
    opacity: 1;
    transform: translateY(-1px); 
}
.chevron-icon {
    font-size: 11px;
}

/* Khung bọc nút ngôn ngữ bên phải (BẮT BUỘC có position: relative để neo popup) */
.translation-lang-wrapper {
    position: relative;
    display: inline-flex;
    align-items: center;
}

/* Nút icon ngôn ngữ bên phải: rê chuột chỉ sáng icon */
.translation-lang-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    outline: none;
    color: inherit;
    opacity: 0.5;
    cursor: pointer;
    padding: 3px 6px;
    border-radius: 4px;
    transition: opacity 0.2s ease, transform 0.2s ease;
}
.translation-lang-btn:hover {
    opacity: 1;
    transform: scale(1.1);
}

.lang-icon {
    font-size: 13px;
}

/* Popup chọn ngôn ngữ */
.lang-dropdown {
    position: absolute;
    right: 0;
    width: 160px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
    padding: 4px;
    display: flex;
    flex-direction: column;
    z-index: 1000;
}

/* Ưu tiên mở xuống dưới */
.lang-dropdown--bottom {
    top: calc(100% + 6px);
    bottom: auto;
}

/* Tự động lật ngược lên trên khi sát đáy */
.lang-dropdown--top {
    bottom: calc(100% + 6px);
    top: auto;
}

/* Nếu là tin nhắn của người khác bên trái: popup bám theo lề trái nếu bong bóng hẹp */
.message--default .lang-dropdown {
    left: auto;
    right: 0;
}

.lang-search-box {
    padding: 2px 2px 5px 2px;
    border-bottom: 1px solid #f1f5f9;
    margin-bottom: 3px;
}

.lang-search-input {
    width: 100%;
    padding: 4px 6px;
    font-size: 11px;
    border: 1px solid #e2e8f0;
    border-radius: 5px;
    outline: none;
    font-family: inherit;
    box-sizing: border-box;
}

.lang-search-input:focus {
    border-color: #6366f1;
}

.lang-list-scroll {
    max-height: 180px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 1px;
}

.lang-opt {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6px 8px;
    font-size: 12px;
    font-family: 'Quicksand', sans-serif;
    font-weight: 500;
    color: #334155;
    border-radius: 5px;
    cursor: pointer;
    transition: background-color 0.15s;
}

.lang-opt:hover {
    background-color: #f1f5f9;
}

.lang-opt--active {
    color: #6366f1;
    font-weight: 600;
    background-color: #f5f3ff;
}

.opt-check {
    font-size: 10px;
}

.lang-empty {
    font-size: 11px;
    color: #94a3b8;
    text-align: center;
    padding: 8px 0;
}
</style>
