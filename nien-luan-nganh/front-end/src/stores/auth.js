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
    async login(identifier, password) {
      const { accessToken, refreshToken } = await authApi.login({ identifier, password })
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      this.accessToken = accessToken
      this.user = await authApi.getMe()
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