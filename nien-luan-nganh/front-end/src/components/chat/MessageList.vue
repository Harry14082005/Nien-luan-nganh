<script setup>
/**
 * =========================================================================
 * Component: MessageList.vue
 * =========================================================================
 * - Hiển thị từng dòng tin nhắn trong khung chat (bong bóng chat, tên người gửi, thời gian).
 * - Hỗ trợ 2 kiểu hiển thị (variant):
 *   + 'default': Tin nhắn nhận từ đối phương (nằm bên trái, nền trắng xanh).
 *   + 'SendTo': Tin nhắn của chính mình gửi đi (nằm bên phải, nền xanh đậm).
 * - Tích hợp nút Ghim / Bỏ ghim (📌).
 * - Tích hợp nút 3 chấm (...) mở menu cho tin nhắn của mình:
 *   + ✏️ Chỉnh sửa nội dung (Edit Message) trực tiếp dạng inline.
 *   + 🗑️ Thu hồi tin nhắn (Revoke Message) -> biến thành "Tin nhắn đã được thu hồi".
 * - Gắn id="msg-{messageId}" và animation flashMessage phục vụ tính năng cuộn tới tin ghim.
 */
import { ref, onMounted, onUnmounted } from 'vue';
import MessageBubble from './MessageBubble.vue';

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
    // Trạng thái tin nhắn đã từng được chỉnh sửa hay chưa
    isEdited: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['togglePin', 'editMessage', 'revokeMessage']);

const showMenu = ref(false); // Trạng thái mở popup tùy chọn 3 chấm
const isEditing = ref(false); // Trạng thái đang chỉnh sửa inline
const editContent = ref(''); // Nội dung văn bản trong ô sửa

// Bật chế độ chỉnh sửa inline
const handleStartEdit = () => {
    showMenu.value = false;
    editContent.value = props.Content_Message;
    isEditing.value = true;
};

// Lưu nội dung sau khi chỉnh sửa
const handleSaveEdit = () => {
    const trimmed = editContent.value.trim();
    if (trimmed && trimmed !== props.Content_Message) {
        emit('editMessage', { messageId: props.messageId, content: trimmed });
    }
    isEditing.value = false;
};

// Hủy bỏ chỉnh sửa
const handleCancelEdit = () => {
    isEditing.value = false;
    editContent.value = '';
};

// Kích hoạt thu hồi tin nhắn
const handleRevoke = () => {
    showMenu.value = false;
    emit('revokeMessage', props.messageId);
};

// Tự động đóng menu 3 chấm khi click ra ngoài
const handleDocumentClick = () => {
    showMenu.value = false;
};

onMounted(() => {
    document.addEventListener('click', handleDocumentClick);
});

onUnmounted(() => {
    document.removeEventListener('click', handleDocumentClick);
});
</script>

<template>
<div :id="messageId ? 'msg-' + messageId : null" :class="['container', `container--${variant}`]">
    <MessageBubble :type="variant"></MessageBubble>
    <div :class="['wrapper', `wrapper--${variant}`]">
        <div class="sender" v-if="Sender">{{ Sender }}</div>

        <!-- Trạng thái tin nhắn đã bị thu hồi -->
        <div v-if="isRevoked" :class="['message', 'message--revoked']">
            <i>Tin nhắn đã được thu hồi</i>
        </div>

        <!-- Trạng thái đang chỉnh sửa trực tiếp (Inline Edit) -->
        <div v-else-if="isEditing" class="edit-box">
            <textarea 
                v-model="editContent" 
                class="edit-textarea" 
                rows="2" 
                @keydown.enter.exact.prevent="handleSaveEdit" 
                @keydown.esc="handleCancelEdit"
                autofocus
            ></textarea>
            <div class="edit-actions">
                <button class="btn-cancel" @click="handleCancelEdit">Hủy</button>
                <button class="btn-save" @click="handleSaveEdit">Lưu</button>
            </div>
        </div>

        <!-- Tin nhắn bình thường -->
        <div 
            v-else 
            :class="['message', `message--${variant}`]"
            @dblclick="variant === 'SendTo' ? handleStartEdit() : null"
            :title="variant === 'SendTo' ? 'Bấm 2 lần để sửa nhanh' : ''"
        >
            {{ Content_Message }}
        </div>

        <!-- Dòng thời gian, nhãn đã sửa, icon ghim và nút 3 chấm (...) -->
        <div class="time-row" :class="`time-row--${variant}`">
            <div :class="['time', `time--${variant}`]">{{ Time }}</div>
            <span v-if="isEdited && !isRevoked" class="edited-badge">(đã chỉnh sửa)</span>

            <!-- Nút Ghim tin nhắn (chỉ hiện khi tin chưa bị thu hồi) -->
            <span 
                v-if="!isRevoked"
                class="pin-btn" 
                :class="{ 'is-pinned': isPinned }" 
                :title="isPinned ? 'Bỏ ghim tin nhắn' : 'Ghim tin nhắn'"
                @click="emit('togglePin', messageId)"
            >
                <font-awesome-icon icon="thumbtack"/>
            </span>

            <!-- Nút 3 chấm mở tùy chọn: Sửa / Thu hồi (chỉ dành cho tin của chính mình) -->
            <div v-if="variant === 'SendTo' && !isRevoked" class="menu-container">
                <span 
                    class="more-btn" 
                    title="Tùy chọn tin nhắn" 
                    @click.stop="showMenu = !showMenu"
                >
                    <font-awesome-icon icon="ellipsis-vertical"/>
                </span>

                <!-- Menu xổ xuống -->
                <div v-if="showMenu" class="action-dropdown" @click.stop>
                    <div class="dropdown-opt" @click="handleStartEdit">
                        <font-awesome-icon icon="pen" class="opt-icon"/>
                        <span>Chỉnh sửa</span>
                    </div>
                    <div class="dropdown-opt opt-danger" @click="handleRevoke">
                        <font-awesome-icon icon="trash" class="opt-icon"/>
                        <span>Thu hồi</span>
                    </div>
                </div>
            </div>
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
}

.container--SendTo {
    flex-direction: row-reverse;
}

.message {
    display: inline-block;
    font-family: "Quicksand", sans-serif; 
    background-color: rgb(243, 247, 255);
    width: fit-content;
    max-width: 100%;
    word-break: break-word;
    overflow-wrap: break-word;
    white-space: pre-wrap;
    height: fit-content;
    border: 1px solid rgba(110, 148, 233, 0.1);
    border-radius: 0.2rem 1.2rem 1.2rem;
    margin-top: 5px;
    padding: 12px 16px;
    color: #1e293b;
    box-sizing: border-box;
}

.message--SendTo {
    color: white;
    background-color: #4b7bec;
    border-radius: 1.2rem 0.2rem 1.2rem 1.2rem;
    margin-right: 5px;
}

/* Tin nhắn đã bị thu hồi */
.message--revoked {
    background-color: #f1f5f9 !important;
    color: #94a3b8 !important;
    border: 1px dashed #cbd5e1 !important;
    font-size: 13px;
    border-radius: 12px !important;
}

/* Khung chỉnh sửa inline */
.edit-box {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 240px;
    max-width: 100%;
    margin-top: 5px;
    margin-right: 5px;
}

.edit-textarea {
    width: 100%;
    padding: 8px 12px;
    border: 1.5px solid #4b7bec;
    border-radius: 8px;
    font-family: "Quicksand", sans-serif;
    font-size: 14px;
    color: #1e293b;
    outline: none;
    resize: none;
    box-sizing: border-box;
    background-color: #ffffff;
}

.edit-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
}

.btn-cancel {
    padding: 4px 12px;
    font-size: 12px;
    border-radius: 6px;
    border: 1px solid #e2e8f0;
    background: #ffffff;
    color: #64748b;
    cursor: pointer;
    font-family: "Quicksand", sans-serif;
    font-weight: 600;
}

.btn-save {
    padding: 4px 12px;
    font-size: 12px;
    border-radius: 6px;
    border: none;
    background: #4b7bec;
    color: #ffffff;
    cursor: pointer;
    font-family: "Quicksand", sans-serif;
    font-weight: 600;
}

.btn-save:hover {
    background: #3867d6;
}

.time-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 2px;
}

.time-row--SendTo {
    flex-direction: row-reverse;
}

.time {
    display: flex;
    font-family: "Quicksand", sans-serif; 
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
    font-family: "Quicksand", sans-serif;
    color: #94a3b8;
    font-style: italic;
}

/* Nút Ghim */
.pin-btn {
    opacity: 0;
    cursor: pointer;
    font-size: 11px;
    color: #a0aec0;
    transition: opacity 0.15s ease, color 0.15s ease;
    padding: 2px 4px;
}

.wrapper:hover .pin-btn,
.pin-btn.is-pinned {
    opacity: 1;
}

.pin-btn.is-pinned {
    color: #4b7bec;
}

.pin-btn:hover {
    color: #2e59d9;
}

/* Nút 3 chấm (...) và Menu xổ xuống */
.menu-container {
    position: relative;
    display: inline-flex;
    align-items: center;
}

.more-btn {
    opacity: 0;
    cursor: pointer;
    font-size: 11px;
    color: #a0aec0;
    transition: opacity 0.15s ease, color 0.15s ease;
    padding: 2px 4px;
}

.wrapper:hover .more-btn {
    opacity: 1;
}

.more-btn:hover {
    color: #1e293b;
}

.action-dropdown {
    position: absolute;
    bottom: 100%;
    right: 0;
    margin-bottom: 6px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    min-width: 120px;
    z-index: 50;
    padding: 4px;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.dropdown-opt {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 10px;
    font-size: 12px;
    font-family: "Quicksand", sans-serif;
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

.sender {
    font-family: "Quicksand", sans-serif; 
    font-weight: 600;
    margin-left: 7px;
    margin-top: 10px;
}

.wrapper {
    display: flex;
    flex-direction: column;
    margin-top: 10px;
    max-width: 70%;
}

.wrapper--SendTo {
    display: flex;
    align-items: flex-end;
    max-width: 70%;
}

.highlight-msg .message {
    animation: flashMessage 1.5s ease-out;
}

@keyframes flashMessage {
    0%, 100% { filter: brightness(1); }
    30% { filter: brightness(1.25); box-shadow: 0 0 12px rgba(75, 123, 236, 0.5); }
}
</style>
