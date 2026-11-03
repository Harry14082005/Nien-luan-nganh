<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import NavigationSidebar from '@/components/common/NavigationSidebar.vue'
import ContactList from '@/components/contacts/ContactList.vue'
import ContactProfileCard from '@/components/contacts/ContactProfileCard.vue'

const router = useRouter()

const contacts = ref([
  {
    id: 1,
    name: 'Minh Anh',
    role: 'Product Designer',
    initials: 'MA',
    avatar: '',
    online: true,
    statusText: 'Đang hoạt động',
    bio: 'Những cuộc trò chuyện ý nghĩa luôn bắt đầu bằng một lời chào.',
    bgColor: '#FFE9DC',
    color: '#D2693B',
  },
  {
    id: 2,
    name: 'Quang Huy',
    role: 'Frontend Developer',
    initials: 'QH',
    avatar: '',
    online: true,
    statusText: 'Đang hoạt động',
    bio: 'Lập trình viên đam mê kiến tạo trải nghiệm người dùng hiện đại và mượt mà.',
    bgColor: '#DDF5EA',
    color: '#2E9A6B',
  },
  {
    id: 3,
    name: 'Linh Nguyễn',
    role: 'Marketing Lead',
    initials: 'LN',
    avatar: '',
    online: false,
    statusText: 'Vừa mới rời đi',
    bio: 'Kết nối mọi người và lan tỏa những câu chuyện truyền cảm hứng.',
    bgColor: '#FFF3D6',
    color: '#B8860B',
  },
  {
    id: 4,
    name: 'An Trần',
    role: 'UI Designer',
    initials: 'AT',
    avatar: '',
    online: false,
    statusText: 'Hoạt động 2 giờ trước',
    bio: 'Thiết kế không chỉ là vẻ bề ngoài mà là cách nó hoạt động.',
    bgColor: '#E3EDFF',
    color: '#3B6FD2',
  },
  {
    id: 5,
    name: 'Bảo Phạm',
    role: 'Project Manager',
    initials: 'BP',
    avatar: '',
    online: false,
    statusText: 'Hoạt động 5 giờ trước',
    bio: 'Xây dựng quy trình làm việc hiệu quả và đồng hành cùng nhóm.',
    bgColor: '#ECE9FF',
    color: '#6B5BD2',
  },
  {
    id: 6,
    name: 'Nguyễn Hoàng Nam',
    role: 'Đang hoạt động',
    initials: 'HN',
    avatar: '',
    online: true,
    statusText: 'Đang hoạt động',
    bio: 'Sẵn sàng kết nối và hỗ trợ khi có bất kỳ câu hỏi nào.',
    bgColor: '#DCE7FE',
    color: '#2563EB',
  },
])

const selectedContactId = ref(1)

const selectedContact = computed(() => {
  return contacts.value.find((c) => c.id === selectedContactId.value) || contacts.value[0]
})

function onSelectContact(contact) {
  selectedContactId.value = contact.id
}

function handleMessageContact(contact) {
  router.push('/chat')
}
</script>

<template>
  <div class="app-container">
    <!-- Cột 1: Navigation Sidebar -->
    <NavigationSidebar current-page="contactList" />

    <!-- Cột 2: Danh sách liên hệ -->
    <div class="ContactListSidebar">
      <ContactList
        :contacts="contacts"
        :selected-id="selectedContactId"
        @select="onSelectContact"
      />
    </div>

    <!-- Cột 3: Chi tiết liên hệ -->
    <div class="contacts-view">
      <!-- Header / Topbar -->
      <div class="contacts-topbar">
        <div class="breadcrumb-area">
          <span class="live-dot"></span>
          <span class="breadcrumb-title">Danh bạ của bạn</span>
        </div>

        <div class="topbar-actions">
          <button class="topbar-btn" title="Thông báo">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
          </button>
          <div class="user-avatar-badge">HN</div>
        </div>
      </div>

      <!-- Main Body Content -->
      <div class="contacts-body">
        <!-- Title & Introduction Section -->
        <div class="welcome-section">
          <span class="sub-caption">KẾT NỐI CỦA BẠN</span>
          <h1 class="main-title">Gần nhau hơn, mỗi ngày.</h1>
          <p class="description">Giữ liên lạc với những người tạo nên câu chuyện của bạn.</p>
        </div>

        <!-- Contact Profile Card -->
        <div class="card-display-area">
          <ContactProfileCard
            v-if="selectedContact"
            :contact="selectedContact"
            @message="handleMessageContact"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.app-container {
  display: grid;
  grid-template-columns: 80px 430px 1fr;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
}

.ContactListSidebar {
  height: 100%;
  overflow: hidden;
}

.contacts-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f8f9fd;
  border-left: 1px solid #e6eaf2;
  overflow-y: auto;
}

/* Topbar */
.contacts-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 70px;
  background-color: #ffffff;
  border-bottom: 1px solid #e6eaf2;
  flex-shrink: 0;
}

.breadcrumb-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.live-dot {
  width: 8px;
  height: 8px;
  background-color: #10b981;
  border-radius: 50%;
}

.breadcrumb-title {
  font-size: 15px;
  font-weight: 600;
  color: #475569;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.topbar-btn:hover {
  background-color: #f1f5f9;
  color: #1e293b;
}

.user-avatar-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  background-color: #4b88f6;
  color: #ffffff;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.user-avatar-badge:hover {
  transform: scale(1.05);
}

/* Body Content */
.contacts-body {
  flex: 1;
  padding: 36px 48px;
  display: flex;
  flex-direction: column;
}

.welcome-section {
  margin-bottom: 32px;
}

.sub-caption {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: #64748b;
  text-transform: uppercase;
}

.main-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 6px 0 8px 0;
}

.description {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.card-display-area {
  display: flex;
  justify-content: flex-start;
}
</style>

