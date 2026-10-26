<script setup>
import ChatHeader from '@/components/chat/ChatHeader.vue';
import PinnedMessage from '@/components/chat/PinnedMessage.vue';
import MessageList from '@/components/chat/MessageList.vue';
import ChatInput from '@/components/chat/ChatInput.vue';
import NavigationSidebar from '@/components/common/NavigationSidebar.vue';
</script>
<template>
  <div class="app-container">
  <NavigationSidebar></NavigationSidebar>
  <div class="ChatListSidebar"></div>
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
  display: flex;
  flex-direction: column;
  height: 95vh;      
}

.chat-body {
  flex: 1;                
  min-height: 0;          
  overflow-y: auto;   
}
.app-container{
  display:grid;
  grid-template-columns: 75px 400px 1fr;
}

</style>