import { createApp } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import pinia from './stores'

import '@/assets/styles/variables.scss'
import '@/assets/styles/global.scss'
import '@/assets/styles/element.scss'

const app = createApp(App)

app.use(pinia)
app.use(router)

app.config.globalProperties.$ELEMENT = {
  locale: zhCn
}

app.mount('#app')
