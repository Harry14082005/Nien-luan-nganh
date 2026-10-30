<script setup>
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
        type:Int32Array,
        default:5
    },
    currentFilter:{
        type:String,
        default:"SelectAll"
    }
})
import SearchBar from '../common/SearchBar.vue';
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
            <div class="NumberOfMessages">{{ NoMessages }}</div>
            </section>
            <section class="right-namePage">
                <font-awesome-icon icon="plus"></font-awesome-icon>
            </section>
        </section>
    <div class="search_bar"><SearchBar></SearchBar></div>
    <div class="filter">
        <div class="SelectedFilter":class="{active: currentFilter ==='SelectAll'}">Tất cả</div>
        <div class="SelectedFilter":class="{active: currentFilter ==='Unread'}">Chưa đọc</div>
    </div>
    <span>GẦN ĐÂY</span>
    <div class="ChatList"></div>
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