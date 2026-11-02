import { defineStore } from 'pinia'
import * as authApi from '@/api/auth.api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    accessToken: localStorage.getItem('accessToken'),
  }),

  getters: {
    isLoggedIn: (s) => !!s.accessToken,
  },

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

    async login(identifier, password) {
      const data = await authApi.login({ identifier, password })
      this.saveSession(data)
      this.user = data?.user ?? (await authApi.getMe())
    },

    async register(body) {
      const data = await authApi.register(body)
      this.saveSession(data)
      this.user = data?.user ?? (await authApi.getMe())
    },

    async logout() {
      try {
        await authApi.logout(localStorage.getItem('refreshToken'))
      } catch {}
      localStorage.clear()
      this.$reset()
    },
  },
})
