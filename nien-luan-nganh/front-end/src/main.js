import { createApp } from 'vue'
import { createPinia } from 'pinia'

import './assets/main.css'

import App from './App.vue'
import router from './router'
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

import {
  faMagnifyingGlass,
  faUser,
  faCircleInfo,
  faList,
  faThumbTack,
  faX,
  faPaperclip,
  faFaceLaughSquint,
  faPaperPlane,
  faUserGroup,
  faGear,
  faComment,
  faBars,
  faPlus,
  faUserPlus,
  faChevronDown,
  faChevronUp,
  faChevronRight,
  faEllipsisVertical,
  faPen,
  faTrash,
  faClockRotateLeft,
  faCheck,
  faReply,
  faUsers,
  faBell,
  faBellSlash,
  faFolderOpen,
  faImage,
  faVideo,
  faFileLines,
  faPlay,
  faArrowDown,
} from '@fortawesome/free-solid-svg-icons'

import { faHouse, faMessage, faFaceSmile } from '@fortawesome/free-regular-svg-icons'

library.add(
  faMagnifyingGlass,
  faUser,
  faCircleInfo,
  faList,
  faThumbTack,
  faX,
  faPaperclip,
  faFaceLaughSquint,
  faFaceSmile,
  faPaperPlane,
  faHouse,
  faMessage,
  faUserGroup,
  faGear,
  faComment,
  faBars,
  faPlus,
  faUserPlus,
  faChevronDown,
  faChevronUp,
  faChevronRight,
  faEllipsisVertical,
  faPen,
  faTrash,
  faClockRotateLeft,
  faCheck,
  faReply,
  faUsers,
  faBell,
  faBellSlash,
  faFolderOpen,
  faImage,
  faVideo,
  faFileLines,
  faPlay,
  faArrowDown,
)

async function enableMocking() {
  if (import.meta.env.VITE_USE_MOCK !== 'true') return
  const { worker } = await import('./mocks/browser')
  return worker.start({ onUnhandledRequest: 'bypass' })
}

enableMocking().then(() => {
  const app = createApp(App)

  app.use(createPinia())
  app.use(router)
  app.component('font-awesome-icon', FontAwesomeIcon)
  app.mount('#app')
})