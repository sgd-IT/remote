<template>
  <div class="iot-dashboard">
    <el-card shadow="never" class="stats-card-wrapper">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="6">
          <StatsCard
            :label="$t('dashboard.totalDevices')"
            :value="stats.deviceStats.totalDevices"
            icon="ep:device"
            icon-color="#409eff"
            icon-bg-color="#ecf5ff"
            :loading="statsLoading"
            :error="statsError.device"
            @retry="retryStats('device')"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <StatsCard
            :label="$t('dashboard.onlineDevices')"
            :value="stats.deviceStats.onlineDevices"
            icon="ep:circle-check"
            icon-color="#67c23a"
            icon-bg-color="#f0f9eb"
            :loading="statsLoading"
            :error="statsError.device"
            @retry="retryStats('device')"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <StatsCard
            :label="$t('dashboard.alertCount')"
            :value="stats.alertStats.todayAlerts"
            icon="ep:warning"
            icon-color="#e6a23c"
            icon-bg-color="#fdf6ec"
            :loading="statsLoading"
            :error="statsError.alert"
            @retry="retryStats('alert')"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <StatsCard
            :label="$t('dashboard.productCount')"
            :value="stats.productStats.totalProducts"
            icon="ep:box"
            icon-color="#909399"
            icon-bg-color="#f4f4f5"
            :loading="statsLoading"
            :error="statsError.product"
            @retry="retryStats('product')"
          />
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="map-wrapper">
      <template #header>
        <div class="card-header">
          <span class="title">
            <Icon icon="ep:map-location" />
            {{ $t('dashboard.deviceDistribution') }}
          </span>
          <el-tag type="info">
            {{ filteredDevices.length }} / {{ devices.length }}
            {{ $t('dashboard.devices') }}
          </el-tag>
        </div>
      </template>

      <MapSearchBar
        @search="handleSearch"
        @filter-change="handleFilterChange"
        @view-mode-change="handleViewModeChange"
      />

      <div class="map-content">
        <DeviceMap
          :devices="filteredDevices"
          :loading="mapLoading"
          :error="mapError"
          :clustering="viewMode === 'cluster'"
          @retry="retryMap"
          @marker-click="handleMarkerClick"
          @device-click="handleDeviceClick"
        />

        <DeviceInfoCard
          :device="selectedDevice"
          :visible="deviceInfoVisible"
          @close="handleCloseDeviceInfo"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import StatsCard from './components/StatsCard.vue'
import DeviceMap from './components/DeviceMap.vue'
import MapSearchBar from './components/MapSearchBar.vue'
import DeviceInfoCard from './components/DeviceInfoCard.vue'
import {
  getDeviceStats,
  getAlertStats,
  getProductStats,
  getDeviceLocations
} from '@/api/eiot/dashboard'
import type { DeviceLocation } from '@/api/eiot/dashboard'

defineOptions({ name: 'IoTDashboard' })

const stats = reactive({
  deviceStats: {
    totalDevices: 0,
    onlineDevices: 0,
    offlineDevices: 0,
    alertDevices: 0
  },
  alertStats: {
    totalAlerts: 0,
    todayAlerts: 0,
    criticalAlerts: 0,
    warningAlerts: 0
  },
  productStats: {
    totalProducts: 0,
    activeProducts: 0
  }
})

const devices = ref<DeviceLocation[]>([])
const filteredDevices = ref<DeviceLocation[]>([])
const selectedDevice = ref<DeviceLocation | null>(null)
const deviceInfoVisible = ref(false)

const statsLoading = ref(false)
const mapLoading = ref(false)
const mapError = ref(false)

const statsError = reactive({
  device: false,
  alert: false,
  product: false
})

const searchFilters = reactive({
  keyword: '',
  status: '',
  productKey: ''
})

const viewMode = ref<'marker' | 'cluster' | 'heatmap'>('marker')

const fetchStats = async () => {
  try {
    statsLoading.value = true
    const [deviceStats, alertStats, productStats] = await Promise.all([
      getDeviceStats(),
      getAlertStats(),
      getProductStats()
    ])
    
    stats.deviceStats = deviceStats || {
      totalDevices: 0,
      onlineDevices: 0,
      offlineDevices: 0,
      alertDevices: 0
    }
    stats.alertStats = alertStats || {
      totalAlerts: 0,
      todayAlerts: 0,
      criticalAlerts: 0,
      warningAlerts: 0
    }
    stats.productStats = productStats || {
      totalProducts: 0,
      activeProducts: 0
    }
    
    statsError.device = false
    statsError.alert = false
    statsError.product = false
  } catch (error) {
    console.error('Failed to fetch stats:', error)
    statsError.device = true
    statsError.alert = true
    statsError.product = true
  } finally {
    statsLoading.value = false
  }
}

const fetchDeviceLocations = async () => {
  try {
    mapLoading.value = true
    mapError.value = false
    
    const locations = await getDeviceLocations({
      pageNum: 1,
      pageSize: 100
    })
    
    devices.value = locations || []
    filteredDevices.value = locations || []
  } catch (error) {
    console.error('Failed to fetch device locations:', error)
    mapError.value = true
  } finally {
    mapLoading.value = false
  }
}

const retryStats = async (type: 'device' | 'alert' | 'product') => {
  await fetchStats()
}

const retryMap = () => {
  fetchDeviceLocations()
}

const applyFilters = () => {
  let result = [...devices.value]

  if (searchFilters.keyword) {
    const keyword = searchFilters.keyword.toLowerCase()
    result = result.filter(
      (device) =>
        (device.name && device.name.toLowerCase().includes(keyword)) ||
        (device.serialNo && device.serialNo.toLowerCase().includes(keyword))
    )
  }

  if (searchFilters.status) {
    result = result.filter((device) => device.status === searchFilters.status)
  }

  if (searchFilters.productKey) {
    result = result.filter((device) => device.productKey === searchFilters.productKey)
  }

  filteredDevices.value = result
}

const handleSearch = (keyword: string) => {
  searchFilters.keyword = keyword
  applyFilters()
}

const handleFilterChange = (filters: { keyword: string; status: string; productKey: any }) => {
  searchFilters.keyword = filters.keyword
  searchFilters.status = filters.status
  searchFilters.productKey = filters.productKey
  applyFilters()
}

const handleViewModeChange = (mode: 'marker' | 'cluster' | 'heatmap') => {
  viewMode.value = mode
}

const handleMarkerClick = (device: DeviceLocation) => {
  selectedDevice.value = device
  deviceInfoVisible.value = true
}

const handleDeviceClick = (device: DeviceLocation) => {
  selectedDevice.value = device
  deviceInfoVisible.value = true
}

const handleCloseDeviceInfo = () => {
  deviceInfoVisible.value = false
  selectedDevice.value = null
}

onMounted(async () => {
  await Promise.all([
    fetchStats(),
    fetchDeviceLocations()
  ])
})
</script>

<style scoped lang="scss">
.iot-dashboard {
  padding: 20px;
  background: var(--el-bg-color-page);
  min-height: calc(100vh - 140px);
}

.stats-card-wrapper {
  margin-bottom: 16px;
}

.map-wrapper {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
    }
  }
}

.map-content {
  position: relative;
  min-height: 650px;
}

@media (max-width: 768px) {
  .iot-dashboard {
    padding: 12px;
  }

  .map-content {
    min-height: 500px;
  }
}
</style>
