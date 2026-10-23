<script setup>
import MessageBubble from './MessageBubble.vue';
defineProps({
    Time:{
        type:String,
        default:'12:35'
    },
    Content_Message:{
        type:String,
        default:'Họp review giao diện lúc 14:00 thứ Sáu tuần này'
    },
    Sender:{
        type:String,
        default:'Minh Anh'
    },
    variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'SendTo'].includes(v),
  }
})</script>
<template>
<div :class="['container',`container--${variant}`]">
<MessageBubble :type="variant"></MessageBubble>
<div :class="['wrapper',`wrapper--${variant}`]">
<div class="sender">{{ Sender }}</div>
<div :class="['message',`message--${variant}`]">
    {{ Content_Message }}
    </div>
<div :class="['time',`time--${variant}`]">{{ Time }}</div>
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
    display: flex;
    font-family:"Quicksand", sans-serif; 
    background-color:rgb(243, 247, 255);
    width: fit-content;
    height: fit-content;
    border: 1px solid rgba(110, 148, 233,0.1);
    border-radius: 0.2rem 1.2rem 1.2rem;
    margin-top: 5px;
    padding:15px;
    justify-content: center;
    align-content: center;
}
.message--SendTo{
    display: flex;
    float: right;
    font-family:"Quicksand", sans-serif; 
    color:white;
    background-color:#4b7bec;
    width: fit-content;
    height: fit-content;
    border-radius:  1.2rem 0.2rem 1.2rem 1.2rem;
    margin-top: 5px;
    padding:15px;
    justify-content: center;
    align-content: center;
    margin-right: 5px;
    justify-self: left;
}
.time{
    display: flex;
    font-family:"Quicksand", sans-serif; 
    margin-left: 10px;
    margin-top: 2px;
}
.time--SendTo{
    display:flex;
    margin-left: auto;
    margin-right:10px;
    float: right;
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
}
.wrapper--SendTo{
    display:flex;
    float:right;
}
</style>