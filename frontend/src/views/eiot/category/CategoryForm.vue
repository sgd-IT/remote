<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="父分类id" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
          :data="categoryTree"
          :props="defaultProps"
          check-strictly
          default-expand-all
          placeholder="请选择父分类id"
        />
      </el-form-item>
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入分类名称" />
      </el-form-item>
      <el-form-item label="分类排序" prop="sort">
        <el-input v-model="formData.sort" placeholder="请输入分类排序" />
      </el-form-item>
      <el-form-item label="开启状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                    :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="图片地址" prop="imgUrl">
        <el-input v-model="formData.imgUrl" placeholder="请输入图片地址" />
      </el-form-item>
<!--      <el-form-item label="是否系统通用" prop="isSys">-->
<!--        <el-radio-group v-model="formData.isSys">-->
<!--          <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_BOOLEAN_INT)"-->
<!--                    :key="dict.value" :label="dict.label" :value="dict.value"/>-->
<!--        </el-radio-group>-->
<!--      </el-form-item>-->
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { CategoryApi, CategoryVO } from '@/api/eiot/category'
import { defaultProps, handleTree } from '@/utils/tree'
import {getBoolDictOptions, DICT_TYPE, getIntDictOptions} from "@/utils/dict";

/** IOT产品分类 表单 */
defineOptions({ name: 'CategoryForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  parentId: undefined,
  name: undefined,
  sort: undefined,
  status: undefined,
  imgUrl: undefined,
  // isSys: undefined
})
const formRules = reactive({
  parentId: [{ required: true, message: '父分类id不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '开启状态不能为空', trigger: 'blur' }],
  // isSys: [{ required: true, message: '是否系统通用不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref
const categoryTree = ref() // 树形结构

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
      formData.value = await CategoryApi.getCategory(id)
    } finally {
      formLoading.value = false
    }
  }
  await getCategoryTree()
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as CategoryVO
    if (formType.value === 'create') {
      await CategoryApi.createCategory(data)
      message.success(t('common.createSuccess'))
    } else {
      await CategoryApi.updateCategory(data)
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
    parentId: undefined,
    name: undefined,
    sort: undefined,
    status: undefined,
    imgUrl: undefined,
    // isSys: undefined
  }
  formRef.value?.resetFields()
}

/** 获得IOT产品分类树 */
const getCategoryTree = async () => {
  categoryTree.value = []
  const data = await CategoryApi.getCategoryList()
  const root: Tree = { id: 0, name: '顶级IOT产品分类', children: [] }
  root.children = handleTree(data, 'id', 'parentId')
  categoryTree.value.push(root)
}
</script>
