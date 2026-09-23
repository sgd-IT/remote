<template>
  <div class="brand-panel">
    <!-- 背景层 -->
    <div class="bg-layer">
      <svg class="grid-bg" width="100%" height="100%">
        <defs>
          <pattern id="gisGrid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#4fc3f7" stroke-width="0.5" opacity="0.06"/>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#gisGrid)"/>
      </svg>
      <div class="building-silhouette">
        <svg viewBox="0 0 400 120" preserveAspectRatio="none">
          <path d="M0,120 L0,80 L20,80 L20,60 L40,60 L40,50 L60,50 L60,70 L80,70 L80,40 L100,40 L100,55 L120,55 L120,35 L150,35 L150,65 L170,65 L170,45 L200,45 L200,70 L220,70 L220,50 L250,50 L250,30 L280,30 L280,60 L300,60 L300,40 L330,40 L330,75 L360,75 L360,55 L400,55 L400,120Z" fill="#4fc3f7" opacity="0.08"/>
        </svg>
      </div>
    </div>

    <!-- 内容层 -->
    <div class="brand-content"> 
      <!-- 顶部品牌栏 -->
      <div class="brand-header">
        <div class="brand-logo">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round">
            <path d="M12 2L12 8"/>
            <path d="M8 6C8 6 10 4 12 4C14 4 16 6 16 6"/>
            <path d="M12 8C8 8 5 11 5 15C5 18.87 8.13 22 12 22C15.87 22 19 18.87 19 15C19 11 16 8 12 8Z"/>
          </svg>
        </div>
        <div class="brand-text">
          <div class="brand-name">智慧消防</div>
          <div class="brand-sub">REMOTE FIRE MONITORING</div>
        </div>
        <div class="system-status">
          <span class="status-dot"></span>
          <span class="status-text">系统运行正常</span>
        </div>
      </div>

      <!-- 中部 -->
      <div class="brand-middle">
        <DeviceNetwork />
        <div class="brand-title-area">
          <div class="title-bar"></div>
          <div>
            <h1 class="main-title">智慧消防远程值守系统</h1>
            <p class="sub-title">
              物联网实时监控 · AI 智能预警 · 24 小时远程值守<br/>
              GIS 可视化 · 视频监控联动 · 安消一体化管理
            </p>
          </div>
        </div>
      </div>

      <!-- 底部数据 -->
      <DataStats />
    </div>
  </div>
</template>

<script lang="ts" setup>
import DeviceNetwork from './DeviceNetwork.vue'
import DataStats from './DataStats.vue'
</script>

<style lang="scss" scoped>
.brand-panel {
  position: relative;
  width: 100%;
  height: 100%;
  background: linear-gradient(160deg, #060b14 0%, #0c192d 25%, #0f1f35 50%, #0a1420 100%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 30px 35px;
  box-sizing: border-box;
  overflow: hidden;
}
.bg-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.grid-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  animation: drift 30s linear infinite;
}
.building-silhouette {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 35%;
}
.building-silhouette svg {
  width: 100%;
  height: 100%;
}
.brand-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
}
.brand-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.brand-logo {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #ff5252, #d32f2f 40%, #1976d2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 15px rgba(255, 82, 82, 0.25);
  flex-shrink: 0;
}
.brand-text {
  flex: 1;
}
.brand-name {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1.5px;
}
.brand-sub {
  color: rgba(79, 195, 247, 0.5);
  font-size: 12px;
  letter-spacing: 3px;
}
.system-status {
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-dot {
  width: 8px;
  height: 8px;
  background: #00e676;
  border-radius: 50%;
  box-shadow: 0 0 6px #00e676;
  animation: statusPulse 2s infinite ease-in-out;
}
.status-text {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}
.brand-middle {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}
.brand-title-area {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 20px;
}
.title-bar {
  width: 3px;
  height: 40px;
  background: linear-gradient(180deg, #ff5252, #1976d2);
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 4px;
}
.main-title {
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0 0 12px;
}
.sub-title {
  color: rgba(255, 255, 255, 0.45);
  font-size: 14px;
  line-height: 1.9;
  margin: 0;
}

@keyframes drift {
  from { transform: translateY(0); }
  to { transform: translateY(-40px); }
}
@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (max-width: 767px) {
  .brand-panel {
    padding: 20px;
    justify-content: center;
    align-items: center;
    text-align: center;
  }
  .brand-header {
    flex-direction: column;
    gap: 8px;
  }
  .brand-text {
    text-align: center;
  }
  .system-status {
    display: none;
  }
  .brand-middle {
    display: none;
  }
  .brand-title-area {
    margin-top: 0;
    flex-direction: column;
    align-items: center;
  }
  .title-bar {
    display: none;
  }
  .main-title {
    font-size: 16px;
  }
  .sub-title {
    display: none;
  }
  .data-stats {
    display: none;
  }
}
</style>
