<template>
  <div class="device-network">
    <svg class="network-svg" viewBox="0 0 300 220">
      <!-- 连接线 -->
      <line
        v-for="(line, idx) in connections"
        :key="`line-${idx}`"
        :x1="center.x" :y1="center.y"
        :x2="line.x" :y2="line.y"
        stroke="rgba(79,195,247,0.12)"
        stroke-width="1"
      />

      <!-- 中心节点 -->
      <g class="center-node">
        <circle :cx="center.x" :cy="center.y" r="14" fill="rgba(79,195,247,0.1)" stroke="#4fc3f7" stroke-width="1.5"/>
        <circle :cx="center.x" :cy="center.y" r="5" fill="#4fc3f7"/>
        <circle :cx="center.x" :cy="center.y" r="22" fill="none" stroke="rgba(79,195,247,0.2)" stroke-width="1" stroke-dasharray="4 4" class="rotating-ring"/>
      </g>

      <!-- 外围节点 -->
      <g
        v-for="(node, idx) in nodes"
        :key="`node-${idx}`"
        class="outer-node"
        :class="{ alarm: node.type === 'alarm' }"
      >
        <circle
          :cx="node.x" :cy="node.y" r="9"
          :fill="node.type === 'alarm' ? 'rgba(255,82,82,0.1)' : 'rgba(0,230,118,0.1)'"
          :stroke="node.type === 'alarm' ? '#ff5252' : '#00e676'"
          stroke-width="1"
        />
        <text
          :x="node.x" :y="node.y + 18"
          text-anchor="middle"
          :fill="node.type === 'alarm' ? '#ff5252' : '#00e676'"
          font-size="7"
        >{{ node.label }}</text>
      </g>
    </svg>
  </div>
</template>

<script lang="ts" setup>
const center = { x: 150, y: 110 }

const nodes = [
  { x: 90, y: 70, label: '烟感', type: 'normal' },
  { x: 210, y: 70, label: '消防栓', type: 'normal' },
  { x: 150, y: 50, label: '温感', type: 'normal' },
  { x: 70, y: 140, label: '告警', type: 'alarm' },
  { x: 230, y: 140, label: '监控', type: 'normal' },
  { x: 150, y: 170, label: '手报', type: 'normal' },
]

const connections = nodes.map(n => ({ x: n.x, y: n.y }))
</script>

<style lang="scss" scoped>
.device-network {
  width: 100%;
  max-width: 380px;
  margin: 0 auto;
}
.network-svg {
  width: 100%;
  height: auto;
}
.rotating-ring {
  transform-origin: 150px 110px;
  animation: rotate 20s linear infinite;
}
.outer-node.alarm circle:first-child {
  animation: alarmPulse 2s infinite ease-in-out;
}
@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@keyframes alarmPulse {
  0%, 100% {
    filter: drop-shadow(0 0 0 rgba(255, 82, 82, 0.4));
  }
  50% {
    filter: drop-shadow(0 0 8px rgba(255, 82, 82, 0.4));
  }
}
</style>
