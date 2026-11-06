<template>
  <aside class="chat-info-sidebar">
    <div class="chat-info-sidebar-inner">
      <!-- 1. HEADER SIDEBAR (CHUYỂN ĐỔI THEO VIEW) -->
      <!-- A. Header màn hình thông tin chính -->
      <div v-if="currentView === 'INFO'" class="sidebar-header">
        <div class="header-title">
          <h3>Thông tin hội thoại</h3>
        </div>
        <button class="btn-close" title="Đóng (Esc)" @click="$emit('close')">
          <font-awesome-icon icon="x" />
        </button>
      </div>

      <!-- B. Header Sub-view xem toàn bộ File phương tiện (Chuẩn Zalo PC / Telegram Desktop) -->
      <div v-else class="sidebar-header subview-header">
        <div class="subview-header-left" @click="backToInfoView">
          <button class="btn-back" title="Quay lại thông tin">
            <font-awesome-icon icon="chevron-left" />
          </button>
          <div class="header-title subview-title">
            <h3>File phương tiện</h3>
          </div>
        </div>
        <button class="btn-close" title="Đóng (Esc)" @click="$emit('close')">
          <font-awesome-icon icon="x" />
        </button>
      </div>

      <!-- 2. NỘI DUNG CHÍNH -->
      <!-- VIEW 1: THÔNG TIN HỘI THOẠI -->
      <div v-if="currentView === 'INFO'" class="sidebar-scroll-content">
        <!-- 2.1 AVATAR & TÊN PHÒNG -->
        <div class="room-profile-section">
          <UserAvatar
            :name="displayRoomName"
            :src="resolveFileUrl(displayRoomAvatar)"
            :size="72"
          />
          <h4 class="room-title">{{ displayRoomName }}</h4>
        </div>

        <!-- 2.2 BẬT / TẮT THÔNG BÁO -->
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

        <!-- 2.3 DANH SÁCH THÀNH VIÊN (COLLAPSIBLE ACCORDION) - CHỈ CHO NHÓM CHAT -->
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

            <!-- CSS Grid Accordion Collapse -->
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

        <!-- 2.4 FILE PHƯƠNG TIỆN (PREVIEW GỌN GÀNG + NÚT XEM TẤT CẢ) -->
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

                <!-- PREVIEW NỘI DUNG (CHỈ 6 MỤC, KHÔNG CÓ THANH CUỘN CỤC BỘ) -->
                <div class="media-preview-container">
                  <!-- A. TAB ẢNH & VIDEO (GRID 3 CỘT X 2 HÀNG) -->
                  <div
                    v-if="currentMediaTab === 'IMAGE' || currentMediaTab === 'VIDEO'"
                    class="media-grid"
                  >
                    <div
                      v-for="item in currentTabMediaList.slice(0, 6)"
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

                    <div v-if="currentTabMediaList.length === 0" class="empty-media-hint">
                      Chưa có {{ currentMediaTab === 'IMAGE' ? 'ảnh' : 'video' }} nào
                    </div>
                  </div>

                  <!-- B. TAB FILE TÀI LIỆU (PREVIEW TỐI ĐA 3 FILE) -->
                  <div
                    v-else
                    class="files-list"
                  >
                    <a
                      v-for="file in currentTabMediaList.slice(0, 3)"
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

                    <div v-if="currentTabMediaList.length === 0" class="empty-media-hint">
                      Chưa có tài liệu nào
                    </div>
                  </div>
                </div>

                <!-- NÚT XEM TẤT CẢ (CHUYỂN SANG SUB-VIEW GALLERY CHUYÊN DỤNG) -->
                <button
                  v-if="currentTabMediaList.length > 0"
                  type="button"
                  class="btn-open-gallery"
                  @click="openMediaView(currentMediaTab)"
                >
                  <span>Xem tất cả ({{ currentTabMediaList.length }})</span>
                  <font-awesome-icon icon="chevron-right" class="btn-arrow" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- VIEW 2: SUB-VIEW GALLERY TOÀN BỘ FILE PHƯƠNG TIỆN (CHUẨN TELEGRAM / ZALO PC) -->
      <div v-else class="subview-media-content">
        <!-- THANH TAB SEGMENTED CONTROL TO RÕ TRÊN ĐẦU SUBVIEW -->
        <div class="subview-tabs-bar">
          <button
            type="button"
            class="subview-tab-btn"
            :class="{ active: currentMediaTab === 'IMAGE' }"
            @click="currentMediaTab = 'IMAGE'"
          >
            <font-awesome-icon icon="image" />
            <span>Ảnh ({{ props.mediaData?.images?.length || 0 }})</span>
          </button>
          <button
            type="button"
            class="subview-tab-btn"
            :class="{ active: currentMediaTab === 'VIDEO' }"
            @click="currentMediaTab = 'VIDEO'"
          >
            <font-awesome-icon icon="video" />
            <span>Video ({{ props.mediaData?.videos?.length || 0 }})</span>
          </button>
          <button
            type="button"
            class="subview-tab-btn"
            :class="{ active: currentMediaTab === 'FILE' }"
            @click="currentMediaTab = 'FILE'"
          >
            <font-awesome-icon icon="file-lines" />
            <span>File ({{ props.mediaData?.files?.length || 0 }})</span>
          </button>
        </div>

        <!-- DANH SÁCH TOÀN BỘ PHƯƠNG TIỆN - CUỘN DUY NHẤT 1 THANH SIÊU MƯỢT TỪ ĐẦU ĐẾN CUỐI -->
        <div class="subview-gallery-scroll">
          <!-- A. TAB ẢNH & VIDEO (GRID 3 CỘT) -->
          <div
            v-if="currentMediaTab === 'IMAGE' || currentMediaTab === 'VIDEO'"
            class="subview-media-grid"
          >
            <div
              v-for="item in currentTabMediaList"
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

            <div v-if="currentTabMediaList.length === 0" class="empty-media-hint">
              Chưa có {{ currentMediaTab === 'IMAGE' ? 'ảnh' : 'video' }} nào
            </div>
          </div>

          <!-- B. TAB TÀI LIỆU (DANH SÁCH FILE ĐẦY ĐỦ) -->
          <div
            v-else
            class="subview-files-list"
          >
            <a
              v-for="file in currentTabMediaList"
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

            <div v-if="currentTabMediaList.length === 0" class="empty-media-hint">
              Chưa có tài liệu nào
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
        @click.self="closeMemberProfile"
      >
        <div class="profile-modal-card-wrap">
          <!-- Nhóm nút hành động ở góc trên bên phải Card (3 chấm & Đóng X) -->
          <div class="profile-modal-top-actions">
            <!-- Nút 3 chấm mở menu tùy chọn (chỉ hiện khi không phải chính mình) -->
            <div v-if="!selectedMemberProfile.isMe" class="profile-menu-wrapper">
              <button
                type="button"
                class="profile-action-btn profile-more-btn"
                :class="{ active: isProfileMenuOpen }"
                title="Tùy chọn khác"
                @click="isProfileMenuOpen = !isProfileMenuOpen"
              >
                <font-awesome-icon icon="ellipsis-vertical" />
              </button>

              <!-- Dropdown Menu -->
              <div v-if="isProfileMenuOpen" class="profile-menu-dropdown">
                <!-- 1. Điều hướng qua danh bạ xem chi tiết -->
                <button
                  type="button"
                  class="menu-dropdown-item"
                  @click="handleNavigateToContacts(selectedMemberProfile)"
                >
                  <font-awesome-icon icon="address-book" class="item-icon" />
                  <span>Xem trong danh bạ</span>
                </button>

                <!-- 2. Hủy kết bạn (chỉ hiện khi đã là bạn bè) -->
                <button
                  v-if="selectedMemberProfile.isFriend"
                  type="button"
                  class="menu-dropdown-item item-danger"
                  @click="handleUnfriendFromProfile(selectedMemberProfile)"
                >
                  <font-awesome-icon icon="user-minus" class="item-icon" />
                  <span>Hủy kết bạn</span>
                </button>

                <!-- 3. Chặn người dùng / Bỏ chặn -->
                <button
                  type="button"
                  class="menu-dropdown-item"
                  :class="selectedMemberProfile.isBlocked ? 'item-primary' : 'item-danger'"
                  @click="handleToggleBlockFromProfile(selectedMemberProfile)"
                >
                  <font-awesome-icon
                    :icon="selectedMemberProfile.isBlocked ? 'unlock' : 'ban'"
                    class="item-icon"
                  />
                  <span>{{ selectedMemberProfile.isBlocked ? 'Bỏ chặn người dùng' : 'Chặn người dùng' }}</span>
                </button>
              </div>
            </div>

            <!-- Nút Đóng X -->
            <button
              type="button"
              class="profile-action-btn profile-close-btn"
              title="Đóng (Esc)"
              @click="closeMemberProfile"
            >
              <font-awesome-icon icon="x" />
            </button>
          </div>

          <ContactProfileCard
            :contact="selectedMemberProfile"
            @message="handleDirectMessageWithMember"
            @add-friend="handleAddFriendFromProfile"
            @cancel-friend-request="handleCancelFriendRequestFromProfile"
            @unblock="handleToggleBlockFromProfile"
          />
        </div>
      </div>
    </Teleport>
  </aside>
</template>

<script setup>
import { resolveFileUrl } from '@/api/storage.api';
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import UserAvatar from '@/components/common/UserAvatar.vue';
import ContactProfileCard from '@/components/contacts/ContactProfileCard.vue';
import { useChatStore } from '@/stores/chat';
import { useAuthStore } from '@/stores/auth';
import {
  getFriends,
  getPendingFriendships,
  sendFriendRequest,
  cancelFriendRequest,
  getFriendshipStatus,
  blockUser,
  unblockUser
} from '@/api/contacts.api';

const router = useRouter();

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

// Chế độ xem: 'INFO' (thông tin phòng) | 'MEDIA' (toàn bộ phương tiện)
const currentView = ref('INFO');

const openMediaView = (tab = 'IMAGE') => {
  currentMediaTab.value = tab;
  currentView.value = 'MEDIA';
};

const backToInfoView = () => {
  currentView.value = 'INFO';
};

// Tự động điều hướng trở lại màn hình Thông tin hội thoại khi chuyển sang phòng chat khác
watch(
  [() => props.room?.roomId, () => props.room?.id, () => chatStore.activeRoomId],
  () => {
    currentView.value = 'INFO';
  }
);

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

const displayRoomName = computed(() => {
  return props.room?.roomName || props.room?.name || 'Phòng trò chuyện';
});

const displayRoomAvatar = computed(() => {
  return props.room?.roomAvatarURL || props.room?.avatarUrl || '';
});

const displayMembers = computed(() => {
  return Array.isArray(props.members) ? props.members : [];
});

const switchMediaTab = (tab) => {
  currentMediaTab.value = tab;
};

// Lọc dữ liệu theo tab hiện tại, luôn sắp xếp DESC (mới nhất lên đầu)
const currentTabMediaList = computed(() => {
  let list = [];
  if (currentMediaTab.value === 'IMAGE') list = props.mediaData?.images || [];
  else if (currentMediaTab.value === 'VIDEO') list = props.mediaData?.videos || [];
  else list = props.mediaData?.files || [];

  return [...list].sort((a, b) => {
    const tA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
    const tB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
    return tB - tA;
  });
});

const handleToggleNotification = (e) => {
  isNotificationMuted.value = !e.target.checked;
  emit('toggle-notification', isNotificationMuted.value);
};

const openMediaPreview = (item) => {
  emit('preview-media', item);
};

// Quản lý quan hệ bạn bè để hiển thị trạng thái và nút kết bạn / thu hồi / hủy kết bạn / chặn
const friendIds = ref(new Set());
const pendingIds = ref(new Set());
const blockedIds = ref(new Set());
const pendingMap = ref(new Map()); // userId -> friendshipId (thu hồi kết bạn)
const friendShipMap = ref(new Map()); // userId -> friendshipId (hủy kết bạn)

const fetchFriendshipStatus = async () => {
  try {
    const [friendsRes, pendingRes] = await Promise.allSettled([
      getFriends({ page: 0, size: 500 }),
      getPendingFriendships({ page: 0, size: 500 })
    ]);
    const friends = (friendsRes.status === 'fulfilled' && (Array.isArray(friendsRes.value) ? friendsRes.value : friendsRes.value?.content)) || [];
    const pending = (pendingRes.status === 'fulfilled' && (Array.isArray(pendingRes.value) ? pendingRes.value : pendingRes.value?.content)) || [];

    friendIds.value = new Set(friends.map((f) => f.friend?.userId).filter(Boolean));

    pendingMap.value.clear();
    pending.forEach((p) => {
      const uId = p.friend?.userId;
      if (uId) {
        pendingMap.value.set(uId, p.friendshipId);
      }
    });
    pendingIds.value = new Set(pendingMap.value.keys());

    friendShipMap.value.clear();
    friends.forEach((f) => {
      const uId = f.friend?.userId;
      if (uId && f.friendshipId) {
        friendShipMap.value.set(uId, f.friendshipId);
      }
    });
  } catch (err) {
    console.error('Lỗi tải trạng thái bạn bè:', err);
  }
};

onMounted(() => {
  fetchFriendshipStatus();
});

// Quản lý popup xem hồ sơ thành viên (ContactProfileCard)
const selectedMemberProfile = ref(null);
const isProfileMenuOpen = ref(false);

const closeMemberProfile = () => {
  selectedMemberProfile.value = null;
  isProfileMenuOpen.value = false;
};

const openMemberProfile = (member) => {
  if (!member) return;
  isProfileMenuOpen.value = false;
  const targetId = member.userId;
  const isMe = !!(authStore.user?.userId && targetId === authStore.user.userId);
  const isFriend = friendIds.value.has(targetId);
  const isPending = pendingIds.value.has(targetId);
  const isBlocked = blockedIds.value.has(targetId);
  const friendshipId = pendingMap.value.get(targetId) || friendShipMap.value.get(targetId) || null;

  selectedMemberProfile.value = {
    id: targetId,
    name: member.username || 'Thành viên',
    role: (member.role === 'ADMIN' || member.roomMemberRole === 'ADMIN') ? 'Trưởng nhóm' : (isMe ? 'Bạn (Thành viên)' : 'Thành viên'),
    avatar: resolveFileUrl(member.avatarUrl || member.avatar || ''),
    online: member.status === 'ACTIVE' || member.isOnline || false,
    statusText: member.status === 'ACTIVE' ? 'Đang hoạt động' : 'Offline',
    bio: member.bio || member.description || 'Thành viên trong phòng trò chuyện.',
    isMe,
    isFriend,
    isPending,
    isBlocked,
    friendshipId
  };

  // Đồng bộ trạng thái mới nhất từ server (tránh trường hợp cache cũ)
  if (!isMe && targetId) {
    getFriendshipStatus(targetId)
      .then((rel) => {
        if (!selectedMemberProfile.value || selectedMemberProfile.value.id !== targetId) return;
        if (!rel) {
          selectedMemberProfile.value.isFriend = false;
          selectedMemberProfile.value.isPending = false;
          selectedMemberProfile.value.isBlocked = false;
          selectedMemberProfile.value.friendshipId = null;
          pendingIds.value.delete(targetId);
          pendingMap.value.delete(targetId);
          friendIds.value.delete(targetId);
          friendShipMap.value.delete(targetId);
          blockedIds.value.delete(targetId);
        } else if (rel.status === 'ACCEPTED') {
          selectedMemberProfile.value.isFriend = true;
          selectedMemberProfile.value.isPending = false;
          selectedMemberProfile.value.isBlocked = false;
          selectedMemberProfile.value.friendshipId = rel.friendshipId;
          friendIds.value.add(targetId);
          friendShipMap.value.set(targetId, rel.friendshipId);
          pendingIds.value.delete(targetId);
          pendingMap.value.delete(targetId);
          blockedIds.value.delete(targetId);
        } else if (rel.status === 'PENDING') {
          selectedMemberProfile.value.isFriend = false;
          selectedMemberProfile.value.isPending = true;
          selectedMemberProfile.value.isBlocked = false;
          selectedMemberProfile.value.friendshipId = rel.friendshipId;
          pendingIds.value.add(targetId);
          pendingMap.value.set(targetId, rel.friendshipId);
          blockedIds.value.delete(targetId);
        } else if (rel.status === 'BLOCKED') {
          const amIBlocker = rel.direction === 'OUTGOING';
          selectedMemberProfile.value.isBlocked = amIBlocker;
          selectedMemberProfile.value.isFriend = false;
          selectedMemberProfile.value.isPending = false;
          selectedMemberProfile.value.friendshipId = rel.friendshipId;
          if (amIBlocker) blockedIds.value.add(targetId);
        }
      })
      .catch(() => {});
  }
};

const handleAddFriendFromProfile = async (contact) => {
  if (!contact?.id) return;
  try {
    const res = await sendFriendRequest(contact.id);
    const fId = res?.friendshipId;
    pendingIds.value.add(contact.id);
    if (fId) {
      pendingMap.value.set(contact.id, fId);
    }
    if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
      selectedMemberProfile.value.isPending = true;
      if (fId) selectedMemberProfile.value.friendshipId = fId;
    }
  } catch (err) {
    console.error('Lỗi gửi lời mời kết bạn từ hồ sơ:', err);
  }
};

const handleCancelFriendRequestFromProfile = async (contact) => {
  if (!contact?.id) return;
  try {
    let fId = contact.friendshipId || pendingMap.value.get(contact.id);
    if (!fId) {
      const rel = await getFriendshipStatus(contact.id);
      fId = rel?.friendshipId;
    }
    if (fId) {
      await cancelFriendRequest(fId);
    }
    pendingIds.value.delete(contact.id);
    pendingMap.value.delete(contact.id);
    if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
      selectedMemberProfile.value.isPending = false;
      selectedMemberProfile.value.friendshipId = null;
    }
  } catch (err) {
    console.error('Lỗi thu hồi lời mời kết bạn:', err);
  }
};

//Xy ly huy ket ban o profile
const handleUnfriendFromProfile = async (contact) => {
  if (!contact?.id) return;
  isProfileMenuOpen.value = false;
  try {
    let fId = contact.friendshipId || friendShipMap.value.get(contact.id);
    if (!fId) {
      const rel = await getFriendshipStatus(contact.id);
      fId = rel?.friendshipId;
    }
    if (fId) {
      await cancelFriendRequest(fId);
    }
    friendIds.value.delete(contact.id);
    friendShipMap.value.delete(contact.id);
    if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
      selectedMemberProfile.value.isFriend = false;
      selectedMemberProfile.value.friendshipId = null;
    }
  } catch (err) {
    console.error('Lỗi hủy kết bạn:', err);
  }
};

const handleToggleBlockFromProfile = async (contact) => {
  if (!contact?.id) return;
  isProfileMenuOpen.value = false;
  try {
    if (contact.isBlocked) {
      // Đang chặn -> Bỏ chặn
      await unblockUser(contact.id);
      blockedIds.value.delete(contact.id);
      if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
        selectedMemberProfile.value.isBlocked = false;
        selectedMemberProfile.value.isFriend = false;
        selectedMemberProfile.value.isPending = false;
        selectedMemberProfile.value.friendshipId = null;
      }
    } else {
      // Chưa chặn -> Chặn người dùng
      await blockUser(contact.id);
      blockedIds.value.add(contact.id);
      friendIds.value.delete(contact.id);
      friendShipMap.value.delete(contact.id);
      pendingIds.value.delete(contact.id);
      pendingMap.value.delete(contact.id);
      if (selectedMemberProfile.value && selectedMemberProfile.value.id === contact.id) {
        selectedMemberProfile.value.isBlocked = true;
        selectedMemberProfile.value.isFriend = false;
        selectedMemberProfile.value.isPending = false;
      }
    }
  } catch (err) {
    console.error('Lỗi thao tác chặn / bỏ chặn:', err);
  }
};

const handleNavigateToContacts = (contact) => {
  closeMemberProfile();
  emit('close');
  router.push({
    path: '/contacts',
    query: { userId: contact.id }
  });
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
  width: 350px;
  height: 100%;
  background: #ffffff;
  border-left: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  user-select: text;
  box-sizing: border-box;
  overflow: hidden;
}

.chat-info-sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* 1. Sidebar Header */
.sidebar-header {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
  background-color: #ffffff;
}

.header-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.btn-close {
  background: none;
  border: none;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: #64748b;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-close:hover {
  background: #f1f5f9;
  color: #1e293b;
}

/* Sub-view Header (Quay lại) */
.subview-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.btn-back {
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.subview-header-left:hover .btn-back {
  background: #eff6ff;
  color: #2563eb;
}

.subview-header-left:hover .subview-title h3 {
  color: #2563eb;
}

.subview-title h3 {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  transition: color 0.15s ease;
}

/* Content cuộn của màn hình chính */
.sidebar-scroll-content {
  flex: 1;
  overflow-y: auto;
  scrollbar-gutter: stable;
  padding: 10px 0 16px;
}

.sidebar-scroll-content::-webkit-scrollbar,
.subview-gallery-scroll::-webkit-scrollbar {
  width: 5px;
}

.sidebar-scroll-content::-webkit-scrollbar-thumb,
.subview-gallery-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.sidebar-scroll-content::-webkit-scrollbar-thumb:hover,
.subview-gallery-scroll::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 2. Room Profile */
.room-profile-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 18px 8px;
  text-align: center;
}

.room-title {
  margin: 8px 0 2px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

/* 3. Notification Setting */
.notification-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 18px;
  margin: 0;
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
  margin: 6px 0;
}

/* 4. Accordion Section */
.accordion-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 18px;
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
  padding: 4px 18px 8px;
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
  gap: 4px;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 9px;
  margin-bottom: 8px;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 6px 0;
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
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.tab-icon {
  font-size: 11px;
}

/* Grid ảnh & video */
.media-grid,
.subview-media-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
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
.files-list,
.subview-files-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-item-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
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

/* Nút Xem tất cả (Mở Sub-view Gallery) */
.btn-open-gallery {
  width: 100%;
  padding: 8px 12px;
  margin-top: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #2563eb;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: 'Quicksand', sans-serif;
  transition: all 0.15s ease;
}

.btn-open-gallery:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.btn-arrow {
  font-size: 11px;
}

.empty-media-hint {
  grid-column: span 3;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  padding: 16px 0;
}

/* ============================================================ */
/* SUB-VIEW CHUYÊN DỤNG XEM TOÀN BỘ FILE PHƯƠNG TIỆN (ZALO / TELEGRAM) */
/* ============================================================ */
.subview-media-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: calc(100% - 70px);
  overflow: hidden;
}

.subview-tabs-bar {
  display: flex;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 9px;
  margin: 12px 16px 8px;
  gap: 4px;
  flex-shrink: 0;
}

.subview-tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 7px 0;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Quicksand', sans-serif;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.subview-tab-btn.active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.subview-gallery-scroll {
  flex: 1;
  overflow-y: auto;
  scrollbar-gutter: stable;
  padding: 6px 16px 24px;
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
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
  animation: modalFadeIn 0.2s ease-out;
}

.profile-modal-card-wrap {
  position: relative;
  max-width: 500px;
  width: 100%;
  display: flex;
  justify-content: center;
  animation: modalScaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.profile-modal-card-wrap :deep(.profile-card) {
  width: 100% !important;
  max-width: 500px;
  box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(0, 0, 0, 0.05);
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes modalScaleUp {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.profile-modal-top-actions {
  position: absolute;
  top: 14px;
  right: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 50;
}

.profile-menu-wrapper {
  position: relative;
}

.profile-action-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.92);
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: all 0.18s cubic-bezier(0.16, 1, 0.3, 1);
  font-size: 14px;
}

.profile-action-btn:hover {
  background: #ffffff;
  color: #0f172a;
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.16);
}

.profile-action-btn.active {
  background: #ffffff;
  color: #2563eb;
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.profile-menu-dropdown {
  position: absolute;
  top: 42px;
  right: 0;
  min-width: 205px;
  background: #ffffff;
  border-radius: 14px;
  padding: 6px;
  box-shadow: 0 12px 30px -4px rgba(0, 0, 0, 0.18), 0 0 0 1px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 3px;
  z-index: 60;
  animation: modalScaleUp 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}

.menu-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: 9px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  text-align: left;
  width: 100%;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.menu-dropdown-item:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.menu-dropdown-item.item-danger {
  color: #ef4444;
}

.menu-dropdown-item.item-danger:hover {
  background: #fef2f2;
  color: #dc2626;
}

.menu-dropdown-item.item-primary {
  color: #2563eb;
}

.menu-dropdown-item.item-primary:hover {
  background: #eff6ff;
  color: #1d4ed8;
}

.menu-dropdown-item .item-icon {
  width: 16px;
  font-size: 14px;
  text-align: center;
  flex-shrink: 0;
}
</style>
