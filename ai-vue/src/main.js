import { createApp } from 'vue'
import './style.css'
// 必须添加的 CSS
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import {createPinia} from 'pinia'
import ElementPlus  from 'element-plus'
const app = createApp(App)

const pinia = createPinia()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(router).use(pinia).use(ElementPlus).mount('#app')
