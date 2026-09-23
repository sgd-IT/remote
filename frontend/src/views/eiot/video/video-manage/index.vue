<template>
  <ContentWrap>
    <div class="video-monitor-container">
      <!-- 左侧树形 -->
      <div class="video-sidebar">
        <el-input
          v-model="treeKeyword"
          placeholder="搜索摄像头"
          clearable
          style="width: 100%"
        />
        <el-tree
          ref="treeRef"
          :data="cameraTree"
          :props="{ label: 'channelName', children: 'children' }"
          :filter-node-method="filterNode"
          highlight-current
          @node-click="handleTreeNodeClick"
        />
      </div>
      <!-- 右侧播放器区域（WSPlayer 插件自带分屏功能） -->
      <div class="video-main">
        <WsPlayerWrapper
          :camera="currentCamera"
          @close="currentCamera = null"
        />
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ContentWrap } from '@/components/ContentWrap'
import { getVideoCameraPage } from '@/api/eiot/video/videoCamera.api'
import WsPlayerWrapper from '../components/WsPlayerWrapper.vue'

const treeKeyword = ref('')
const treeRef = ref<any>(null)
const cameraTree = ref<any[]>([])
/** 当前选中的摄像头（点击左侧树节点后更新，WSPlayer 自动在当前活动窗口播放） */
const currentCamera = ref<any>(null)

watch(treeKeyword, (val) => {
  treeRef.value?.filter(val)
})

const filterNode = (value: string, data: any) => {
  if (!value) return true
  return data.channelName?.includes(value) || data.channelId?.includes(value)
}

const loadCameras = async () => {
  const res = await getVideoCameraPage({ pageNo: 1, pageSize: 1000 })
  const list = res?.list || []
  cameraTree.value = list.map((c: any) => ({
    ...c,
    channelName: c.channelName || c.channelId
  }))
}

const handleTreeNodeClick = (node: any) => {
  if (!node.id) return
  // 使用新对象引用，确保重复点击同一节点也能触发重新播放
  currentCamera.value = { ...node }
}

onMounted(() => {
  loadCameras()
})
</script>

<style scoped>
.video-monitor-container {
  display: flex;
  height: calc(100vh - 160px);
  gap: 16px;
}
.video-sidebar {
  width: 260px;
  border-right: 1px solid #e4e7ed;
  padding-right: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.video-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
</style>
