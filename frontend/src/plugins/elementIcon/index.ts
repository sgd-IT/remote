import type { App } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'


export const registerElIcon = (app: App<Element>) => {
    // 全局注册图标 ElIconXxxx
    for (const i in ElementPlusIconsVue) {
        const name = `${i}`;
        app.component(name, ElementPlusIconsVue[i])
    }
}