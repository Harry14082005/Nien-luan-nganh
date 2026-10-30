<script setup>
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

defineProps({

})</script>
<template>
<div class="chatInput">
    <div class="input_area">
        <font-awesome-icon class="icon_input" id="paperClip" icon="paperclip"/>
        <font-awesome-icon class="icon_input" id="faceLaugh" icon="face-laugh-squint"/>
        <textarea spellcheck="false" rows="1"></textarea>
        <font-awesome-icon id="paperPlane" icon="paper-plane"/>
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