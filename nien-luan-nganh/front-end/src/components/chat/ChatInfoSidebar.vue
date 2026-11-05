
<template>
  <aside class="chat-info-sidebar">
    <div class="chat-info-sidebar-inner">
      <!-- 1. HEADER SIDEBAR -->
      <div class="sidebar-header">
        <div class="header-title">
          <h3>Thông tin hội thoại</h3>
        </div>
        <button class="btn-close" title="Đóng (Esc)" @click="$emit('close')">
          <font-awesome-icon icon="x" />
        </button>
      </div>

      <div class="sidebar-scroll-content">
        <!-- 2. AVATAR & TÊN PHÒNG -->
        <div class="room-profile-section">
          <UserAvatar
            :name="displayRoomName"
            :src="resolveFileUrl(displayRoomAvatar)"
            :size="72"
          />
          <h4 class="room-title">{{ displayRoomName }}</h4>
          <!-- <span class="member-count-badge">
            <font-awesome-icon icon="users" class="badge-icon" />
            {{ memberCountText }}
          </span> -->
        </div>

        <!-- 3. BẬT / TẮT THÔNG BÁO -->
        <div class="setting-item notification-box">
          <div class="setting-info">
            <div class="setting-icon-wrapper" :class="{ muted: isNotificationMuted }">
              <font-awesome-icon :icon="isNotificationMuted ? 'bell-slash' : 'bell'" />
            </div>
            <div class="setting-text">
              <span class="setting-title">Thông báo</span>
              <span class="setting-subtext">{{ isNotificationMuted ? 'Đang tắt thông báo' : 'Đang bật thông báo' }}</span>
            </div>
          </div>
          <!-- Toggle Switch UI -->
          <label class="switch">
            <input
              type="checkbox"
              :checked="!isNotificationMuted"
              @change="handleToggleNotification"
            />
            <span class="slider round"></span>
          </label>
        </div>

        <hr class="divider" />

        <!-- 4. DANH SÁCH THÀNH VIÊN (COLLAPSIBLE ACCORDION) - CHỈ HIỂN THỊ KHI LÀ NHÓM CHAT (GROUP) -->
        <template v-if="isGroupRoom">
          <div class="accordion-section">
            <div class="accordion-header" @click="isMembersExpanded = !isMembersExpanded">
              <div class="header-left">
                <font-awesome-icon icon="user-group" class="section-icon" />
                <span class="section-title">Thành viên phòng ({{ displayMembers.length }})</span>
              </div>

              <font-awesome-icon
                icon="chevron-right"
                class="arrow-icon"
                :class="{ 'arrow--down': isMembersExpanded }"
              />
            </div>

            <!-- CSS Grid Accordion Collapse (Mượt mà, không lag/delay) -->
            <div class="accordion-collapse" :class="{ 'is-expanded': isMembersExpanded }">
              <div class="accordion-collapse-inner">
                <div class="accordion-body">
                  <!-- Thanh thêm thành viên mới vào phòng -->
                  <div class="add-member-action-bar" @click="$emit('add-member')">
                    <div class="add-member-icon-circle">
                      <font-awesome-icon icon="user-plus" />
                    </div>
                    <span class="add-member-text">Thêm thành viên</span>
                  </div>

                  <div v-if="displayMembers.length === 0" class="empty-members-hint">
                    Chưa có danh sách thành viên
                  </div>
                  <div
                    v-for="member in displayMembers"
                    :key="member.userId || member.username"
                    class="member-item member-item--clickable"
                    title="Bấm để xem thông tin thành viên"
                    @click="openMemberProfile(member)"
                  >
                    <UserAvatar
                      :name="member.username || 'Thành viên'"
                      :src="resolveFileUrl(member.avatarUrl)"
                      :size="34"
                    />
                    <div class="member-info">
                      <span class="member-name">{{ member.username || 'Thành viên' }}</span>
                      <span
                        v-if="member.role === 'ADMIN' || member.roomMemberRole === 'ADMIN'"
                        class="role-badge"
                      >
                        Trưởng nhóm
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <hr class="divider" />
        </template>

        <!-- 5. FILE PHƯƠNG TIỆN (ẢNH / VIDEO / FILE) -->
        <div class="accordion-section media-section">
          <div class="accordion-header" @click="isMediaExpanded = !isMediaExpanded">
            <div class="header-left">
              <font-awesome-icon icon="folder-open" class="section-icon" />
              <span class="section-title">File phương tiện & Tài liệu</span>
            </div>
            <font-awesome-icon
              icon="chevron-right"
              class="arrow-icon"
              :class="{ 'arrow--down': isMediaExpanded }"
            />
          </div>

          <div class="accordion-collapse" :class="{ 'is-expanded': isMediaExpanded }">
            <div class="accordion-collapse-inner">
              <div class="accordion-body">
                <!-- THANH NÚT TAB NGANG HÀNG -->
              <div class="media-tabs-bar">
                <button
                  type="button"
                  class="tab-btn"
                  :class="{ active: currentMediaTab === 'IMAGE' }"
                  @click="switchMediaTab('IMAGE')"
                >
                  <font-awesome-icon icon="image" class="tab-icon" />
                  <span>Ảnh</span>
                </button>
                <button
                  type="button"
                  class="tab-btn"
                  :class="{ active: currentMediaTab === 'VIDEO' }"
                  @click="switchMediaTab('VIDEO')"
                >
                  <font-awesome-icon icon="video" class="tab-icon" />
                  <span>Video</span>
                </button>
                <button
                  type="button"
                  class="tab-btn"
                  :class="{ active: currentMediaTab === 'FILE' }"
                  @click="switchMediaTab('FILE')"
                >
                  <font-awesome-icon icon="file-lines" class="tab-icon" />
                  <span>Tài liệu</span>
                </button>
              </div>

              <!-- NỘI DUNG TƯƠNG ỨNG TỪNG TAB -->
              <div class="media-display-container">
                <!-- A. TAB ẢNH & VIDEO (GRID VIEW) -->
                <div
                  v-if="currentMediaTab === 'IMAGE' || currentMediaTab === 'VIDEO'"
                  class="media-grid"
                  :class="{ 'media-grid--scrollable': isViewAllMedia }"
                >
                  <div
                    v-for="item in displayedMediaItems"
                    :key="item.id || item.url"
                    class="media-thumbnail-card"
                    @click="openMediaPreview(item)"
                  >
                    <img v-if="item.type === 'IMAGE' || currentMediaTab === 'IMAGE'" :src="resolveFileUrl(item.url)" alt="media" />
                    <div v-else class="video-preview-wrapper">
                      <video :src="resolveFileUrl(item.url)" muted></video>
                      <span class="play-overlay-icon">
                        <font-awesome-icon icon="play" />
                      </span>
                    </div>
                  </div>

                  <div v-if="displayedMediaItems.length === 0" class="empty-media-hint">
                    Chưa có {{ currentMediaTab === 'IMAGE' ? 'ảnh' : 'video' }} nào
                  </div>
                </div>

                <!-- B. TAB FILE TÀI LIỆU (LIST VIEW) -->
                <div
                  v-else
                  class="files-list"
                  :class="{ 'files-list--scrollable': isViewAllMedia }"
                >
                  <a
                    v-for="file in displayedMediaItems"
                    :key="file.id || file.url"
                    :href="resolveFileUrl(file.url)"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="file-item-card"
                  >
                    <div class="file-icon-box">
                      <font-awesome-icon icon="file-lines" />
                    </div>
                    <div class="file-meta">
                      <span class="file-name" :title="file.name">{{ file.name }}</span>
                      <span class="file-size">{{ file.size || 'Đính kèm' }}</span>
                    </div>
                    <font-awesome-icon icon="arrow-down" class="file-download-icon" />
                  </a>

                  <div v-if="displayedMediaItems.length === 0" class="empty-media-hint">
                    Chưa có tài liệu nào
                  </div>
                </div>
              </div>

              <!-- NÚT XEM TẤT CẢ / THU GỌN -->
              <button
                v-if="currentTabMediaList.length > PREVIEW_LIMIT"
                type="button"
                class="btn-toggle-view-all"
                @click="isViewAllMedia = !isViewAllMedia"
              >
                <span>{{ isViewAllMedia ? 'Thu gọn' : `Xem tất cả (${currentTabMediaList.length})` }}</span>
                <font-awesome-icon :icon="isViewAllMedia ? 'chevron-up' : 'chevron-down'" />
              </button>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>

    <!-- Popup xem hồ sơ thành viên (tái sử dụng ContactProfileCard) -->
    <Teleport to="body">
      <div
        v-if="selectedMemberProfile"
        class="profile-modal-backdrop"
        @click.self="selectedMemberProfile = null"
      >
        <div class="profile-modal-card-wrap">
          <button
            type="button"
            class="profile-modal-close-btn"
            title="Đóng (Esc)"
            @click="selectedMemberProfile = null"
          >
            <font-awesome-icon icon="x" />
          </button>
          <ContactProfileCard
            :contact="selectedMemberProfile"
            @message="handleDirectMessageWithMember"
          />
        </div>
      </div>
    </Teleport>
  </aside>
</template>

<script setup>
import { resolveFileUrl } from '@/api/storage.api';
import { ref, computed } from 'vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import ContactProfileCard from '@/components/contacts/ContactProfileCard.vue';
import { useChatStore } from '@/stores/chat';
import { useAuthStore } from '@/stores/auth';

const chatStore = useChatStore();
const authStore = useAuthStore();

const props = defineProps({
  room: {
    type: Object,
    default: () => ({})
  },
  members: {
    type: Array,
    default: () => []
  },
  mediaData: {
    type: Object,
    default: () => ({ images: [], videos: [], files: [] })
  }
});

const emit = defineEmits(['close', 'toggle-notification', 'preview-media', 'add-member']);

// Phân biệt nhóm hay phòng trực tiếp 1-1
const isGroupRoom = computed(() => {
  return props.room?.roomType === 'GROUP';
});

// State quản lý Accordion
const isMembersExpanded = ref(true);
const isMediaExpanded = ref(true);

// State bật/tắt thông báo
const isNotificationMuted = ref(false);

// State tab Phương tiện
const currentMediaTab = ref('IMAGE'); // 'IMAGE' | 'VIDEO' | 'FILE'
const isViewAllMedia = ref(false);
const PREVIEW_LIMIT = 6; // Số lượng hiển thị tối đa khi chưa bấm "Xem tất cả"

const displayRoomName = computed(() => {
  return props.room?.roomName || props.room?.name || 'Phòng trò chuyện';
});

const displayRoomAvatar = computed(() => {
  return props.room?.roomAvatarURL || props.room?.avatarUrl || '';
});

const displayMembers = computed(() => {
  return Array.isArray(props.members) ? props.members : [];
});

const memberCountText = computed(() => {
  const count = displayMembers.value.length;
  if (count > 0) return `${count} thành viên`;
  return props.room?.roomType === 'GROUP' ? 'Nhóm trò chuyện' : 'Trò chuyện trực tiếp';
});

const switchMediaTab = (tab) => {
  currentMediaTab.value = tab;
  isViewAllMedia.value = false;
};

// Lọc dữ liệu theo tab hiện tại
const currentTabMediaList = computed(() => {
  if (currentMediaTab.value === 'IMAGE') return props.mediaData?.images || [];
  if (currentMediaTab.value === 'VIDEO') return props.mediaData?.videos || [];
  return props.mediaData?.files || [];
});

// Danh sách sau khi áp dụng chế độ "Xem tất cả" hoặc giới hạn thu gọn
const displayedMediaItems = computed(() => {
  const list = currentTabMediaList.value;
  if (isViewAllMedia.value) {
    return list;
  }
  return list.slice(0, PREVIEW_LIMIT);
});

const handleToggleNotification = (e) => {
  isNotificationMuted.value = !e.target.checked;
  emit('toggle-notification', isNotificationMuted.value);
};

const openMediaPreview = (item) => {
  emit('preview-media', item);
};

// Quản lý popup xem hồ sơ thành viên (ContactProfileCard)
const selectedMemberProfile = ref(null);

const openMemberProfile = (member) => {
  if (!member) return;
  const isMe = authStore.user?.userId && member.userId === authStore.user.userId;
  selectedMemberProfile.value = {
    id: member.userId,
    name: member.username || 'Thành viên',
    role: (member.role === 'ADMIN' || member.roomMemberRole === 'ADMIN') ? 'Trưởng nhóm' : (isMe ? 'Bạn (Thành viên)' : 'Thành viên'),
    avatar: member.avatarUrl || member.avatar || '',
    online: member.status === 'ACTIVE' || member.isOnline || false,
    statusText: member.status === 'ACTIVE' ? 'Đang hoạt động' : 'Offline',
    bio: member.bio || member.description || 'Thành viên trong phòng trò chuyện.',
  };
};

const handleDirectMessageWithMember = async (contact) => {
  selectedMemberProfile.value = null;
  if (!contact?.id) return;
  // Không tự mở tin nhắn 1-1 với chính mình
  if (authStore.user?.userId && contact.id === authStore.user.userId) {
    return;
  }
  emit('close');
  await chatStore.startDirectChat(contact.id);
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700&display=swap');

.chat-info-sidebar {
  width: 340px;
  height: 100%;
  background: #ffffff;
  border-left: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  z-index: 25;
  font-family: 'Quicksand', sans-serif;
  box-sizing: border-box;
  overflow: hidden;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.04);
}

.chat-info-sidebar-inner {
  width: 340px;
  min-width: 340px;
  max-width: 340px;
  height: 100%;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-sizing: border-box;
  transition: transform 0.32s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease;
  will-change: transform, opacity;
}

.sidebar-header {
  height: 70px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 16px;
  color: #3b82f6;
}

.sidebar-header h3 {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 13px;
  color: #94a3b8;
  cursor: pointer;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.btn-close:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.sidebar-scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 14px 0 24px;
}

.sidebar-scroll-content::-webkit-scrollbar {
  width: 5px;
}
.sidebar-scroll-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

/* 2. Room Profile */
.room-profile-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 18px 16px;
  text-align: center;
}

.room-title {
  margin: 12px 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.member-count-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.badge-icon {
  font-size: 11px;
  color: #3b82f6;
}

/* 3. Notification Setting */
.notification-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 18px;
  margin: 4px 0;
}

.setting-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.setting-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #eff6ff;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s ease;
}

.setting-icon-wrapper.muted {
  background: #f1f5f9;
  color: #94a3b8;
}

.setting-text {
  display: flex;
  flex-direction: column;
}

.setting-title {
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
}

.setting-subtext {
  font-size: 11.5px;
  color: #94a3b8;
}

/* Toggle Switch */
.switch {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 22px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd5e1;
  transition: 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 34px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

input:checked + .slider {
  background-color: #3b82f6;
}

input:checked + .slider:before {
  transform: translateX(16px);
}

.divider {
  border: none;
  height: 1px;
  background-color: #f1f5f9;
  margin: 10px 0;
}

/* 4. Accordion Section */
.accordion-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 18px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s ease;
}

.accordion-header:hover {
  background-color: #f8fafc;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  font-size: 14px;
  color: #64748b;
}

.section-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #334155;
}

.arrow-icon {
  font-size: 12px;
  color: #94a3b8;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  transform: rotate(0deg);
}

.arrow--down {
  transform: rotate(90deg);
}

.accordion-body {
  padding: 6px 18px 12px;
}

/* Nút thêm thành viên vào nhóm */
.add-member-action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
  padding: 6px 8px;
}

.add-member-action-bar:hover {
  background-color: #f1f5f9;
}

.add-member-action-bar:hover .add-member-text {
  color: #2563eb;
}

.add-member-icon-circle {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background-color: #eff6ff;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.add-member-action-bar:hover .add-member-icon-circle {
  background-color: #3b82f6;
  color: #ffffff;
  transform: scale(1.05);
}

.add-member-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  transition: color 0.15s ease;
}

/* Members list */
.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px;
  border-radius: 8px;
  transition: all 0.25s ease;
  user-select: none;
}

.member-item--clickable {
  cursor: pointer;
}

.member-item--clickable:hover {
  background-color: #f1f5f9;
}

.member-item--clickable:hover .member-name {
  color: #2563eb;
}

.member-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.member-name {
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-badge {
  font-size: 10.5px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  padding: 1px 6px;
  border-radius: 4px;
  width: fit-content;
  margin-top: 2px;
}

.empty-members-hint {
  font-size: 12px;
  color: #94a3b8;
  padding: 8px 0;
  text-align: center;
}

/* 5. Media Tabs ngang hàng */
.media-tabs-bar {
  display: flex;
  gap: 6px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
  margin-bottom: 12px;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 7px 0;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  border-radius: 7px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: 'Quicksand', sans-serif;
}

.tab-btn.active {
  background: #ffffff;
  color: #3b82f6;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.tab-icon {
  font-size: 11px;
}

/* Grid ảnh & video */
.media-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.media-grid--scrollable {
  max-height: 250px;
  overflow-y: auto;
  padding-right: 2px;
}

.media-thumbnail-card {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f1f5f9;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.media-thumbnail-card:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.media-thumbnail-card img,
.media-thumbnail-card video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.video-preview-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.play-overlay-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #ffffff;
  font-size: 11px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

/* Danh sách file */
.files-list--scrollable {
  max-height: 230px;
  overflow-y: auto;
}

.file-item-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 6px;
  text-decoration: none;
  transition: all 0.15s ease;
}

.file-item-card:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.file-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #e2e8f0;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.file-meta {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
}

.file-name {
  font-size: 12px;
  color: #1e293b;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 10.5px;
  color: #94a3b8;
}

.file-download-icon {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
}

/* Nút Xem tất cả */
.btn-toggle-view-all {
  width: 100%;
  padding: 8px 0;
  margin-top: 10px;
  border: 1px dashed #cbd5e1;
  background: #ffffff;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: 'Quicksand', sans-serif;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-toggle-view-all:hover {
  background: #f8fafc;
  border-color: #94a3b8;
  color: #1e293b;
}

.empty-media-hint {
  grid-column: span 3;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  padding: 18px 0;
}

/* Accordion Transition - Chuẩn CSS Grid 0fr -> 1fr  */
.accordion-collapse {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.22s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.18s ease;
  opacity: 0;
}

.accordion-collapse.is-expanded {
  grid-template-rows: 1fr;
  opacity: 1;
}

.accordion-collapse-inner {
  overflow: hidden;
}

/* Popup Xem hồ sơ thành viên (tái sử dụng ContactProfileCard) */
.profile-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  padding: 16px;
  animation: modalFadeIn 0.18s ease-out;
}

.profile-modal-card-wrap {
  position: relative;
  width: 100%;
  max-width: 500px;
  animation: modalScaleUp 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.profile-modal-close-btn {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.35);
  color: #ffffff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  z-index: 20;
  transition: all 0.15s ease;
}

.profile-modal-close-btn:hover {
  background: rgba(0, 0, 0, 0.65);
  transform: scale(1.08);
}

@keyframes modalFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalScaleUp {
  from {
    transform: translateY(12px) scale(0.96);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}
</style>

