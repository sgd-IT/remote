<template>
  <div class="phone-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" inline class="search-form">
      <el-form-item label="座机号码">
        <el-input v-model="queryParams.phoneNumber" placeholder="座机号码" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="座机名称">
        <el-input v-model="queryParams.name" placeholder="座机名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" icon="Plus" v-hasPermi="['eiot:firewatch:duty-phone:create']" @click="handleCreate">
        录入座机号码
      </el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="phoneNumber" label="座机号码" width="150" align="center" />
      <el-table-column prop="name" label="座机名称" min-width="180" />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" v-hasPermi="['eiot:firewatch:duty-phone:update']" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="danger" size="small" v-hasPermi="['eiot:firewatch:duty-phone:delete']" @click="handleDelete(row)">
            删除
          </el-button>
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

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="formVisible" :title="isEditing ? '编辑座机号码' : '录入座机号码'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="座机号码" prop="phoneNumber">
          <el-input v-model="form.phoneNumber" placeholder="请输入值班中心座机号码，如 0551-1234567" />
        </el-form-item>
        <el-form-item label="座机名称" prop="name">
          <el-input v-model="form.name" placeholder="如：XX值班中心前台" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">
          {{ isEditing ? '保存修改' : '确认录入' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDutyPhonePage,
  createDutyPhone,
  updateDutyPhone,
  deleteDutyPhone,
  getDutyPhone
} from '@/api/eiot/firewatch/callcenter.api'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  phoneNumber: '',
  name: ''
})

const getList = async () => {
  loading.value = true
  try {
    const params: any = { pageNo: queryParams.pageNo, pageSize: queryParams.pageSize }
    if (queryParams.phoneNumber) params.phoneNumber = queryParams.phoneNumber
    if (queryParams.name) params.name = queryParams.name
    const res = await getDutyPhonePage(params)
    if (res) {
      tableData.value = res.list || []
      total.value = res.total || 0
    }
  } catch (e) {
    console.error('获取座机号码列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const handleReset = () => {
  queryParams.phoneNumber = ''
  queryParams.name = ''
  queryParams.pageNo = 1
  getList()
}

// 添加/编辑
const formVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const form = reactive({
  phoneNumber: '',
  name: '',
  remark: ''
})

const formRules = {
  phoneNumber: [
    { required: true, message: '请输入座机号码', trigger: 'blur' },
    { pattern: /^[0-9+\-\s()（）]{5,20}$/, message: '座机号码格式不正确', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入座机名称', trigger: 'blur' }]
}

const handleCreate = () => {
  isEditing.value = false
  editingId.value = null
  form.phoneNumber = ''
  form.name = ''
  form.remark = ''
  formVisible.value = true
}

const handleEdit = async (row: any) => {
  isEditing.value = true
  editingId.value = row.id
  try {
    const res = await getDutyPhone(row.id)
    if (res) {
      form.phoneNumber = res.phoneNumber || ''
      form.name = res.name || ''
      form.remark = res.remark || ''
    }
  } catch (e) {
    console.error('获取座机号码详情失败', e)
  }
  formVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEditing.value && editingId.value !== null) {
      await updateDutyPhone({ id: editingId.value, ...form })
      ElMessage.success('修改成功')
    } else {
      await createDutyPhone({ ...form })
      ElMessage.success('录入成功')
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
  ElMessageBox.confirm(`确认删除座机号码「${row.phoneNumber}」吗？删除后话单将无法匹配到对应值班中心。`, '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteDutyPhone(row.id)
        ElMessage.success('删除成功')
        getList()
      } catch (e) {
        console.error('删除失败', e)
      }
    })
    .catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.phone-container {
  padding: 16px;
}
.search-form {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 16px;
}
.action-bar {
  margin-bottom: 16px;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
