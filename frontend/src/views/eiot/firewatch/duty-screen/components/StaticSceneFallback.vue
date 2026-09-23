<template>
  <section class="scene-fallback" role="alert" aria-live="assertive">
    <div class="scene-fallback__card">
      <span class="scene-fallback__eyebrow">{{ scene === 'park' ? 'SMART PARK' : 'DONGGUAN 3D MAP' }}</span>
      <h2>{{ scene === 'park' ? '智慧园区暂时无法加载' : '三维地图暂时无法加载' }}</h2>
      <p>{{ message }}</p>
      <div class="scene-fallback__actions">
        <button type="button" @click="$emit('retry')">重新加载</button>
        <button v-if="scene === 'park'" type="button" class="secondary" @click="$emit('back')">返回地图</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { SceneFallbackReason, SceneName } from './scene-types'

defineProps<{ scene: SceneName; reason: SceneFallbackReason }>()
defineEmits<{ retry: []; back: [] }>()

const message = '场景资源、图形环境或通信协议未能在规定时间内完成。业务数据和告警面板仍保持可用。'
</script>

<style scoped>
.scene-fallback { display: grid; place-items: center; width: 100%; height: 100%; min-height: 300px; background: radial-gradient(circle at 50% 35%, rgba(23, 67, 120, .9), rgba(5, 16, 35, .98) 72%); color: #fff; }
.scene-fallback__card { width: min(460px, calc(100% - 48px)); padding: 28px; border: 1px solid rgba(93, 170, 255, .42); border-radius: 14px; background: rgba(8, 25, 52, .9); box-shadow: 0 16px 50px rgba(0, 0, 0, .28); }
.scene-fallback__eyebrow { color: #54d7ff; font-size: 11px; letter-spacing: .16em; }
.scene-fallback h2 { margin: 12px 0 8px; font-size: 22px; }
.scene-fallback p { margin: 0; color: #afc5e6; line-height: 1.65; }
.scene-fallback__actions { display: flex; gap: 10px; margin-top: 22px; }
.scene-fallback button { padding: 8px 16px; border: 1px solid #35cfff; border-radius: 6px; color: #071426; background: #35cfff; cursor: pointer; }
.scene-fallback button.secondary { color: #d7e7ff; background: transparent; border-color: #496b94; }
</style>
