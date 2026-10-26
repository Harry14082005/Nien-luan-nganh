<script setup>
defineProps({
    Label:{
        type:String,
        default:'Tin nhắn đã ghim'
    },
    Content_PinnedMessage:{
        type:String,
        default:'Họp review giao diện lúc 14:00 thứ Sáu tuần này nha cả nhà thân yêu của em em yêu cả nhà nhiều lắm hihi dài đủ chưa vậy'
    }
})</script>
<template>
    <div class="ChatListSidebar">
        <div class="nameApp">
            <div>bluespace</div>
            <font-awesome-icon icon="list"/>
        </div>
        <div class="namePage">
            <div>Tin nhắn</div>
            <div>5</div>
            <div></div>
        </div>
    <!--Component search-->
    <div class="filter">
        <div>Tất cả</div>
        <div>Chưa đọc</div>
        <!--icon lọc-->
    </div>
    <div class="ChatList"></div>
    </div>
    


</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
.AvatarRoom{
display: flex;
font-family:"Quicksand", sans-serif; 
font-size: 75%;
font-weight:500;
width:30px;
height: 70%;
margin-left: 10px;
border-radius: 6px;
background-color:rgb(228, 237, 255);
color: rgb(110, 148, 233);
align-self: center;
align-content: center;
justify-content: center;
align-items: center;
}

</style>