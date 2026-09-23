<template>
  <div class="placeholder-page">
    <header class="page-header">
      <div class="header-title">运维工作台</div>
      <el-tooltip content="返回值守大屏" placement="bottom">
        <div class="header-close-btn" @click="goBack">
          <el-icon :size="18"><Close /></el-icon>
        </div>
      </el-tooltip>
    </header>
    <div class="placeholder-body">
      <div class="placeholder-card">
        <el-icon :size="48" color="rgba(79,195,247,0.4)"><SetUp /></el-icon>
        <h2>运维工作台<DevelopingTag tip="未接入任何业务接口，仅为占位页" /></h2>
        <p>功能建设中，敬请期待...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import DevelopingTag from '@/components/DevelopingTag/index.vue'

const router = useRouter()
const goBack = () => router.push('/duty-screen')
</script>

<style lang="scss" scoped>
$bg1: #0a1628;
$slate: #8b9dc5;

.placeholder-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg1;
  font-family: 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 52px;
  flex-shrink: 0;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border-bottom: 1px solid rgba(79,195,247,0.1);
  box-shadow: 0 2px 10px rgba(0,0,0,0.18), inset 0 1px 0 rgba(79,195,247,0.04);
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  background: linear-gradient(90deg, #fff, #7ec8f8, #54a0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 2px;
}

.header-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  color: $slate;
  cursor: pointer;
  border: 1px solid rgba(79,195,247,0.12);
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 77, 79, 0.15);
    border-color: rgba(255, 77, 79, 0.4);
    color: #ff4d4f;
  }
}

.placeholder-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-card {
  text-align: center;
  padding: 60px 80px;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border: 1px solid rgba(79,195,247,0.1);
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.3);
  h2 { color: #fff; font-size: 22px; margin: 20px 0 10px; letter-spacing: 2px; }
  p { color: #8b9dc5; font-size: 14px; margin: 0; }
}
</style>
