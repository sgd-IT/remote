<template>
  <div class="sub-header">
    <div class="header-left">
      <div class="device-name">{{ deviceName || '请选择设备' }}</div>
      <div class="info-group">
        <div class="info-item">
          <span class="info-label">单位名称：</span>
          <span class="info-value">{{ companyName }}</span>
        </div>
      <div class="info-item">
        <span class="info-label">主机品牌：</span>
        <span class="info-value">{{ brand }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">主机型号：</span>
        <span class="info-value">{{ model }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">通讯状态：</span>
        <span class="info-value status-online">通信正常</span>
      </div>
      <div class="info-item">
        <span class="info-label">点位数量：</span>
        <span class="info-value">{{ pointTotal }}/{{ pointOnline }}</span>
      </div>
    </div>
    </div>

    <div class="contacts">
      <template v-if="props.dutyPersons.length">
        <div v-for="(c, i) in props.dutyPersons" :key="i" class="contact-item">
          <el-icon :size="13"><Phone /></el-icon>
          <span class="contact-label">{{ c.dutyUserPosition || '值班员' }}：{{ c.dutyUserName }}</span>
          <span class="contact-phone">{{ c.dutyUserPhone || '--' }}</span>
        </div>
      </template>
      <div v-else class="contact-item">
        <el-icon :size="13"><Phone /></el-icon>
        <span class="contact-label">值班员：--</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { Phone } from '@element-plus/icons-vue'

/** 当前值班人员项 */
export interface DutyPerson {
  dutyUserName?: string
  dutyUserPhone?: string
  dutyUserPosition?: string
}

const props = withDefaults(
  defineProps<{
    deviceName?: string
    companyName?: string
    brand?: string
    model?: string
    pointTotal?: number
    pointOnline?: number
    /** 当前单位当前时间段的值班人员列表 */
    dutyPersons?: DutyPerson[]
  }>(),
  {
    dutyPersons: () => []
  }
)
</script>

<style lang="scss" scoped>
@import '../styles/common.scss';

.sub-header {
  height: 48px;
  background: $bg-panel;
  border-bottom: 1px solid $border-color;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  color: $text-primary;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.device-name {
  font-size: 17px;
  font-weight: 700;
  color: #ffffff;
  white-space: nowrap;
  flex-shrink: 0;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.25);
}

.info-group {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.info-item {
  font-size: 13px;
}

.info-label {
  color: $text-secondary;
}

.info-value {
  color: $text-primary;
}

.status-online {
  color: $success;
}

.contacts {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-secondary;
}

.contact-label {
  color: $text-muted;
}

.contact-phone {
  color: $text-primary;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}
</style>
