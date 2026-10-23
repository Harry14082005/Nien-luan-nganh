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
<div :class="['chatheader',`chatheader-${type}`]">
    <div class="left-header">
    <div class="AvatarRoom">
        <font-awesome-icon icon="thumbtack"/>
        </div>
    <div class="info-group">
        <div class="Label">{{ Label }}</div>
        <div class="Content_PinnedMessage">{{ Content_PinnedMessage }}</div>
    </div>
    </div>
    <div class="right-header">
    <font-awesome-icon icon="x"/>
    </div>
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
.chatheader{
    display: flex;
    background-color:rgb(243, 247, 255);
    padding-top:10px;
    padding-bottom:10px;
    width: 100%;
    height: 4vh;
    max-height: fit-content;
    min-height: 40px;
    justify-content: space-between;
    border-top: 1px solid rgba(110, 148, 233,0.1);
    border-bottom: 1px solid rgba(110, 148, 233,0.1);
}
.left-header{
    display: flex;
}
.right-header{
    display: flex;
    color:rgba(34, 52, 81,0.7);
    gap:10px;
    font-size: 90%;
    font-weight: 200;
    margin-right: 10px;
    align-self: center;
    justify-items: center;
    align-content: center;
}
.info-group{
    display: flex;
    flex-direction: column;
    font-size: 12px;
    font-family:"Quicksand", sans-serif; 
    margin-left: 10px;
    align-self: center;
    justify-items: center;
    align-content: center;
    justify-content: left; 
}
.Label{
    display: flex;
    font-family:"Quicksand", sans-serif; 
    font-size: 13px;
    font-weight: 600;
    color:rgb(34, 52, 81);
}
.Content_PinnedMessage{
    color:rgb(34, 52, 81);
}
</style>