<script setup>
import { computed } from 'vue'

const props = defineProps({
    RoomName:{
        type:String,
        default:'Tên nhóm'
    },
    StatusRoom:{
        type:String,
        default:'6 thành viên - 3 đang hoạt động'
    },
    isOnline: {
        type: Boolean,
        default: false
    },
    isSearching: {
        type: Boolean,
        default: false
    },
    isInfoOpen: {
        type: Boolean,
        default: false
    }
})

const emit = defineEmits(['toggleSearch', 'toggleInfo'])

const isUserOnline = computed(() => {
    return Boolean(props.isOnline || props.StatusRoom === 'Đang hoạt động')
})

const getInitials = (name) => {
    if (!name) return 'CS';
    const words = name.trim().split(/\s+/);
    if (words.length === 1) return words[0].substring(0, 2).toUpperCase();
    return (words[0][0] + words[words.length - 1][0]).toUpperCase();
}
</script>
<template>
<div class="chatheader">
    <div class="left-header">
    <div class="AvatarRoom">{{ getInitials(RoomName) }}</div>
    <div class="info-group">
        <div class="RoomName">{{ RoomName }}</div>
        <div class="status-wrap">
            <span class="StatusRoom">{{ StatusRoom }}</span>
            <span v-if="isUserOnline" class="online-dot" title="Đang trực tuyến"></span>
        </div>
    </div>
    </div>
    <div class="right-header">
        <font-awesome-icon 
            class="icon" 
            :class="{ 'icon--active': isSearching }"
            icon="magnifying-glass"
            title="Tìm kiếm tin nhắn trong đoạn chat (Ctrl+F)"
            @click="emit('toggleSearch')"
        />
        <font-awesome-icon 
            class="icon" 
            :class="{ 'icon--active': isInfoOpen }"
            icon="list"
            title="Thông tin hội thoại"
            @click="emit('toggleInfo')"
        />
    </div>
</div>

</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
.AvatarRoom{
    display: flex;
    font-family:"Quicksand", sans-serif; 
    font-size: 100%;
    font-weight:600;
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background-color:rgb(233, 228, 255);
    color: rgb(115, 104, 210);
    align-self: center;
    align-content: center;
    justify-content: center;
    align-items: center;
}
.chatheader{
    display: flex;
    flex-shrink: 0;
    background-color:white;
    width: 100%;
    height: 70px;
    min-height: 70px;
    border-bottom: 1px solid #e6eaf2;
    padding: 0 24px;
    box-sizing: border-box;
    align-items: center;
    justify-content: space-between;
}
.left-header{
    display: flex;
    align-items: center;
}
.right-header{
    display: flex;
    color:rgba(34, 52, 81,0.3);
    gap:2px;
    font-size: 100%;
    font-weight: 200;
    margin-right: 5px;
    align-self: center;
    justify-items: center;
    align-content: center;
}
.info-group{
    display: flex;
    flex-direction: column;
    font-family:"Quicksand", sans-serif; 
    margin-left: 12px;
    justify-content: center; 
}
.RoomName{
    display: flex;
    font-family:"Quicksand", sans-serif; 
    font-size: 17px;
    font-weight: 600;
    color:rgb(34, 52, 81);
}
.status-wrap{
    display: flex;
    align-items: center;
    gap: 6px;
}
.StatusRoom{
    font-size:14px;
    color:#64748b;
}
.online-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background-color: #22c55e;
    flex-shrink: 0;
    box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.25);
}
.icon{
    padding:8px;
    cursor: pointer;
    border-radius: 1.25rem;
    transition: all 0.15s ease;
}
.icon:hover,
.icon--active {
    color:#4b7bec;
    padding:8px;
    background-color: #e6edfd;
}

</style>
