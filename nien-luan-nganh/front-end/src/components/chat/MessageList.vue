<script setup>
/**
 * =========================================================================
 * Component: MessageList.vue
 * =========================================================================
 * - Hiển thị từng dòng tin nhắn trong khung chat (bong bóng chat, tên người gửi, thời gian).
 * - Hỗ trợ 2 kiểu hiển thị (variant):
 *   + 'default': Tin nhắn nhận từ đối phương (nằm bên trái, nền trắng xanh).
 *   + 'SendTo': Tin nhắn của chính mình gửi đi (nằm bên phải, nền xanh đậm).
 * - Tích hợp nút Ghim / Bỏ ghim (📌) hiển thị khi hover hoặc luôn sáng nếu tin đang được ghim.
 * - Gắn id="msg-{messageId}" và animation flashMessage phục vụ tính năng cuộn tới tin ghim.
 */
import MessageBubble from './MessageBubble.vue';

defineProps({
    // Mốc thời gian gửi tin nhắn (vd: 14:30)
    Time:{
        type:String,
        default:'12:35'
    },
    // Nội dung văn bản của tin nhắn
    Content_Message:{
        type:String,
        default:'Họp review giao diện lúc 14:00 thứ Sáu tuần này'
    },
    // Tên người gửi (ẩn đi nếu là tin nhắn của chính mình)
    Sender:{
        type:String,
        default:'Minh Anh'
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
    }
})

// Bắn sự kiện togglePin kèm messageId lên ChatView để thực hiện gọi API ghim / bỏ ghim
const emit = defineEmits(['togglePin'])
</script>
<template>
<div :id="messageId ? 'msg-' + messageId : null" :class="['container',`container--${variant}`]">
<MessageBubble :type="variant"></MessageBubble>
<div :class="['wrapper',`wrapper--${variant}`]">
<div class="sender" v-if="Sender">{{ Sender }}</div>
<div :class="['message',`message--${variant}`]">
    {{ Content_Message }}
</div>
<div :class="['time-row',`time-row--${variant}`]">
    <div :class="['time',`time--${variant}`]">{{ Time }}</div>
    <span 
        class="pin-btn" 
        :class="{ 'is-pinned': isPinned }" 
        :title="isPinned ? 'Bỏ ghim tin nhắn' : 'Ghim tin nhắn'"
        @click="emit('togglePin', messageId)"
    >
        <font-awesome-icon icon="thumbtack"/>
    </span>
</div>
</div>
</div>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
.container{
    display:flex;
    width: 100%;
    flex-direction: row;
}
.container--SendTo{
    flex-direction: row-reverse;
}
.message{
    display: inline-block;
    font-family:"Quicksand", sans-serif; 
    background-color:rgb(243, 247, 255);
    width: fit-content;
    max-width: 100%;
    word-break: break-word;
    overflow-wrap: break-word;
    white-space: pre-wrap;
    height: fit-content;
    border: 1px solid rgba(110, 148, 233,0.1);
    border-radius: 0.2rem 1.2rem 1.2rem;
    margin-top: 5px;
    padding:15px;
}
.message--SendTo{
    display: inline-block;
    font-family:"Quicksand", sans-serif; 
    color:white;
    background-color:#4b7bec;
    width: fit-content;
    max-width: 100%;
    word-break: break-word;
    overflow-wrap: break-word;
    white-space: pre-wrap;
    height: fit-content;
    border-radius: 1.2rem 0.2rem 1.2rem 1.2rem;
    margin-top: 5px;
    padding:15px;
    margin-right: 5px;
}
.time-row{
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 2px;
}
.time-row--SendTo{
    flex-direction: row-reverse;
}
.time{
    display: flex;
    font-family:"Quicksand", sans-serif; 
    margin-left: 10px;
    font-size: 11px;
    color: #a0aec0;
}
.time--SendTo{
    display:flex;
    margin-left: auto;
    margin-right:10px;
}
.pin-btn{
    opacity: 0;
    cursor: pointer;
    font-size: 11px;
    color: #a0aec0;
    transition: opacity 0.15s ease, color 0.15s ease;
    padding: 2px 4px;
}
.wrapper:hover .pin-btn,
.pin-btn.is-pinned{
    opacity: 1;
}
.pin-btn.is-pinned{
    color: #4b7bec;
}
.pin-btn:hover{
    color: #2e59d9;
}
.sender{
    font-family:"Quicksand", sans-serif; 
    font-weight: 600;
    margin-left: 7px;
    margin-top: 10px;
}
.wrapper{
    display:flex;
    flex-direction: column;
    margin-top:10px;
    max-width: 70%;
}
.wrapper--SendTo{
    display:flex;
    align-items: flex-end;
    max-width: 70%;
}
.highlight-msg .message{
    animation: flashMessage 1.5s ease-out;
}
@keyframes flashMessage{
    0%, 100% { filter: brightness(1); }
    30% { filter: brightness(1.25); box-shadow: 0 0 12px rgba(75, 123, 236, 0.5); }
}
</style>
