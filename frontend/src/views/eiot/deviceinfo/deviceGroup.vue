<template>
  <yt-crud
  v-bind="options"
  ref="crudRef" :data="data" :column="column"
  :loading="state.loading"
  :total="state.total"
  v-model:page="state.page"
  v-model:query="state.query"
  :tableProps=" {
    selection: false,
    viewBtn: false,
    delBtn: true,
    menuSlot: true,
    menuWidth: 300,
  }"
  :fun-props="{
    delBtn: false,
  }"
  @on-load="getData"
  @delFun="onDelete"
  @saveFun="onSave"
  >
    <template #rightToolbar>
      <el-col :span="12" style="margin-right: 5px">
        <el-button plain @click="handleImport">
          <Icon icon="ep:upload" />
          <span style="color: rgb(0, 112, 255);">导入分组</span>
        </el-button>
      </el-col>
      <el-col :span="12">
        <el-button plain @click="handleDownloadTemplate">
          <Icon icon="ep:download" />
          <span style="color: rgb(0, 112, 255);">下载模板</span>
        </el-button>
      </el-col>
    </template>
    <template #menuSlot="scope">
      <el-tooltip class="box-item" effect="dark" content="查看分组设备" placement="top">
        <el-button link type="primary" @click="handleToDevices(scope.row.id)">
          <Icon icon="ep:cpu" />
        </el-button>
      </el-tooltip>
    </template>
  </yt-crud>

   <!-- 添加文件上传 -->
   <el-dialog
      :title="fileUploadDialog.title"
      v-model="fileUploadDialog.visible"
      width="500px"
      :close-on-press-escape="false"
      :close-on-click-modal="false"
      append-to-body
      destroy-on-close
    >
      <el-form v-if="fileUploadDialog.visible" ref="ossFormRef" label-width="80px">
        <el-form-item label="文件名">
          <fileUpload :fileSize="10" :fileType="['xlsx', 'xls']" :limit="1" uploadType="url"
          uploadUrl="/eiot/device/group/importData" @upload-success="handleUploadSuccess"/>
        </el-form-item>
      </el-form>
    </el-dialog>

</template>
<script lang="ts" setup>
import { IColumn } from '@/components/common/types/tableCommon'
import YtCrud from '@/components/common/yt-crud.vue'
import { getDeviceGroupsList,saveDeviceGroup,deleteDeviceGroup,getGroupTemplate } from '@/api/eiot/deviceinfo/devices.api'
import download from '@/utils/download'
import FileUpload from '@/components/FileUpload/index.vue'

const column: IColumn[] = [{
  label: '分组id',
  key: 'id',
  addHide: true,
  editDisabled: true,
  rules: [{ required: true, message: '请填写组ID' }],
}, {
  label: '分组名',
  key: 'name',
  search: true,
  rules: [{ required: true, message: '请填写组名' }],
},

{
  label: '备注',
  key: 'remark',
}]

const state = reactive({
  page: {
    pageSize: 10,
    pageNo: 1,
  },
  total: 0,
  loading: false,
  query: {
    name: ''
  },
})

const data = ref()

const fileUploadDialog = ref({
  title: '导入分组数据',
  visible: false
})

const handleUploadSuccess = (res) => {
  ElMessage.success(res.message)
  getData()
  fileUploadDialog.value.visible = false
}

const getData = () => {
  state.loading = true
  getDeviceGroupsList({...state.page, ...state.query}).then(res => {
    console.log(res)
    data.value = res.list
    state.total = res.total
  }).finally(() => {
    state.loading = false
  })
}

// 保存数据
const onSave = ({type, data, cancel}: any) => {
  state.loading = true
  saveDeviceGroup(toRaw(data)).then(res => {
    ElMessage.success(type === 'add' ? '添加成功' : '编辑成功')
    cancel()
    getData()
  }).finally(() => {
    state.loading = false
  })
}
// 删除
const onDelete = async (row: any) => {
  state.loading = true
  let data = {
    id : row.id
  }
  await deleteDeviceGroup(data)
  ElMessage.success('删除成功!')
  state.loading = false
  getData()
}

// 跳转配置
const router = useRouter()
const handleToDevices = (id: string) => {
  if (!id) return
  router.push(`deviceGroupDetail/${id}`)
}

const handleDownloadTemplate = async () => {
  const res = await getGroupTemplate()
  download.excel(res, "设备分组模板.xls")
}

const handleImport = () => {
  fileUploadDialog.value.visible = true
}


const options = reactive({
  ref: 'crudRef',
  tableProps: {
    selection: true,
    delBtn: true,
    menuSlot: true,
    menuWidth: 300,
  },
  funProps: {
    delBtn: true,
  },
  searchProps: {
    labelWidth: 120,
  },
  data,
  column,
})

</script>

<!-- <style lang="scss" scoped>

</style> -->
