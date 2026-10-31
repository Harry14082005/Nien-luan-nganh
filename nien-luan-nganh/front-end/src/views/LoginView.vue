<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import AuthSidebar    from '@/components/common/AuthSidebar.vue'
import IconMail       from '@/components/icons/IconMail.vue'
import IconLock       from '@/components/icons/IconLock.vue'
import IconEye        from '@/components/icons/IconEye.vue'
import IconEyeOff     from '@/components/icons/IconEyeOff.vue'
import IconArrowRight from '@/components/icons/IconArrowRight.vue'

const router = useRouter()
const emailOrPhone = ref('')
const password = ref('')
const rememberMe = ref(false)
const showPassword = ref(false)
const auth = useAuthStore()
const errorMessage = ref('')
const loading = ref(false)

async function handleLogin() {
  errorMessage.value = ''

  if (!emailOrPhone.value.trim() || !password.value) {
    errorMessage.value = 'Vui lòng nhập email/số điện thoại và mật khẩu'
    return
  }

  loading.value = true
  try {
    await auth.login(emailOrPhone.value.trim(), password.value)
    router.push('/chat')
  } catch (err) {
    errorMessage.value =
      err.response?.data?.message || 'Không thể kết nối tới máy chủ, vui lòng thử lại'
  } finally {
    loading.value = false
  }
}

function goToRegister() {
  router.push('/register')
}
</script>

<template>
  <div class="auth-page">
    <AuthSidebar />

    <main class="auth-main">
      <div class="auth-card">
        <p class="auth-label">CHÀO MỪNG TRỞ LẠI</p>
        <h2 class="auth-title">Đăng nhập</h2>
        <div class="auth-tabs">
          <button class="tab active" id="tab-login">Đăng nhập</button>
          <button class="tab" id="tab-register-from-login" @click="goToRegister">Đăng ký</button>
        </div>


        <form @submit.prevent="handleLogin" class="auth-form">
          <div class="field-group">
            <label class="field-label">Email hoặc số điện thoại</label>
            <div class="input-wrapper">
              <IconMail :size="16" class="input-icon" />
              <input
                id="login-email"
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
                id="login-password"
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

          <div class="form-row">
            <label class="checkbox-label">
              <input id="login-remember" type="checkbox" v-model="rememberMe" />
              <span>Ghi nhớ đăng nhập</span>
            </label>
            <a href="#" class="forgot-link">Quên mật khẩu?</a>
          </div>

          <button id="login-submit" type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập vào' }}
          <IconArrowRight v-if="!loading" :size="16" />
          </button>
        </form>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <p class="auth-switch">
          Chưa có tài khoản?
          <a id="go-to-register" href="#" @click.prevent="goToRegister">Đăng ký ngay</a>
        </p>
      </div>
    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.auth-page {
  display: grid;
  grid-template-columns: 480px 1fr;
  height: 100vh;
  font-family: 'Quicksand', sans-serif;
}

.auth-main {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px;
}

.auth-card {
  width: 100%;
  max-width: 440px;
}

.auth-label {
  font-size: 18px;
  font-weight: 700;
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
  margin-bottom: 28px;
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
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

.checkbox-label input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: #4263eb;
  cursor: pointer;
}

.forgot-link {
  font-size: 13px;
  color: #4263eb;
  text-decoration: none;
  font-weight: 500;
}

.forgot-link:hover {
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

.auth-switch {
  text-align: center;
  font-size: 13px;
  color: #6b7280;
  margin: 20px 0 0;
}

.auth-switch a {
  color: #4263eb;
  font-weight: 600;
  text-decoration: none;
}

.auth-switch a:hover {
  text-decoration: underline;
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
