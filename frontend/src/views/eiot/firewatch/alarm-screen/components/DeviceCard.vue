<template>
  <div
    class="device-card"
    :class="{ selected: selected, 'has-alarm': hasAlarm, 'has-fault': hasFault, 'layout-h': horizontal }"
    @click="$emit('select', device)">
    <!-- 标题栏 -->
    <div class="dc-title-bar">
      <span class="dc-title">{{ device.name }}</span>
      <span class="dc-online-dot"></span>
    </div>

    <!-- 内容包裹层 -->
    <div class="dc-content-wrap">
      <div class="dc-content">
        <!-- 历史图表按钮 -->
        <div
          v-if="showHistory"
          class="dc-history-btn"
          title="点击查看设备数据（图表）"
          @click.stop="$emit('history', device)">
          <el-icon><Clock /></el-icon>
        </div>

        <div class="dc-card-smoke">
          <!-- ====== 横向布局：左设备图 | 右状态+元数据 / 底部Tab ====== -->
          <template v-if="horizontal">
            <div class="dc-h-main">
              <div class="dc-left-panel">
                <div class="dc-device-center">
                  <div class="dc-device-img">
                    <img :src="iconDeviceHost" alt="" />
                  </div>
                  <div class="dc-bar-graph">
                    <span class="dc-bar" style="height: 40%"></span>
                    <span class="dc-bar" style="height: 65%"></span>
                    <span class="dc-bar" style="height: 50%"></span>
                    <span class="dc-bar" style="height: 80%"></span>
                  </div>
                  <div class="dc-indicator-dots">
                    <span></span><span></span><span></span><span></span><span></span>
                  </div>
                </div>
              </div>
              <div class="dc-right-panel">
                <div class="dc-status">
                  <div class="dc-status-item">
                    <div class="dc-status-icon-wrap"><img :src="iconAlarm" alt="" /></div>
                    <div class="dc-status-text">
                      <div>疑似火警</div>
                      <div class="dc-fire-val">{{ device.fireCount }}</div>
                    </div>
                  </div>
                  <div class="dc-status-item">
                    <div class="dc-status-icon-wrap"><img :src="iconElectric" alt="" /></div>
                    <div class="dc-status-text">
                      <div>故障</div>
                      <div class="dc-fault-val">{{ device.faultCount }}</div>
                    </div>
                  </div>
                </div>
                <div class="dc-meta-row">
                  <span class="dc-meta-label">名称：</span>
                  <span class="dc-meta-val" :title="device.fullName">{{ device.fullName || '-' }}</span>
                </div>
                <div class="dc-meta-row dc-alarm-row">
                  <span class="dc-meta-label">告警：</span>
                  <span class="dc-meta-val" :class="{ 'has-alarm': device.alarmText }">{{ device.alarmText || '—' }}</span>
                </div>
                <div class="dc-meta-row dc-position" :title="device.address">
                  <div class="dc-first-part">
                    <span class="dc-meta-label">地址：</span>
                    <span class="dc-meta-val">{{ device.address || '-' }}</span>
                  </div>
                  <div class="dc-gps">
                    <el-icon class="dc-loc-icon"><Location /></el-icon>
                  </div>
                </div>
              </div>
            </div>
            <div class="dc-tabs">
              <div class="dc-tab-wrap">
                <button
                  class="dc-btn dc-btn-ghost"
                  :class="{ active: device.activeTab === 'sub' }"
                  @click.stop="$emit('tab-change', device, 'sub')">子设备</button>
              </div>
              <div v-if="device.deviceType === 'transfer'" class="dc-tab-wrap">
                <button
                  class="dc-btn dc-btn-ghost"
                  :class="{ active: device.activeTab === 'bus' }"
                  @click.stop="$emit('tab-change', device, 'bus')">总线/多线</button>
              </div>
              <div class="dc-tab-wrap">
                <button
                  class="dc-btn dc-btn-ghost"
                  :class="{ active: device.activeTab === 'remote' }"
                  @click.stop="goRemoteConsole">远程控制</button>
              </div>
            </div>
          </template>

          <!-- ====== 方形布局（alarm-screen 默认） ====== -->
          <template v-else>
            <div class="dc-left-panel">
              <div v-if="showActions" class="dc-actions">
                <button class="dc-btn dc-btn-primary" @click.stop="$emit('action', device, 'reset')">复 位</button>
                <button class="dc-btn dc-btn-primary" @click.stop="$emit('action', device, 'confirm')">确 认</button>
                <button class="dc-btn dc-btn-primary" @click.stop="$emit('action', device, 'silence')">消 音</button>
              </div>
              <div class="dc-device-center">
                <div class="dc-device-img">
                  <img :src="iconDeviceHost" alt="" />
                </div>
                <div class="dc-bar-graph">
                  <span class="dc-bar" style="height: 40%"></span>
                  <span class="dc-bar" style="height: 65%"></span>
                  <span class="dc-bar" style="height: 50%"></span>
                  <span class="dc-bar" style="height: 80%"></span>
                </div>
                <div class="dc-indicator-dots">
                  <span></span><span></span><span></span><span></span><span></span>
                </div>
              </div>
              <div class="dc-status">
                <div class="dc-status-item">
                  <div class="dc-status-icon-wrap"><img :src="iconAlarm" alt="" /></div>
                  <div class="dc-status-text">
                    <div>疑似火警</div>
                    <div class="dc-fire-val">{{ device.fireCount }}</div>
                  </div>
                </div>
                <div class="dc-status-item">
                  <div class="dc-status-icon-wrap"><img :src="iconElectric" alt="" /></div>
                  <div class="dc-status-text">
                    <div>故障</div>
                    <div class="dc-fault-val">{{ device.faultCount }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="dc-right-panel">
              <div class="dc-tabs">
                <div class="dc-tab-wrap">
                  <button
                    class="dc-btn dc-btn-ghost"
                    :class="{ active: device.activeTab === 'sub' }"
                    @click.stop="$emit('tab-change', device, 'sub')">子设备</button>
                </div>
                <div v-if="device.deviceType === 'transfer'" class="dc-tab-wrap">
                  <button
                    class="dc-btn dc-btn-ghost"
                    :class="{ active: device.activeTab === 'bus' }"
                    @click.stop="$emit('tab-change', device, 'bus')">总线/多线</button>
                </div>
                <div class="dc-tab-wrap">
                  <button
                    class="dc-btn dc-btn-ghost"
                    :class="{ active: device.activeTab === 'remote' }"
                    @click.stop="goRemoteConsole">远程控制</button>
                </div>
              </div>
              <div class="dc-meta-row">
                <span class="dc-meta-label">名称：</span>
                <span class="dc-meta-val" :title="device.fullName">{{ device.fullName || '-' }}</span>
              </div>
              <div class="dc-meta-row dc-alarm-row">
                <span class="dc-meta-label">告警：</span>
                <span class="dc-meta-val" :class="{ 'has-alarm': device.alarmText }">{{ device.alarmText || '—' }}</span>
              </div>
              <div class="dc-meta-row dc-position" :title="device.address">
                <div class="dc-first-part">
                  <span class="dc-meta-label">地址：</span>
                  <span class="dc-meta-val">{{ device.address || '-' }}</span>
                </div>
                <div class="dc-gps">
                  <el-icon class="dc-loc-icon"><Location /></el-icon>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Location, Clock } from '@element-plus/icons-vue'
import iconDeviceHost from '@/assets/svg/device-host.svg'
import iconAlarm from '@/assets/svg/icon-alarm.svg'
import iconElectric from '@/assets/svg/icon-electric.svg'

defineOptions({ name: 'DeviceCard' })

const router = useRouter()

const props = withDefaults(defineProps<{
  device: any
  selected?: boolean
  showHistory?: boolean
  showActions?: boolean
  horizontal?: boolean
}>(), {
  selected: false,
  showHistory: true,
  showActions: true,
  horizontal: false
})

defineEmits<{
  (e: 'select', device: any): void
  (e: 'action', device: any, action: string): void
  (e: 'tab-change', device: any, tab: string): void
  (e: 'history', device: any): void
}>()

const hasAlarm = computed(() => props.device.fireCount > 0 || props.device.faultCount > 0)
const hasFault = computed(() => props.device.faultCount > 0)

const goRemoteConsole = () => {
  router.push({ path: '/remote-console', query: { deviceId: props.device.id } })
}
</script>

<style lang="scss" scoped>
$blue: #199b96; $red: #e65353; $orange: #f3a42a; $green: #53b57d;
$t1: #293b3d; $t2: #52686a; $t3: #7b8e90;

.device-card {
  position: relative;
  aspect-ratio: 1;
  cursor: pointer;
  border-radius: 6px;
  transition: transform 0.35s, border-color 0.35s, box-shadow 0.35s;

  background: #ffffff;
  border: 1px solid #cfe2e1;
  box-shadow: 0 2px 8px rgba(57, 91, 92, 0.06);

  &:hover, &.selected {
    transform: translateY(-2px);
    border-color: #8fc8c5;
    box-shadow: 0 5px 16px rgba(57, 91, 92, 0.12);
  }

  &.has-fault { border-left: 4px solid $orange; }
  &.has-alarm { border-left: 4px solid $red; }

  // ====== 横向布局 ======
  &.layout-h {
    aspect-ratio: auto;
    min-height: 180px;
  }

  // ====== 标题栏 ======
  .dc-title-bar {
    position: absolute;
    top: 3%;
    left: 0; right: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    z-index: 3;
    padding: 0 12%;
    .dc-title {
      font-size: 15px;
      font-weight: bold;
      color: $t1;
      letter-spacing: 2px;
      transition: color 0.35s;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .dc-online-dot {
      width: 7px; height: 7px;
      border-radius: 50%;
      background: $green;
      flex-shrink: 0;
    }
  }

  &:hover, &.selected {
    .dc-title-bar .dc-title { color: $t1; }
  }

  .dc-content-wrap {
    position: absolute;
    top: 0; left: 0;
    width: 100%;
    height: 100%;
  }

  .dc-content {
    position: absolute;
    top: 3%;
    left: 0; right: 0;
    bottom: 2%;
    z-index: 2;
    color: $t1;
    transition: filter 0.35s;
  }
  &:hover .dc-content,
  &.selected .dc-content {
    filter: brightness(1.02);
  }

  .dc-history-btn {
    position: absolute;
    top: 0;
    left: 15px;
    cursor: pointer;
    color: $t3;
    font-size: 20px;
    z-index: 3;
    transition: color 0.25s;
    &:hover { color: $blue; }
  }

  // ====== cardSmoke 主体 ======
  .dc-card-smoke {
    position: relative;
    width: 100%;
    height: 100%;
    display: grid;
    grid-template-rows: 58% 42%;
    grid-template-columns: 100%;
    padding: 30px 12px 4px;
    font-size: 13px;
    color: $t1;
    box-sizing: border-box;
  }

  // ====== 左侧面板 ======
  .dc-left-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 0;
    overflow: hidden;
  }

  // ====== 右侧面板 ======
  .dc-right-panel {
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }

  // ====== 操作按钮 ======
  .dc-actions {
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    .dc-btn-primary {
      padding: 4px 10px;
      font-size: 13px;
      color: $blue;
      background: #ffffff;
      border: 1px solid #9bcac8;
      border-radius: 5px;
      cursor: pointer;
      transition: all 0.25s;
      white-space: nowrap;
      letter-spacing: 1px;
      & + .dc-btn-primary { margin-top: 10px; }
      &:hover {
        background: #eef8f7;
        border-color: $blue;
      }
    }
  }

  // ====== 中央设备区域 ======
  .dc-device-center {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 0;
    gap: 2px;
  }

  .dc-device-img {
    display: flex;
    align-items: center;
    justify-content: center;
    img { width: 90px; height: 90px; }
  }

  .dc-bar-graph {
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: 5px;
    height: 22px;
    margin-top: 2px;
    .dc-bar {
      width: 5px;
      background: $blue;
      border-radius: 2px;
      min-height: 4px;
      opacity: 0.7;
    }
  }

  .dc-indicator-dots {
    display: flex;
    justify-content: center;
    gap: 6px;
    margin-top: 4px;
    span {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #c5dfde;
      &:nth-child(3) { background: $blue; }
    }
  }

  // ====== 状态指示 ======
  .dc-status {
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    .dc-status-item {
      display: flex;
      align-items: center;
      gap: 4px;
      white-space: nowrap;
      & + .dc-status-item { margin-top: 6px; }
      .dc-status-icon-wrap img {
        width: 30px;
        height: 30px;
        display: block;
      }
      .dc-status-text {
        display: flex;
        flex-direction: column;
        line-height: 1.3;
        > div:first-child {
          font-size: 14px;
          color: $t2;
        }
      }
      .dc-fire-val {
        font-size: 16px;
        font-weight: bold;
        color: $red;
        min-width: 14px;
      }
      .dc-fault-val {
        font-size: 16px;
        font-weight: bold;
        color: $orange;
        min-width: 14px;
      }
    }
  }

  // ====== Tab 按钮 ======
  .dc-tabs {
    display: flex;
    justify-content: center;
    flex-shrink: 0;
    margin-bottom: 2px;
    .dc-tab-wrap {
      margin-right: 8px;
      &:last-child { margin-right: 0; }
      .dc-btn-ghost {
        padding: 4px 10px;
        font-size: 13px;
        color: $t2;
        background: #ffffff;
        border: 1px solid #c5dfde;
        border-radius: 5px;
        cursor: pointer;
        transition: all 0.25s;
        white-space: nowrap;
        letter-spacing: 1px;
        &:hover {
          color: $blue;
          background: #eef8f7;
          border-color: #8fc8c5;
        }
        &.active {
          color: #ffffff;
          background: $blue;
          border-color: $blue;
        }
      }
    }
  }

  // ====== 元数据 ======
  .dc-meta-row {
    display: flex;
    align-items: center;
    padding: 0;
    font-size: 13px;
    line-height: 1.3;
    overflow: hidden;
    .dc-meta-label {
      color: $t2;
      flex-shrink: 0;
      letter-spacing: 0.3em;
      margin-right: 4px;
    }
    .dc-meta-val {
      color: $t1;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      &.has-alarm { color: $red; }
    }
  }

  .dc-alarm-row {
    font-weight: bold;
    .dc-meta-label { color: $t2; }
    .dc-meta-val {
      color: $t2;
      &.has-alarm { color: $red; }
    }
  }

  .dc-position {
    display: flex;
    align-items: center;
    justify-content: space-between;
    .dc-first-part {
      display: flex;
      align-items: center;
      flex: 1;
      min-width: 0;
    }
    .dc-gps {
      flex-shrink: 0;
      margin-left: 4px;
      display: flex;
      align-items: center;
      .dc-loc-icon {
        font-size: 14px;
        color: $t2;
      }
    }
  }

  // ====== 横向布局覆盖 ======
  &.layout-h {
    .dc-content-wrap {
      position: relative;
      width: 100%;
      height: 100%;
    }
    .dc-content {
      position: relative;
      top: auto; left: auto; right: auto; bottom: auto;
      width: 100%;
      height: 100%;
    }
    .dc-card-smoke {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 30px 14px 8px;
    }
    .dc-h-main {
      display: flex;
      flex: 1;
      gap: 10px;
      min-height: 0;
    }
    .dc-left-panel {
      width: 35%;
      justify-content: center;
    }
    .dc-right-panel {
      flex: 1;
      justify-content: center;
      gap: 2px;
    }
    .dc-device-center {
      flex: none;
    }
    .dc-device-img img {
      width: 72px;
      height: 72px;
    }
    .dc-status {
      flex-direction: row;
      justify-content: flex-start;
      gap: 16px;
      margin-bottom: 4px;
      .dc-status-item + .dc-status-item { margin-top: 0; }
    }
    .dc-tabs {
      margin-bottom: 0;
      margin-top: 2px;
    }
  }
}
</style>
