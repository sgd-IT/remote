<template>
  <div>
    <yt-crud
      ref="crudRef"
      :data="list"
      :column="column"
      v-model:page="queryParams"
      v-model:query="queryParams"
      :total="state.total"
      :loading="state.loading"
      :tableProps="{
        selection: true,
        viewBtn: true,
        delBtn: true,
        customTable: layoutType === 'card',
        menuSlot: true,
        menuWidth: 200
      }"
      :fun-props="{
        exportBtn: true,
        delBtn: false
      }"
      :addFunc="handleAdd"
      @del-fun="handleDelete"
      @export-fun="handleExport"
      @on-load="getList"
    >
      <template #customTable>
        <el-row class="card-list flex">
          <el-col class="card-item" v-for="(item, index) in list" :key="index">
            <div class="text-box">
              <div class="title flex align-center">
                <div class="icon">
                  <Icon icon="fa:cube" class="design-iconfont" />
                </div>
                {{ item.name }}
              </div>
              <div class="text flex">
                <div class="txt">
                  <div class="txt-item">
                    <div class="label">所属分类</div>
                    <div class="value active">{{ item.categoryName }}</div>
                  </div>
                  <div class="txt-item">
                    <div class="label">产品类型</div>
                    <div class="value">{{ getNodeTypeNmae(item.nodeType) }}</div>
                  </div>
                  <div class="txt-item">
                    <div class="label">透传设备</div>
                    <div class="value">
                      <span v-if="item.transparent">是</span>
                      <span v-if="!item.transparent">否</span>
                    </div>
                  </div>
                </div>
                <div class="img">
                  <img :src="item.img || defaultImg" alt="" />
                </div>
              </div>
            </div>
            <div class="btn-group">
              <el-button class="cu-btn" type="primary" plain @click="handleUpdate(item)">
                <Icon icon="ep:edit-pen" />
                编辑
              </el-button>
              <el-button class="cu-btn" type="warning" plain @click="handleView(item)">
                <Icon icon="ep:view" />
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
      <template #menuSlot="scope">
        <el-tooltip class="box-item" effect="dark" content="物模型" placement="top">
          <el-button link type="primary" @click="openObjectModel(scope.row)">
            <Icon icon="ep:credit-card" />
          </el-button>
        </el-tooltip>
      </template>
      <template #rightToolbar>
        <el-radio-group v-model="layoutType">
          <el-radio-button value="table">
            <Icon icon="ep:document" :color="layoutType === 'table' ? '#fff' : '#333'" />
          </el-radio-button>
          <el-radio-button value="card">
            <Icon icon="ep:grid" :color="layoutType === 'card' ? '#fff' : '#333'" />
          </el-radio-button>
        </el-radio-group>
      </template>
    </yt-crud>
    <object-model ref="objectModelRef" />
  </div>
</template>

<script setup lang="ts">
import defaultImg from '@/assets/imgs/eiot/pic_device.png'
import download from '@/utils/download'
import ObjectModel from './modules/objectModel.vue'
import { ProductApi, ProductVO } from '@/api/eiot/product'
import EiotProductForm from './EiotProductForm.vue'
import { getIntDictOptions, DICT_TYPE, getDictOptions } from '@/utils/dict'
import { ThingModelApi, ThingModelVO } from '@/api/eiot/thingmodel'
import ThingModelForm from './ThingModelForm.vue'
import { IColumn } from '@/components/common/types/tableCommon'

/** 物联网产品 列表 */
defineOptions({ name: 'EiotProduct' })
const layoutType = ref('card')
const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const list = ref<ProductVO[]>([]) // 列表的数据

/** 打开详情 */
const { push } = useRouter()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  categoryId: undefined,
  productKey: undefined,
  mcuCode: undefined,
  remark: undefined,
  status: undefined,
  createTime: [],
  nodeType: undefined,
  protocolCode: undefined,
  transparent: undefined
})
const state = reactive({
  total: 0,
  loading: false
})

const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
const getList = async () => {
  state.loading = true
  try {
    const data = await ProductApi.getProductPage(queryParams)
    list.value = data.list
    state.total = data.total
  } finally {
    state.loading = false
  }
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
  { value: 3, label: '监控设备' }
]

const column = ref<IColumn[]>([
  {
    label: '产品密钥',
    key: 'productSecret',
    hide: true,
    addHide: true,
    editDisabled: true
  },
  {
    label: '产品Key',
    key: 'productKey',
    search: true,
    editDisabled: true,
    addDisabled: false,
    rules: [{ required: true, message: '产品Key不能为空' }]
  },
  {
    label: '产品名称',
    key: 'name',
    search: true,
    rules: [{ required: true, message: '产品名称不能为空' }]
  },
  {
    label: '品类',
    key: 'category',
    type: 'select',
    tableWidth: 80,
    componentProps: {
      labelAlias: 'name',
      valueAlias: 'id'
    },
    rules: [{ required: true, message: '品类不能为空' }]
  },
  {
    label: '节点类型',
    key: 'nodeType',
    type: 'radio',
    componentProps: {
      defaultValue: 0,
      options: nodeTypeOptions
    }
  },
  {
    label: '设备协议',
    key: 'protocol',
    type: 'select',
    tableWidth: 80,
    componentProps: {
      labelAlias: 'name',
      valueAlias: 'id'
    }
  },
  {
    label: '透传设备',
    key: 'transparent',
    type: 'radio',
    tableWidth: 80,
    componentProps: {
      defaultValue: false,
      options: [
        {
          value: true,
          label: '是'
        },
        {
          value: false,
          label: '否'
        }
      ]
    }
  },
  {
    label: '保活时长(秒)',
    key: 'keepAliveTime',
    type: 'number',
    search: false,
    tableWidth: 110,
    rules: [{ required: true, message: '保活时长不能为空' }]
  },
  {
    label: '设备定位',
    key: 'isOpenLocate',
    type: 'radio',
    tableWidth: 80,
    componentProps: {
      defaultValue: false,
      options: [
        {
          value: true,
          label: '开启'
        },
        {
          value: false,
          label: '关闭'
        }
      ]
    },
    formWatch: (scope) => {
      scope.column.forEach((f: IColumn) => {
        if (f.key === 'locateUpdateType') {
          f.formHide = !scope.value
        }
      })
      column.value = scope.column
    }
  },
  {
    label: '定位方式',
    key: 'locateUpdateType',
    type: 'radio',
    tableWidth: 80,
    formHide: true,
    componentProps: {
      defaultValue: 'manual',
      options: [
        {
          value: 'manual',
          label: '手动定位'
        },
        {
          value: 'device',
          label: '设备上报'
        }
      ]
    }
  },
  {
    label: '产品图片',
    key: 'img',
    type: 'image',
    componentProps: {
      uploadType: 'url'
    }
  },
  {
    label: '创建时间',
    key: 'createTime',
    type: 'date',
    tableWidth: 110,
    sortable: true,
    formHide: true
  }
])

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 添加/修改操作 */
const formRef = ref()

const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}
// 新增
const handleAdd = () => {
  push({
    path: `/eiot/products/productEdit/0`,
    state: {
      type: 'create'
    }
  })
}
// 查看
const handleView = (item) => {
  push({
    path: `/eiot/products/productEdit/${item.id}`,
    state: {
      data: toRaw(item),
      type: 'view'
    }
  })
}

// 编辑
const handleUpdate = (item) => {
  push({
    path: `/eiot/products/productEdit/${item.id}`,
    state: {
      data: toRaw(item),
      type: 'edit'
    }
  })
}

/** 删除按钮操作 */
const handleDelete = async (item) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await ProductApi.deleteProduct(item.id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await ProductApi.exportProduct(queryParams)
    download.excel(data, '物联网产品.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const getNodeTypeNmae = (type) => {
  return nodeTypeOptions.find((f) => f.value === type)?.label || ''
}

const objectModelRef = ref()
const openObjectModel = (row: any) => {
  objectModelRef.value.getInfo(row.productKey)
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
::v-deep(.el-radio-button__inner) {
  padding: 8px;
}

::v-deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #0070ff1a;
  border: 1px solid #0070ffff;
  box-shadow: none;

  svg {
    path {
      fill: #fff;
    }
  }
}

.card-list {
  .card-item {
    width: calc(25% - 12px);
    margin-right: 16px;
    margin-bottom: 16px;
    border: 1px solid #d8dee5;
    border-radius: 3px;
    flex: 0 0 calc(25% - 12px);

    &:nth-child(4n) {
      margin-right: 0;
    }

    .text-box {
      padding: 16px;

      .title {
        margin-bottom: 12px;
        font-size: 16px;
        font-weight: 600;
        align-items: center;

        .icon {
          display: flex;
          margin-right: 10px;
          align-items: center;
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
          }

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
            word-wrap: break-word; /* 在单词内换行 */
            word-break: break-word; /* 允许在单词内换行 */
            white-space: normal; /* 允许换行 */

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
      display: flex;
      align-items: center;
      .cu-btn {
        flex: 1;
        // width: calc((100% - 80px) / 3);
      }

      .el-button {
        padding: 8px;
      }
    }
  }
}

@media screen and (width <= 1560px) {
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

@media screen and (width <= 1400px) {
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
