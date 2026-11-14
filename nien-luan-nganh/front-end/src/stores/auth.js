import { defineStore } from 'pinia'
import * as authApi from '@/api/auth.api'
import { initGlobalWebSocket, wsService } from '@/services/websocket'

export const useAuthStore = defineStore('auth', {
  // ███████████████████████████████████████████████████████████████████████████
  // PHẦN 1: STATE & GETTERS (THÔNG TIN USER & TOKEN ĐĂNG NHẬP)
  // ███████████████████████████████████████████████████████████████████████████
  state: () => ({
    user: JSON.parse(localStorage.getItem('currentUser') || 'null'),
    accessToken: localStorage.getItem('accessToken'),
  }),

  getters: {
    isLoggedIn: (s) => !!s.accessToken,
  },

  // ███████████████████████████████████████████████████████████████████████████
  // PHẦN 2: ACTIONS (LƯU PHIÊN, ĐĂNG NHẬP, ĐĂNG KÝ & ĐĂNG XUẤT)
  // ███████████████████████████████████████████████████████████████████████████
  actions: {
    saveSession(data) {
      if (!data) return
      const accessToken = data.accessToken
      const refreshToken = data.refreshToken
      if (accessToken) {
        localStorage.setItem('accessToken', accessToken)
        this.accessToken = accessToken
      }
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken)
      }
    },

    async initUser() {
      if (this.accessToken && !this.user) {
        try {
          this.user = await authApi.getMe()
          if (this.user) {
            localStorage.setItem('currentUser', JSON.stringify(this.user))
          }
        } catch (e) {
          console.warn('Lỗi lấy thông tin cá nhân:', e)
        }
      }
      return this.user
    },

    async login(identifier, password) {
      const data = await authApi.login({ identifier, password })
      this.saveSession(data)
      this.user = data?.user ?? (await authApi.getMe())
      if (this.user) {
        localStorage.setItem('currentUser', JSON.stringify(this.user))
      }
      if (this.accessToken) {
        initGlobalWebSocket(this.accessToken)
      }
    },

    async register(body) {
      const data = await authApi.register(body)
      this.saveSession(data)
      this.user = data?.user ?? (await authApi.getMe())
      if (this.user) {
        localStorage.setItem('currentUser', JSON.stringify(this.user))
      }
      if (this.accessToken) {
        initGlobalWebSocket(this.accessToken)
      }
    },

    async logout() {
      try {
        await authApi.logout(localStorage.getItem('refreshToken'))
      } catch {}
      wsService.disconnect()
      localStorage.clear()
      this.$reset()
    },
  },
})
