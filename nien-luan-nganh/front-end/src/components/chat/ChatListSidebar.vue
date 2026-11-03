<script setup>
/**
 * =========================================================================
 * Component: ChatListSidebar.vue
 * =========================================================================
 * - Cột danh sách các cuộc trò chuyện bên trái (Sidebar).
 * - Hiển thị danh sách phòng chat kèm avatar, tên phòng, tin nhắn cuối, mốc thời gian, số tin chưa đọc.
 * - Tự động hiển thị chấm xanh Online/Offline của đối phương cho phòng 1-1.
 * - Tự động hiển thị "✍️ [Tên] đang soạn..." khi đối phương đang gõ chữ.
 * - Tự động hiển thị "📌 [Tên] đã ghim 1 tin nhắn" khi có sự kiện ghim tin nhắn.
 * - Hỗ trợ lọc theo tab "Tất cả" / "Chưa đọc" và tìm kiếm theo tên phòng / nội dung tin nhắn.
 * - Nút "+" mở Modal tạo cuộc trò chuyện 1-1 mới hoặc tạo Nhóm chat mới.
 */
import { ref, computed, onMounted, onUnmounted } from 'vue';
import ChatItem from './ChatItem.vue';
import NewChatModal from './NewChatModal.vue';
import SearchBar from '../common/SearchBar.vue';
import { useChatStore } from '@/stores/chat';
import { useAuthStore } from '@/stores/auth';
import { initGlobalWebSocket } from '@/services/websocket';

defineProps({
    Label:{
        type:String,
        default:'Tin nhắn đã ghim'
    },
    Content_PinnedMessage:{
        type:String,
        default:'Họp review giao diện lúc 14:00 thứ Sáu tuần này nha cả nhà thân yêu của em em yêu cả nhà nhiều lắm hihi dài đủ chưa vậy'
    },
    NoMessages:{
        type:[Number, Object],
        default:5
    },
    currentFilter:{
        type:String,
        default:"SelectAll"
    }
})

const chatStore = useChatStore();
const authStore = useAuthStore();

const activeFilter = ref('SelectAll'); // 'SelectAll' | 'Unread'
const searchQuery = ref(''); // Từ khóa tìm kiếm phòng chat
const showNewChatModal = ref(false); // Trạng thái hiển thị popup tạo chat mới
let syncTimer = null; // Timer chạy ngầm đồng bộ phòng định kỳ mỗi 4 giây

onMounted(async () => {
    // 1. Tải danh sách phòng từ Backend
    await chatStore.fetchRooms();

    // 2. Kích hoạt kết nối WebSocket STOMP toàn cục nếu đã có Access Token
    if (authStore.accessToken) {
        initGlobalWebSocket(authStore.accessToken);
    }

    // 3. Đồng bộ ngầm mỗi 4s để tự động nhận phòng mới khi người khác tạo phòng với mình mà không cần F5
    syncTimer = setInterval(() => {
        chatStore.syncRoomsSilently();
    }, 4000);
});

onUnmounted(() => {
    // Dọn dẹp timer khi component bị unmount
    if (syncTimer) clearInterval(syncTimer);
});

/**
 * Định dạng thời gian hiển thị gọn gàng (vd: 14:30 hôm nay, Hôm qua, hoặc 02/11)
 * @param {string} isoString Chuỗi thời gian chuẩn ISO
 */
function formatTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return '';

    const now = new Date();
    const isToday =
        date.getDate() === now.getDate() &&
        date.getMonth() === now.getMonth() &&
        date.getFullYear() === now.getFullYear();

    if (isToday) {
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
    }

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const isYesterday =
        date.getDate() === yesterday.getDate() &&
        date.getMonth() === yesterday.getMonth() &&
        date.getFullYear() === yesterday.getFullYear();

    if (isYesterday) return 'Hôm qua';

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${day}/${month}`;
}

/**
 * Tính toán nội dung văn bản hiển thị cho dòng tin nhắn cuối của phòng chat trên Sidebar
 * Ưu tiên:
 * 1. Trạng thái đang soạn tin nhắn (Typing indicator)
 * 2. Thông báo ghim tin nhắn ("A đã ghim 1 tin nhắn")
 * 3. Nội dung tin nhắn bình thường kèm tên người gửi
 */
function getLastMessageText(room) {
    if (!room) return 'Chưa có tin nhắn';

    // 1. Kiểm tra xem có thành viên nào đang soạn tin nhắn trong phòng này không
    const roomTyping = chatStore.typingMap[String(room.roomId)];
    if (roomTyping) {
        const users = Object.values(roomTyping);
        if (users.length > 0) {
            return users.length === 1
                ? `✍️ ${users[0].username} đang soạn...`
                : '✍️ Đang soạn tin nhắn...';
        }
    }

    if (!room.lastMessageContent) return 'Chưa có tin nhắn';

    // 2. Hiển thị thông báo khi có tin nhắn được ghim: "A đã ghim 1 tin nhắn"
    if (room.lastMessageContent.includes('đã ghim')) {
        const isMyAction = authStore.user?.username && room.lastMessageSenderName === authStore.user.username;
        const sender = isMyAction ? 'Bạn' : (room.lastMessageSenderName || 'Thành viên');
        return `📌 ${sender} đã ghim 1 tin nhắn`;
    }

    // 3. Tin nhắn bình thường
    if (room.lastMessageSenderName) {
        return `${room.lastMessageSenderName}: ${room.lastMessageContent}`;
    }
    return room.lastMessageContent;
}

/**
 * Danh sách phòng chat sau khi lọc theo Tab (Tất cả / Chưa đọc) và Từ khóa tìm kiếm
 */
const displayRooms = computed(() => {
    let list = chatStore.rooms;

    if (activeFilter.value === 'Unread') {
        list = list.filter(r => r.hasUnread || (r.unreadCount && r.unreadCount > 0));
    }

    const q = searchQuery.value.trim().toLowerCase();
    if (q) {
        list = list.filter(r =>
            (r.roomName && r.roomName.toLowerCase().includes(q)) ||
            (r.lastMessageContent && r.lastMessageContent.toLowerCase().includes(q))
        );
    }

    return list;
});
</script>

<template>
    <div class="ChatListSidebar">
        <section class="nameApp">
            <div>bluespace</div>
            <font-awesome-icon icon="bars"/>
        </section>
        <section class="namePage">
            <section class="left-namePage">
            <div>Tin nhắn</div>
            <div class="NumberOfMessages">{{ chatStore.totalUnreadCount }}</div>
            </section>
            <section class="right-namePage" style="cursor: pointer;" title="Tạo cuộc trò chuyện / nhóm mới" @click="showNewChatModal = true">
                <font-awesome-icon icon="plus"></font-awesome-icon>
            </section>
        </section>
    <div class="search_bar"><SearchBar v-model="searchQuery"></SearchBar></div>
    <div class="filter">
        <div class="SelectedFilter" :class="{active: activeFilter ==='SelectAll'}" @click="activeFilter = 'SelectAll'">Tất cả</div>
        <div class="SelectedFilter" :class="{active: activeFilter ==='Unread'}" @click="activeFilter = 'Unread'">Chưa đọc</div>
    </div>
    <span>GẦN ĐÂY</span>
    <div class="ChatList">
        <ChatItem
        v-for="c in displayRooms"
        :key="c.roomId"
        :name="c.roomName || 'Cuộc trò chuyện'"
        :avatar="c.roomAvatarURL || ''"
        :last-message="getLastMessageText(c)"
        :time="formatTime(c.lastMessageAt)"
        :unread="c.unreadCount || 0"
        :online="c.roomType === 'DIRECT' ? !!c.isOnline : false"
        :active="c.roomId === chatStore.activeRoomId"
        @select="chatStore.selectRoom(c.roomId)"
        />
    </div>
    <NewChatModal v-if="showNewChatModal" @close="showNewChatModal = false" />
    </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
.ChatListSidebar{
display: flex;
flex-direction: column;
font-family:"Quicksand", sans-serif; 
font-size: 75%;
font-weight:500;
width:430px;
height: 100%;
border-radius: 6px;
color: rgb(110, 148, 233);
}
.nameApp{
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    font-size: 26px;
    margin-top: 3px;
    margin-left: 20px;
    padding:16px;
    color:#b4bccb;
background: linear-gradient(
    to right, 
    #0a192f 0%,    /* 1. Xanh thẫm (Navy cực đậm) */
    #1e3a8a 25%,   /* 2. Xanh than */
    #2563eb 50%,   /* 3. Xanh dương trung tính */
    #3b82f6 75%,   /* 4. Xanh lam sáng */
    #60a5fa 100%   /* 5. Xanh nhạt cuối dải */
  );  
  /* Cắt nền theo khung chữ và làm trong suốt màu chữ gốc */
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.nameApp:first-child{
    font-weight: 700;
}

.namePage{
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-right: 20px;
}
.left-namePage{
    display: flex;
    font-size: 26px;
    float:left;
    color:#223451;
    font-weight: 700;
    margin-left: 20px;
    padding:10px 20px;
}
.right-namePage{
    background-color: #e6edfd;
    padding:9px;
    border-radius: 0.7rem;
    cursor:pointer;
}
.NumberOfMessages{
    font-size: 20px;
    font-weight: 600;
    margin-left: 8px;
    color:#4b7bec;
    background-color: #e6edfd;
    border-radius: 0.7rem;
    width: 26px;
    align-content: center;
    text-align: center;
}
.filter{
    display:flex;
    gap:10px;
    font-size:15px;
    margin-left: 20px;
    padding-left:20px;
    padding-top:8px;
    padding-bottom:15px;
    border-bottom:1px solid#e6edfd ;
}
.SelectedFilter{
    padding:7px;
    cursor:pointer;
    color:#4a5a78;
}
.SelectedFilter.active{
    border-radius: 0.8rem;
    background-color: #e6edfd;
    padding:7px;
    cursor:pointer;
    color:#2f5fd0;
}
.search_bar{
    display: flex;
    align-self: center;
    width: 95%;
    padding: 0 20px;
    box-sizing: border-box;
    margin-left: 20px;
    margin-bottom: 10px;
}
span{
    margin-left: 20px;
    padding: 0 20px;
    margin-top:8px;
    font-size: 15px;
    font-weight: 700;
    color:#b4bccb;
    margin-bottom: 8px;
}

</style>