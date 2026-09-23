<template>
  <div>
    <!-- 添加区域 -->
    <div class="mb-4">
      <div class="text-sm font-medium text-gray-700 mb-2">添加摄像头</div>
      <div class="flex items-center gap-2">
        <el-select
          v-model="selectedCameraIds"
          multiple
          filterable
          placeholder="选择要添加的摄像头"
          style="width: 400px"
          clearable
        >
          <el-option
            v-for="cam in availableCameras"
            :key="cam.id"
            :label="cam.channelName || cam.channelId"
            :value="cam.id"
          />
        </el-select>
        <el-button type="primary" :disabled="selectedCameraIds.length === 0" @click="handleBind">
          添加绑定
        </el-button>
      </div>
    </div>

    <!-- 已绑定摄像头列表 -->
    <div>
      <div class="text-sm font-medium text-gray-700 mb-2">
        已绑定摄像头（{{ boundCameras.length }}）
      </div>
      <el-table :data="boundCameras" border v-loading="loading">
        <el-table-column prop="channelName" label="摄像头名称" min-width="150" />
        <el-table-column prop="channelId" label="通道编码" min-width="180" />
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button v-if="showPlay" link type="primary" @click="handlePlay(row)">
              播放
            </el-button>
            <el-button link type="danger" @click="handleUnbind(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && boundCameras.length === 0" description="暂无绑定摄像头" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCamerasByDevice,
  getRelationsByDevice,
  bindDeviceCamera,
  unbindDeviceCamera
} from '@/api/eiot/video/deviceCameraRelation.api'
import { getVideoCameraList } from '@/api/eiot/video/videoCamera.api'
import { useRouter } from 'vue-router'

const props = defineProps<{
  deviceId: number
  showPlay?: boolean
}>()

const router = useRouter()
const loading = ref(false)
const boundCameras = ref<any[]>([])
const relations = ref<any[]>([])
const allCameras = ref<any[]>([])
const selectedCameraIds = ref<number[]>([])

const availableCameras = computed(() => {
  const boundIds = new Set(boundCameras.value.map((c: any) => c.id))
  return allCameras.value.filter((c: any) => !boundIds.has(c.id))
})

const loadData = async () => {
  loading.value = true
  try {
    const [camRes, relRes, allRes] = await Promise.all([
      getCamerasByDevice(props.deviceId),
      getRelationsByDevice(props.deviceId),
      getVideoCameraList()
    ])
    boundCameras.value = camRes || []
    relations.value = relRes || []
    allCameras.value = allRes || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleBind = async () => {
  try {
    for (const cameraId of selectedCameraIds.value) {
      await bindDeviceCamera(props.deviceId, cameraId)
    }
    ElMessage.success('绑定成功')
    selectedCameraIds.value = []
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '绑定失败')
  }
}

const handleUnbind = async (camera: any) => {
  const relation = relations.value.find((r: any) => r.cameraId === camera.id)
  if (!relation) {
    ElMessage.warning('未找到绑定关系')
    return
  }
  try {
    await ElMessageBox.confirm('确认删除该摄像头的绑定？', '提示', { type: 'warning' })
    await unbindDeviceCamera(relation.id)
    ElMessage.success('解绑成功')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '解绑失败')
    }
  }
}

const handlePlay = (camera: any) => {
  router.push({
    path: '/video/video-manage',
    query: { cameraId: camera.id }
  })
}

onMounted(() => {
  loadData()
})

watch(() => props.deviceId, () => {
  loadData()
})
</script>
