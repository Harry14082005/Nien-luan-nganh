<script setup>
import { ref } from 'vue'

const props = defineProps({
  email: {
    type: String,
    default: '',
  },
  phoneNumber: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close'])

const notificationMsg = ref('')

function handleMockAction(field) {
  notificationMsg.value = `Chức năng đổi ${field} đang được phát triển và sẽ sớm ra mắt!`
  setTimeout(() => {
    notificationMsg.value = ''
  }, 3500)
}
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal-card">
      <!-- Header -->
      <div class="modal-header">
        <div class="header-left">
          <div class="icon-wrap">
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
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
          </div>
          <div>
            <h3>Thông tin xác thực</h3>
            <p class="header-desc">Quản lý email, số điện thoại và mật khẩu đăng nhập</p>
          </div>
        </div>
        <button class="close-btn" @click="emit('close')" title="Đóng">✕</button>
      </div>

      <!-- Thông báo mockup -->
      <div v-if="notificationMsg" class="notice-toast">
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span>{{ notificationMsg }}</span>
      </div>

      <!-- Body / Các mục thông tin xác thực -->
      <div class="modal-body">
        <!-- Mục 1: Email -->
        <div class="credential-item">
          <div class="cred-left">
            <div class="cred-icon-badge blue">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                <polyline points="22,6 12,13 2,6" />
              </svg>
            </div>
            <div class="cred-info">
              <div class="cred-label">Địa chỉ Email</div>
              <div class="cred-val">{{ props.email || 'Chưa thiết lập email' }}</div>
            </div>
          </div>
          <button class="btn-mock" @click="handleMockAction('Email')">
            Đổi email
          </button>
        </div>

        <div class="cred-divider"></div>

        <!-- Mục 2: Số điện thoại -->
        <div class="credential-item">
          <div class="cred-left">
            <div class="cred-icon-badge green">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"
                />
              </svg>
            </div>
            <div class="cred-info">
              <div class="cred-label">Số điện thoại</div>
              <div class="cred-val">{{ props.phoneNumber || 'Chưa liên kết số điện thoại' }}</div>
            </div>
          </div>
          <button class="btn-mock" @click="handleMockAction('Số điện thoại')">
            Đổi số điện thoại
          </button>
        </div>

        <div class="cred-divider"></div>

        <!-- Mục 3: Mật khẩu -->
        <div class="credential-item">
          <div class="cred-left">
            <div class="cred-icon-badge purple">
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </div>
            <div class="cred-info">
              <div class="cred-label">Mật khẩu đăng nhập</div>
              <div class="cred-val">••••••••••••</div>
            </div>
          </div>
          <button class="btn-mock" @click="handleMockAction('Mật khẩu')">
            Đổi mật khẩu
          </button>
        </div>
      </div>

      <!-- Footer -->
      <div class="modal-footer">
        <span class="footnote"></span>
        <button class="btn-close-modal" @click="emit('close')">Đóng</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  font-family: 'Quicksand', sans-serif;
  backdrop-filter: blur(3px);
}

.modal-card {
  width: 520px;
  max-width: 92vw;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalFadeIn 0.2s ease-out;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
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

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background-color: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.header-desc {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #64748b;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.15s ease;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.notice-toast {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 14px 24px 0;
  padding: 10px 14px;
  border-radius: 10px;
  background-color: #eff6ff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #bfdbfe;
}

.modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.credential-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.cred-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.cred-icon-badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cred-icon-badge.blue {
  background-color: #eff6ff;
  color: #2563eb;
}

.cred-icon-badge.green {
  background-color: #ecfdf5;
  color: #059669;
}

.cred-icon-badge.purple {
  background-color: #f5f3ff;
  color: #7c3aed;
}

.cred-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.cred-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}

.cred-val {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-mock {
  padding: 7px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background-color: #ffffff;
  color: #2563eb;
  font-family: 'Quicksand', sans-serif;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-mock:hover {
  background-color: #eff6ff;
  border-color: #bfdbfe;
}

.cred-divider {
  height: 1px;
  background-color: #f1f5f9;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background-color: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.footnote {
  font-size: 12px;
  color: #94a3b8;
}

.btn-close-modal {
  padding: 8px 18px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-close-modal:hover {
  background-color: #f1f5f9;
}
</style>

