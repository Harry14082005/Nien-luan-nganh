<script setup>
/**
 * =========================================================================
 * Component: ChatInput.vue
 * =========================================================================
 * - Thanh nhập liệu tin nhắn ở dưới cùng của khung chat.
 * - Hỗ trợ gửi tin nhắn bằng cách bấm Enter hoặc click vào nút PaperPlane.
 * - Tích hợp cơ chế Typing Indicator thông minh (Debounce):
 *   + Khi bắt đầu gõ ký tự đầu tiên -> gửi tín hiệu isTyping = true qua STOMP.
 *   + Nếu ngừng gõ sau 2.5 giây -> tự động gửi tín hiệu isTyping = false.
 *   + Khi bấm gửi tin nhắn -> hủy trạng thái gõ ngay lập tức để ẩn dấu nhấp nháy bên người nhận.
 */
import { ref } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useChatStore } from '@/stores/chat';

const chatStore = useChatStore();
const text = ref(''); // Nội dung văn bản đang soạn trong ô textarea

let typingTimer = null; // Bộ đếm thời gian debounce phát hiện người dùng dừng gõ
let isCurrentlyTyping = false; // Cờ ghi nhận trạng thái đang gõ phím hiện tại

/**
 * Xử lý sự kiện khi người dùng gõ phím trong ô nhập tin nhắn
 */
const handleInput = () => {
    if (text.value.trim().length > 0) {
        // Nếu trước đó chưa ở trạng thái gõ -> gửi tín hiệu bắt đầu gõ
        if (!isCurrentlyTyping) {
            isCurrentlyTyping = true;
            chatStore.sendTypingStatus(true);
        }

        // Gia hạn thêm 2.5s mỗi khi có phím mới được gõ
        clearTimeout(typingTimer);
        typingTimer = setTimeout(() => {
            isCurrentlyTyping = false;
            chatStore.sendTypingStatus(false);
        }, 2500);
    } else {
        // Nếu xóa sạch nội dung trong ô -> gửi tín hiệu dừng gõ ngay
        if (isCurrentlyTyping) {
            isCurrentlyTyping = false;
            clearTimeout(typingTimer);
            chatStore.sendTypingStatus(false);
        }
    }
};

/**
 * Xử lý gửi tin nhắn (bấm Enter hoặc click icon Gửi)
 */
const handleSend = async () => {
    const content = text.value.trim();
    if (!content) return;
    text.value = ''; // Xóa sạch ô nhập liệu ngay lập tức

    // Dừng trạng thái gõ phím ngay khi gửi tin
    clearTimeout(typingTimer);
    if (isCurrentlyTyping) {
        isCurrentlyTyping = false;
        chatStore.sendTypingStatus(false);
    }

    // Gọi action gửi tin nhắn trong Pinia Store (ưu tiên WebSocket STOMP, fallback REST API)
    await chatStore.sendChatMessage(content);
};
</script>
<template>
<div class="chatInput">
    <div class="input_area">
        <font-awesome-icon class="icon_input" id="paperClip" icon="paperclip"/>
        <font-awesome-icon class="icon_input" id="faceLaugh" icon="face-laugh-squint"/>
        <textarea 
            v-model="text" 
            placeholder="Nhập tin nhắn..." 
            spellcheck="false" 
            rows="1"
            @input="handleInput"
            @keydown.enter.exact.prevent="handleSend"
        ></textarea>
        <font-awesome-icon 
            id="paperPlane" 
            icon="paper-plane" 
            style="cursor: pointer;"
            @click="handleSend"
        />
    </div>
</div>

</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
div.input_area textarea{
    width: 90%;
    resize: none; 
    border: none;
    background-color: #f8f9fd;
    font-family:"Quicksand", sans-serif;
    line-height: 1.4;
    max-height: 120px;         /* giới hạn chiều cao, quá thì cuộn */
    overflow-y: auto;
    padding:0;
}
div.input_area textarea:focus{
    outline: none;
}
div.input_area textarea:focus + #paperPlane{
    color:#4b7bec;
}
div.input_area:focus-within{
    border-color: #4b7bec;
}

.chatInput{
    font-family:"Quicksand", sans-serif; 
    font-size: 100%;
    display: flex;
    align-items: center;
    background-color:white;
    width: 100%;
    height:100%;
    max-height: fit-content;
    justify-content: center;
    flex-shrink: 0;
    padding: 12px 2px;
    border-top:1px solid #e6eaf2;
    box-sizing: border-box;
}
.input_area{
    display: flex;
    background-color: #f8f9fd;
    padding:5px;
    width: 95%;
    font-size:100%;
    font-family:"Quicksand", sans-serif; 
    align-items: center;
    align-self: center;
    justify-self: center;
    font-size: 95%;
    border-radius: 1.25rem;
    border:1px solid #e6eaf2;
    gap:2px;
    color:#b4bccb;
    margin:0;
}
#paperPlane{
    margin-right: 5px;
    padding:5px;
    align-self: center;
    justify-self: center;
}
#paperClip{
    margin-left:3px;
}
#faceLaugh{
    margin-right: 3px;
}
#paperPlane:hover{
    background-color: #e6edfd;
    color:#4b7bec;
    border-radius: 0.5rem;
}
.icon_input{
    padding:5px;
}
.icon_input:hover{
    color:#4b7bec;
    padding:5px;
    background-color: #e6edfd;
    border-radius: 1.25rem;
}
</style>
