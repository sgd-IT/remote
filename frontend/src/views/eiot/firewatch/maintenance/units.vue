<template>
  <div class="tab-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="单位名称">
        <el-input v-model="queryParams.unitName" placeholder="单位名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="queryParams.contactPerson" placeholder="联系人" clearable style="width: 130px" />
      </el-form-item>
      <el-form-item label="资质等级">
        <el-select v-model="queryParams.qualificationLevel" placeholder="资质等级" clearable style="width: 120px">
          <el-option label="甲级" value="甲级" />
          <el-option label="乙级" value="乙级" />
          <el-option label="丙级" value="丙级" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" icon="Plus" @click="handleCreate">添加维保单位</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="unitName" label="单位名称" min-width="180" />
      <el-table-column prop="contactPerson" label="联系人" width="120" align="center" />
      <el-table-column prop="contactPhone" label="联系电话" width="140" align="center" />
      <el-table-column prop="address" label="地址" min-width="200" />
      <el-table-column prop="qualificationLevel" label="资质等级" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.qualificationLevel === '甲级' ? 'danger' : row.qualificationLevel === '乙级' ? 'warning' : 'info'" size="small">
            {{ row.qualificationLevel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button link type="success" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewVisible" title="维保单位详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="单位名称">{{ currentUnit.unitName }}</el-descriptions-item>
        <el-descriptions-item label="资质等级">{{ currentUnit.qualificationLevel }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentUnit.contactPerson }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentUnit.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ currentUnit.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentUnit.remark || '暂无备注' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑维保单位对话框 -->
    <el-dialog v-model="formVisible" :title="isEditing ? '编辑维保单位' : '添加维保单位'" width="550px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="单位名称" prop="unitName"><el-input v-model="form.unitName" placeholder="请输入单位名称" /></el-form-item>
        <el-form-item label="联系人" prop="contactPerson"><el-input v-model="form.contactPerson" placeholder="请输入联系人" /></el-form-item>
        <el-form-item label="联系电话" prop="contactPhone"><el-input v-model="form.contactPhone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="地址" prop="address"><el-input v-model="form.address" placeholder="请输入地址" /></el-form-item>
        <el-form-item label="资质等级" prop="qualificationLevel">
          <el-select v-model="form.qualificationLevel" style="width:100%">
            <el-option label="甲级" value="甲级" />
            <el-option label="乙级" value="乙级" />
            <el-option label="丙级" value="丙级" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">{{ isEditing ? '保存修改' : '确认添加' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMaintenanceUnitPage,
  createMaintenanceUnit,
  updateMaintenanceUnit,
  deleteMaintenanceUnit,
  getMaintenanceUnit
} from '@/api/eiot/firewatch/firewatch.api'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  unitName: '',
  contactPerson: '',
  qualificationLevel: ''
})

const getList = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: queryParams.pageNo, pageSize: queryParams.pageSize }
    if (queryParams.unitName) params.unitName = queryParams.unitName
    if (queryParams.contactPerson) params.contactPerson = queryParams.contactPerson
    if (queryParams.qualificationLevel) params.qualificationLevel = queryParams.qualificationLevel
    const res = await getMaintenanceUnitPage(params)
    if (res) {
      tableData.value = res.list || []
      total.value = res.total || 0
    }
  } catch (e) {
    console.error('获取维保单位列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const handleReset = () => {
  queryParams.unitName = ''
  queryParams.contactPerson = ''
  queryParams.qualificationLevel = ''
  queryParams.pageNo = 1
  getList()
}

// 查看详情
const viewVisible = ref(false)
const currentUnit = ref<any>({})

const handleView = async (row: any) => {
  try {
    const res = await getMaintenanceUnit(row.id)
    if (res) {
      currentUnit.value = res
      viewVisible.value = true
    }
  } catch (e) {
    console.error('获取维保单位详情失败', e)
  }
}

// 添加/编辑
const formVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const form = reactive({
  unitName: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  qualificationLevel: '乙级',
  remark: ''
})

const handleCreate = () => {
  isEditing.value = false
  editingId.value = null
  form.unitName = ''
  form.contactPerson = ''
  form.contactPhone = ''
  form.address = ''
  form.qualificationLevel = '乙级'
  form.remark = ''
  formVisible.value = true
}

const handleEdit = async (row: any) => {
  isEditing.value = true
  editingId.value = row.id
  try {
    const res = await getMaintenanceUnit(row.id)
    if (res) {
      form.unitName = res.unitName || ''
      form.contactPerson = res.contactPerson || ''
      form.contactPhone = res.contactPhone || ''
      form.address = res.address || ''
      form.qualificationLevel = res.qualificationLevel || '乙级'
      form.remark = res.remark || ''
    }
  } catch (e) {
    console.error('获取维保单位详情失败', e)
  }
  formVisible.value = true
}

const submitForm = async () => {
  submitLoading.value = true
  try {
    if (isEditing.value && editingId.value !== null) {
      await updateMaintenanceUnit({ id: editingId.value, ...form })
      ElMessage.success('修改成功')
    } else {
      await createMaintenanceUnit({ ...form })
      ElMessage.success('添加成功')
    }
    formVisible.value = false
    getList()
  } catch (e) {
    console.error('保存失败', e)
  } finally {
    submitLoading.value = false
  }
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除维保单位「${row.unitName}」吗？`, '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteMaintenanceUnit(row.id)
      ElMessage.success('删除成功')
      getList()
    } catch (e) {
      console.error('删除失败', e)
    }
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.tab-container { padding: 16px; }
.search-form { background: #f5f7fa; padding: 16px; border-radius: 6px; margin-bottom: 16px; }
.action-bar { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
