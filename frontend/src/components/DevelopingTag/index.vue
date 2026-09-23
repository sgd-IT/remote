<template>
  <span class="dev-tag" :class="{ 'is-mini': mini }" :title="tip || DEFAULT_TIP">敬请期待</span>
</template>

<script setup lang="ts">
/**
 * 后端接口尚未实现（空壳/占位）时，前端统一的视觉标识。
 * 用法参考：views/eiot/video/components/wsplayer/api.js 对未集成能力返回明确提示的做法。
 */
defineProps<{
  mini?: boolean // 行内紧凑模式（菜单项、标签栏）
  tip?: string // 悬浮说明，默认「该功能接口尚未实现」
}>()

const DEFAULT_TIP = '该功能尚未对接后端接口，敬请期待'
</script>

<style lang="scss" scoped>
// 采用 Element Plus warning 色，深色大屏与浅色后台列表均可辨识
.dev-tag {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0.5px;
  white-space: nowrap;
  flex-shrink: 0;
  margin-left: 6px; // 默认接在标题/菜单文字后面，调用方无需再定位
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.45);
  border-radius: 3px;

  &.is-mini {
    height: 14px;
    padding: 0 4px;
    font-size: 10px;
  }
}
</style>
