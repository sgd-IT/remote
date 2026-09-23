<template>
  <div class="app-container">
    <!-- 搜索工作栏 -->
    <ContentWrap>
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="100px"
      >
        <el-form-item label="组件名称" prop="name">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入组件名称"
            clearable
            @keyup.enter="handleQuery"
            class="!w-240px"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择状态"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in statusOptions"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
<!--          组件不允许手动新增,组件运行时会自动注册-->

<!--          <el-button type="primary" plain @click="handleAdd" v-hasPermi="['eiot:component:create']">-->
<!--            <Icon icon="ep:plus" class="mr-5px" /> 新增-->
<!--          </el-button>-->
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表 -->
    <ContentWrap>
      <el-table v-loading="loading" :data="list">
        <el-table-column label="组件编号" align="center" prop="id" />
        <el-table-column label="组件名称" align="center" prop="name" />
        <el-table-column label="组件类型" align="center" prop="type" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime"  :formatter="dateFormatter" width="180"/>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button
              type="text"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['eiot:component:update']"
              >修改</el-button
            >
            <el-popconfirm title="是否确认删除该组件？" @confirm="handleDelete(scope.row)">
              <template #reference>
                <el-button type="text" icon="Delete" v-hasPermi="['eiot:component:delete']"
                  >删除</el-button
                >
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </ContentWrap>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="组件名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入组件名称" />
        </el-form-item>
        <el-form-item label="组件类型" prop="type">
          <el-input v-model="form.type" :disabled="!!form.id" placeholder="请输入组件类型,请确保唯一" />
        </el-form-item>
        <el-form-item label="组件配置" prop="config">
          <el-input
            v-model="form.config"
            type="textarea"
            placeholder="请输入JSON格式的组件配置"
            :rows="4"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in statusOptions" :key="dict.value" :label="dict.value">{{
              dict.label
            }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as ComponentApi from '@/api/eiot/component'
import type { ComponentVO, ComponentForm } from '@/api/eiot/component'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const total = ref(0)
const list = ref<ComponentVO[]>([])
const open = ref(false)
const title = ref('')

// 状态选项
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: undefined
})

// 表单参数
const form = reactive<ComponentForm>({
  id: undefined,
  name: '',
  type: '',
  config: '',
  status: 1,
  remark: ''
})

// 表单校验规则
const rules = {
  name: [{ required: true, message: '组件名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '请选择组件类型', trigger: 'change' }],
  config: [{ required: true, message: '组件配置不能为空', trigger: 'blur' }]
}

const formRef = ref()

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const res = await ComponentApi.getComponentList(queryParams)
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/** 取消按钮 */
const cancel = () => {
  open.value = false
  reset()
}

/** 表单重置 */
const reset = () => {
  form.id = undefined
  form.name = ''
  form.type = ''
  form.config = ''
  form.status = 1
  form.remark = ''
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 新增按钮操作 */
const handleAdd = () => {
  reset()
  open.value = true
  title.value = '添加组件'
}

/** 修改按钮操作 */
const handleUpdate = async (row: ComponentVO) => {
  reset()
  const res = await ComponentApi.getComponent(row.id)
  Object.assign(form, res)
  open.value = true
  title.value = '修改组件'
}

/** 提交按钮 */
const submitForm = async () => {
  // 校验表单
  await formRef.value?.validate()
  // 提交请求
  if (form.id) {
    await ComponentApi.updateComponent(form)
    ElMessage.success('修改成功')
  } else {
    await ComponentApi.addComponent(form)
    ElMessage.success('新增成功')
  }
  open.value = false
  await getList()
}

/** 删除按钮操作 */
const handleDelete = async (row: ComponentVO) => {
  await ComponentApi.delComponent(row.id)
  ElMessage.success('删除成功')
  await getList()
}

/** 修改状态操作 */
const handleStatusChange = async (row: ComponentVO) => {
  try {
    await ComponentApi.updateComponent(row)
    ElMessage.success('修改成功')
  } catch (error) {
    row.status = row.status === 0 ? 1 : 0
    console.error('修改状态失败:', error)
  }
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>
