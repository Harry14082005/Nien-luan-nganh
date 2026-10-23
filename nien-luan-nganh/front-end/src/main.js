import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { library } from '@fortawesome/fontawesome-svg-core' //lay cai loi library tu FA
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome' 
import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons'
import { faCircleInfo } from '@fortawesome/free-solid-svg-icons'
import { faList } from '@fortawesome/free-solid-svg-icons'
import { faThumbTack } from '@fortawesome/free-solid-svg-icons'
import { faX } from '@fortawesome/free-solid-svg-icons'
import { faPaperclip } from '@fortawesome/free-solid-svg-icons'
import { faFaceLaughSquint } from '@fortawesome/free-solid-svg-icons/faFaceLaughSquint'
import { faPaperPlane } from '@fortawesome/free-solid-svg-icons'
library.add(faMagnifyingGlass,faCircleInfo,faList,faThumbTack,faX,faPaperclip,faFaceLaughSquint,faPaperPlane)
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.component('font-awesome-icon',FontAwesomeIcon)
app.mount('#app')

