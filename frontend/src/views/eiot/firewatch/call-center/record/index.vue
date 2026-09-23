<template>
  <div class="record-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="归属租户">
        <el-select v-model="queryParams.tenantId" placeholder="全部租户" clearable filterable style="width: 180px">
          <el-option v-for="t in tenantOptions" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="匹配号码">
        <el-input v-model="queryParams.matchPhone" placeholder="座机号码" clearable style="width: 130px" />
      </el-form-item>
      <el-form-item label="主叫号码">
        <el-input v-model="queryParams.callerNbr" placeholder="主叫号码" clearable style="width: 130px" />
      </el-form-item>
      <el-form-item label="被叫号码">
        <el-input v-model="queryParams.calledNbr" placeholder="被叫号码" clearable style="width: 130px" />
      </el-form-item>
      <el-form-item label="通话方向">
        <el-select v-model="queryParams.callDirection" placeholder="全部" clearable style="width: 110px">
          <el-option label="呼出" :value="1" />
          <el-option label="呼入" :value="2" />
          <el-option label="未匹配" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="通话状态">
        <el-select v-model="queryParams.disposition" placeholder="全部" clearable style="width: 130px">
          <el-option label="已应答" value="ANSWERED" />
          <el-option label="未接听" value="NO ANSWER" />
          <el-option label="呼叫失败" value="FAILED" />
          <el-option label="忙线" value="BUSY" />
          <el-option label="拥塞" value="CONGESTION" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间">
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 340px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="startTime" label="开始时间" width="150" align="center" />
      <el-table-column label="归属租户" min-width="140">
        <template #default="{ row }">{{ tenantNameMap[row.tenantId] || `租户ID:${row.tenantId}` }}</template>
      </el-table-column>
      <el-table-column prop="matchPhone" label="匹配座机" width="130" align="center" />
      <el-table-column prop="callerNbr" label="主叫号码" width="130" align="center" />
      <el-table-column prop="calledNbr" label="被叫号码" width="130" align="center" />
      <el-table-column label="方向" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.callDirection === 1 ? 'success' : row.callDirection === 2 ? 'primary' : 'info'" size="small">
            {{ directionText(row.callDirection) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时长" width="80" align="center">
        <template #default="{ row }">{{ row.duration ? `${row.duration}s` : '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.disposition === 'ANSWERED' ? 'success' : 'warning'" size="small">
            {{ dispositionText(row.disposition) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <el-dropdown v-if="hasAnyVoice(row)" @command="(cmd: string) => playVoice(row, cmd)" style="margin-left: 8px">
            <el-button link type="success" size="small">
              录音<el-icon style="margin-left: 2px"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="row.voiceAddress" command="mixed">混合录音</el-dropdown-item>
                <el-dropdown-item v-if="row.voiceAddressIn" command="in">主叫录音</el-dropdown-item>
                <el-dropdown-item v-if="row.voiceAddressOut" command="out">被叫录音</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span v-else style="color: #c0c4cc; font-size: 12px">无录音</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 30, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="getList"
        @current-change="getList"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="通话详情" width="680px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="通话唯一标志" :span="2">{{ currentRecord.sessionId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="显示号码">{{ currentRecord.displayNbr || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务号码">{{ currentRecord.serviceNbr || '-' }}</el-descriptions-item>
        <el-descriptions-item label="主叫号码">{{ currentRecord.callerNbr || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被叫号码">{{ currentRecord.calledNbr || '-' }}</el-descriptions-item>
        <el-descriptions-item label="匹配座机">{{ currentRecord.matchPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="通话方向">
          {{ directionText(currentRecord.callDirection) }}
        </el-descriptions-item>
        <el-descriptions-item label="拨打时间">{{ currentRecord.dialTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="振铃时间">{{ currentRecord.ringTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentRecord.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentRecord.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="通话时长(秒)">{{ currentRecord.duration || '-' }}</el-descriptions-item>
        <el-descriptions-item label="通话时长(分)">{{ currentRecord.minDuration || '-' }}</el-descriptions-item>
        <el-descriptions-item label="话单类型">{{ currentRecord.serviceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="通话状态">{{ dispositionText(currentRecord.disposition) }}</el-descriptions-item>
        <el-descriptions-item label="分机号">{{ currentRecord.extensionNum || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应答时刻">{{ currentRecord.answer || '-' }}</el-descriptions-item>
        <el-descriptions-item label="挂机时刻">{{ currentRecord.hungUp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="挂断发起方">{{ currentRecord.hungUpOriginator === '0' ? '主叫' : currentRecord.hungUpOriginator === '1' ? '被叫' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="响铃时刻">{{ currentRecord.startRing || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上平台时间">{{ currentRecord.callIn || '-' }}</el-descriptions-item>
        <el-descriptions-item label="录音地址" :span="2">
          <div v-if="hasAnyVoice(currentRecord)">
            <div v-if="currentRecord.voiceAddress">混合：{{ currentRecord.voiceAddress }}</div>
            <div v-if="currentRecord.voiceAddressIn">主叫：{{ currentRecord.voiceAddressIn }}</div>
            <div v-if="currentRecord.voiceAddressOut">被叫：{{ currentRecord.voiceAddressOut }}</div>
          </div>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRecord.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 录音播放对话框 -->
    <el-dialog v-model="playerVisible" :title="playerTitle" width="520px" :close-on-click-modal="false">
      <div class="player-wrapper" v-loading="playerLoading">
        <audio v-if="audioUrl" :src="audioUrl" controls autoplay class="audio-player" @ended="releaseAudioUrl"></audio>
        <div v-else-if="!playerLoading" class="player-empty">暂无录音</div>
      </div>
      <template #footer>
        <el-button @click="closePlayer">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive, computed } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCallRecordPage, getCallRecordVoice } from '@/api/eiot/firewatch/callcenter.api'
import { getTenantPage } from '@/api/system/tenant'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const tenantOptions = ref<any[]>([])
const timeRange = ref<string[] | null>(null)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  tenantId: undefined as number | undefined,
  matchPhone: '',
  callerNbr: '',
  calledNbr: '',
  callDirection: undefined as number | undefined,
  disposition: ''
})

const tenantNameMap = computed(() => {
  const map: Record<number, string> = {}
  tenantOptions.value.forEach((t: any) => {
    map[t.id] = t.name
  })
  return map
})

// 通话方向（以归属方视角判断）：1=呼出(归属方为主叫) 2=呼入(归属方为被叫) 0=未匹配
const directionText = (d: number | undefined) => {
  if (d === 1) return '呼出'
  if (d === 2) return '呼入'
  return '未匹配'
}

const dispositionText = (d: string | undefined) => {
  const map: Record<string, string> = {
    ANSWERED: '已应答',
    'NO ANSWER': '未接听',
    FAILED: '呼叫失败',
    BUSY: '忙线',
    CONGESTION: '拥塞'
  }
  return map[d || ''] || d || '-'
}

const hasAnyVoice = (row: any) => {
  return !!(row?.voiceAddress || row?.voiceAddressIn || row?.voiceAddressOut)
}

// 加载租户下拉
const loadTenants = async () => {
  try {
    const res = await getTenantPage({ pageNo: 1, pageSize: 100 })
    if (res && res.list) {
      tenantOptions.value = res.list
    }
  } catch (e) {
    console.error('获取租户列表失败', e)
  }
}

const getList = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: queryParams.pageNo, pageSize: queryParams.pageSize }
    if (queryParams.tenantId) params.tenantId = queryParams.tenantId
    if (queryParams.matchPhone) params.matchPhone = queryParams.matchPhone
    if (queryParams.callerNbr) params.callerNbr = queryParams.callerNbr
    if (queryParams.calledNbr) params.calledNbr = queryParams.calledNbr
    if (queryParams.callDirection !== undefined) params.callDirection = queryParams.callDirection
    if (queryParams.disposition) params.disposition = queryParams.disposition
    if (timeRange.value && timeRange.value.length === 2) {
      params.startTimeBegin = timeRange.value[0]
      params.startTimeEnd = timeRange.value[1]
    }
    const res = await getCallRecordPage(params)
    if (res) {
      tableData.value = res.list || []
      total.value = res.total || 0
    }
  } catch (e) {
    console.error('获取通话记录列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const handleReset = () => {
  queryParams.tenantId = undefined
  queryParams.matchPhone = ''
  queryParams.callerNbr = ''
  queryParams.calledNbr = ''
  queryParams.callDirection = undefined
  queryParams.disposition = ''
  timeRange.value = null
  queryParams.pageNo = 1
  getList()
}

// 详情
const detailVisible = ref(false)
const currentRecord = ref<any>({})

const handleDetail = (row: any) => {
  currentRecord.value = row
  detailVisible.value = true
}

// 录音播放
const playerVisible = ref(false)
const playerLoading = ref(false)
const playerTitle = ref('录音播放')
const audioUrl = ref('')

const playVoice = async (row: any, side: string) => {
  const sideText = side === 'mixed' ? '混合录音' : side === 'in' ? '主叫录音' : '被叫录音'
  playerTitle.value = `${row.matchPhone || row.serviceNbr || ''} ${sideText}`
  playerVisible.value = true
  playerLoading.value = true
  audioUrl.value = ''
  try {
    const blob = await getCallRecordVoice(row.id, side)
    if (blob && blob.size > 0) {
      audioUrl.value = URL.createObjectURL(blob)
    } else {
      ElMessage.warning('录音文件为空')
    }
  } catch (e) {
    console.error('获取录音失败', e)
  } finally {
    playerLoading.value = false
  }
}

const releaseAudioUrl = () => {
  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value)
  }
}

const closePlayer = () => {
  releaseAudioUrl()
  audioUrl.value = ''
  playerVisible.value = false
}

onMounted(() => {
  loadTenants()
  getList()
})

onUnmounted(() => {
  releaseAudioUrl()
})
</script>

<style scoped>
.record-container {
  padding: 16px;
}
.search-form {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 16px;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.player-wrapper {
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.audio-player {
  width: 100%;
}
.player-empty {
  color: #909399;
  font-size: 13px;
}
</style>
