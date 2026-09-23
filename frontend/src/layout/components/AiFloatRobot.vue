<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineOptions({ name: 'AiFloatRobot' })

const route = useRoute()
const router = useRouter()

// 当前已在 AI 分析页时隐藏悬浮机器人
const visible = computed(() => route.path !== '/eiot/firewatch/ai-analysis')

// 跳转 AI 分析页
const goToAiAnalysis = () => {
  router.push('/eiot/firewatch/ai-analysis')
}
</script>

<template>
  <el-tooltip v-if="visible" content="AI分析" placement="left">
    <div class="ai-float-robot" @click="goToAiAnalysis">
      <Icon icon="mdi:robot" :size="30" color="#fff" />
      <span class="ai-float-badge">AI</span>
    </div>
  </el-tooltip>
</template>

<style lang="scss" scoped>
.ai-float-robot {
  position: fixed;
  right: 32px;
  bottom: 128px;
  z-index: 2500;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: linear-gradient(135deg, #4f8cff 0%, #7a5cff 100%);
  box-shadow: 0 6px 20px rgba(79, 140, 255, 0.45);
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
  animation: ai-float-breathe 3s ease-in-out infinite;

  &:hover {
    animation-play-state: paused;
    transform: scale(1.1);
    box-shadow: 0 10px 28px rgba(79, 140, 255, 0.6);
  }

  .ai-float-badge {
    position: absolute;
    top: -4px;
    right: -4px;
    min-width: 20px;
    height: 20px;
    padding: 0 4px;
    border-radius: 10px;
    background: #ff4d4f;
    color: #fff;
    font-size: 11px;
    font-weight: 600;
    line-height: 20px;
    text-align: center;
    box-sizing: border-box;
  }
}

@keyframes ai-float-breathe {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-8px);
  }
}
</style>
