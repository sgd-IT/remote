<template>
  <div class="leaflet-map-wrapper">
    <div v-if="props.isWrite" class="address-select">
      <el-input
        v-model="searchQuery"
        placeholder="输入地址搜索位置"
        clearable
        @input="onSearchInput"
        @keyup.enter="searchAddress"
      >
        <template #append>
          <el-button icon="Search" @click="searchAddress" />
        </template>
      </el-input>
      <div v-if="searchResults.length > 0" class="search-results">
        <div
          v-for="(result, index) in searchResults"
          :key="index"
          class="search-result-item"
          @click="selectSearchResult(result)"
        >
          {{ result.name }}
        </div>
      </div>
    </div>
    <div v-if="!props.isWrite" class="address-display">
      <span>位置: {{ displayAddress || '暂无位置信息' }}</span>
    </div>
    <div ref="mapContainer" class="map-container"></div>
  </div>
</template>

<script setup lang="ts">
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { propTypes } from '@/utils/propTypes'

const AMAP_KEY = 'a6347f91e48a9830a5005eefad6fbff1'

const emits = defineEmits(['locateChange', 'addressChange'])
const props = defineProps({
  clickMap: propTypes.bool.def(false),
  isWrite: propTypes.bool.def(false),
  center: propTypes.string.def(''),
  address: propTypes.string.def('')
})

const mapContainer = ref<HTMLElement>()
const searchQuery = ref('')
const searchResults = ref<any[]>([])
const displayAddress = ref('')
let map: L.Map | null = null
let marker: L.Marker | null = null
let searchTimer: ReturnType<typeof setTimeout> | null = null

// JSONP 请求封装，用于高德 Web API 跨域调用
const jsonpRequest = (url: string): Promise<any> => {
  return new Promise((resolve, reject) => {
    const callbackName = '_amap_cb_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11)
    ;(window as any)[callbackName] = (data: any) => {
      delete (window as any)[callbackName]
      document.body.removeChild(script)
      resolve(data)
    }
    const script = document.createElement('script')
    script.src = url + '&callback=' + callbackName
    script.onerror = () => {
      delete (window as any)[callbackName]
      if (script.parentNode) document.body.removeChild(script)
      reject(new Error('JSONP 请求失败'))
    }
    document.body.appendChild(script)
  })
}

const initMap = () => {
  if (!mapContainer.value) return

  const defaultCenter: [number, number] = [39.9042, 116.4074]
  const initialCenter = props.center ? parseCenter(props.center) : defaultCenter

  map = L.map(mapContainer.value).setView(initialCenter, 13)

  // GeoQ（智图）瓦片服务 — 国内可访问，标准 ArcGIS 格式
  L.tileLayer('https://map.geoq.cn/ArcGIS/rest/services/ChinaOnlineCommunity/MapServer/tile/{z}/{y}/{x}', {
    attribution: '&copy; <a href="https://www.geoq.cn/">GeoQ</a>',
    maxZoom: 18
  }).addTo(map)

  if (props.clickMap) {
    map.on('click', handleMapClick)
  }

  if (props.center) {
    const coords = parseCenter(props.center)
    setMarker(coords)
  }

  if (props.address) {
    displayAddress.value = props.address
  }
}

const parseCenter = (center: string): [number, number] => {
  if (!center) return [39.9042, 116.4074]
  const parts = center.split(',')
  if (parts.length >= 2) {
    // 传入格式为高德格式 "经度,纬度"
    const lng = parseFloat(parts[0].trim())
    const lat = parseFloat(parts[1].trim())
    if (!isNaN(lng) && !isNaN(lat)) {
      // Leaflet 使用 [lat, lng] 格式
      return [lat, lng]
    }
  }
  return [39.9042, 116.4074]
}

const setMarker = (coords: [number, number]) => {
  if (!map) return

  if (marker) {
    map.removeLayer(marker)
  }

  // 使用内联 SVG 图标，避免依赖外部 CDN
  const defaultIcon = L.divIcon({
    className: 'custom-leaflet-marker',
    html: `<svg xmlns="http://www.w3.org/2000/svg" width="25" height="41" viewBox="0 0 25 41">
      <path d="M12.5 0C5.6 0 0 5.6 0 12.5c0 9.4 12.5 28.5 12.5 28.5S25 21.9 25 12.5C25 5.6 19.4 0 12.5 0z" fill="#1677ff"/>
      <circle cx="12.5" cy="12.5" r="5" fill="white"/>
    </svg>`,
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34]
  })

  marker = L.marker(coords, { icon: defaultIcon }).addTo(map)
  map.setView(coords, 15)
}

const handleMapClick = (e: L.LeafletMouseEvent) => {
  const { lat, lng } = e.latlng
  setMarker([lat, lng])
  reverseGeocode(lat, lng)
  // 保持与高德格式一致：[经度, 纬度]
  emits('locateChange', [lng, lat])
}

// 防抖输入，300ms 后触发搜索
const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    searchAddress()
  }, 300)
}

const searchAddress = async () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }

  try {
    // 高德输入提示 API（JSONP 跨域）
    const url = `https://restapi.amap.com/v3/assistant/inputtips?key=${AMAP_KEY}&keywords=${encodeURIComponent(searchQuery.value)}&output=json`
    const data = await jsonpRequest(url)
    if (data.status === '1' && data.tips) {
      searchResults.value = data.tips.filter(
        (tip: any) => tip.location && tip.location.length > 0
      )
    }
  } catch (error) {
    console.error('地址搜索失败:', error)
  }
}

const selectSearchResult = (result: any) => {
  // 高德 location 格式: "lng,lat"（经度在前，纬度在后）
  const parts = result.location.split(',')
  const lng = parseFloat(parts[0])
  const lat = parseFloat(parts[1])
  const address = (result.district || '') + result.name

  setMarker([lat, lng])
  displayAddress.value = address
  searchResults.value = []
  searchQuery.value = ''

  // 保持与高德格式一致：[经度, 纬度]
  emits('locateChange', [lng, lat])
  emits('addressChange', address)
}

const reverseGeocode = async (lat: number, lon: number) => {
  try {
    // 高德逆地理编码 API（JSONP 跨域）
    const url = `https://restapi.amap.com/v3/geocode/regeo?key=${AMAP_KEY}&location=${lon},${lat}&output=json`
    const data = await jsonpRequest(url)
    if (data.status === '1' && data.regeocode) {
      displayAddress.value = data.regeocode.formatted_address || ''
      emits('addressChange', data.regeocode.formatted_address || '')
    }
  } catch (error) {
    console.error('逆地理编码失败:', error)
  }
}

watch(() => props.center, (newCenter) => {
  if (newCenter && map) {
    const coords = parseCenter(newCenter)
    setMarker(coords)
  }
})

watch(() => props.address, (newAddress) => {
  if (newAddress) {
    displayAddress.value = newAddress
  }
})

onMounted(() => {
  nextTick(() => {
    initMap()
  })
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
})
</script>

<style scoped>
.leaflet-map-wrapper {
  width: 100%;
}

.address-select {
  margin-bottom: 10px;
  position: relative;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 1000;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.search-result-item {
  padding: 10px;
  cursor: pointer;
  font-size: 14px;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:hover {
  background-color: #f5f7fa;
}

.search-result-item:last-child {
  border-bottom: none;
}

.address-display {
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
  padding: 10px 0;
}

.map-container {
  width: 100%;
  height: 300px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

:deep(.leaflet-control-attribution) {
  font-size: 10px;
}

:deep(.custom-leaflet-marker) {
  background: transparent !important;
  border: none !important;
}
</style>
