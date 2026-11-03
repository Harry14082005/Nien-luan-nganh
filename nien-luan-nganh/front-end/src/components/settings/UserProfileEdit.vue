<script setup>
import { ref, reactive, watch } from 'vue'

const props = defineProps({
  user: {
    type: Object,
    default: () => ({
      displayName: 'Nguyễn Hoàng Nam',
      username: 'hoangnam',
      initials: 'HN',
      statusText: 'Đang hoạt động',
      bio: 'Thiết kế những trải nghiệm thật ý nghĩa.',
      avatar: '',
      online: true,
    }),
  },
})

const emit = defineEmits(['save'])

const isEditing = ref(false)

const editForm = reactive({
  displayName: '',
  username: '',
  bio: '',
})

function openEditModal() {
  editForm.displayName = props.user.displayName || ''
  editForm.username = (props.user.username || '').replace(/^@/, '')
  editForm.bio = props.user.bio || ''
  isEditing.value = true
}

function closeEditModal() {
  isEditing.value = false
}

function handleSave() {
  const updated = {
    ...props.user,
    displayName: editForm.displayName.trim() || props.user.displayName,
    username: editForm.username.trim() || props.user.username,
    bio: editForm.bio.trim(),
  }
  emit('save', updated)
  isEditing.value = false
}
</script>

<template>
  <div class="user-profile-container">
    <!-- Card 1: Main Profile Banner & Overview -->
    <div class="profile-main-card">
      <!-- Decorative Banner with subtle sparkles -->
      <div class="banner">
        <svg
          class="banner-pattern"
          viewBox="0 0 600 160"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          preserveAspectRatio="none"
        >
          <circle cx="480" cy="80" r="120" stroke="rgba(255, 255, 255, 0.12)" stroke-width="1.5" />
          <circle cx="480" cy="80" r="70" stroke="rgba(255, 255, 255, 0.1)" stroke-width="1.5" />
          <!-- Star sparkle icon -->
          <path
            d="M480 35 L485 55 L505 60 L485 65 L480 85 L475 65 L455 60 L475 55 Z"
            fill="rgba(255, 255, 255, 0.2)"
          />
        </svg>
      </div>

      <!-- Card Body -->
      <div class="card-body">
        <!-- Top Row: Avatar & Edit Button -->
        <div class="avatar-action-row">
          <div class="avatar-wrapper">
            <img
              v-if="user.avatar"
              :src="user.avatar"
              :alt="user.displayName"
              class="avatar-image"
            />
            <div v-else class="avatar-text">
              {{ user.initials || 'HN' }}
            </div>
            <span v-if="user.online" class="status-dot"></span>
          </div>

          <button class="btn-edit" @click="openEditModal">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
            </svg>
            <span>Chỉnh sửa hồ sơ</span>
          </button>
        </div>

        <!-- Name & Handle Details -->
        <div class="details-section">
          <h2 class="user-display-name">{{ user.displayName }}</h2>
          <div class="user-meta">
            <span class="user-handle">@{{ (user.username || 'hoangnam').replace(/^@/, '') }}</span>
            <span class="meta-separator">-</span>
            <span class="user-status">{{ user.statusText || 'Đang hoạt động' }}</span>
          </div>
          <p class="user-bio-text">{{ user.bio }}</p>
        </div>
      </div>
    </div>

    <!-- Card 2: Thông tin cá nhân (đã bỏ mục Quyền riêng tư theo yêu cầu) -->
    <div class="profile-info-card">
      <div class="info-card-header">
        <h3>Thông tin cá nhân</h3>
      </div>

      <div class="info-list">
        <!-- Tên hiển thị -->
        <div class="info-row">
          <div class="info-label-group">
            <svg
              class="info-icon"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <span class="info-label">Tên hiển thị</span>
          </div>
          <div class="info-value">{{ user.displayName }}</div>
        </div>

        <div class="divider"></div>

        <!-- Tên người dùng -->
        <div class="info-row">
          <div class="info-label-group">
            <svg
              class="info-icon"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="4" />
              <path d="M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-3.92 7.94" />
            </svg>
            <span class="info-label">Tên người dùng</span>
          </div>
          <div class="info-value">@{{ (user.username || 'hoangnam').replace(/^@/, '') }}</div>
        </div>
      </div>
    </div>

    <!-- Modal chỉnh sửa hồ sơ -->
    <div v-if="isEditing" class="modal-backdrop" @click.self="closeEditModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Chỉnh sửa hồ sơ</h3>
          <button class="btn-close" @click="closeEditModal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div class="form-group">
            <label>Tên hiển thị</label>
            <input
              v-model="editForm.displayName"
              type="text"
              placeholder="Nhập tên hiển thị"
              spellcheck="false"
            />
          </div>

          <div class="form-group">
            <label>Tên người dùng (Handle)</label>
            <div class="input-handle-wrap">
              <span class="prefix">@</span>
              <input
                v-model="editForm.username"
                type="text"
                placeholder="hoangnam"
                spellcheck="false"
              />
            </div>
          </div>

          <div class="form-group">
            <label>Tiểu sử</label>
            <textarea
              v-model="editForm.bio"
              rows="3"
              placeholder="Viết một chút về bản thân bạn..."
              spellcheck="false"
            ></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" @click="closeEditModal">Hủy</button>
          <button class="btn-primary" @click="handleSave">Lưu thay đổi</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.user-profile-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  font-family: 'Quicksand', sans-serif;
  width: 100%;
  max-width: 780px;
}

/* Card 1: Main Profile Card */
.profile-main-card {
  background-color: #ffffff;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.banner {
  position: relative;
  height: 140px;
  background: linear-gradient(135deg, #2b6cb0 0%, #3b82f6 60%, #60a5fa 100%);
  overflow: hidden;
}

.banner-pattern {
  position: absolute;
  top: 0;
  right: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.card-body {
  padding: 0 32px 30px 32px;
}

.avatar-action-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: -44px;
  margin-bottom: 16px;
}

.avatar-wrapper {
  position: relative;
  width: 86px;
  height: 86px;
  border-radius: 20px;
  background-color: #ffffff;
  padding: 4px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  object-fit: cover;
}

.avatar-text {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  background-color: #3b82f6;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.status-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 14px;
  height: 14px;
  background-color: #10b981;
  border-radius: 50%;
  border: 3px solid #ffffff;
}

.btn-edit {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 16px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.btn-edit:hover {
  background-color: #f8fafc;
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.details-section {
  display: flex;
  flex-direction: column;
}

.user-display-name {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 14px;
}

.user-handle {
  font-weight: 600;
  color: #475569;
}

.meta-separator {
  color: #94a3b8;
}

.user-status {
  color: #10b981;
  font-weight: 600;
}

.user-bio-text {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

/* Card 2: Thông tin cá nhân */
.profile-info-card {
  background-color: #ffffff;
  border-radius: 20px;
  padding: 24px 32px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.info-card-header h3 {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px 0;
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
}

.info-label-group {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748b;
}

.info-icon {
  color: #94a3b8;
}

.info-label {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.divider {
  height: 1px;
  background-color: #f1f5f9;
}

/* Modal Edit */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  backdrop-filter: blur(2px);
}

.modal-content {
  background-color: #ffffff;
  border-radius: 18px;
  width: 90%;
  max-width: 480px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  animation: modalFadeIn 0.2s ease-out;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: color 0.15s;
}

.btn-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.form-group input,
.form-group textarea {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 14px;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  color: #1e293b;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.input-handle-wrap {
  display: flex;
  align-items: center;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0 12px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.input-handle-wrap:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.input-handle-wrap .prefix {
  color: #94a3b8;
  font-weight: 600;
  margin-right: 4px;
}

.input-handle-wrap input {
  border: none;
  padding: 10px 0;
  width: 100%;
}

.input-handle-wrap input:focus {
  box-shadow: none;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  background-color: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.btn-secondary {
  padding: 9px 18px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  background-color: #f1f5f9;
}

.btn-primary {
  padding: 9px 20px;
  background-color: #2f5fd0;
  border: none;
  border-radius: 10px;
  font-family: 'Quicksand', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-primary:hover {
  background-color: #254eb2;
}
</style>

