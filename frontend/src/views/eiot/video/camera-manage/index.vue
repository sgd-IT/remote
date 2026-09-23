<template>
  <ContentWrap>
    <div class="mb-4 flex justify-between">
      <div class="flex gap-2">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>新增摄像头
        </el-button>
        <el-button v-if="isPlatformTenant" type="success" @click="openIccBatchSelector">
          <el-icon><Download /></el-icon>ICC 自动获取
        </el-button>
      </div>
    </div>

    <el-table :data="cameraList" v-loading="loading" border>
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="deviceId" label="设备编码" width="140" />
      <el-table-column prop="channelId" label="通道号" width="80" />
      <el-table-column prop="channelName" label="摄像头名称" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="quality" label="码流" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.quality === '1' ? 'warning' : 'info'">
            {{ row.quality === '1' ? '主码流' : '辅码流' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="pullType" label="拉流" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.pullType === 1 ? 'primary' : 'success'">
            {{ row.pullType === 1 ? '大华ICC' : '海康威视' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="cameraTypeTag(row.type)">{{
            cameraTypeText(row.type)
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="supportReplay" label="回放" width="80">
        <template #default="{ row }">
          <el-icon :size="16" :color="row.supportReplay === 1 ? '#67C23A' : '#C0C4CC'">
            <component :is="row.supportReplay === 1 ? CircleCheckFilled : CircleCloseFilled" />
          </el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="address" label="安装地址" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑摄像头' : '新增摄像头'" width="560px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="设备编码" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备编码，如 100001" />
        </el-form-item>
        <el-form-item label="通道号" prop="channelId">
          <el-input v-model="form.channelId" placeholder="请输入通道号，如 2" />
        </el-form-item>
        <el-form-item label="摄像头名称" prop="channelName">
          <el-input v-model="form.channelName" placeholder="请输入摄像头名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="拉流类型">
              <el-select v-model="form.pullType" placeholder="请选择">
                <el-option label="海康威视" :value="0" />
                <el-option label="大华ICC" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="码流类型">
              <el-select v-model="form.quality" placeholder="请选择">
                <el-option label="主码流" value="1" />
                <el-option label="辅码流" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="摄像头类型">
              <el-select v-model="form.type" placeholder="请选择">
                <el-option label="录像机" :value="-1" />
                <el-option label="普通" :value="0" />
                <el-option label="漏铝" :value="1" />
                <el-option label="AI人员统计" :value="2" />
                <el-option label="热成像" :value="3" />
                <el-option label="其他" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支持回放">
              <el-switch v-model="form.supportReplay" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="经度">
          <el-input v-model.number="form.longitude" placeholder="请输入经度" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input v-model.number="form.latitude" placeholder="请输入纬度" />
        </el-form-item>
        <el-form-item label="安装地址">
          <el-input v-model="form.address" placeholder="请输入安装地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="isEdit" type="success" @click="handleCopyAdd">在此基础新增</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="iccSelectorVisible" title="ICC 自动获取" width="800px">
      <div class="mb-3 flex items-center gap-2">
        <span class="text-sm">目标租户：</span>
        <el-select
          v-model="targetTenantId"
          placeholder="请选择要添加到的租户"
          filterable
          clearable
          style="width: 260px"
        >
          <el-option v-for="t in tenantOptions" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </div>
      <el-table
        :data="iccDeviceRows"
        v-loading="iccLoading"
        border
        height="420"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        @selection-change="handleIccSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="deviceName" label="设备名称" width="180" />
        <el-table-column prop="deviceId" label="设备编码" width="150" />
        <el-table-column prop="channelId" label="通道号" width="90" />
        <el-table-column prop="channelName" label="通道名称" />
      </el-table>
      <template #footer>
        <el-button @click="iccSelectorVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="iccSelectedChannels.length === 0 || !targetTenantId"
          @click="handleIccBatchSubmit"
        >
          批量添加（{{ iccSelectedChannels.length }}）
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, CircleCheckFilled, CircleCloseFilled, Download } from '@element-plus/icons-vue'
import { ContentWrap } from '@/components/ContentWrap'
import { getTenantId } from '@/utils/auth'
import {
  getVideoCameraList,
  createVideoCamera,
  updateVideoCamera,
  deleteVideoCamera,
  getIccDevices,
  batchCreateVideoCamera,
  getTenantOptions
} from '@/api/eiot/video/videoCamera.api'

// 仅平台租户（系统管理员）可见 ICC 自动获取入口，企业端只允许表单手动添加
const isPlatformTenant = computed(() => Number(getTenantId()) === 1)

const loading = ref(false)
const cameraList = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<any>(null)
const form = ref<any>({
  id: null,
  deviceId: '',
  channelId: '',
  channelName: '',
  longitude: null,
  latitude: null,
  address: '',
  remark: '',
  quality: '2',
  pullType: 0,
  type: 0,
  supportReplay: 0
})

const cameraTypeText = (t: number) =>
  ['录像机', '普通', '漏铝', 'AI人员统计', '热成像', '其他'][(t ?? 0) + 1] || '未知'
const cameraTypeTag = (t: number) =>
  (['info', 'primary', 'warning', 'danger', 'danger', 'info'] as const)[(t ?? 0) + 1] || 'info'

const iccSelectorVisible = ref(false)
const iccLoading = ref(false)
const iccDeviceRows = ref<any[]>([])
const iccSelectedRows = ref<any[]>([])
const targetTenantId = ref<number | undefined>(undefined)
const tenantOptions = ref<any[]>([])

const rules = {
  channelId: [{ required: true, message: '通道编码不能为空', trigger: 'blur' }],
  deviceId: [{ required: true, message: '设备编码不能为空', trigger: 'blur' }],
  quality: [{ required: true, message: '码流类型不能为空', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getVideoCameraList()
    cameraList.value = res || []
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  isEdit.value = false
  form.value = {
    id: null,
    deviceId: '',
    channelId: '',
    channelName: '',
    longitude: null,
    latitude: null,
    address: '',
    remark: '',
    quality: '2',
    pullType: 0,
    type: 0,
    supportReplay: 0
  }
  dialogVisible.value = true
}

const handleCopyAdd = async () => {
  const copyData = {
    deviceId: form.value.deviceId,
    channelId: form.value.channelId,
    channelName: (form.value.channelName || ''),
    longitude: form.value.longitude,
    latitude: form.value.latitude,
    address: form.value.address,
    remark: form.value.remark,
    quality: form.value.quality,
    pullType: form.value.pullType,
    type: form.value.type,
    supportReplay: form.value.supportReplay
  }
  try {
    await createVideoCamera(copyData)
    ElMessage.success('新增成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '新增失败')
  }
}

/** 加载可分配摄像头的租户列表（仅平台租户可见此入口） */
const loadTenantOptions = async () => {
  try {
    tenantOptions.value = (await getTenantOptions()) || []
  } catch (e: any) {
    ElMessage.error(e.message || '获取租户列表失败')
  }
}

/** 打开 ICC 自动获取弹窗：拉取设备列表并构建设备-通道树（勾选设备=全选其通道，也可展开单独勾选） */
const openIccBatchSelector = async () => {
  iccSelectorVisible.value = true
  iccLoading.value = true
  targetTenantId.value = undefined
  loadTenantOptions()
  try {
    const res = await getIccDevices()
    iccDeviceRows.value = (res || []).map((d: any) => {
      const deviceId = d.deviceCode
      const deviceName = d.deviceName || d.deviceCode
      return {
        id: `device-${deviceId}`,
        deviceId,
        deviceName,
        children: (d.units || []).flatMap((u: any) =>
          (u.channels || []).map((c: any) => ({
            id: `channel-${deviceId}-${c.channelSeq != null ? c.channelSeq : c.channelCode}`,
            deviceId,
            deviceName,
            // 通道号回显具体通道序号（channelSeq），而非完整通道编码（channelCode）
            channelId: c.channelSeq != null ? String(c.channelSeq) : c.channelCode,
            channelName: c.channelName || c.channelCode
          }))
        )
      }
    })
  } catch (e: any) {
    ElMessage.error(e.message || '获取 ICC 设备列表失败')
  } finally {
    iccLoading.value = false
  }
}

const handleIccSelectionChange = (rows: any[]) => {
  iccSelectedRows.value = rows
}

/** 过滤出选中的通道行（排除设备父行） */
const iccSelectedChannels = computed(() =>
  iccSelectedRows.value.filter((r: any) => r.channelId)
)

/** 批量添加选中的 ICC 通道 */
const handleIccBatchSubmit = async () => {
  if (iccSelectedChannels.value.length === 0) return
  if (!targetTenantId.value) {
    ElMessage.warning('请先选择目标租户')
    return
  }
  try {
    const cameras = iccSelectedChannels.value.map((r: any) => ({
      deviceId: r.deviceId,
      channelId: r.channelId,
      channelName: r.channelName,
      quality: '2',
      pullType: 1,
      type: 0,
      supportReplay: 0
    }))
    const res: any = await batchCreateVideoCamera({ tenantId: targetTenantId.value, cameras })
    const successCount = res?.successCount ?? 0
    const skippedCount = res?.skippedCount ?? 0
    ElMessage.success(
      `添加成功 ${successCount} 个${skippedCount > 0 ? `，跳过已存在 ${skippedCount} 个` : ''}`
    )
    iccSelectorVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '批量添加失败')
  }
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    deviceId: row.deviceId || '',
    channelId: row.channelId || '',
    channelName: row.channelName || '',
    longitude: row.longitude ?? null,
    latitude: row.latitude ?? null,
    address: row.address || '',
    remark: row.remark || '',
    quality: row.quality || '2',
    pullType: row.pullType ?? 0,
    type: row.type ?? 0,
    supportReplay: row.supportReplay ?? 0
  }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该摄像头？', '提示', { type: 'warning' })
    await deleteVideoCamera(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {}
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const data = {
      id: form.value.id,
      deviceId: form.value.deviceId,
      channelId: form.value.channelId,
      channelName: form.value.channelName,
      longitude: form.value.longitude,
      latitude: form.value.latitude,
      address: form.value.address,
      remark: form.value.remark,
      quality: form.value.quality,
      pullType: form.value.pullType,
      type: form.value.type,
      supportReplay: form.value.supportReplay
    }
    if (isEdit.value) {
      await updateVideoCamera(data)
    } else {
      await createVideoCamera(data)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => {
  loadData()
})
</script>
