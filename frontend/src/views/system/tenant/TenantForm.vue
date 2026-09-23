<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="60%">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <!-- 基本信息 -->
      <div class="form-section-title">基本信息</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="租户名" prop="name">
            <el-input v-model="formData.name" placeholder="请输入租户名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="租户套餐" prop="packageId">
            <el-select v-model="formData.packageId" clearable placeholder="请选择租户套餐" style="width: 100%">
              <el-option
                v-for="item in packageList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <div class="account-type-tip" v-if="formData.packageId !== undefined">
        <el-tag v-if="formData.packageId === 0" type="danger" size="small">
          <el-icon style="vertical-align: middle; margin-right: 2px;"><UserFilled /></el-icon>
          系统管理员账号 — 登录后进入值守端大屏，拥有所有功能权限，可查看所有企业数据
        </el-tag>
        <el-tag v-else type="success" size="small">
          <el-icon style="vertical-align: middle; margin-right: 2px;"><OfficeBuilding /></el-icon>
          企业账号 — 登录后进入监控中心大屏，仅查看本企业数据
        </el-tag>
      </div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="联系人" prop="contactName">
            <el-input v-model="formData.contactName" placeholder="请输入联系人" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系手机" prop="contactMobile">
            <el-input v-model="formData.contactMobile" placeholder="请输入联系手机" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="客户类型" prop="customerType">
            <el-select v-model="formData.customerType" clearable placeholder="请选择客户类型" style="width: 100%">
              <el-option label="工厂" value="factory" />
              <el-option label="商业综合体" value="commercial" />
              <el-option label="住宅小区" value="residential" />
              <el-option label="学校" value="school" />
              <el-option label="医院" value="hospital" />
              <el-option label="仓储物流" value="warehouse" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="风险等级" prop="riskLevel">
            <el-select v-model="formData.riskLevel" clearable placeholder="请选择风险等级" style="width: 100%">
              <el-option label="重大风险" value="critical" />
              <el-option label="较大风险" value="major" />
              <el-option label="一般风险" value="normal" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="信用代码" prop="creditCode">
            <el-input v-model="formData.creditCode" placeholder="请输入统一社会信用代码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item v-if="formData.id === undefined" label="用户名称" prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名称" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 企业地址信息 -->
      <div class="form-section-title">企业地址</div>
      <el-form-item label="所在地区" prop="areaPath">
        <el-cascader
          v-model="areaPath"
          :options="areaTree"
          :props="{ value: 'id', label: 'name', checkStrictly: false }"
          placeholder="请选择省/市/区"
          clearable
          filterable
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="详细地址" prop="address">
        <el-input v-model="formData.address" placeholder="请输入详细地址" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="经度" prop="longitude">
            <el-input-number
              v-model="formData.longitude"
              :precision="7"
              :min="-180"
              :max="180"
              controls-position="right"
              placeholder="请输入经度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="纬度" prop="latitude">
            <el-input-number
              v-model="formData.latitude"
              :precision="7"
              :min="-90"
              :max="90"
              controls-position="right"
              placeholder="请输入纬度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 账号配置 -->
      <div class="form-section-title">账号配置</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item v-if="formData.id === undefined" label="用户密码" prop="password">
            <el-input
              v-model="formData.password"
              placeholder="请输入用户密码"
              show-password
              type="password"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="账号额度" prop="accountCount">
            <el-input-number
              v-model="formData.accountCount"
              :min="0"
              controls-position="right"
              placeholder="请输入账号额度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="过期时间" prop="expireTime">
            <el-date-picker
              v-model="formData.expireTime"
              clearable
              placeholder="请选择过期时间"
              type="date"
              value-format="x"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="绑定域名" prop="website">
            <el-input v-model="formData.website" placeholder="请输入绑定域名" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="租户状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { UserFilled, OfficeBuilding } from '@element-plus/icons-vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as TenantApi from '@/api/system/tenant'
import { validatePassword } from '@/utils/formRules'
import { CommonStatusEnum } from '@/utils/constants'
import * as TenantPackageApi from '@/api/system/tenantPackage'
import { getAreaTree } from '@/api/system/area'

defineOptions({ name: 'SystemTenantForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗
const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const areaTree = ref([]) // 省市区级联数据
const areaPath = ref<number[]>([]) // 级联选中值（区域编码数组）
const formData = ref({
  id: undefined,
  name: undefined,
  packageId: undefined,
  contactName: undefined,
  contactMobile: undefined,
  customerType: undefined,
  riskLevel: undefined,
  creditCode: undefined,
  provinceCode: undefined as number | undefined,
  province: undefined as string | undefined,
  cityCode: undefined as number | undefined,
  city: undefined as string | undefined,
  districtCode: undefined as number | undefined,
  district: undefined as string | undefined,
  address: undefined,
  longitude: undefined,
  latitude: undefined,
  accountCount: undefined,
  expireTime: undefined,
  website: undefined,
  status: CommonStatusEnum.ENABLE,
  // 新增专属
  username: undefined,
  password: undefined
})
const formRules = reactive({
  name: [{ required: true, message: '租户名不能为空', trigger: 'blur' }],
  packageId: [{ required: true, message: '租户套餐不能为空', trigger: 'blur' }],
  contactName: [{ required: true, message: '联系人不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '租户状态不能为空', trigger: 'blur' }],
  accountCount: [{ required: true, message: '账号额度不能为空', trigger: 'blur' }],
  expireTime: [{ required: true, message: '过期时间不能为空', trigger: 'blur' }],
  website: [{ required: true, message: '绑定域名不能为空', trigger: 'blur' }],
  username: [{ required: true, message: '用户名称不能为空', trigger: 'blur' }],
  password: [{ validator: validatePassword('用户密码不能为空'), trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref
const packageList = ref([] as TenantPackageApi.TenantPackageVO[]) // 租户套餐

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await TenantApi.getTenant(id)
    } finally {
      formLoading.value = false
    }
  }
  // 加载套餐列表
  packageList.value = await TenantPackageApi.getTenantPackageList()
  // 加载省市区级联数据（仅首次）
  if (areaTree.value.length === 0) {
    areaTree.value = await getAreaTree()
  }
  // 编辑时：将 province_code/city_code/district_code 转换为级联选中值
  if (type === 'update' && formData.value.provinceCode) {
    const path = [formData.value.provinceCode, formData.value.cityCode, formData.value.districtCode].filter(Boolean)
    areaPath.value = path as unknown as number[]
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 将级联选中值拆分为 province/city/district（名称+编码）
  if (areaPath.value && areaPath.value.length > 0) {
    // 根据选中的编码查找对应名称
    const findNameByCode = (code: number): string => {
      const findInTree = (nodes: any[], targetCode: number): string | null => {
        for (const node of nodes) {
          if (node.id === targetCode) return node.name
          if (node.children) {
            const found = findInTree(node.children, targetCode)
            if (found) return found
          }
        }
        return null
      }
      return findInTree(areaTree.value, code) || ''
    }
    formData.value.provinceCode = areaPath.value[0] || undefined
    formData.value.province = areaPath.value[0] ? findNameByCode(areaPath.value[0]) : ''
    formData.value.cityCode = areaPath.value[1] || undefined
    formData.value.city = areaPath.value[1] ? findNameByCode(areaPath.value[1]) : ''
    formData.value.districtCode = areaPath.value[2] || undefined
    formData.value.district = areaPath.value[2] ? findNameByCode(areaPath.value[2]) : ''
  }
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as TenantApi.TenantVO
    if (formType.value === 'create') {
      await TenantApi.createTenant(data)
      message.success(t('common.createSuccess'))
    } else {
      await TenantApi.updateTenant(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    name: undefined,
    packageId: undefined,
    contactName: undefined,
    contactMobile: undefined,
    customerType: undefined,
    riskLevel: undefined,
    creditCode: undefined,
    provinceCode: undefined as number | undefined,
    province: undefined as string | undefined,
    cityCode: undefined as number | undefined,
    city: undefined as string | undefined,
    districtCode: undefined as number | undefined,
    district: undefined as string | undefined,
    address: undefined,
    longitude: undefined,
    latitude: undefined,
    accountCount: undefined,
    expireTime: undefined,
    website: undefined,
    status: CommonStatusEnum.ENABLE,
    username: undefined,
    password: undefined
  }
  formRef.value?.resetFields()
  areaPath.value = []
}
</script>
<style scoped>
.account-type-tip {
  margin-top: 6px;
  margin-bottom: 12px;
  padding-left: 110px;
}
.form-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-left: 4px;
  border-left: 3px solid #409eff;
}
</style>
