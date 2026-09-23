<template>
  <div class="map-wrapper">
    <div v-if="props.isWrite" class="address-select">
      <el-select
        style="width: 100%"
        v-model="state.address"
        clearable
        filterable
        remote
        reserve-keyword
        placeholder="请输入地址搜索位置"
        :remote-method="autoSearch"
        @change="regeoCode"
        :loading="state.loading"
      >
        <el-option v-for="item in state.mapAddrOptions" :key="item.value" :label="item.name" :value="item.value" />
      </el-select>
    </div>
    <div v-if="props.isWrite" id="rwMap" class="mapContainer"></div>
    <div v-if="!props.isWrite">
      <div class="address-display">位置: {{ state.address || '暂无位置信息' }}</div>
      <div id="rMap" class="mapContainer"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AMapLoader from '@amap/amap-jsapi-loader'
import { propTypes } from '@/utils/propTypes'

const emits = defineEmits(['locateChange', 'update:center', 'addressChange'])
const state = reactive({
  lonLat : '',
  address : '',
  loading: false,
  latitude: '',
  longitude: '',
  map: null as any,
  mapAddrOptions: [] as any[],
  mapMarker: null as any,
  geocoder: null as any,
  autoComplete: null as any,
})
const props = defineProps({
  clickMap: propTypes.bool.def(false),
  isWrite: propTypes.bool.def(false),
  center: propTypes.string.def(''),
  address: propTypes.string.def(''),
})
const loadMap = () => {
  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: '330a3d930b185c5b005d31e90fa80b6e',
  }
  state.address = ''
  state.latitude = ''
  state.longitude = ''
  AMapLoader.load({
    key: '775f302305d6bbbf2da5ec525db91c74',
    version: '2.0',
    plugins: [
      'AMap.Geocoder',
      'AMap.AutoComplete',
    ],
  }).then(() => {
    initMap()
    if (props.clickMap) {
      state.map.on('click', (e) => {
        state.lonLat = e.lnglat.lng + ',' + e.lnglat.lat
        regeoCode(state.lonLat)
      })
    }
    initGeocoder()
    initAutoComplete()
    if (props.center) {
      regeoCode(props.center)
    }
  }).catch((err) => {
    console.error('高德地图 SDK 加载失败:', err)
  })
}

const initMap = () => {
  let mapId = props.isWrite ? 'rwMap' : 'rMap'
  state.map = new (window as any).AMap.Map(mapId, {
    resizeEnable: true,
    zoom: 11,
    keyboardEnable: false,
  })
}

const initGeocoder = () => {
  state.geocoder = new (window as any).AMap.Geocoder({
    city: '010',
    radius: 500,
    extensions: 'all'
  })
}

const initAutoComplete = () => {
  const autoOptions = {
    city: '全国',
  }
  state.autoComplete = new (window as any).AMap.AutoComplete(autoOptions)
}

const autoSearch = (queryValue: string) => {
  if (!queryValue || !state.autoComplete) {
    return
  }
  state.autoComplete.search(queryValue, (_status, result) => {
    const res = result.tips || []
    const temp = ref<any[]>([])
    res.forEach((p) => {
      if (p.location && p.location.lng && p.location.lat) {
        temp.value.push({
          name: (p.district || '') + p.name,
          value: p.location.lng + ',' + p.location.lat
        })
      }
    })
    state.mapAddrOptions = temp.value
  })
}

const setMarker = (lnglat) => {
  if (lnglat) {
    if (state.mapMarker !== null) {
      state.map.remove(state.mapMarker)
      state.lonLat = ''
    }
    state.mapMarker = new (window as any).AMap.Marker({
      position: new (window as any).AMap.LngLat(lnglat[0], lnglat[1]),
    })
    state.map.add(state.mapMarker)
    state.map.setCenter(lnglat)
    state.map.setZoom(16)
    state.mapMarker.setPosition(lnglat)
  }
}

const regeoCode = (lonLat) => {
  if (lonLat) {
    let lnglat = lonLat.split(',')
    state.latitude = lnglat[0]
    state.longitude = lnglat[1]
    emits('locateChange', lnglat)
    emits('update:center', lonLat)
    setMarker(lnglat)
    getAddress(lnglat)
  }
}

const getAddress = (lnglat) => {
  state.geocoder.getAddress(lnglat, (status, result) => {
    if (status === 'complete' && result.info === 'OK') {
      if (result && result.regeocode) {
        state.address = result.regeocode.formattedAddress
        emits('addressChange', result.regeocode.formattedAddress)
      }
    }
  })
}

onMounted(() => {
  loadMap()
})

watch(() => props.center, (newCenter) => {
  if (newCenter && state.map) {
    regeoCode(newCenter)
  }
})

watch(() => props.address, (newAddress) => {
  if (newAddress) {
    state.address = newAddress
  }
})
</script>

<style scoped>
.map-wrapper {
  width: 100%;
}
.address-select {
  margin-bottom: 10px;
}
.address-display {
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
  padding: 10px 0;
}
.mapContainer {
  width: 100%;
  height: 300px;
}
</style>
