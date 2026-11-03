import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '@/views/ChatView.vue'
import ContactsView from '@/views/ContactsView.vue'
import PrivateSpaceView from '@/views/PrivateSpaceView.vue'
import SettingsView from '@/views/SettingsView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/chat',
      name: 'chat',
      component: ChatView,
      meta: { guestOnly: true },
    },
    {
      path: '/contacts',
      name: 'contacts',
      component: ContactsView,
      meta: { guestOnly: true },
    },
    {
      path: '/private-space',
      name: 'private-space',
      component: PrivateSpaceView,
      meta: { guestOnly: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: SettingsView,
      meta: { guestOnly: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { guestOnly: true },
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.guestOnly) {
    return auth.isLoggedIn ? { name: 'chat' } : true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login' }
  }
})

export default router
