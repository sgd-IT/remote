<template>
  <ContentWrap>
    <div class="mb-4">
      <el-select v-model="selectedDeviceId" placeholder="选择消防部件" @change="loadRelations">
        <el-option
          v-for="device in deviceList"
          :key="device.id"
          :label="device.name || device.deviceName || device.id"
          :value="device.id"
        />
      </el-select>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <h3 class="mb-2">已绑定摄像头</h3>
        <el-table :data="boundCameras" border>
          <el-table-column prop="channelName" label="摄像头名称" />
          <el-table-column prop="channelId" label="通道编码" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="danger" link @click="handleUnbind(row)">解绑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
      <el-col :span="12">
        <h3 class="mb-2">可选摄像头</h3>
        <el-table :data="availableCameras" border>
          <el-table-column prop="channelName" label="摄像头名称" />
          <el-table-column prop="channelId" label="通道编码" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleBind(row)">绑定</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { ContentWrap } from '@/components/ContentWrap'
import {
  getCamerasByDevice,
  getRelationsByDevice,
  bindDeviceCamera,
  unbindDeviceCamera
} from '@/api/eiot/video/deviceCameraRelation.api'
import { getVideoCameraList } from '@/api/eiot/video/videoCamera.api'
import { getDevicesList } from '@/api/eiot/deviceinfo/devices.api'

const selectedDeviceId = ref<number | undefined>(undefined)
const deviceList = ref<any[]>([])
const boundCameras = ref<any[]>([])
const allCameras = ref<any[]>([])
const relations = ref<any[]>([])

const availableCameras = computed(() => {
  const boundIds = new Set(boundCameras.value.map(c => c.id))
  return allCameras.value.filter(c => !boundIds.has(c.id))
})

const loadDevices = async () => {
  const res = await getDevicesList({ pageNo: 1, pageSize: 100 })
  deviceList.value = res?.list || []
}

const loadAllCameras = async () => {
  const res = await getVideoCameraList()
  allCameras.value = res || []
}

const loadRelations = async () => {
  if (!selectedDeviceId.value) return
  const [camRes, relRes] = await Promise.all([
    getCamerasByDevice(selectedDeviceId.value),
    getRelationsByDevice(selectedDeviceId.value)
  ])
  boundCameras.value = camRes || []
  relations.value = relRes || []
}

const handleBind = async (camera: any) => {
  if (!selectedDeviceId.value) {
    ElMessage.warning('请先选择消防部件')
    return
  }
  try {
    await bindDeviceCamera(selectedDeviceId.value, camera.id)
    ElMessage.success('绑定成功')
    loadRelations()
  } catch (e: any) {
    ElMessage.error(e.message || '绑定失败')
  }
}

const handleUnbind = async (camera: any) => {
  const relation = relations.value.find(r => r.cameraId === camera.id)
  if (!relation) return
  try {
    await unbindDeviceCamera(relation.id)
    ElMessage.success('解绑成功')
    loadRelations()
  } catch (e: any) {
    ElMessage.error(e.message || '解绑失败')
  }
}

onMounted(() => {
  loadDevices()
  loadAllCameras()
})
</script>
