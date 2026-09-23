<template>
  <div class="function-config">
    <el-card shadow="never">
      <div style="display: flex;justify-content: space-between">
        <el-form :inline="true" :model="state.query">
          <el-form-item label="标识符"> <el-input v-model="state.query.identifier" placeholder="标识符" clearable /> </el-form-item>
          <el-form-item label="功能名称"> <el-input v-model="state.query.name" placeholder="功能名称" clearable /> </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleQuery" icon="Search" style="padding: 8px" />
            <el-button @click="resetQuery" icon="Refresh" style="padding: 8px" />
          </el-form-item>
        </el-form>
        <div style="display: flex;">
          <el-button type="primary" @click="handleAdd">新增</el-button>
          <el-button type="primary" @click="importHandle">导入</el-button>
          <el-button type="primary" @click="syncProduct">同步产品</el-button>
        </div>
      </div>

      <yt-table
        style="margin-top: 10px"
        :page-hide="true"
        :column="column"
        :tableProps=" {
          selection: false,
          viewBtn: false,

          }"
        :view-btn="false"
        :data="dataX"
        v-model:page="state.page"
        v-model:query="state.query"
        :total="state.total"
        :loading="state.loading"
        @handle-delete="handleDel"
        @handle-update="handleUpdate"
      >
        <template #dataType="{ row }">
          {{ row.raw?.dataType?.type || '-' }}
        </template>
        <template #regAddr="{ row }">
          {{ row.raw?.regAddr}}
        </template>
      </yt-table>
    </el-card>
    <function-detail ref="functionDetailRef" :id="id" :model="model" />
    <!-- 添加文件上传 -->
    <el-dialog
      title="点位导入"
      v-model="fileUploadDialog.visible"
      width="500px"
      :close-on-press-escape="false"
      :close-on-click-modal="false"
      append-to-body
      destroy-on-close
    >
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="fileUploadDialog.headers"
        :action="fileUploadDialog.action"
        :data="fileUploadDialog.dataFrom"
        :disabled="fileUploadDialog.isUploading"
        :on-success="handleUploadSuccess"
        :on-error="(res)=>{ message.error('上传失败')}"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <span>仅允许导入xls、xlsx格式文件。</span>
            <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="handleDownloadTemplate"
              >下载模板</el-link
            >
          </div>
        </template>
      </el-upload>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="()=>{fileUploadDialog.visible=false}">取消</el-button>
          <el-button type="primary" :loading="state.loading" class="title" @click="excelSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {IColumn} from '@/components/common/types/tableCommon'
import {propTypes} from '@/utils/propTypes'
import download from '@/utils/download'
import { ModBusInfoApi } from '@/api/eiot/modbus'
import FunctionDetail from './modeuls/functionDetail.vue'
import YtTable from '@/components/common/yt-table'
import {useEmitt} from '@/hooks/web/useEmitt'
import {getAccessToken} from '@/utils/auth'
import { ElUpload } from 'element-plus'
const message = useMessage() // 消息弹窗

const { emitter } = useEmitt()

const props = defineProps({
  id: propTypes.string.def(''),
  model: propTypes.object.def({}),
});

const state = reactive({
  total: 0,
  page: {
    pageSize: 10,
    pageNum: 1,
  },
  query: {
    identifier:'',
    name:'',
  },
  loading: false
})

const dataX = ref<any[]>([])
const handleQuery =()=>{

  let x= data.value.filter(x=>{
    if(state.query.identifier){
     return x.identifier.indexOf(state.query.identifier.trim())!=-1
    }
    return true
  }).filter(
    x=>{
      if(state.query.name){
        return x.name.indexOf(state.query.name.trim())!=-1
      }
      return true
    }
  )
  dataX.value=x
}
const resetQuery =()=>{
  state.query.identifier='',
  state.query.name=''
  handleQuery()
}


const baseUrl = import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL +'/eiot/modbus/importData'
const headers = ref({ Authorization: 'Bearer ' + getAccessToken() })

const uploadRef = ref(ElUpload)
const exportLoading = ref(false) // 导出的加载中

const fileUploadDialog = ref({
  visible: false,
  headers:headers,
  action:baseUrl,
  isUploading:false,
  dataFrom:{ productKey:props.id},
})
const handleDownloadTemplate = async () => {
  try {
    exportLoading.value = true
    const data = await ModBusInfoApi.exportDeviceTemplate();
    download.excel(data, '点位模版.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}
const excelSubmit =()=>{
  console.info(11)
  uploadRef.value.submit()
}

const handleUploadSuccess = (res: any) => {
  if (res.code === 0) {
    message.success(res.msg)
    emitter.emit('updateObjectModelX')
    fileUploadDialog.value.visible = false
  }else{
    message.error(res.msg)
  }

}

const importHandle=()=>{
  fileUploadDialog.value.visible = true
}

const functionDetailRef = ref()
const handleAdd = () => {
  functionDetailRef.value.openDialog()
}
const syncProduct= async ()=>{
  const res = await ModBusInfoApi.syncToProduct({productKey:props.id});
  console.log(res)
  if(res.code === 0){
    message.success('产品同步成功')
  }else{
    message.error(res.msg)
  }
}
const handleUpdate = (row: any) => {
  let tmpModel = JSON.parse(JSON.stringify(row))
  console.log('tmpModel', tmpModel)
  if (!tmpModel.raw.dataType) {
    tmpModel.raw.dataType = {}
  }
  tmpModel.raw.inputData = tmpModel.raw.inputData
    ? tmpModel.raw.inputData
    : []
  tmpModel.raw.outputData = tmpModel.raw.outputData
    ? tmpModel.raw.outputData
    : []
  let prop = tmpModel.raw
  const props: any = {}
  if (prop.dataType.type == 'enum') {
    let enumSpecs: any[] = []
    for (var p in prop.dataType.specs) {
      enumSpecs.push({
        name: prop.dataType.specs[p],
        value: p,
      })
    }
    props.enumItems = enumSpecs
  } else if (prop.dataType.type == 'bool') {
    props.boolItem = {
      _true: prop.dataType.specs['0'],
      _false: prop.dataType.specs['1'],
    }
  }
  functionDetailRef.value.openDialog(toRaw(row), props)
}

const submitThingModelChange = async () => {
  if (props.model) {
    // eslint-disable-next-line vue/no-mutating-props
    props.model.services = props.model.services || []
    // eslint-disable-next-line vue/no-mutating-props
    props.model.properties = props.model.properties || []
    // eslint-disable-next-line vue/no-mutating-props
    props.model.events = props.model.events || []
  }
  await ModBusInfoApi.saveObjectModel({
    productKey: props.id,
    model: JSON.stringify(props.model),
  })
  state.loading = false
  emitter.emit('updateObjectModelX')
}

// 删除
const handleDel = (row: any) => {
  state.loading = true
  if (row.type == 'property') {
        let index = props.model.properties.findIndex((p) => {
          return p.identifier == row.identifier
        })
        // eslint-disable-next-line vue/no-mutating-props
        props.model.properties.splice(index, 1)
      } else if (row.type == 'service') {
        let index = props.model.services.findIndex((s) => {
          return s.identifier == row.identifier
        })
        // eslint-disable-next-line vue/no-mutating-props
        props.model.services.splice(index, 1)
      } else if (row.type == 'event') {
        let index = props.model.events.findIndex((e) => {
          return e.identifier == row.identifier
        })
        // eslint-disable-next-line vue/no-mutating-props
        props.model.events.splice(index, 1)
      }
      submitThingModelChange()
}

const column = ref<IColumn[]>([
  {
    label: '功能类型',
    key: 'type',
  },
  {
    label: '功能名称',
    key: 'name',
    search: true,
  },
  {
    label: '标识符',
    key: 'identifier',
    search: true,
  },
  {
    label: '寄存器类型',
    key: 'regType',
    type:'select',
    componentProps: {
      options:  [{value:'01',label:'线圈状态(RW)',},
        {value:'02',label:'离散输入(RO)',},
       {value:'03',label:'保持寄存器(RW)',},
       {value:'04',label:'输入寄存器(RO)',},]
    },
  },
  {
    label: '寄存器地址',
    key: 'regAddr',
    slot: true,
  },
  {
    label: '数据类型',
    key: 'dataType',
    slot: true,
  },
  {
    label: '数据定义',
    key: 'params',
    slot: true,
  }
])
const data = ref<any[]>([])
// 获取数据
const getInfo = (model) => {
  const modelObj = model || {
    properties: [],
    events: [],
    services: [],
  }
  let modelFuncs: any[] = []
  modelObj.properties.forEach((p) => {
    let params = JSON.stringify(p.dataType.specs || '{}')
    modelFuncs.push({
      raw: p,
      type: 'property',
      name: p.name,
      identifier: p.identifier,
      description: p.description,
      unit: p.unit,
      regType: p.regType,
      regAddr: p.regAddr,
      regNum: p.regNum,
      processor: p.processor,
      sort: p.sort,
      dataTypeName: p.dataType.type,
      params: params == '{}' ? '' : params,
    })
  })
  modelObj.events.forEach((e) => {
    let output = {}
    e.outputData.forEach((p) => {
      output[p.identifier] = p.name
    })
    modelFuncs.push({
      raw: e,
      type: 'event',
      name: e.name,
      description: e.description,
      identifier: e.identifier,
      dataTypeName: '-',
      params: JSON.stringify(output),
    })
  })
  modelObj.services.forEach((s) => {
    let input: any = {}
    s.inputData.forEach((p) => {
      input[p.identifier] = p.name
    })
    let output = {}
    s.outputData.forEach((p) => {
      output[p.identifier] = p.name
    })
    modelFuncs.push({
      raw: s,
      type: 'service',
      name: s.name,
      description: s.description,
      identifier: s.identifier,
      dataTypeName: '-',
      params:
        '输入:' +
        JSON.stringify(input) +
        ',输出:' +
        JSON.stringify(output),
    })
  })

  console.log('modelFuncs', modelFuncs)
  data.value = modelFuncs
  handleQuery()
}

watch(() => props.model, (newV) => {
  if (newV) getInfo(newV)
}, {
  deep: true,
  immediate: true,
})
</script>

<style lang="scss" scoped></style>
