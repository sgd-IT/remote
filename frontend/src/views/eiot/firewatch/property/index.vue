<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="物业名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入物业名称" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="物业编码" prop="code">
        <el-input v-model="queryParams.code" placeholder="请输入物业编码" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="城市" prop="city">
        <el-input v-model="queryParams.city" placeholder="请输入城市" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="风险等级" prop="riskLevel">
        <el-select v-model="queryParams.riskLevel" placeholder="请选择" clearable style="width: 120px">
          <el-option label="重大风险" value="critical" />
          <el-option label="较大风险" value="major" />
          <el-option label="一般风险" value="normal" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 100px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 操作栏 -->
  <ContentWrap>
    <el-row :gutter="10" class="mb-10px">
      <el-col :span="1.5">
        <el-button type="primary" @click="handleCreate"><Icon icon="ep:plus" class="mr-5px" />新增物业</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" @click="handleExport"><Icon icon="ep:download" class="mr-5px" />导出</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column label="物业编码" prop="code" width="150" />
      <el-table-column label="物业名称" prop="name" min-width="200" show-overflow-tooltip />
      <el-table-column label="地址" min-width="250">
        <template #default="{ row }">
          {{ row.province }}{{ row.city }}{{ row.district }}{{ row.address }}
        </template>
      </el-table-column>
      <el-table-column label="联系人" prop="contactName" width="100" />
      <el-table-column label="联系电话" prop="contactPhone" width="120" />
      <el-table-column label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag :type="getRiskLevelTag(row.riskLevel)">{{ row.riskLevelName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="物业类型" prop="propertyType" width="100" />
      <el-table-column label="楼层数" prop="floorCount" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" @click="handleUpdate(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </ContentWrap>

  <!-- 新增/编辑对话框 -->
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-row>
        <el-col :span="12">
          <el-form-item label="物业名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入物业名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="物业编码" prop="code">
            <el-input v-model="formData.code" placeholder="自动生成" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="省份" prop="province">
            <el-input v-model="formData.province" placeholder="请输入省份" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="城市" prop="city">
            <el-input v-model="formData.city" placeholder="请输入城市" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="区县" prop="district">
            <el-input v-model="formData.district" placeholder="请输入区县" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="详细地址" prop="address">
        <el-input v-model="formData.address" placeholder="请输入详细地址" />
      </el-form-item>
      <el-row>
        <el-col :span="12">
          <el-form-item label="联系人" prop="contactName">
            <el-input v-model="formData.contactName" placeholder="请输入联系人" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="风险等级" prop="riskLevel">
            <el-select v-model="formData.riskLevel" placeholder="请选择" style="width: 100%">
              <el-option label="重大风险" value="critical" />
              <el-option label="较大风险" value="major" />
              <el-option label="一般风险" value="normal" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="物业类型" prop="propertyType">
            <el-select v-model="formData.propertyType" placeholder="请选择" style="width: 100%">
              <el-option label="住宅小区" value="住宅小区" />
              <el-option label="商业综合体" value="商业综合体" />
              <el-option label="写字楼" value="写字楼" />
              <el-option label="工业园区" value="工业园区" />
              <el-option label="学校" value="学校" />
              <el-option label="医院" value="医院" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="楼层数" prop="floorCount">
            <el-input-number v-model="formData.floorCount" :min="1" :max="200" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="建筑面积" prop="buildingArea">
            <el-input v-model="formData.buildingArea" placeholder="请输入面积">
              <template #append>平方米</template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="经度" prop="longitude">
            <el-input v-model="formData.longitude" placeholder="WGS84 十进制度，-180 至 180" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="纬度" prop="latitude">
            <el-input v-model="formData.latitude" placeholder="WGS84 十进制度，-90 至 90" />
          </el-form-item>
        </el-col>
      </el-row>
      <div class="el-form-item__description">先经度、后纬度；请同时填写或同时留空。合法的东莞市外坐标可保存，但不会在东莞大屏显示。</div>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>

  <!-- 详情对话框 -->
  <el-dialog v-model="detailVisible" title="物业详情" width="700px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="物业编码">{{ detailData.code }}</el-descriptions-item>
      <el-descriptions-item label="物业名称">{{ detailData.name }}</el-descriptions-item>
      <el-descriptions-item label="省份">{{ detailData.province }}</el-descriptions-item>
      <el-descriptions-item label="城市">{{ detailData.city }}</el-descriptions-item>
      <el-descriptions-item label="区县">{{ detailData.district }}</el-descriptions-item>
      <el-descriptions-item label="详细地址" :span="3">{{ detailData.address }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ detailData.contactName }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ detailData.contactPhone }}</el-descriptions-item>
      <el-descriptions-item label="风险等级">
        <el-tag :type="getRiskLevelTag(detailData.riskLevel)">{{ detailData.riskLevelName }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="物业类型">{{ detailData.propertyType }}</el-descriptions-item>
      <el-descriptions-item label="楼层数">{{ detailData.floorCount }}层</el-descriptions-item>
      <el-descriptions-item label="建筑面积">{{ detailData.buildingArea }}平方米</el-descriptions-item>
      <el-descriptions-item label="设备数量">{{ detailData.deviceCount || 0 }}台</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="detailData.status === 1 ? 'success' : 'info'">
          {{ detailData.status === 1 ? '启用' : '停用' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/config/axios'
import download from '@/utils/download'
import {
  getPropertyPage,
  createProperty,
  updateProperty,
  deleteProperty,
  updatePropertyStatus,
  getPropertyStatistics
} from '@/api/eiot/firewatch/firewatch.api'
import { parseDecimal } from '@/views/eiot/firewatch/duty-screen/components/property-coordinate'
import { announcePropertyMapChange } from '@/views/eiot/firewatch/duty-screen/components/property-map-events'

defineOptions({ name: 'FireWatchProperty' })

const loading = ref(false)
const total = ref(0)
const tableData = ref([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  code: undefined,
  city: undefined,
  riskLevel: undefined,
  status: undefined
})

const queryFormRef = ref<FormInstance>()

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const formData = ref<any>({
  id: undefined,
  name: '',
  code: '',
  address: '',
  province: '',
  city: '',
  district: '',
  longitude: undefined,
  latitude: undefined,
  contactName: '',
  contactPhone: '',
  riskLevel: 'normal',
  propertyType: '',
  floorCount: undefined,
  buildingArea: undefined
})

const coordinateRule = (field: 'longitude' | 'latitude', min: number, max: number) => (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
  const other = field === 'longitude' ? formData.value.latitude : formData.value.longitude
  const empty = (item: unknown) => item === null || item === undefined || String(item).trim() === ''
  if (empty(value) && empty(other)) return callback()
  if (empty(value) || empty(other)) return callback(new Error('经纬度必须同时填写或同时留空'))
  if (parseDecimal(value, min, max) === null) return callback(new Error(`${field === 'longitude' ? '经度' : '纬度'}必须是 ${min} 至 ${max} 的十进制度`))
  callback()
}

const formRules = reactive<FormRules>({
  name: [{ required: true, message: '请输入物业名称', trigger: 'blur' }],
  longitude: [{ validator: coordinateRule('longitude', -180, 180), trigger: 'blur' }],
  latitude: [{ validator: coordinateRule('latitude', -90, 90), trigger: 'blur' }],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
})

// 详情
const detailVisible = ref(false)
const detailData = ref<any>({})

// 获取列表
const getList = async () => {
  loading.value = true
  try {
    const res = await getPropertyPage(queryParams)
    // getPropertyPage 内部 request.get 已解包 CommonResult.data，res 就是分页数据
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

// 重置
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 新增
const handleCreate = () => {
  resetForm()
  dialogTitle.value = '新增物业'
  dialogVisible.value = true
}

// 编辑
const handleUpdate = (row: any) => {
  resetForm()
  dialogTitle.value = '编辑物业'
  formData.value = { ...row }
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该物业？删除后无法恢复', '提示', { type: 'warning' })
  await deleteProperty(row.id)
  announcePropertyMapChange()
  ElMessage.success('删除成功')
  getList()
}

// 详情
const handleDetail = async (row: any) => {
  const res = await getPropertyStatistics(row.id)
  detailData.value = res || row
  detailVisible.value = true
}

// 状态变更
const handleStatusChange = async (row: any) => {
  const status = row.status === 1 ? 1 : 0
  await updatePropertyStatus(row.id, status)
  announcePropertyMapChange()
  ElMessage.success(status === 1 ? '已启用' : '已停用')
}

// 导出
const handleExport = async () => {
  try {
    ElMessage.info('正在导出单位数据...')
    try {
      const res = await request.download({
        url: '/eiot/firewatch/property/export',
        params: queryParams
      })
      download.excel(res as any, `单位管理_${new Date().toLocaleDateString()}.xlsx`)
      ElMessage.success('导出成功')
    } catch {
      // 后端接口不可用时，使用前端CSV导出
      const headers = ['物业编码', '物业名称', '地址', '联系人', '联系电话', '风险等级', '物业类型', '楼层数', '状态', '创建时间']
      const riskMap: Record<string, string> = { critical: '重大风险', major: '较大风险', normal: '一般风险' }
      const rows = tableData.value.map((row: any) => [
        row.code, row.name, `${row.province || ''}${row.city || ''}${row.district || ''}${row.address || ''}`,
        row.contactName, row.contactPhone, riskMap[row.riskLevel] || row.riskLevel,
        row.propertyType, row.floorCount, row.status === 1 ? '启用' : '停用', row.createTime
      ])
      const BOM = '\uFEFF'
      const csvContent = BOM + [headers.join(','), ...rows.map(row => row.map(cell => `"${cell || ''}"`).join(','))].join('\n')
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const href = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = href
      link.download = `单位管理_${new Date().toLocaleDateString()}.csv`
      link.click()
      URL.revokeObjectURL(href)
      ElMessage.success('已导出为CSV文件')
    }
  } catch {
    ElMessage.error('导出失败')
  }
}

// 重置表单
const resetForm = () => {
  formData.value = {
    id: undefined,
    name: '',
    code: '',
    address: '',
    province: '',
    city: '',
    district: '',
    longitude: undefined,
    latitude: undefined,
    contactName: '',
    contactPhone: '',
    riskLevel: 'normal',
    propertyType: '',
    floorCount: undefined,
    buildingArea: undefined
  }
  formRef.value?.resetFields()
}

// 提交表单
const submitForm = async () => {
  await formRef.value?.validate()
  const coordinates = {
    longitude: String(formData.value.longitude ?? '').trim() || null,
    latitude: String(formData.value.latitude ?? '').trim() || null
  }
  const payload = { ...formData.value, ...coordinates }
  if (formData.value.id) {
    await updateProperty(payload)
    ElMessage.success('更新成功')
  } else {
    await createProperty(payload)
    ElMessage.success('创建成功')
  }
  announcePropertyMapChange()
  dialogVisible.value = false
  getList()
}

// 获取风险等级标签
const getRiskLevelTag = (level: string): 'danger' | 'warning' | 'success' | 'info' => {
  const map: Record<string, 'danger' | 'warning' | 'success'> = {
    critical: 'danger',
    major: 'warning',
    normal: 'success'
  }
  return map[level] || 'info'
}

// 初始化
onMounted(() => {
  getList()
})
</script>
