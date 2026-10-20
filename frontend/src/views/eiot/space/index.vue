<template>
  <yt-crud
    ref="crudRef"
    :data="data"
    :column="column"
    :loading="state.loading"
    :tableProps="{
      selection: false,
      viewBtn: false,
      editBtn: true,
      delBtn: true,
      pageHide: true,
    }"
    :funProps="{
      addBtn: true,
      delBtn: false,
      addPermi: ['eiot:firewatch:space:create'],
    }"
    @on-load="getData"
    @save-fun="onSave"
    @del-fun="onDelete"
  >
    <template #addRight>
      <!-- 所属企业筛选：与新增按钮同行，选择即过滤，清空恢复全量 -->
      <el-select
        v-model="state.query.tenantId"
        clearable
        placeholder="所属企业"
        style="width: 220px; margin-left: 10px"
        @change="getData()"
      >
        <el-option
          v-for="item in tenantOptions"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
    </template>
  </yt-crud>
</template>
<script lang="ts" setup>
import { IColumn } from '@/components/common/types/tableCommon'
import YtCrud from '@/components/common/yt-crud.vue'
import { SpaceApi } from '@/api/eiot/firewatch/space.api'
import { getTenantPage } from '@/api/system/tenant'

defineOptions({ name: 'SpaceManagement' })

// 租户下拉选项（所属企业筛选）
const tenantOptions = ref<any[]>([])

const column: IColumn[] = [
  {
    label: '所属企业',
    key: 'tenantName',
    addHide: true,
    editHide: true, // 所属企业不可变更（更新保持原租户）
    tableWidth: 220,
  },
  {
    label: '楼栋',
    key: 'building',
    tableWidth: 160,
    rules: [{ required: true, message: '请输入楼栋（如：1号楼/A栋）' }],
  },
  {
    label: '楼层',
    key: 'floor',
    tableWidth: 140,
    rules: [{ required: true, message: '请输入楼层（如：3层/-1层）' }],
  },
  {
    label: '区域',
    key: 'area',
    tableWidth: 160,
  },
  {
    label: '详细位置',
    key: 'position',
    tableWidth: 180,
  },
  {
    label: '备注',
    key: 'remark',
  },
]

const state = reactive({
  loading: false,
  query: {} as Record<string, any>,
})

const data = ref<any[]>([])

// 加载空间列表（接口不分页，一次性返回全量）：query.tenantId 为所属企业筛选（仅系统租户管理员视角生效；空串归一为 undefined 查全量）
const getData = (query?: Record<string, any>) => {
  if (query) state.query = query
  state.loading = true
  SpaceApi.getSpaceList(state.query.tenantId || undefined)
    .then((res) => {
      data.value = res || []
    })
    .finally(() => {
      state.loading = false
    })
}

// 保存（新增/编辑）：表单校验通过后回调，成功后关闭弹窗并刷新列表
const onSave = ({ type, data: row, cancel }: any) => {
  if (type !== 'add' && type !== 'update') return
  const isAdd = type === 'add'
  state.loading = true
  const promise = isAdd ? SpaceApi.createSpace(toRaw(row)) : SpaceApi.updateSpace(toRaw(row))
  promise
    .then(() => {
      ElMessage.success(isAdd ? '添加成功' : '编辑成功')
      cancel()
      getData()
    })
    .finally(() => {
      state.loading = false
    })
}

// 删除：行内删除确认后回调；存在设备引用的空间后端会拒绝
const onDelete = async (row: any) => {
  state.loading = true
  try {
    await SpaceApi.deleteSpace(row.id)
    ElMessage.success('删除成功')
    getData()
  } finally {
    state.loading = false
  }
}

// 加载租户下拉选项（所属企业筛选）
const loadTenantOptions = async () => {
  const res = await getTenantPage({ pageNo: 1, pageSize: 100 })
  tenantOptions.value = res?.list || []
}
loadTenantOptions()
</script>
