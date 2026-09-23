<template>
  <div>
    <yt-crud
      ref="crudRef"
      :data="data"
      :column="column"
      :loading="state.loading"
      :total="state.total"
      :tableProps=" {
    selection: false,
    viewBtn: true,
    delBtn: true,
    menuSlot: true
    }"
      :fun-props="{
      delBtn: false,
    }"
      v-model:page="state.page"
      v-model:query="state.query"
      @openBeforeFun="openBeforeFun"
      @delFun="onDelete"
      @saveFun="onSave"
      @onLoad="getData"
    >
      <template #remarkForm="scope">
        <el-input v-model="scope.row.remark" type="textarea" :rows="2" maxlength="255" />
      </template>
      <template #menuSlot="scope">
        <el-tooltip class="box-item" effect="dark" content="物模型" placement="top">
          <el-button link type="primary" icon="ScaleToOriginal" @click="openObjectModel(scope.row)" />
        </el-tooltip>
      </template>
    </yt-crud>
    <object-model ref="objectModelRef" />
  </div>
</template>
<script lang="ts" setup>
import {IColumn} from '@/components/common/types/tableCommon'
import ObjectModel from './modules/objectModel.vue'
import YtCrud from '@/components/common/yt-crud.vue'
import { ModBusInfoApi, ModBusInfoVO } from '@/api/eiot/modbus'

const {t} = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const randomString = (len: number) => {
  len = len || 32
  var $chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678'
  var maxPos = $chars.length
  var pwd = ''
  for (var i = 0; i < len; i++) {
    pwd += $chars.charAt(Math.floor(Math.random() * maxPos))
  }
  if (data.value.findIndex((f) => f.productKey === pwd) !== -1) {
    return randomString(len)
  }
  return pwd
}
const openBeforeFun = ({type, data}) => {
  if (type === 'add') {
    data.productKey = randomString(16)
  }
}


const column: IColumn[] = [
  {
    label: '产品Key',
    key: 'productKey',
    editDisabled: true,
    addDisabled: true,
    rules: [{required: true, message: '产品Key不能为空'}],
  },
  {
    label: '产品名称',
    key: 'name',
    search: true,
    rules: [{required: true, message: '产品名称不能为空'}],
  },
  {
    label: '说明',
    key: 'remark',
    formSlot: true
  },
  {
    label: '创建时间',
    key: 'createTime',
    type: 'date',
    componentProps: {
      format: 'YYYY-MM-DD HH:mm',
    },
    formHide: true
  },
  {
    label: '修改时间',
    key: 'updateTime',
    type: 'date',
    componentProps: {
      format: 'YYYY-MM-DD HH:mm',
    },
    formHide: true
  }

]

const state = reactive({
  total: 0,
  page: {
    pageSize: 12,
    pageNo: 1,
  },
  query: {},
  loading: false,
})
const data = ref<ModBusInfoVO[]>([])
const getData = async () => {
  state.loading = true

  await ModBusInfoApi.getModbusList({
    ...state.page,
    ...state.query,
  }).then((res) => {
    data.value = res.list || []
    state.total = res.total
  }).finally(() => {
    state.loading = false
  });

}
// 保存数据
const onSave = async ({type, data, cancel}: any) => {
  state.loading = true
  try {
    await ModBusInfoApi.saveModbus(toRaw(data))
    if (type === 'add') {
      message.success(t('common.createSuccess'))
    } else {
      message.success(t('common.updateSuccess'))
    }
    cancel()
    await getData()
  } finally {
    state.loading = false
  }
}

// 删除
const onDelete = async (row: any) => {
  state.loading = true
  let ids = row.id
  await ModBusInfoApi.deleteModbus(ids)
  .then((res) => {
    message.success(t('common.delSuccess'))
  }).finally(() => {
    state.loading = false
  })
  await getData()
  
}

const objectModelRef=ref()
const openObjectModel = (row: any) => {
  objectModelRef.value.getInfo(row.productKey)
}
</script>

<!-- <style lang="scss" scoped>

</style> -->
