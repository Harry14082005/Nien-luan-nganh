import { createApp } from 'vue'
import { createPinia } from 'pinia'

import './assets/main.css'

import App from './App.vue'
import router from './router'
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faMagnifyingGlass, faUser } from '@fortawesome/free-solid-svg-icons'
import { faCircleInfo } from '@fortawesome/free-solid-svg-icons'
import { faList } from '@fortawesome/free-solid-svg-icons'
import { faThumbTack } from '@fortawesome/free-solid-svg-icons'
import { faX } from '@fortawesome/free-solid-svg-icons'
import { faPaperclip } from '@fortawesome/free-solid-svg-icons'
import { faFaceLaughSquint } from '@fortawesome/free-solid-svg-icons/faFaceLaughSquint'
import { faPaperPlane } from '@fortawesome/free-solid-svg-icons'
import { faHouse } from '@fortawesome/free-regular-svg-icons/faHouse'
import { faMessage } from '@fortawesome/free-regular-svg-icons/faMessage'
import { faFaceSmile as faFaceSmileRegular } from '@fortawesome/free-regular-svg-icons/faFaceSmile'
import { faUserGroup } from '@fortawesome/free-solid-svg-icons'
import { faGear } from '@fortawesome/free-solid-svg-icons'
import { faComment } from '@fortawesome/free-solid-svg-icons'
import { faBars } from '@fortawesome/free-solid-svg-icons'
import { faPlus } from '@fortawesome/free-solid-svg-icons'
import { faFaceSmile } from '@fortawesome/free-solid-svg-icons'
import { faUserPlus } from '@fortawesome/free-solid-svg-icons'
import { faUserXmark } from '@fortawesome/free-solid-svg-icons'
import {
  faChevronDown,
  faChevronUp,
  faChevronRight,
  faChevronLeft,
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
  faBan,
  faUnlock,
  faUserMinus,
  faAddressBook,
} from '@fortawesome/free-solid-svg-icons'
library.add(
  faHouse,
  faMessage,
  faFaceSmileRegular,
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
  faUserGroup,
  faGear,
  faComment,
  faBars,
  faPlus,
  faUserPlus,
  faUserXmark,
  faChevronDown,
  faChevronUp,
  faChevronRight,
  faChevronLeft,
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
  faBan,
  faUnlock,
  faUserMinus,
  faAddressBook,
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