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
import { ref, computed, watch, nextTick } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useChatStore, getReplySnippet } from '@/stores/chat';
import { useAuthStore } from '@/stores/auth';

const chatStore = useChatStore();
const authStore = useAuthStore();
const emit = defineEmits(['messageSent']);
const text = ref(''); // Nội dung văn bản đang soạn trong ô textarea
const drafts = ref({}); // Lưu trữ nội dung nháp riêng cho từng phòng chat: { [roomId]: string }
const textareaRef = ref(null); // Ref tới ô textarea để tự động focus
const imageInputRef = ref(null);
const fileInputRef = ref(null);

// MẢNG LƯU TRỮ CÁC FILE/ẢNH ĐANG CHỜ GỬI (ĐÃ CHỌN)
const selectedFiles = ref([]);

let typingTimer = null; // Bộ đếm thời gian debounce phát hiện người dùng dừng gõ
let isCurrentlyTyping = false; // Cờ ghi nhận trạng thái đang gõ phím hiện tại

/**
 * Kiểm tra xem tin nhắn đang được phản hồi có phải của chính mình hay không
 */
const isReplyingToMe = computed(() => {
    if (!chatStore.replyingMessage) return false;
    const currentUserId = authStore.user?.userId;
    const currentUsername = authStore.user?.username;
    const senderId = chatStore.replyingMessage.sender?.userId;
    const senderUsername = chatStore.replyingMessage.sender?.username;
    return (currentUserId && String(senderId) === String(currentUserId)) ||
           (currentUsername && senderUsername && currentUsername === senderUsername);
});

//xử lý hiện hôp thoại khi click
const openImagePicker = () => {
    imageInputRef.value?.click();
};

const openFilePicker = () => {
    fileInputRef.value?.click();
};

/**
 * Tự động focus vào ô textarea khi người dùng bấm Trả lời
 */
watch(
    () => chatStore.replyingMessage,
    (newVal) => {
        if (newVal) {
            nextTick(() => {
                textareaRef.value?.focus();
            });
        }
    }
);

/**
 * Theo dõi sự thay đổi phòng chat:
 * - Khi chuyển phòng -> hủy trạng thái Reply để tránh gửi nhầm repliedMessageId sang phòng khác
 * - Khi chuyển khỏi phòng A -> lưu nội dung đang gõ vào drafts của phòng A, tắt typing indicator
 * - Khi mở phòng B -> khôi phục nội dung nháp của phòng B (nếu có) hoặc để trống
 */
watch(
    () => chatStore.activeRoomId,
    (newRoomId, oldRoomId) => {
        // Hủy trạng thái reply khi chuyển phòng
        chatStore.clearReplyingMessage();

        // 1. Tắt trạng thái typing của phòng cũ nếu đang gõ dở
        if (isCurrentlyTyping) {
            clearTimeout(typingTimer);
            isCurrentlyTyping = false;
            chatStore.sendTypingStatus(false);
        }

        // 2. Lưu lại nội dung chưa gửi của phòng cũ
        if (oldRoomId) {
            drafts.value[String(oldRoomId)] = text.value;
        }

        // 3. Khôi phục lại nội dung nháp riêng của phòng mới
        if (newRoomId) {
            text.value = drafts.value[String(newRoomId)] || '';
        } else {
            text.value = '';
        }
    }
);

// 2. KHI CHỌN ẢNH: Đưa vào mảng chờ (hỗ trợ chọn dồn)
const handleImageSelected = (event) => {
    const files = Array.from(event.target.files);
    if (files.length === 0) return;

    for (const file of files) {
        selectedFiles.value.push({
            file,
            type: 'IMAGE',
            previewUrl: URL.createObjectURL(file), // Tạo link tạm để hiện thumbnail
            name: file.name
        });
    }
    event.target.value = '';
};

// 3. KHI CHỌN FILE: Đưa vào mảng chờ
const handleFileSelected = (event) => {
    const files = Array.from(event.target.files);
    if (files.length === 0) return;

    for (const file of files) {
        selectedFiles.value.push({
            file,
            type: 'FILE',
            name: file.name
        });
    }
    event.target.value = '';
};

// 4. XÓA 1 FILE TRONG DANH SÁCH CHỜ (Bấm nút X ở tag)
const removeSelectedFile = (index) => {
    const item = selectedFiles.value[index];
    if (item && item.previewUrl) {
        URL.revokeObjectURL(item.previewUrl);
    }
    selectedFiles.value.splice(index, 1);
};


/**
 * Xử lý sự kiện khi người dùng gõ phím trong ô nhập tin nhắn
 */
const handleInput = () => {
    // Lưu lại nháp ngay lập tức cho phòng hiện tại
    if (chatStore.activeRoomId) {
        drafts.value[String(chatStore.activeRoomId)] = text.value;
    }

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
    const hasFiles = selectedFiles.value.length > 0;

    if (!content && !hasFiles) return;

    const roomId = chatStore.activeRoomId;
    const repliedMessageId = chatStore.replyingMessage?.messageId || null;

    if (!roomId) return;

    // Reset trạng thái
    text.value = '';
    const filesToSend = [...selectedFiles.value];
    selectedFiles.value = [];
    chatStore.clearReplyingMessage();

    if (chatStore.activeRoomId) {
        delete drafts.value[String(chatStore.activeRoomId)];
    }

    clearTimeout(typingTimer);
    if (isCurrentlyTyping) {
        isCurrentlyTyping = false;
        chatStore.sendTypingStatus(false);
    }

    emit('messageSent');

    // Gửi text trước nếu có
    if (content) {
        await chatStore.sendChatMessage(content);
    }

    // Gửi lần lượt các file/ảnh trong hàng đợi
    for (const item of filesToSend) {
        try {
            if (item.type === 'IMAGE') {
                await chatStore.sendImageMessage(roomId, item.file, repliedMessageId);
            } else {
                await chatStore.sendFileMessage(roomId, item.file, repliedMessageId);
            }
        } catch (err) {
            console.error('Gửi file thất bại:', item.name, err);
        }
    }
};

</script>
<template>
  <div class="chatInput-wrapper">
    <div class="chatInput">
      <!-- Thanh xem trước phản hồi tin nhắn (Reply Banner) -->
      <div v-if="chatStore.replyingMessage" class="reply-banner">
        <div class="reply-banner-left">
          <font-awesome-icon icon="reply" class="reply-banner-icon" />
          <div class="reply-banner-info">
            <div class="reply-banner-title">
              {{ isReplyingToMe ? 'Đang trả lời chính bạn' : `Đang trả lời @${chatStore.replyingMessage.sender?.username || 'Thành viên'}` }}
            </div>
            <div class="reply-banner-snippet">
              {{ getReplySnippet(chatStore.replyingMessage) }}
            </div>
          </div>
        </div>
        <button 
          type="button" 
          class="reply-banner-close-btn" 
          title="Hủy trả lời (Esc)" 
          @click="chatStore.clearReplyingMessage"
        >
          <font-awesome-icon icon="x" />
        </button>
      </div>

      <!-- VÙNG HIỂN THỊ CÁC TAG THUMBNAIL ĐANG CHỜ GỬI -->
      <div v-if="selectedFiles.length > 0" class="pending-files-bar">
        <div v-for="(item, index) in selectedFiles" :key="index" class="pending-file-tag">
          <div v-if="item.type === 'IMAGE'" class="pending-tag-thumb">
            <img :src="item.previewUrl" alt="preview" />
          </div>
          <div v-else class="pending-tag-file-icon">
            <font-awesome-icon icon="file-lines" />
          </div>

          <span class="pending-tag-name" :title="item.name">{{ item.name }}</span>
          
          <button type="button" class="pending-tag-remove" @click="removeSelectedFile(index)" title="Xóa">
            <font-awesome-icon icon="x" />
          </button>
        </div>
      </div>

      <div class="input_area">
      <font-awesome-icon 
          class="icon_input" 
          id="image" 
          :icon="['fas', 'image']" 
          @click="openImagePicker" 
      />

      <font-awesome-icon 
          class="icon_input" 
          id="paperClip" 
          icon="paperclip" 
          @click="openFilePicker" 
      />
      
      <font-awesome-icon class="icon_input" id="faceLaugh" icon="face-laugh-squint" title="Chọn biểu cảm" />
      
      <textarea 
          ref="textareaRef"
          v-model="text" 
          placeholder="Nhập tin nhắn..." 
          spellcheck="false" 
          rows="1"
          @input="handleInput"
          @keydown.enter.exact.prevent="handleSend"
          @keydown.esc="chatStore.clearReplyingMessage"
      ></textarea>
      <font-awesome-icon 
          id="paperPlane" 
          icon="paper-plane" 
          style="cursor: pointer;"
          @click="handleSend"
      />
  </div>
    </div>

    <!-- ĐÃ ĐƯA 2 THẺ INPUT VÀO BÊN TRONG THẺ GỐC CHUNG -->
    <input 
        id="hiddenImageInput"
        ref="imageInputRef" 
        type="file" 
        accept="image/*" 
        multiple 
        style="display: none;" 
        @change="handleImageSelected" 
    />
    <input 
        id="hiddenFileInput"
        ref="fileInputRef" 
        type="file" 
        multiple 
        style="display: none;" 
        @change="handleFileSelected" 
    />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.chatInput {
  font-family: 'Quicksand', sans-serif;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  width: 100%;
  box-sizing: border-box;
  padding: 12px 20px 16px 20px;
  background-color: white;
  border-top: 1px solid #e6edfd;
  gap: 8px;
}

.reply-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f1f5f9;
  border-left: 3.5px solid #4b7bec;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  animation: slideDown 0.18s ease-out;
}

.reply-banner-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.reply-banner-icon {
  color: #4b7bec;
  font-size: 13px;
  flex-shrink: 0;
}

.reply-banner-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  line-height: 1.3;
}

.reply-banner-title {
  font-weight: 700;
  color: #334155;
  font-size: 12px;
}

.reply-banner-snippet {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reply-banner-close-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  font-size: 13px;
  padding: 4px 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  flex-shrink: 0;
  margin-left: 8px;
}

.reply-banner-close-btn:hover {
  color: #e02424;
  background-color: #fee2e2;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.input_area {
  display: flex;
  align-items: center;
  background-color: #f8f9fd;
  padding: 5px 12px;
  width: 100%;
  border-radius: 1.5rem;
  border: 1px solid #e6eaf2;
  gap: 6px;
  color: #94a3b8;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.input_area:focus-within {
  border-color: #4b7bec;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(75, 123, 236, 0.1);
}

.input_area textarea {
  flex: 1;
  resize: none;
  border: none;
  background-color: transparent;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  color: #1e293b;
  line-height: 1.4;
  max-height: 100px;
  overflow-y: auto;
  padding: 4px 6px;
  outline: none;
}

.input_area textarea::placeholder {
  color: #94a3b8;
  font-size: 14px;
}

.icon_input {
  font-size: 15px;
  color: #94a3b8;
  padding: 6px;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.icon_input:hover {
  color: #4b7bec;
  background-color: #e6edfd;
}

#paperPlane {
  font-size: 15px;
  color: #94a3b8;
  padding: 6px;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

#paperPlane:hover {
  background-color: #e6edfd;
  color: #4b7bec;
}

.input_area textarea:focus + #paperPlane,
.input_area textarea:not(:placeholder-shown) + #paperPlane {
  color: #4b7bec;
}

.pending-files-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 4px 2px;
  max-height: 90px;
  scrollbar-width: thin;
}

.pending-file-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 4px 8px;
  position: relative;
  flex-shrink: 0;
  max-width: 180px;
}

.pending-tag-thumb {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  overflow: hidden;
  background: #e2e8f0;
  flex-shrink: 0;
}

.pending-tag-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pending-tag-file-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  background: #e0e7ff;
  border-radius: 4px;
  font-size: 14px;
  flex-shrink: 0;
}

.pending-tag-name {
  font-size: 12px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}

.pending-tag-remove {
  background: rgba(0, 0, 0, 0.1);
  border: none;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 10px;
  color: #475569;
  transition: all 0.15s;
  flex-shrink: 0;
}

.pending-tag-remove:hover {
  background: #ef4444;
  color: white;
}
</style>
