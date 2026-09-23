<template>
  <div class="device-map">
    <el-card class="map-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Location /></el-icon>
            {{ t('dashboard.deviceMap') }}
          </span>
          <div class="map-controls">
            <el-radio-group v-model="displayMode" size="small">
              <el-radio-button label="markers">
                {{ t('dashboard.markers') }}
              </el-radio-button>
              <el-radio-button label="cluster">
                {{ t('dashboard.cluster') }}
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <div v-if="loading" class="map-loading">
        <el-skeleton animated :rows="5" />
      </div>

      <div v-else-if="error" class="map-error">
        <el-empty :description="error" />
      </div>

      <div v-else>
        <div ref="mapContainer" class="map-container"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Location } from '@element-plus/icons-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()

interface Props {
  devices?: any[]
  loading?: boolean
  error?: string
}

const props = withDefaults(defineProps<Props>(), {
  devices: () => [],
  loading: false,
  error: ''
})

const mapContainer = ref<HTMLElement>()
const displayMode = ref('markers')
let map: L.Map | null = null
let markersLayer: L.LayerGroup | null = null
let markerClusterGroup: any = null
let mapInitialized = false

interface DeviceMarker {
  deviceId: string
  name: string
  status: string
  productName: string
  lat: number
  lng: number
}

const initMap = async () => {
  if (mapInitialized || !mapContainer.value) return

  try {
    map = L.map(mapContainer.value).setView([39.9042, 116.4074], 10)

    L.tileLayer('https://map.geoq.cn/ArcGIS/rest/services/ChinaOnlineCommunity/MapServer/tile/{z}/{y}/{x}', {
      attribution: '&copy; <a href="https://www.geoq.cn/">GeoQ</a>',
      maxZoom: 18
    }).addTo(map)

    markersLayer = L.layerGroup().addTo(map)

    mapInitialized = true

    nextTick(() => {
      renderMarkers()
    })
  } catch (error) {
    console.error('地图初始化失败:', error)
  }
}

const renderMarkers = () => {
  if (!map || !markersLayer) return

  markersLayer.clearLayers()

  const devicesWithLocation = props.devices.filter(
    (device) => device.lat && device.lng && device.lat !== 0 && device.lng !== 0
  )

  if (devicesWithLocation.length === 0) {
    return
  }

  if (displayMode.value === 'cluster') {
    renderClusteredMarkers(devicesWithLocation)
  } else {
    renderIndividualMarkers(devicesWithLocation)
  }
}

const renderIndividualMarkers = (devices: any[]) => {
  if (!markersLayer || !map) return

  devices.forEach((device) => {
    const marker = createDeviceMarker(device)
    markersLayer!.addLayer(marker)
  })

  if (devices.length > 0) {
    const group = L.featureGroup(devices.map((d) => createDeviceMarker(d)))
    map.fitBounds(group.getBounds().pad(0.1))
  }
}

const renderClusteredMarkers = (devices: any[]) => {
  if (!markersLayer || !map) return

  devices.forEach((device) => {
    const marker = createDeviceMarker(device)
    markersLayer!.addLayer(marker)
  })

  if (devices.length > 0) {
    const group = L.featureGroup(devices.map((d) => createDeviceMarker(d)))
    map.fitBounds(group.getBounds().pad(0.1))
  }
}

const createDeviceMarker = (device: any): L.Marker => {
  const statusColor = device.status === 'ONLINE' ? '#67C23A' : device.status === 'OFFLINE' ? '#909399' : '#F56C6C'
  const statusText = device.status === 'ONLINE' ? t('dashboard.online') : device.status === 'OFFLINE' ? t('dashboard.offline') : t('dashboard.alert')

  const customIcon = L.divIcon({
    className: 'custom-device-marker',
    html: `
      <div style="
        background: ${statusColor};
        width: 32px;
        height: 32px;
        border-radius: 50%;
        border: 3px solid white;
        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: bold;
        font-size: 14px;
      ">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
        </svg>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32]
  })

  const marker = L.marker([device.lat, device.lng], { icon: customIcon })

  const popupContent = `
    <div style="min-width: 200px; font-family: Arial, sans-serif;">
      <div style="font-weight: bold; font-size: 14px; margin-bottom: 8px; color: #303133;">
        ${device.name || t('dashboard.unknownDevice')}
      </div>
      <div style="font-size: 12px; color: #606266; margin-bottom: 4px;">
        <strong>${t('dashboard.deviceId')}:</strong> ${device.id || '-'}
      </div>
      <div style="font-size: 12px; color: #606266; margin-bottom: 4px;">
        <strong>${t('dashboard.product')}:</strong> ${device.productName || device.product?.name || '-'}
      </div>
      <div style="font-size: 12px; color: #606266; margin-bottom: 4px;">
        <strong>${t('dashboard.status')}:</strong>
        <span style="color: ${statusColor}; font-weight: bold;">${statusText}</span>
      </div>
      <div style="font-size: 12px; color: #606266;">
        <strong>${t('dashboard.location')}:</strong> ${device.lat.toFixed(6)}, ${device.lng.toFixed(6)}
      </div>
    </div>
  `

  marker.bindPopup(popupContent, {
    maxWidth: 300,
    className: 'device-popup'
  })

  marker.on('click', () => {
    marker.openPopup()
  })

  return marker
}

const clearMarkers = () => {
  if (markersLayer) {
    markersLayer.clearLayers()
  }
}

watch(displayMode, () => {
  nextTick(() => {
    renderMarkers()
  })
})

watch(
  [() => props.loading, () => props.error, () => props.devices],
  () => {
    if (!props.loading && !props.error && props.devices.length > 0) {
      nextTick(() => {
        initMap()
      })
    }
  },
  { immediate: true }
)

onMounted(() => {
  nextTick(() => {
    if (!props.loading && !props.error && props.devices.length > 0) {
      initMap()
    }
  })
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped lang="scss">
.device-map {
  width: 100%;
}

.map-card {
  height: 100%;

  :deep(.el-card__header) {
    padding: 12px 20px;
    border-bottom: 1px solid #ebeef5;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.map-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.map-loading,
.map-error {
  height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
}

.map-container {
  width: 100%;
  height: 600px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
}

:deep(.custom-device-marker) {
  background: transparent !important;
  border: none !important;
}

:deep(.device-popup) {
  .leaflet-popup-content-wrapper {
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .leaflet-popup-content {
    margin: 12px;
  }
}

:deep(.leaflet-control-attribution) {
  font-size: 10px;
}
</style>
