<template>
  <div class="bg-[#FFFFFF] py-[20px]" v-if="state.visible" v-loading="state.loading">
    <div class="ml-[50px] mb-[20px]">
      <el-button type="info" @click="goBack">返回列表</el-button>
    </div>
    <el-tabs :tab-position="tabPosition">
      <el-tab-pane>
        <template #label>
          <span>
            <span class="color-[#FF0000]">*</span>
            <span>基本信息</span>
          </span>
        </template>
        <el-form ref="ruleFormRef" :model="form" :rules="rules" label-width="auto" class="w-[600px]">
          <el-form-item v-if="type != 'create'" label="产品密钥" prop="productSecret">
            <el-input v-model="form.productSecret" :disabled="disabled"/>
          </el-form-item>
          <el-form-item label="产品KEY" prop="productKey">
            <el-input v-model="form.productKey" :disabled="disabled"/>
          </el-form-item>
          <el-form-item label="产品名称" prop="name">
            <el-input v-model="form.name" :disabled="disabledView"/>
          </el-form-item>
          <el-form-item label="产品品类" prop="categoryId">
            <el-select v-model="form.categoryId" :disabled="disabledView">
              <el-option v-for="(item, index) in categoryIdOptions" :key="index" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="节点类型" prop="nodeType">
            <el-select v-model="form.nodeType" :disabled="disabled">
              <el-option v-for="(item, index) in getIntDictOptions(DICT_TYPE.EIOT_NODE_TYPE)" :key="index" :label="item.label"
                         :value="item.value"/>
            </el-select>
          </el-form-item>
          <el-form-item label="芯片类型" prop="mcuCode">
            <el-select v-model="form.mcuCode" :disabled="disabled">
              <el-option v-for="(item, index) in getStrDictOptions(DICT_TYPE.EIOT_MUC_CODE)" :key="index" :label="item.label"
                         :value="item.value"/>
            </el-select>
          </el-form-item>
          <el-form-item label="唯一标识" prop="dnType">
            <el-select v-model="form.dnTyp" :disabled="disabled">
              <el-option v-for="(item, index) in getIntDictOptions(DICT_TYPE.EIOT_DN_TYP)" :key="index" :label="item.label"
                         :value="item.value"/>
            </el-select>
          </el-form-item>

          <el-form-item label="设备协议">
            <el-select v-model="form.protocolCode" :disabled="disabled">
              <el-option v-for="item in getStrDictOptions(DICT_TYPE.EIOT_PROTOCOL_CODE)"
                         :key="item.value" :label="item.label" :value="item.value"/>
            </el-select>
          </el-form-item>
          <el-form-item label="透传设备" prop="transparent">
            <el-radio-group v-model="form.transparent" :disabled="disabledView">
              <el-radio v-for="(item, index) in transparentOptions" :key="index"
                        :label="item.value">{{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="保活时长(秒)" prop="keepAliveTime">
            <el-input-number v-model="form.keepAliveTime" :disabled="disabledView"/>
          </el-form-item>

          <el-form-item  label="定位方式" prop="locateType">
            <el-radio-group v-model="form.locateType" :disabled="disabledView">
              <el-radio v-for="(item, index) in locateUpdateTypeOptions" :key="index"
                        :label="item.value">{{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="产品图片">
            <ImageUpload v-model="form.imgUrl" :modelValue="form.imgUrl" :disabled="disabledView"
                         uploadType="url"/>
          </el-form-item>

          <div class="flex justify-center" v-if="type != 'view'">
            <el-button type="primary" @click="submitForm">{{
                type == 'create' ? '新增' : '保存'
              }}
            </el-button>
          </div>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="产品配置" :disabled="!disabled">
        <div class="mt-[20px]">
          <el-button type="primary" @click="submitConfigChange()">保存</el-button>
        </div>
        <el-form ref="configForm">
          <div v-for="item in allConfigItems" :key="item.value" :label="item.label">
            <span>{{ item.label }}</span>
            <span> {{ item.value }}: </span>>
            <span>
              <el-switch
                :key="item"
                :label="item"
                v-model="productConfigCheck[item.value]"
                @change="changeConfigItemSwitch($event, item)"
              />
            </span>
            <vue-json-editor
              v-if="productConfigCheck[item.value]"
              v-model:json="editConfig[item.value]"
              :showBtns="false"
              :mode="'text'"
              :escapeControlCharacters="true"
              lang="zh"
              height="400"
              @change="changeConfigItem($event,item)"
            />
          </div>
        </el-form>
      </el-tab-pane>
<!--      <el-tab-pane v-if="form.nodeType == 0" label="子设备默认配置" :disabled="!disabled">-->
<!--        子设备默认配置-->
<!--      </el-tab-pane>-->
      <el-tab-pane label="物模型" :disabled="!disabled">
        <ThingModel :productInfo="form"/>
      </el-tab-pane>
<!--      <el-tab-pane label="显示模型配置" :disabled="!disabled">-->
<!--        <ModelDetail :productInfo="form" />-->
<!--      </el-tab-pane>-->
      <el-tab-pane label="告警配置" :disabled="!disabled">
        <ProductAlertConfig
          v-if="productId"
          :productId="productId"
        />
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import ImageUpload from '@/components/ImageUpload/index.vue'
import {CategoryApi, CategoryVO} from '@/api/eiot/category'
import {ProductApi, ProductVO} from "@/api/eiot/product"
import {DICT_TYPE, getIntDictOptions, getStrDictOptions} from '@/utils/dict'
import ModelDetail from './modules/modelDetail.vue'
import VueJsonEditor from 'vue3-ts-jsoneditor'

import ThingModel from './modules/thingModel.vue'
import ProductAlertConfig from '@/views/eiot/productalert/config.vue'
import {ComponentInternalInstance, toRaw} from 'vue'

const {t} = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const route = useRoute()

const router = useRouter()
const {proxy} = getCurrentInstance() as ComponentInternalInstance
const type = ref(undefined)

const ruleFormRef = ref()

const disabledView = ref(false)
const disabled = ref(false)

const productId = ref<number | undefined>(undefined)

const tabPosition = ref('left')

const transparentOptions = [
  {
    value: true,
    label: '开启',
  },
  {
    value: false,
    label: '关闭',
  },
]



const locateUpdateTypeOptions = [
  {
    value: 0,
    label: '手动定位',
  },
  {
    value: 1,
    label: '设备上报',
  },
]


const rules = reactive({
  productKey: [
    {required: true, message: '产品KEY不能为空', trigger: 'blur'}
  ],
  name: [
    {required: true, message: '产品名称不能为空', trigger: 'blur'}
  ],
  categoryId: [
    {required: true, message: '产品品类不能为空', trigger: 'blur'}
  ],
  keepAliveTime: [
    {required: true, message: '保活时长(秒)不能为空', trigger: 'blur'}
  ],
  nodeType: [
    {required: true, message: '节点类型不能为空', trigger: 'blur'}
  ],
  protocolCode: [
    {required: true, message: '协议不能为空', trigger: 'blur'}
  ],
  mcuCode: [
    {required: true, message: 'MCU类型不能为空', trigger: 'blur'}
  ],
  dnTyp: [
    {required: true, message: '唯一标识类型不能为空', trigger: 'blur'}
  ],
})

const state = reactive({
  loading: false,
  visible: false,
})

const form = ref({
  categoryId: undefined,
  locateType: 0,
  name: undefined,
  nodeType: 0,
  productKey: '',
  transparent: false,
  config: undefined,
  productSecret: undefined,
  imgUrl: undefined,
  keepAliveTime: 0,
  protocolCode: 'N',
  dnTyp: 1,
  mcuCode: 'N',
  status: 0,
})

const configInit = () => {
  // getProductConfig(form.value.productKey).then(
  //   (res)=>{
  //     if(res){
  //       editConfig.value = res.data?.config ? JSON.parse(res.data?.config) : {}
  //       const configItems = res.data?.configItems? res.data?.configItems: ''
  //       for(const item of allConfigItems){
  //         productConfigCheck.value[item.value] = configItems.includes(item.value)
  //       }
  //     }
  //   }
  // )
}

const editConfig = ref({})
const allConfigItems = [{value: 'basic', label: '基础配置'}, {
  value: 'interface',
  label: '接口配置'
},
  {value: 'model', label: '物模型配置'}, {value: 'subBasic', label: '子设备基础配置'},
  {value: 'subInterface', label: '子设备接口配置'}, {value: 'subModel', label: '子设备物模型配置'}]

const productConfigCheck = ref({})

const changeConfigItem = (e, item) => {
  console.log(e.text)
  if (e.text) {
    editConfig.value[item.value] = JSON.parse(e.text)
  } else {
    editConfig.value[item.value] = null
  }
  console.log('editconfig', editConfig.value)
}

const changeConfigItemSwitch = (val, item) => {
  console.log('change switch ', val, item)
  console.log('productConfigCheck', productConfigCheck.value)
}

const submitConfigChange = () => {
  let items = Object.entries(productConfigCheck.value).filter(e => e[1] == true).map(en => en[0]).join(',')

  // editProductConfig({productKey: form.value.productKey,config: editConfig.value? JSON.stringify(editConfig.value):'', configItems: items?items:'' })
  //   .then((res) => {
  //     ElMessage.success( '编辑成功')
  //   })
  //   .finally(() => {
  //
  //   })
}


const keyMode = ref(import.meta.env.VITE_PRODUCT_KEY_MODE)

const categoryIdOptions = ref([])

const getDict = async () => {
  state.visible = true
  state.loading = true
  await CategoryApi.getCategoryList(null).then((res) => {
    res = res || {}
    categoryIdOptions.value = res as [] || []
  })
}

const goBack = () => {
  router.push({
    path: '/device/eiotproduct'
  })
}

const randomString = (len: number): string => {
  len = len || 32
  var $chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678'
  if (keyMode.value === 'uppercase') {
    $chars = 'ABCDEFGHJKMNPQRSTWXYZ2345678'
  } else if (keyMode.value === 'lowercase') {
    $chars = 'abcdefhijkmnprstwxyz2345678'
  }
  var maxPos = $chars.length
  var pwd = ''
  for (var i = 0; i < len; i++) {
    pwd += $chars.charAt(Math.floor(Math.random() * maxPos))
  }
  // if (data.value.findIndex((f) => f.productKey === pwd) !== -1) {
  //   return randomString(len)
  // }
  return pwd
}

const submitForm = async () => {
  await ruleFormRef.value.validate((valid, fields) => {
    if (valid) {
      onSave()
    } else {
      console.log('error submit!', fields)
    }
  })
}

const onSave = async () => {

  const data = form.value as unknown as ProductVO
  if (type.value === 'create') {
    await ProductApi.createProduct(data).then((newId)=>{
      message.success(t('common.createSuccess'))
      router.push({ path: `/eiot/products/productEdit/${newId}`, state: {
          data: {id:newId},
          type: 'view'
        }
      })
    })
  } else {
    await ProductApi.updateProduct(data)
    message.success(t('common.updateSuccess'))
  }

}

// const changeNodeType = (e) => {
//   console.log(e)
// }

const initData = async () => {
  const formType = window.history.state.type;
  if (formType == 'create') {
    form.value.productKey = randomString(16);
    form.value.dnTyp = 1; // 设置默认值，假设默认值为1
  } else {
    const id = route.params.id;
    if (id) {
      productId.value = Number(id)
      try {
        const res = await ProductApi.getProduct(id);
        form.value = res;
      } catch (error) {
        console.error('Error fetching product data:', error);
        message.error('获取产品数据失败');
      }
    } else {
      console.error('Product ID is not provided in the route parameters');
      message.error('产品ID未提供');
    }
  }
  configInit();
  await getDict();
  type.value = formType;
  disabledView.value = formType == 'view' ? true : false;
  disabled.value = formType != 'create' ? true : false;
  state.loading = false;
}
initData()



console.log(window.history.state)
</script>

<style scoped>
.c {
  color: red;
}
</style>
