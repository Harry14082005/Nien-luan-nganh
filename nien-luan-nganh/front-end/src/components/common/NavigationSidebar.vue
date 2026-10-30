<script setup>
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

defineProps({
  currentPage: {
    type: String,
    default:'message'
  }
})</script>
<template>
<div class="sidebar">
    <font-awesome-icon class="logo" icon="comment"/>
    <div class="navigation">
        <div class="nav-item" :class="{active: currentPage ==='home'}">
            <div class="icon-box">
                <font-awesome-icon class="nav-icon":icon="['far', 'house']"/>
                <span class="dot"></span>
            </div>
        </div>
        <div class="nav-item" :class="{active: currentPage ==='message'}">
            <div class="icon-box">
                <font-awesome-icon class="nav-icon" id="current_page":icon="['far','message']"/>
                <span class="dot"></span>
            </div>
        </div>
        <div class="nav-item" :class="{active: currentPage ==='contactList'}">
            <div class="icon-box">
                <font-awesome-icon class="nav-icon" icon="user-group"/>
                <span class="dot"></span>
            </div>
        </div>
         <div class="nav-item" :class="{active: currentPage ==='setting'}">
            <div class="icon-box">
                <font-awesome-icon class="nav-icon" icon="gear"/>
                <span class="dot"></span>
            </div>
        </div>
    </div>
    <div class="account">HN</div>
</div>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
.sidebar{
    display: flex;
    flex-direction: column;
    font-family:"Quicksand", sans-serif; 
    font-size: 100%;
    font-weight:600;
    width: 80px;
    height: 100vh;
    background-color:#0f2547;
    color: #e6edfd;
    align-self: center;
    justify-content: space-between;
    align-items: center;
}
.logo{
    font-size: 20px;
    margin-top: 15px;
    color:white;
    padding:12px;
    background-color: #2f5fd0;
    border-radius: 1rem;
    cursor:pointer;
}
.navigation{
    display:flex;
    flex-direction: column;
    gap:40px;
    font-size: 27px;
    margin-bottom: 25vh;
    margin-top:40px;
    color:#6b81a2;
}
.nav-icon{
    padding:7px;
    transition: all 0.2s ease-in;
    cursor:pointer;
}
.nav-icon:hover{
    color:white;
    background-color: #294a86;
    border-radius: 0.7rem;
}
.account{
    margin-bottom:20px;
    background-color: #6ba3f5;
    padding:10px;
    border-radius: 0.9rem;
    cursor:pointer;
}
#current_page{
    background-color: #294a86;
    padding:7px;
    border-radius: 0.7rem;
    color:white;
}
.nav-item {
    position: relative;
    width: 100%;
    display: flex;
    justify-content: center;
}

.icon-box {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    width: 48px;
    height: 48px;
    border-radius: 0.7rem;
    color: #6b81a2;
}

.nav-item.active .icon-box {
    background-color: #294a86;
    color: white;
}

/* === DẤU GẠCH TRẠNG THÁI ACTIVE === */
.nav-item.active .icon-box::before {
    content: "";
    position: absolute;
    left: -13.5px; /* Đẩy vạch ra sát mép trái của thanh sidebar */
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 28px;
    background-color: #799bf1; 
    border-radius: 0 4px 4px 0;
}

/* === ĐỊNH DẠNG DẤU CHẤM === */
.dot {
    display: none; /* Ẩn mặc định ở tất cả các nút */
    position: absolute;
    top: 6px;
    right: 6px;
    width: 10px;
    height: 10px;
    background-color: #799bf1;
    border-radius: 50%;
    border: 2px solid #294a86;
}

/* CHỈ HIỂN THỊ DẤU CHẤM Ở NÚT ĐANG ACTIVE */
.nav-item.active .dot {
    display: block; 
}
</style>