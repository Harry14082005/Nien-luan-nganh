<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRoomInviteInfo, joinRoomByInviteCode } from '@/api/room.api'
import { resolveFileUrl } from '@/api/storage.api'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

const inviteCode = route.params.inviteCode
const groupInfo = ref(null)
const loading = ref(true)
const joining = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

// Tải thông tin nhóm từ backend
onMounted(async () => {
  if (!inviteCode) {
    errorMsg.value = 'Liên kết tham gia nhóm không hợp lệ.'
    loading.value = false
    return
  }

  try {
    const data = await getRoomInviteInfo(inviteCode)
    groupInfo.value = data
  } catch (err) {
    console.error('Lỗi tải thông tin nhóm:', err)
    errorMsg.value = err?.response?.data?.message || 'Liên kết tham gia nhóm không hợp lệ hoặc đã hết hạn.'
  } finally {
    loading.value = false
  }
})

// Xử lý bấm nút "Tham gia nhóm" (hoặc "Vào trò chuyện" nếu đã là thành viên)
const handleAction = async () => {
  // 1. Nếu chưa đăng nhập -> chuyển hướng sang Login và lưu redirect URL
  if (!authStore.isLoggedIn) {
    router.push({
      path: '/login',
      query: { redirect: `/join/${inviteCode}` },
    })
    return
  }

  // 2. Nếu đã là thành viên -> Mở trực tiếp phòng chat
  if (groupInfo.value?.isAlreadyMember) {
    if (groupInfo.value?.roomId) {
      await chatStore.selectRoom(groupInfo.value.roomId)
    }
    router.push('/chat')
    return
  }

  // 3. Tiến hành tham gia nhóm
  joining.value = true
  errorMsg.value = ''
  successMsg.value = ''

  try {
    const res = await joinRoomByInviteCode(inviteCode)
    // Nếu nhóm yêu cầu phê duyệt thành viên mới
    if (res?.pendingApproval) {
      successMsg.value = 'Yêu cầu tham gia nhóm của bạn đã được gửi tới Quản trị viên phê duyệt.'
      return
    }
    // Cập nhật lại danh sách phòng
    await chatStore.fetchRooms()
    if (res?.roomId) {
      await chatStore.selectRoom(res.roomId)
    }
    router.push('/chat')
  } catch (err) {
    const message = err?.response?.data?.message || ''
    // Trường hợp nhóm yêu cầu xét duyệt
    if (message.includes('chờ phê duyệt') || err?.response?.data?.code === 1033) {
      successMsg.value = 'Yêu cầu tham gia nhóm của bạn đã được gửi tới Quản trị viên phê duyệt.'
    } else {
      errorMsg.value = message || 'Không thể tham gia nhóm lúc này. Vui lòng thử lại.'
    }
  } finally {
    joining.value = false
  }
}
</script>

<template>
  <div class="join-container">
    <div class="join-card">
      <!-- Logo ứng dụng -->
      <div class="logo-row">
        <span class="logo-text">bluespace.</span>
      </div>

      <!-- Trạng thái Đang tải -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Đang tải thông tin nhóm...</p>
      </div>

      <!-- Trạng thái Lỗi (Link sai hoặc hết hạn) -->
      <div v-else-if="errorMsg && !groupInfo" class="error-state">
        <div class="error-icon-box">
          <font-awesome-icon icon="circle-exclamation" />
        </div>
        <h3 class="error-title">Không tìm thấy nhóm</h3>
        <p class="error-desc">{{ errorMsg }}</p>
        <button type="button" class="btn-primary" @click="router.push('/chat')">
          Về trang chủ
        </button>
      </div>

      <!-- Trạng thái Hiển thị nhóm hợp lệ -->
      <div v-else-if="groupInfo" class="group-content">
        <div class="avatar-wrap">
          <img
            v-if="groupInfo.roomAvatarURL"
            :src="resolveFileUrl(groupInfo.roomAvatarURL)"
            :alt="groupInfo.roomName"
            class="group-avatar"
          />
          <div v-else class="group-avatar-fallback">
            <font-awesome-icon icon="users" />
          </div>
        </div>

        <h2 class="group-title">{{ groupInfo.roomName || 'Nhóm trò chuyện' }}</h2>
        <p class="group-subtitle">
          <font-awesome-icon icon="user-group" class="meta-icon" />
          <span>{{ groupInfo.memberCount || 0 }} thành viên</span>
        </p>

        <!-- Thông báo nhóm cần duyệt -->
        <div v-if="groupInfo.requireApproval && !groupInfo.isAlreadyMember" class="approval-notice">
          <font-awesome-icon icon="shield-halved" />
          <span>Nhóm này yêu cầu Quản trị viên phê duyệt trước khi vào.</span>
        </div>

        <!-- Thông báo gửi duyệt thành công -->
        <div v-if="successMsg" class="success-banner">
          <font-awesome-icon icon="circle-check" />
          <span>{{ successMsg }}</span>
        </div>

        <!-- Thông báo lỗi khi tham gia -->
        <div v-if="errorMsg" class="error-banner">
          <font-awesome-icon icon="triangle-exclamation" />
          <span>{{ errorMsg }}</span>
        </div>

        <!-- Nút hành động -->
        <div class="action-wrap">
          <button
            v-if="!successMsg"
            type="button"
            class="btn-primary"
            :disabled="joining"
            @click="handleAction"
          >
            <span v-if="joining">Đang xử lý...</span>
            <span v-else-if="groupInfo.isAlreadyMember">Vào cuộc trò chuyện</span>
            <span v-else-if="groupInfo.requireApproval">Gửi yêu cầu tham gia</span>
            <span v-else>Tham gia nhóm</span>
          </button>

          <button
            v-else
            type="button"
            class="btn-primary"
            @click="router.push('/chat')"
          >
            Về trang tin nhắn
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700&display=swap');

.join-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4fd 0%, #e8f0fe 100%);
  font-family: 'Quicksand', sans-serif;
  padding: 20px;
}

.join-card {
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 12px 36px rgba(37, 99, 235, 0.08), 0 4px 12px rgba(0, 0, 0, 0.04);
  padding: 36px 32px;
  text-align: center;
  box-sizing: border-box;
}

.logo-row {
  margin-bottom: 24px;
}

.logo-text {
  font-size: 26px;
  font-weight: 800;
  color: #2563eb;
  letter-spacing: -0.5px;
}

.avatar-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.group-avatar,
.group-avatar-fallback {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.15);
}

.group-avatar-fallback {
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  border: 2px solid #bfdbfe;
}

.group-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px 0;
  word-break: break-word;
}

.group-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.meta-icon {
  font-size: 13px;
  color: #94a3b8;
}

.approval-notice {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 12.5px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  margin-bottom: 20px;
}

.approval-notice svg {
  color: #3b82f6;
  font-size: 14px;
  flex-shrink: 0;
}

.success-banner {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  color: #065f46;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  margin-bottom: 20px;
}

.success-banner svg {
  color: #10b981;
  font-size: 16px;
  flex-shrink: 0;
}

.error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  color: #991b1b;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  margin-bottom: 20px;
}

.error-banner svg {
  color: #ef4444;
  font-size: 16px;
  flex-shrink: 0;
}

.action-wrap {
  margin-top: 10px;
}

.btn-primary {
  width: 100%;
  height: 46px;
  background: #2563eb;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
}

.btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* Loading & Error View */
.loading-state,
.error-state {
  padding: 30px 10px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  margin: 0 auto 16px auto;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-state p {
  color: #64748b;
  font-size: 14px;
}

.error-icon-box {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fef2f2;
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  margin: 0 auto 16px auto;
}

.error-title {
  font-size: 19px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.error-desc {
  font-size: 13.5px;
  color: #64748b;
  margin: 0 0 24px 0;
  line-height: 1.5;
}
</style>

