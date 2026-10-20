<template>
  <div>
    <yt-crud
      v-bind="options"
      ref="crudRef"
      @on-load="getData"
      :fun-props="{
        exportBtn: true,
        delBtn: layoutType !== 'card'
      }"
      :table-props="{
        selection: true,
        delBtn: false,
        viewBtn: false,
        editBtn: true,
        customTable: layoutType === 'card',
        menuSlot: true,
        menuWidth: 340
      }"
      :loading="state.loading"
      :total="state.total"
      v-model:page="state.page"
      v-model:query="state.query"
      @del-fun="handleDelete"
      @export-fun="handleExport"
      :addFunc="handleAdd"
      @open-before-fun="openBeforeFun"
      @change="onChange"
      :data="data"
    >
      <template #rightToolbar>
        <el-col :span="12" style="margin-right: 15px; display: flex">
          <el-button plain @click="handleImport">
            <Icon icon="ep:upload" />
            <span style="color: rgb(0, 112, 255)">导入设备</span>
          </el-button>

          <el-button plain @click="handleDownloadTemplate">
            <Icon icon="ep:download" />
            <span style="color: rgb(0, 112, 255)">下载模板</span>
          </el-button>
        </el-col>

        <el-button
          type="primary"
          class="addDeviceToGroup"
          :disabled="multipleSelection.length === 0"
          @click="handleToGroup"
          >添加设备到组
        </el-button>

        <el-button type="success" @click="handleBatchAssignTenant">批量分配租户 </el-button>

        <el-radio-group v-model="layoutType">
          <el-radio-button value="table">
            <Icon icon="ep:document" :color="layoutType === 'table' ? '#fff' : '#333'" />
          </el-radio-button>
          <el-radio-button value="card">
            <Icon icon="ep:grid" :color="layoutType === 'card' ? '#fff' : '#333'" />
          </el-radio-button>
        </el-radio-group>
      </template>
      <template #customTable>
        <el-row class="card-list flex">
          <el-col
            class="card-item"
            v-for="(item, index) in data"
            :key="index"
            :class="item.state === 1 ? 'success-box' : 'error-box'"
          >
            <div class="text-box">
              <div class="title flex align-center">
                <div class="title-l">
                  <div class="icon">
                    <Icon icon="ep:grid" />
                  </div>
                  {{ item.name }}
                </div>
                <div class="title-r">
                  <status-tag :type="getStateType(item.state)" :text="getStateText(item.state)" />
                </div>
              </div>
              <div class="text flex">
                <div class="txt">
                  <div class="txt-item">
                    <div class="label">序列号</div>
                    <div class="value active">{{ item.serialNo }}</div>
                  </div>
                  <div class="txt-item">
                    <div class="label">所属产品</div>
                    <div class="value active">{{ item.productName }}</div>
                  </div>
                  <div class="txt-item">
                    <div class="label">设备类型</div>
                    <div class="value">{{ getNodeTypeName(item.nodeType) }}</div>
                  </div>
                  <!--                  <div class="txt-item">-->
                  <!--                    <div class="copy-tag" v-copyText="item.deviceId" v-copyText:callback="copyIdSuccess">-->
                  <!--                      <svg-icon icon-class="copy" />-->
                  <!--                      设备ID-->
                  <!--                    </div>-->
                  <!--                  </div>-->
                </div>
                <div class="img">
                  <img :src="defaultImg" alt="" />
                </div>
              </div>
            </div>
            <div class="btn-group flex">
              <el-button
                v-if="item.nodeType == 0"
                class="cu-btn flex-1"
                type="success"
                plain
                @click="showChidrenDevices(item)"
              >
                <Icon icon="ep:box" class="mr-5px" />
                子设备
              </el-button>
              <el-divider v-if="item.nodeType == 0" direction="vertical" />
              <el-button class="cu-btn flex-1" type="info" plain @click="handleEdit(item)">
                <Icon icon="ep:edit" class="mr-5px" />
                编辑
              </el-button>
              <el-divider direction="vertical" />
              <el-button class="cu-btn flex-1" type="warning" plain @click="handleView(item)">
                <Icon icon="ep:view" class="mr-5px" />
                详情
              </el-button>
              <el-divider direction="vertical" />
              <el-popconfirm title="是否确认删除?" @confirm="handleDelete(item)">
                <template #reference>
                  <el-button type="danger" plain>
                    <Icon icon="ep:delete" />
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </el-col>
        </el-row>
      </template>
      <template #state="scope">
        <el-tag v-if="scope.row.state === 1" type="success" size="small">在线</el-tag>
        <el-tag v-if="scope.row.state === 2" type="success" size="small">未激活</el-tag>
        <el-tag v-if="scope.row.state === 3" type="success" size="small">禁用</el-tag>
        <el-tag v-if="scope.row.state === 0" type="danger" size="small">离线</el-tag>
      </template>

      <template #group="scope">
        <el-tag v-for="i in scope.row.group" :key="i.id">{{ i.name }}</el-tag>
      </template>

      <template #menuSlot="scope">
        <!-- TODO: 没接口,nodeType无法获取，得改成 ！= 0 -->
        <div class="flex items-center">
          <el-tooltip class="box-item" effect="dark" content="子设备" placement="top">
            <el-button
              link
              :disabled="scope.row.nodeType == 0"
              @click="showChidrenDevices(scope.row)"
            >
              <Icon icon="ep:box" />
            </el-button>
          </el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip class="box-item" effect="dark" content="编辑" placement="top">
            <el-button link type="primary" @click="handleEdit(scope.row)">
              <Icon icon="ep:edit" />
            </el-button>
          </el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip class="box-item" effect="dark" content="详情" placement="top">
            <el-button link type="primary" @click="handleView(scope.row)">
              <Icon icon="ep:view" />
            </el-button>
          </el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip class="box-item" effect="dark" content="关联摄像头" placement="top">
            <el-button link type="primary" @click="openBindDialog(scope.row)">
              <Icon icon="ep:video-camera" />
            </el-button>
          </el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip class="box-item" effect="dark" content="删除" placement="top">
            <el-popconfirm title="是否确认删除该数据" @confirm="handleDelete(scope.row)">
              <template #reference>
                <el-button link type="danger">
                  <Icon icon="ep:delete" />
                </el-button>
              </template>
            </el-popconfirm>
          </el-tooltip>
        </div>
      </template>
      <template #type="{ row }">{{ getNodeTypeName(row.nodeType) }}</template>
      <template #deviceMapFormItem="{ row }">
        <div v-if="state.showDeviceMap">
          <Map
            :clickMap="true"
            @locate-change="(lnglat) => locateChange(lnglat, row)"
            :isWrite="true"
            v-model:center="state.mapLnglat"
          />
        </div>
      </template>
      <template #slots2Search>
        <el-input
          readonly
          :value="productObj?.productKey"
          :placeholder="$t('productKey')"
          width="200"
        >
          <template #append>
            <el-button @click="selectProduct">{{ $t('选择产品') }}</el-button>
          </template>
        </el-input>
      </template>
      <template #exportRight>
        <el-select
          v-model="state.query.tenantId"
          clearable
          placeholder="所属企业"
          style="width: 220px; margin-left: 10px"
          @change="handleTenantChange"
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
    <children-dialog ref="childrenDialogRef" />

    <el-dialog title="添加设备到组" v-model="state.showDeviceToGroup" width="350px" append-to-body>
      <el-row style="align-items: center">
        <el-col :span="7">
          <span><b> 请选择分组：</b></span>
        </el-col>

        <el-col :span="16">
          <el-select v-model="state.toGroupId" placeholder="请选择分组">
            <el-option
              v-for="item in groupOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-col>
      </el-row>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="state.showDeviceToGroup = false">取 消</el-button>
          <el-button type="primary" @click="handleAddDeviceToDeviceGroup">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量分配租户弹窗 -->
    <el-dialog
      title="批量分配租户"
      v-model="state.showBatchAssignTenant"
      width="900px"
      append-to-body
      destroy-on-close
    >
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
        <template #title> 请勾选需要分配租户的设备，已有租户的设备不会被修改。 </template>
      </el-alert>

      <!-- 设备选择表格 -->
      <el-table
        ref="batchDeviceTableRef"
        :data="batchDeviceList"
        @selection-change="handleBatchSelectionChange"
        max-height="400"
        border
        style="margin-bottom: 16px"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="设备ID" width="180" />
        <el-table-column prop="name" label="设备名称" width="150" />
        <el-table-column prop="dn" label="唯一标识" width="180" />
        <el-table-column prop="serialNo" label="序列号" width="180" />
        <el-table-column prop="tenantName" label="所属企业" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.tenantId && row.tenantId !== 0" type="success">{{
              row.tenantName || '已分配'
            }}</el-tag>
            <el-tag v-else type="info">未分配</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: space-between; align-items: center">
        <div style="color: #606266">
          已选择 <b style="color: #409eff">{{ batchSelectedDevices.length }}</b> 个设备
        </div>
        <el-form label-width="80px" style="display: flex; align-items: center">
          <el-form-item label="选择租户" style="margin-bottom: 0">
            <el-select v-model="state.assignTenantId" placeholder="请选择租户" style="width: 200px">
              <el-option
                v-for="item in tenantOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="state.showBatchAssignTenant = false">取 消</el-button>
          <el-button
            type="primary"
            @click="confirmBatchAssignTenant"
            :disabled="batchSelectedDevices.length === 0 || !state.assignTenantId"
          >
            确 定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 添加文件上传 -->
    <el-dialog
      :title="fileUploadDialog.title"
      v-model="fileUploadDialog.visible"
      width="500px"
      :close-on-press-escape="false"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="ossFormRef" v-model="fileUploadDialog.visible" label-width="80px">
        <el-form-item label="产品">
          <el-input readonly :value="importProductObj?.name" :placeholder="$t('productKey')">
            <template #append>
              <el-button @click="selectImportProduct">{{ $t('选择产品') }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <!-- 新增：父设备选择（可选） -->
        <el-form-item label="父设备">
          <div style="display: flex; align-items: center; gap: 8px">
            <select-device
              :id="importParentObj?.id?.toString() || ''"
              :node-type="0"
              size="default"
              placeholder="请选择父设备（可选）"
              @on-select="handleSelectParent"
              @update:id="
                (val) => {
                  if (!val) clearImportParent()
                }
              "
              style="flex: 1"
            />
            <el-button v-if="importParentObj" link type="danger" @click="clearImportParent"
              >清除</el-button
            >
          </div>
        </el-form-item>
        <el-form-item label="文件名">
          <fileUpload
            :fileSize="10"
            :fileType="['xlsx', 'xls']"
            :limit="1"
            uploadType="url"
            :uploadUrl="computedUploadUrl"
            @upload-success="handleUploadSuccess"
          />
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
  <!-- 选择产品 -->
  <product-list ref="productListRef" @product-event="getProductData" />
  <product-list ref="productListImportRef" @product-event="getImportProductData" />

  <!-- 关联摄像头弹窗 -->
  <el-dialog
    :title="`关联摄像头 — ${currentBindDevice.name || ''}`"
    v-model="bindDialogVisible"
    width="700px"
    append-to-body
  >
    <CameraBindPanel
      v-if="bindDialogVisible && currentBindDevice.id"
      :deviceId="currentBindDevice.id"
    />
  </el-dialog>
</template>

<script lang="ts" setup>
import defaultImg from '@/assets/imgs/eiot/pic_device.png'
import { IColumn } from '@/components/common/types/tableCommon'
import { ComponentInternalInstance, computed, ref } from 'vue'
import { DeviceInfoApi } from '@/api/eiot/deviceinfo'
import { ProductApi, ProductVO } from '@/api/eiot/product'

import Map from '@/components/Map/index.vue'
import ChildrenDialog from './modules/childrenDialog.vue'
import YtCrud from '@/components/common/yt-crud.vue'
import { ElPopconfirm } from 'element-plus'
import StatusTag from '@/components/StatusTag/index.vue'
import {
  getDeviceTemplate,
  getGroupTemplate,
  getDeviceGroupsList
} from '@/api/eiot/deviceinfo/devices.api'
import download from '@/utils/download'
import productList from './product-list.vue'
import CameraBindPanel from '@/views/eiot/video/components/CameraBindPanel.vue'
import SelectDevice from '@/components/EiotSelect/select-device.vue'
import { getTenantPage } from '@/api/system/tenant'

defineOptions({ name: 'DeviceInfo' })
const message = useMessage() // 消息弹窗
const productListRef = ref()
const bindDialogVisible = ref(false)
const currentBindDevice = ref<any>({})

const openBindDialog = (row: any) => {
  currentBindDevice.value = row
  bindDialogVisible.value = true
}
const { proxy } = getCurrentInstance() as ComponentInternalInstance
const state = reactive({
  page: {
    pageSize: 12,
    pageNo: 1
  },
  total: 0,
  loading: false,
  showDeviceMap: false,
  mapLnglat: '' as any,
  query: {} as Record<string, any>,
  showDeviceToGroup: false,
  toGroupId: '',
  showBatchAssignTenant: false,
  assignTenantId: undefined as number | undefined
})
const layoutType = ref('card')

// 批量分配租户相关
const batchDeviceList = ref<any[]>([])
const batchSelectedDevices = ref<any[]>([])
const batchDeviceTableRef = ref()

const importDeviceUrl = ref('/eiot/device/importData')
// 查看详情
const router = useRouter()
const handleView = (row: any) => {
  if (!row.id) return
  let showMap = true
  router.push({
    path: `/eiot/device/deviceDetail/${row.id}`,
    state: {
      type: 'view'
    }
  })
}
const selectProduct = async () => {
  await productListRef.value.open()
}

const productListImportRef = ref()

const selectImportProduct = async () => {
  await productListImportRef.value.open()
}

const handleEdit = (row: any) => {
  if (!row.id) return
  let showMap = true
  router.push({
    path: `/eiot/device/deviceDetail/${row.id}`,
    state: {
      type: 'edit'
    }
  })
}
const nodeTypeOptions = [
  {
    value: 0,
    label: '网关设备'
  },
  {
    value: 1,
    label: '网关子设备'
  },
  {
    value: 2,
    label: '直连设备'
  },
  {
    value: 3,
    label: '视频设备'
  }
]

const productObj = ref()
const importProductObj = ref()
const importParentObj = ref()

const getProductData = (product) => {
  productObj.value = product
  state.query.productKey = product?.productKey
}

const getImportProductData = (product) => {
  importProductObj.value = product
}

const handleSelectParent = (row: any) => {
  importParentObj.value = row
}

const clearImportParent = () => {
  importParentObj.value = undefined
}

const computedUploadUrl = computed(() => {
  let url = importDeviceUrl.value + '?productId=' + (importProductObj.value?.id || '')
  if (importParentObj.value?.id) {
    url += '&parentId=' + importParentObj.value.id
  }
  return url
})

// 打开子设备
const childrenDialogRef = ref()
const showChidrenDevices = (row: any) => {
  childrenDialogRef.value.openDialog(row)
}

// 复制ID
const copyIdSuccess = () => {
  message.success('复制成功')
}

// 产品字典
const productOptions = ref<ProductVO[]>([])

// 组列表
const groupOptions = ref()

const getGroupsList = () => {
  getDeviceGroupsList({ pageNo: 1, pageSize: 100, name: '' }).then((res) => {
    groupOptions.value = res.list
  })
}

getGroupsList()

// 租户列表
const tenantOptions = ref<any[]>([])

//拉取企业列表
const loadTenantOptions = () => {
  getTenantPage({ pageNo: 1, pageSize: 100 }).then((res) => {
    tenantOptions.value = res.list || []
  })
}
loadTenantOptions()
// 按租户筛选：回第 1 页重新查询；清空时删掉条件恢复全量
const handleTenantChange = () => {
  if (!state.query.tenantId) {
    delete state.query.tenantId
  }
  state.page.pageNo = 1
  getData()
}
// 导出：把当前筛选条件（含租户）一起带给后端
const handleExport = async () => {
  try {
    const res = await DeviceInfoApi.exportDeviceInfo({ ...state.query })
    download.excel(res, '设备信息.xls')
  } catch (e) {
    ElMessage.error('导出失败，请稍后重试')
  }
}

const column = ref<IColumn[]>([
  {
    label: '设备ID',
    key: 'id',
    formHide: true,
    rules: [{ required: true, message: '设备ID不能为空' }]
  },
  {
    label: '名称',
    key: 'name',
    search: true,
    tableWidth: 240,
    componentProps: {
      placeholder: '别名'
    }
  },
  {
    label: '唯一标识',
    key: 'dn',
    search: true,
    tableWidth: 240,
    componentProps: {
      placeholder: '一般为设备mac'
    },
    rules: [{ required: true, message: '设备标识不能为空' }]
  },
  {
    label: '产品',
    key: 'slots2',
    tableWidth: 200,
    search: true,
    hide: true,
    searchSlot: true
  },
  {
    label: '序列号',
    key: 'serialNo',
    tableWidth: 180,
    formHide: true,
    search: true,
    rules: [{ required: true, message: 'serialNo不能为空' }]
  },
  {
    label: '产品',
    key: 'productKey',
    type: 'select',
    // search: true,
    colSpan: 12,
    tableWidth: 240,
    editDisabled: true,
    componentProps: {
      labelAlias: 'name',
      valueAlias: 'productKey',
      options: []
    },
    rules: [{ required: true, message: '产品名称不能为空' }],
    formWatch: (scope) => {
      scope.column.forEach((f: IColumn) => {
        if (['parentId', 'longitude', 'latitude'].includes(f.key)) {
          if (!scope.value) {
            f.formHide = true
            state.showDeviceMap = false
            return
          }
          productOptions.value.forEach((p) => {
            if (p.productKey == scope.value) {
              if (f.key === 'parentId') {
                f.formHide = p.nodeType !== 1
              } else {
                state.showDeviceMap = true
                f.formHide = false
              }
            }
          })
        }
      })
      column.value = scope.column
    }
  },
  {
    label: '设备类型',
    key: 'type',
    tableWidth: 120,
    slot: true,
    formHide: true
  },
  {
    label: '网关设备',
    key: 'parentId',
    type: 'select',
    colSpan: 12,
    tableWidth: 120,
    hide: true,
    formHide: true,
    componentProps: {
      labelAlias: 'deviceName',
      valueAlias: 'id',
      options: [],
      placeholder: '子设备可选择父设备'
    }
  },

  {
    label: '设备经度',
    key: 'longitude',
    hide: true,
    formHide: true,
    colSpan: 12
  },
  {
    label: '设备纬度',
    key: 'latitude',
    hide: true,
    formHide: true,
    colSpan: 12
  },
  {
    label: '设备地图',
    key: 'deviceMap',
    hide: true,
    formItemSlot: true
  },
  // {
  //   label: '分组',
  //   key: 'group',
  //   type: 'select',
  //   search: true,
  //   componentProps: {
  //     labelAlias: 'name',
  //     valueAlias: 'id',
  //     options: groupOptions,
  //   },
  //   slot: true,
  // },
  {
    label: '状态',
    key: 'state',
    type: 'select',
    componentProps: {
      style: 'width: 120px',
      options: [
        {
          label: '在线',
          value: 1
        },
        {
          label: '离线',
          value: 0
        },
        {
          label: '未激活',
          value: 2
        },
        {
          label: '禁用',
          value: 3
        }
      ]
    },
    search: true,
    formHide: true,
    tableWidth: 120,
    slot: true
  },

  {
    label: '所属企业',
    key: 'tenantName',
    tableWidth: 160,
    formHide: true,
    search: false
  },
  {
    label: '创建时间',
    key: 'createTime',
    tableWidth: 180,
    sortable: true,
    type: 'date',
    formHide: true
  }
])

const getStateType = (state: number) => {
  switch (state) {
    case 0:
      return 'danger' // 离线
    case 1:
      return 'success' // 在线
    case 2:
      return 'warning' // 未激活
    case 3:
      return 'danger' // 禁用
    default:
      return 'primary'
  }
}

const getStateText = (state: number) => {
  switch (state) {
    case 0:
      return '离线'
    case 1:
      return '在线'
    case 2:
      return '未激活'
    case 3:
      return '禁用'
    default:
      return ''
  }
}

const crudRef = ref()
const data = ref<any[]>([])
const getData = () => {
  state.loading = true
  DeviceInfoApi.getDeviceInfoPage({
    ...state.page,
    ...state.query
  })
    .then((res) => {
      data.value = res.list
      state.total = res.total
    })
    .finally(() => {
      state.loading = false
    })
}

// const getDict = () => {
//   ProductApi.getProductPage({
//     pageNo: 1,
//     pageSize: 100,
//   }).then((res) => {
//     productOptions.value = res.list || []
//     column.value.forEach((item) => {
//       if (item.key === 'productKey') {
//         item.componentProps.options = productOptions.value
//       }
//     })
//   })
// }
// getDict()
const getProductName = (p: any) => {
  if (!p) {
    return ''
  }
  return p['name']
}
const getNodeTypeName = (nodeType) => {
  if (!nodeType) {
    return '直连设备'
  }

  return ['网关设备', '网关子设备', '直连设备', '监控设备'][nodeType]
}
// 保存数据
const handleAdd = async () => {
  router.push({
    path: `/eiot/device/deviceDetail/0`,
    state: {
      type: 'add'
    }
  })
}
// 弹窗前置操作
const openBeforeFun = ({ type, data }) => {
  if (type === 'add') {
    state.mapLnglat = ''
  } else if (type === 'update') {
    const latitude = data?.locate?.latitude || ''
    const longitude = data?.locate?.longitude || ''
    state.mapLnglat = longitude + ',' + latitude
  }
}

// 删除
const handleDelete = async (row: any) => {
  state.loading = true
  if (row instanceof Array) {
    await DeviceInfoApi.deleteByIds(row.map((m) => m.id))
  } else {
    await DeviceInfoApi.deleteDeviceInfo(row.id)
  }
  ElMessage.success('删除成功!')
  state.loading = false
  getData()
}

const multipleSelection = ref<any[]>([])

const onChange = (data) => {
  multipleSelection.value = data
}

// 添加设备到组弹框
const handleToGroup = () => {
  state.showDeviceToGroup = true
}

// 添加设备到组
const handleAddDeviceToDeviceGroup = () => {
  // addDeviceToDeviceGroup({ group: state.toGroupId, devices: multipleSelection.value.map((m) => m.deviceId) }).then((res) => {
  //   if (res.code == 200) {
  //     ElMessage.success('添加成功!')
  //     state.showDeviceToGroup = false
  //     getData()
  //   } else {
  //     ElMessage.error(res.msg)
  //   }
  // })
}

// 批量分配租户
const handleBatchAssignTenant = async () => {
  // 加载租户列表
  await loadTenantOptions()
  // 加载设备列表（全部设备，用于选择）
  await loadBatchDevices()
  state.assignTenantId = undefined
  batchSelectedDevices.value = []
  state.showBatchAssignTenant = true
}

const loadBatchDevices = async () => {
  try {
    const res = await DeviceInfoApi.getDeviceInfoPage({
      pageNo: 1,
      pageSize: 1000
    })
    batchDeviceList.value = res.list || []
  } catch (e) {
    ElMessage.error('加载设备列表失败')
  }
}

const handleBatchSelectionChange = (selection: any[]) => {
  batchSelectedDevices.value = selection
}

const confirmBatchAssignTenant = async () => {
  if (batchSelectedDevices.value.length === 0) {
    ElMessage.warning('请至少选择一个设备')
    return
  }
  if (!state.assignTenantId) {
    ElMessage.warning('请选择租户')
    return
  }
  const deviceIds = batchSelectedDevices.value.map((m) => m.id)
  try {
    const res = await DeviceInfoApi.batchAssignTenant({
      deviceIds,
      tenantId: state.assignTenantId
    })
    console.log('[批量分配租户] 响应:', res)

    // 处理响应：请求工具可能已解包，直接返回 data；也可能返回完整的 {code, data} 结构
    const data = res?.data || res
    const successCount = data?.successCount ?? 0
    const skippedCount = data?.skippedCount ?? 0
    const totalCount = data?.totalCount ?? deviceIds.length

    console.log('[批量分配租户] 结果:', { successCount, skippedCount, totalCount })

    // 根据结果展示不同的提示
    if (successCount > 0 && skippedCount === 0) {
      // 全部成功
      ElMessage.success(`成功为 ${successCount} 个设备分配租户`)
    } else if (successCount > 0 && skippedCount > 0) {
      // 部分成功
      ElMessage.warning(
        `成功分配 ${successCount} 个设备，${skippedCount} 个设备因已有租户而跳过（共 ${totalCount} 个）`
      )
    } else if (successCount === 0 && skippedCount > 0) {
      // 全部跳过 - 这不是错误，是正常情况
      ElMessage.info(`所选 ${skippedCount} 个设备均已分配过租户，无法重复修改`)
    } else {
      // 都没有
      ElMessage.info('没有设备被分配租户')
    }

    getData()
  } catch (e: any) {
    // 异常捕获
    console.error('[批量分配租户] 异常:', e)
    ElMessage.error(e?.message || e?.msg || '分配失败，请稍后重试')
  }
}

const handleUploadSuccess = (res) => {
  fileUploadDialog.value.visible = false

  if (res.code == 0) {
    ElMessage.success(res.msg)
    getData()
  }
}

const fileUploadDialog = ref({
  title: '导入设备数据',
  visible: false
})

const handleImport = () => {
  fileUploadDialog.value.visible = true
}

const handleDownloadTemplate = async () => {
  const res = await getDeviceTemplate()
  download.excel(res, '设备导入模板.xls')
}

const locateChange = (e, row) => {
  if (!e) return
  row.longitude = e[0] || ''
  row.latitude = e[1] || ''
}
const options = reactive({
  ref: 'crudRef',
  data,
  column
})
</script>

<style lang="scss" scoped>
::v-deep(.el-radio-button__inner) {
  padding: 8px;
}

::v-deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  border: 1px solid #0070ffff;
  background: #0070ff1a;
  box-shadow: none;

  svg {
    path {
      fill: #fff;
    }
  }
}

.addDeviceToGroup {
  margin-right: 10px;
}

.el-radio-group {
  flex-wrap: nowrap;
}

.card-list {
  .card-item {
    border: 1px solid #d8dee5;
    border-radius: 3px;
    margin-right: 16px;
    margin-bottom: 16px;
    flex: 0 0 calc(25% - 12px);
    width: calc(25% - 12px);

    &.success-box {
      .text-box {
        background: linear-gradient(141.6deg, rgb(238, 250, 255) 0%, rgba(255, 255, 255, 0) 80%);
      }
    }

    &.error-box {
      .text-box {
        background: linear-gradient(141.6deg, rgb(255, 241, 241) 0%, rgba(255, 255, 255, 0) 80%);
      }
    }

    &:nth-child(4n) {
      margin-right: 0;
    }

    .text-box {
      padding: 16px;

      .title {
        font-size: 16px;
        font-weight: 600;
        align-items: center;
        margin-bottom: 12px;
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .title-l {
          display: flex;
          align-items: center;
          flex: 1;
          min-width: 0; // 添加这行

          // 添加这个样式块
          > div:last-child {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }

      .text {
        align-items: center;
        font-size: 14px;

        .txt {
          flex: 1;

          .txt-item {
            display: flex;
            margin-bottom: 10px;
            &:last-child {
              margin-bottom: 0;
            }

            .copy-tag {
              padding: 4px 8px;
              background-color: #fff7ef;
              color: #ff7d00;
              display: inline-flex;
              align-items: center;
              transition: 0.3s ease;
              cursor: pointer;

              &:hover {
                opacity: 0.8;
                transform: translateY(-2px);
              }

              svg {
                margin-right: 8px;
              }
            }
          }

          border-radius: 2px;

          .label {
            display: inline-block;
            margin-right: 10px;
            color: #717c8e;
            width: 60px;
          }

          .value {
            display: inline-block;
            color: #0b1d30;
            flex: 1;
            overflow: hidden; // 添加这行
            text-overflow: ellipsis; // 添加这行
            white-space: nowrap; // 修改这行
            max-width: 180px; // 添加这行

            &.active {
              color: #0070ff;
            }
          }
        }

        .img {
          width: 100px;
          height: 100px;

          img {
            width: 100%;
            height: auto;
          }
        }
      }
    }

    .btn-group {
      padding: 12px 16px;
      border-top: 1px solid #dcdfe1;

      .cu-btn {
        width: calc((100% - 73px) / 3);
      }

      .el-button {
        padding: 8px;
      }

      display: flex;
      justify-content: flex-end;
      align-items: center;
    }
  }
}

@media screen and (max-width: 1560px) {
  .card-list .card-item .btn-group {
    padding: 12px;

    .el-button {
      font-size: 12px;
    }

    .cu-btn {
      width: calc((100% - 59px) / 3);
    }

    .el-button + .el-button {
      margin-left: 6px;
    }
  }
}

@media screen and (max-width: 1400px) {
  .card-list .card-item {
    width: calc(100% / 3 - 8px);
    flex: 0 0 calc(100% / 3 - 8px);
    margin-right: 12px;

    &:nth-child(4n) {
      margin-right: 12px;
    }

    &:nth-child(3n) {
      margin-right: 0;
    }
  }
}
</style>
