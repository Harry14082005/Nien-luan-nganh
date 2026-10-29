<script setup>
import ChatHeader from '@/components/chat/ChatHeader.vue';
import PinnedMessage from '@/components/chat/PinnedMessage.vue';
import MessageList from '@/components/chat/MessageList.vue';
import ChatInput from '@/components/chat/ChatInput.vue';
import NavigationSidebar from '@/components/common/NavigationSidebar.vue';
import ChatListSidebar from '@/components/chat/ChatListSidebar.vue';
</script>
<template>
  <div class="app-container">
  <NavigationSidebar></NavigationSidebar>
  <div class="ChatListSidebar">
    <ChatListSidebar></ChatListSidebar>
  </div>
  <div class="chat-view">
    <ChatHeader />
    <PinnedMessage />

    <div class="chat-body">
      <MessageList />
      <MessageList variant="SendTo" Sender="" />
      <MessageList />
      <MessageList variant="SendTo" Sender="" />
    
    </div>
    <div class="input_text"><ChatInput/></div>
  </div> 
  </div>
</template>
<style scoped>
.chat-view {
  display: grid;
  grid-template-rows: 80px 62px 1fr 60px;
  height: 100vh;  
  background-color: #f8f9fd;
  border-right: 1px solid #e6eaf2;   
  border-left: 1px solid #e6eaf2;
}

.chat-body {
  flex: 1;                
  min-height: 0;          
  overflow-y: auto; 
  padding:10px;  
}
.app-container{
  display:grid;
  grid-template-columns: 90px 450px 1fr;
}
.input_text{
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>