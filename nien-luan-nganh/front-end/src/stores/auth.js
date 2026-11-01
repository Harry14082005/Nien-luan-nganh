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
  saveSession({ accessToken, refreshToken }) {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    this.accessToken = accessToken
  },

  async login(identifier, password) {
    const tokens = await authApi.login({ identifier, password })
    this.saveSession(tokens)
    this.user = await authApi.getMe()
  },

  async register(body) {
    const { accessToken, refreshToken, user } = await authApi.register(body)
    this.saveSession({ accessToken, refreshToken })
    this.user = user ?? (await authApi.getMe())
  },

  async logout() {
    try {
      await authApi.logout(localStorage.getItem('refreshToken'))
    } catch {}
    localStorage.clear()
    this.$reset()
  },
},

    async logout() {
      try {
        await authApi.logout(localStorage.getItem('refreshToken'))
      } catch {}
      localStorage.clear()
      this.$reset()
    },
  },
)