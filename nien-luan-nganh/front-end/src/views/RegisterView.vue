<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import AuthSidebar    from '@/components/common/AuthSidebar.vue'
import IconUser       from '@/components/icons/IconUser.vue'
import IconMail       from '@/components/icons/IconMail.vue'
import IconPhone      from '@/components/icons/IconPhone.vue'
import IconLock       from '@/components/icons/IconLock.vue'
import IconEye        from '@/components/icons/IconEye.vue'
import IconEyeOff     from '@/components/icons/IconEyeOff.vue'
import IconArrowRight from '@/components/icons/IconArrowRight.vue'

const router = useRouter()
const auth = useAuthStore()
const fullName        = ref('')
const gender          = ref('male')
const emailOrPhone    = ref('')
const password        = ref('')
const confirmPassword = ref('')
const showPassword        = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')
const loading = ref(false)

function validate() {
  const name = fullName.value.trim()
  if (name.length < 2 || name.length > 50) return 'Họ và tên phải từ 2 đến 50 ký tự'

  const identifier = emailOrPhone.value.trim().replace(/\s/g, '')

  if (!identifier) {
    return 'Vui lòng nhập email hoặc số điện thoại'
  }
  // Regex kiểm tra email theo đúng ý bạn
  const emailRegex = /^[A-Za-z0-9]+([._%+-]?[A-Za-z0-9]+)*@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+$/
  // Regex kiểm tra số điện thoại: có thể bắt đầu bằng dấu + (tùy chọn), theo sau là các chữ số (độ dài thông thường từ 7 đến 15 chữ số)
  const phoneRegex = /^\+?[0-9]{7,15}$/
  if (!emailRegex.test(identifier) && !phoneRegex.test(identifier)) {
    return 'Email hoặc số điện thoại không đúng định dạng'
  }
  
  if (password.value.length < 6) return 'Mật khẩu phải có ít nhất 6 ký tự'
  if (password.value !== confirmPassword.value) return 'Mật khẩu xác nhận không khớp'
  return ''
}

async function handleRegister() {
  errorMessage.value = validate()
  if (errorMessage.value) return

  const body = {
    username: fullName.value.trim(),
    identifier: emailOrPhone.value.trim(),
    password: password.value,
    confirmPassword: confirmPassword.value,
    gender: gender.value.toUpperCase(),
    preferredLanguage: 'vi',
  }

  loading.value = true
  try {
    await auth.register(body)
    router.push('/chat')
  } catch (err) {
    console.error('Lỗi đăng ký:', err)
    errorMessage.value =
      err.response?.data?.message || err.message || 'Không thể kết nối tới máy chủ, vui lòng thử lại'
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="auth-page">
    <AuthSidebar />

    <main class="auth-main">
      <div class="auth-card">
        <p class="auth-label">BẮT ĐẦU CÙNG BLUESPACE</p>
        <h2 class="auth-title">Tạo tài khoản</h2>
        <div class="auth-tabs">
          <button class="tab" id="tab-login-from-register" @click="goToLogin">Đăng nhập</button>
          <button class="tab active" id="tab-register">Đăng ký</button>
        </div>

        <form @submit.prevent="handleRegister" novalidate class="auth-form">

          <div class="field-group">
            <label class="field-label">Họ và tên</label>
            <div class="input-wrapper">
              <IconUser :size="16" class="input-icon" />
              <input
                id="register-fullname"
                v-model="fullName"
                type="text"
                class="auth-input"
                placeholder="Tên người dùng"
              />
            </div>
          </div>

          <div class="field-group">
            <label class="field-label">Giới tính</label>
            <div class="gender-group">
              <button type="button" id="gender-male"
                :class="['gender-btn', { active: gender === 'male' }]"
                @click="gender = 'male'">Nam</button>
              <button type="button" id="gender-female"
                :class="['gender-btn', { active: gender === 'female' }]"
                @click="gender = 'female'">Nữ</button>
            </div>
          </div>

          <div class="field-group">
            <label class="field-label">Email hoặc số điện thoại</label>
            <div class="input-wrapper">
              <IconMail :size="16" class="input-icon" />
              <input
                id="register-identifier"
                v-model="emailOrPhone"
                type="text"
                class="auth-input"
                placeholder="Email hoặc số điện thoại"
              />
            </div>
          </div>
          <div class="field-group">
            <label class="field-label">Mật khẩu</label>
            <div class="input-wrapper">
              <IconLock :size="16" class="input-icon" />
              <input
                id="register-password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                class="auth-input"
                placeholder="Nhập mật khẩu"
              />
              <button type="button" class="toggle-pw" @click="showPassword = !showPassword">
                <IconEye v-if="!showPassword" :size="16" />
                <IconEyeOff v-else :size="16" />
              </button>
            </div>
          </div>

          <div class="field-group">
            <label class="field-label">Xác nhận mật khẩu</label>
            <div class="input-wrapper">
              <IconLock :size="16" class="input-icon" />
              <input
                id="register-confirm-password"
                v-model="confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                class="auth-input"
                placeholder="Nhập lại mật khẩu"
              />
              <button type="button" class="toggle-pw" @click="showConfirmPassword = !showConfirmPassword">
                <IconEye v-if="!showConfirmPassword" :size="16" />
                <IconEyeOff v-else :size="16" />
              </button>
            </div>
          </div>
          <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
          <button id="register-submit" type="submit" class="btn-primary" :disabled="loading">
           {{ loading ? 'Đang tạo tài khoản...' : 'Tạo tài khoản' }}
            <IconArrowRight v-if="!loading" :size="16" />
          </button>
        </form>
      </div>
    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.auth-page {
  display: flex;
  height: 100vh;
  font-family: 'Quicksand', sans-serif;
  overflow: hidden;
}

:deep(.auth-sidebar) {
  width: 480px;
  flex-shrink: 0;
}

.auth-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px;
  overflow-y: auto;
  transition: padding 0.35s ease;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  padding: 10px 0;
}

.auth-label {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 1.5px;
  color: #4263eb;
  text-transform: uppercase;
  margin: 0 0 10px;
}

.auth-title {
  font-size: 36px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.auth-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 24px;
}

.tab {
  padding: 10px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  color: #9ca3af;
  transition: all 0.2s;
}

.tab.active {
  background: white;
  color: #4b7bec;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  color: #9ca3af;
  pointer-events: none;
}

.auth-input {
  width: 100%;
  padding: 12px 14px 12px 42px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  color: #111827;
  background: #fafafa;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.auth-input:focus {
  border-color: #4263eb;
  background: #fff;
}

.auth-input::placeholder {
  color: #d1d5db;
}

/* Ẩn icon mắt mặc định của trình duyệt (Edge, Chrome) */
.auth-input::-ms-reveal,
.auth-input::-ms-clear,
.auth-input::-webkit-credentials-auto-fill-button {
  display: none !important;
}

.toggle-pw {
  position: absolute;
  right: 14px;
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  display: flex;
  align-items: center;
  padding: 0;
}

.toggle-pw:hover {
  color: #4263eb;
}

.gender-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.gender-btn {
  padding: 10px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.gender-btn.active {
  border-color: #4263eb;
  background: #eef2ff;
  color: #4263eb;
  font-weight: 600;
}

.gender-btn:hover:not(.active) {
  border-color: #c7d2fe;
  background: #f5f7ff;
}

.contact-toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.contact-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.contact-btn.active {
  border-color: #4263eb;
  background: #eef2ff;
  color: #4263eb;
  font-weight: 600;
}

.contact-btn:hover:not(.active) {
  border-color: #c7d2fe;
  background: #f5f7ff;
}

.checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  line-height: 1.5;
}

.checkbox-label input[type='checkbox'] {
  width: 16px;
  height: 16px;
  margin-top: 2px;
  accent-color: #4263eb;
  cursor: pointer;
  flex-shrink: 0;
}

.link {
  color: #4263eb;
  font-weight: 500;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}

.btn-primary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px;
  background: #4263eb;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
  width: 100%;
}

.btn-primary:hover {
  background: #3451d1;
}

.btn-primary:active {
  transform: scale(0.99);
}
.form-error {
  margin: 0;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 13px;
  font-weight: 500;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
