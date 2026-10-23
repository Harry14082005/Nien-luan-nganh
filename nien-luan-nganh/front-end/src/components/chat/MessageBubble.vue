<script setup>
defineProps({
    type:{
        type:String,
        default:'SendTo'
    }
    
})</script>
<template>
<div :class="['message_bubble',`message_bubble--${type}`]">
    TK
</div>

</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
.message_bubble{
     display: flex;
    font-family:"Quicksand", sans-serif; 
    font-size: 100%;
    font-weight:600;
    width: 37px;
    height: 37px;
    margin-left: 10px;
    margin-right:6px;
    border-radius: 10px;
    background-color:rgb(233, 228, 255);
    color: rgb(115, 104, 210);
    align-content: center;
    justify-content: center;
    align-items: center;
    margin-top: 20px;

}
.message_bubble--SendTo{
    display:none;
}
</style>