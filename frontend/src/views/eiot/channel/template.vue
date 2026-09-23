<template>
  <yt-crud
    ref="crudRef"
    :data="data"
    :column="column"
    v-model:page="state.page"
    v-model:query="state.query"
    :total="state.total"
    :loading="state.loading"
    :table-props="{
        selection: false,
      }"
    @onLoad="getData"
    @saveFun="onSave"
    @delFun="onDelete"
  >
    <!-- 审核状态插槽 -->
    <template #status="{ row }">
      <div class="flex items-center">
        <el-tag
          :type="getStatusType(row.status)"
          size="small"
        >
          {{ getStatusText(row.status) }}
        </el-tag>
        <!-- 审核失败时显示提示 -->
        <el-tooltip
          v-if="row.status === 2"
          content="审核失败，请前往运营商后台查看失败原因"
          placement="top"
        >
          <el-icon class="ml-2 text-red-500 cursor-pointer">
            <Warning />
          </el-icon>
        </el-tooltip>
      </div>
    </template>

    <!-- 模板编号插槽 -->
    <template #templateCode="{ row }">
      <span v-if="row.templateCode" class="text-gray-600">{{ row.templateCode }}</span>
      <span v-else class="text-gray-400">-</span>
    </template>
    
    <!-- 审核状态表单插槽 -->
    <template #statusForm="{ type, row }">
      <div v-if="type === 'view' || type === 'update'" class="flex items-center">
        <el-tag
          :type="getStatusType(row.status)"
          size="small"
        >
          {{ getStatusText(row.status) }}
        </el-tag>
        <!-- 审核失败时显示提示 -->
        <el-tooltip
          v-if="row.status === 2"
          content="审核失败，请前往运营商后台查看失败原因"
          placement="top"
        >
          <el-icon class="ml-2 text-red-500 cursor-pointer">
            <Warning />
          </el-icon>
        </el-tooltip>
      </div>
    </template>
    
    <!-- 模板编号表单插槽 -->
    <template #templateCodeForm="{ type, row }">
      <span v-if="(type === 'view') || (type === 'update' && row.channelCode !== 'VMS')">
        <span v-if="row.templateCode" class="text-gray-600">{{ row.templateCode }}</span>
        <span v-else class="text-gray-400">-</span>
      </span>
      <el-input v-else v-model="row.templateCode" placeholder="（仅语音通道须配置，需在运营商后台创建对应模板）" />
    </template>

    <!-- 短信模板编码表格插槽 -->
    <template #smsTemplateCode="{ row }">
      <span v-if="row.smsTemplateCode" class="text-blue-600">{{ row.smsTemplateCode }}</span>
      <el-tag v-else type="info" size="small">未关联</el-tag>
    </template>
  </yt-crud>
</template>

<script lang="ts" setup>
import { IColumn } from '@/components/common/types/tableCommon'
import { Warning } from '@element-plus/icons-vue'

import YtCrud from '@/components/common/yt-crud.vue'
import {
  getTemplatesList,
  saveTemplate,
  deleteTemplate,
  IChannelTemplateVo,
  updateTemplate
} from '@/api/eiot/channel/templates.api'
import { getConfigAll } from '@/api/eiot/channel/configs.api'
import * as SmsTemplateApi from '@/api/system/sms/smsTemplate'

const data = ref<IChannelTemplateVo[]>([])
const smsTemplateList = ref<any[]>([])
const column = ref<IColumn[]>([{
  label: '模板名称',
  key: 'title',
  tableWidth: 200,
  rules: [{ required: true, message: '模板名称不能为空' }],
}, {
  label: '通道配置',
  key: 'channelConfigId',
  tableWidth: 150,
  type: 'select',
  componentProps: {
    labelAlias: 'title',
    valueAlias: 'id'
  },
  rules: [{ required: true, message: '通道配置不能为空' }]
}, {
  label: '模板内容',
  key: 'content',
  componentProps: {
    type: 'textarea',
    rows: 4,
  }
}, {
  label: '审核状态',
  key: 'status',
  tableWidth: 120,
  slot: true,
  formSlot: true,
  addHide: true,
  editHide: false
}, {
  label: '模板编号',
  key: 'templateCode',
  tableWidth: 150,
  slot: true,
  formSlot: true,
  addHide: false,
  editHide: false,
  componentProps: {
    disabled: true
  }
}, {
  label: '短信模板编码',
  key: 'smsTemplateCode',
  tableWidth: 160,
  type: 'select',
  filterable: true,
  slot: true,
  componentProps: {
    labelAlias: 'name',
    valueAlias: 'code',
    options: [] as any[]
  },
  rules: []
}])

const state = reactive({
  total: 0,
  page: {
    pageSize: 10,
    pageNum: 1,
  },
  query: {},
  loading: false
})
const getData = () => {
  state.loading = true
  getTemplatesList({
    ...state.page,
    ...state.query,
  }).then(res => {
    data.value = res.list || []
    state.total = res.total
  }).finally(() => {
    state.loading = false
  })
}
// 保存数据
const onSave = ({type, data, cancel}: any) => {
  state.loading = true
  if(type === 'add') {
    saveTemplate(toRaw(data)).then(res => {
      ElMessage.success(type === 'add' ? '添加成功' : '编辑成功')
      cancel()
      getData()
    }).finally(() => {
      state.loading = false
    })
  }else{
    updateTemplate(toRaw(data)).then(res => {
      ElMessage.success(type === 'add'? '添加成功' : '编辑成功')
      cancel()
      getData()
    })

  }

}
// 获取通道配置
const getConfig = () => {
  getConfigAll().then(res => {
    const configOptions = res || []
    column.value.forEach(item => {
      if (item.key === 'channelConfigId') {
        item.componentProps.options = configOptions
      }
    })
  })
}
getConfig()

// 获取系统短信模板列表，用于smsTemplateCode下拉选择
const loadSmsTemplates = async () => {
  try {
    const res = await SmsTemplateApi.getSmsTemplatePage({ pageNo: 1, pageSize: 100 })
    smsTemplateList.value = res?.list || []
    column.value.forEach(item => {
      if (item.key === 'smsTemplateCode') {
        item.componentProps.options = smsTemplateList.value.map((t: any) => ({
          code: t.code,
          name: `${t.name}(${t.code})`
        }))
      }
    })
  } catch (e) {
    console.warn('获取短信模板列表失败', e)
  }
}
loadSmsTemplates()
// 删除
const onDelete = async (row: any) => {
  state.loading = true
  await deleteTemplate(row.id)
  ElMessage.success('删除成功!')
  state.loading = false
  getData()
}

// 获取状态文本
const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '待审核'
    case 1:
      return '已生效'
    case 2:
      return '审核失败'
    default:
      return '未知状态'
  }
}

// 获取状态标签类型
const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'warning'
    case 1:
      return 'success'
    case 2:
      return 'danger'
    default:
      return 'info'
  }
}
</script>
