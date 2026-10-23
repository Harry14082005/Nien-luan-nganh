<script setup>
import ChatHeader from '@/components/chat/ChatHeader.vue';
import PinnedMessage from '@/components/chat/PinnedMessage.vue';
import MessageList from '@/components/chat/MessageList.vue';
import ChatInput from '@/components/chat/ChatInput.vue';
</script>
<template>
    <div class="chat-view">
    <ChatHeader />
    <PinnedMessage />

    <div class="chat-body">
      <MessageList />
      <MessageList variant="SendTo" Sender="" />
      <MessageList />
      <MessageList variant="SendTo" Sender="" />
    
    </div>

    <ChatInput />
  </div>
</template>
<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 95vh;      
}

.chat-body {
  flex: 1;                /* chiếm hết phần còn lại */
  min-height: 0;          /* bắt buộc, nếu thiếu thì cuộn không hoạt động */
  overflow-y: auto;   
  /* tin nhắn nhiều thì cuộn trong vùng này */
}
</style>