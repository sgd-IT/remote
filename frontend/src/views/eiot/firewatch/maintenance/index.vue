<template>
  <div class="maintenance-container">
    <!-- 告警横幅 -->
    <div class="alert-banners">
      <!-- 合同到期提醒横幅 -->
      <div class="alert-banner alert-banner-warning" v-if="dashboardData.contractExpiryReminders?.length">
        <div class="banner-content">
          <div class="banner-icon"><el-icon><WarningFilled /></el-icon></div>
          <div class="banner-info">
            <span class="banner-title">维保合同到期提醒</span>
            <span class="banner-desc" v-for="(item, idx) in dashboardData.contractExpiryReminders" :key="idx">
              {{ item.propertyName }}/{{ item.maintenanceUnit }} | 剩余{{ item.remainingDays }}天
            </span>
          </div>
        </div>
        <el-button type="warning" size="small">提醒续签</el-button>
      </div>

      <!-- 维保逾期提醒横幅 -->
      <div class="alert-banner alert-banner-danger" v-if="dashboardData.overdueOrders > 0 || dashboardData.overduePlans > 0">
        <div class="banner-content">
          <div class="banner-icon"><el-icon><CircleCloseFilled /></el-icon></div>
          <div class="banner-info">
            <span class="banner-title">维保未按期执行提醒</span>
            <span class="banner-desc">共有 {{ dashboardData.overdueOrders }} 个超期工单，{{ dashboardData.overduePlans }} 个逾期计划未执行</span>
          </div>
        </div>
        <el-button
          type="danger"
          size="small"
          :loading="urging"
          :disabled="!dashboardData.pendingUrgeCount && (dashboardData.overdueOrders > 0 || dashboardData.overduePlans > 0)"
          @click="handleUrgeExecution">
          {{ !dashboardData.pendingUrgeCount && (dashboardData.overdueOrders > 0 || dashboardData.overduePlans > 0) ? '已自动发送' : '催促执行' }}
        </el-button>
      </div>
    </div>

    <!-- 统计指标卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card stat-card-pending">
          <div class="stat-icon"><el-icon><WarningFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ dashboardData.pendingDispatch || 0 }}</div>
            <div class="stat-label">待派单</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-maintenance">
          <div class="stat-icon"><el-icon><Tools /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ dashboardData.inMaintenance || 0 }}</div>
            <div class="stat-label">维保中</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-contract">
          <div class="stat-icon"><el-icon><Box /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ dashboardData.contractExpiring || 0 }}</div>
            <div class="stat-label">合同即将到期</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-overdue">
          <div class="stat-icon"><el-icon><Clock /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ (dashboardData.overdueOrders || 0) + (dashboardData.overduePlans || 0) }}</div>
            <div class="stat-label">逾期计划</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 标签页导航 -->
    <div class="maintenance-tabs">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="故障工单" name="repair-order">
          <RepairOrders />
        </el-tab-pane>
        <el-tab-pane label="维保计划" name="plan">
          <Plans />
        </el-tab-pane>
        <el-tab-pane label="合同管理" name="contract">
          <Contracts />
        </el-tab-pane>
        <el-tab-pane label="维保报告" name="report">
          <Reports />
        </el-tab-pane>
        <el-tab-pane label="维保单位平台" name="unit">
          <Units />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { WarningFilled, CircleCloseFilled, Tools, Box, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMaintenanceDashboard, sendUrgeNotification } from '@/api/eiot/firewatch/firewatch.api'
import RepairOrders from './repair-orders.vue'
import Plans from './plans.vue'
import Contracts from './contracts.vue'
import Reports from './reports.vue'
import Units from './units.vue'

const activeTab = ref('repair-order')
const urging = ref(false)
const dashboardData = reactive({
  pendingDispatch: 0,
  inMaintenance: 0,
  pendingOrders: 0,
  overdueOrders: 0,
  contractExpiring: 0,
  overduePlans: 0,
  pendingUrgeCount: 0,
  contractExpiryReminders: [] as any[],
  overdueMaintenanceReminders: [] as any[]
})

const loadDashboard = async () => {
  try {
    const res = await getMaintenanceDashboard()
    if (res) {
      Object.assign(dashboardData, res)
    }
  } catch (e) {
    console.error('获取仪表板数据失败', e)
  }
}

const handleUrgeExecution = async () => {
  urging.value = true
  try {
    const res = await sendUrgeNotification()
    if (res) {
      if (res.successCount > 0) {
        ElMessage.success(
          `催促通知已发送，成功通知 ${res.successCount} 家企业` +
          (res.failCount > 0 ? `，${res.failCount} 家发送失败（无联系方式）` : '')
        )
      } else {
        ElMessage.info(res.message || '所有超期项目已自动发送催办通知')
      }
      // 刷新仪表板数据
      await loadDashboard()
    }
  } catch (e) {
    ElMessage.error('发送催促通知失败')
    console.error('发送催促通知失败', e)
  } finally {
    urging.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.maintenance-container {
  padding: 0;
}

/* 告警横幅 */
.alert-banners {
  margin-bottom: 16px;
}

.alert-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.alert-banner-warning {
  background: #fef0e6;
  border: 1px solid #fbe2c3;
}

.alert-banner-danger {
  background: #fef0f0;
  border: 1px solid #fde2e2;
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.banner-icon {
  font-size: 20px;
}

.alert-banner-warning .banner-icon { color: #e6a23c; }
.alert-banner-danger .banner-icon { color: #f56c6c; }

.banner-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.banner-title {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.banner-desc {
  font-size: 13px;
  color: #666;
}

/* 统计卡片 */
.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-card-pending .stat-icon { background: #fdf6ec; color: #e6a23c; }
.stat-card-maintenance .stat-icon { background: #f5f0ff; color: #7c3aed; }
.stat-card-contract .stat-icon { background: #f4f4f5; color: #909399; }
.stat-card-overdue .stat-icon { background: #fef0f0; color: #f56c6c; }

.stat-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-card-pending .stat-num { color: #e6a23c; }
.stat-card-maintenance .stat-num { color: #7c3aed; }
.stat-card-contract .stat-num { color: #909399; }
.stat-card-overdue .stat-num { color: #f56c6c; }

.stat-label {
  font-size: 13px;
  color: #909399;
}

/* 标签页 */
.maintenance-tabs {
  background: #fff;
  border-radius: 8px;
}

.maintenance-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.maintenance-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0 16px;
}
</style>
