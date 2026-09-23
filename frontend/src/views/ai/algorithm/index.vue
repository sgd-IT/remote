<template>
  <div class="app-container">
    <!-- 搜索工作栏 -->
    <ContentWrap>
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="100px"
      >
        <el-form-item label="模型名称" prop="name">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入模型名称"
            clearable
            @keyup.enter="handleQuery"
            class="!w-240px"
          />
        </el-form-item>
        <el-form-item label="模型框架" prop="framework">
          <el-select
            v-model="queryParams.framework"
            placeholder="请选择模型框架"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="item in frameworkOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模型类别" prop="category">
          <el-select
            v-model="queryParams.category"
            placeholder="请选择模型类别"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
          <el-button
            type="primary"
            plain
            @click="handleAdd"
            v-hasPermi="['ai:algorithm:create']"
          >
            <Icon icon="ep:plus" class="mr-5px" /> 新增
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表 -->
    <ContentWrap>
      <el-table v-loading="loading" :data="algorithmList">
        <el-table-column label="模型名称" align="center" prop="name" />
        <el-table-column label="模型框架" align="center" prop="framework" />
        <el-table-column label="模型格式" align="center" prop="format" />
        <el-table-column label="模型类别" align="center" prop="category" />
        <el-table-column label="模型文件" align="center" prop="modelFile">
          <template #default="scope">
            <el-button
              type="text"
              @click="downloadModel(scope.row.modelFile)"
            >{{ scope.row.modelFile }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180" :formatter="dateFormatter"/>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button
              type="text"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['ai:algorithm:update']"
            >修改</el-button>
            <el-popconfirm
              title="是否确认删除该模型？"
              @confirm="handleDelete(scope.row)"
            >
              <template #reference>
                <el-button
                  type="text"
                  icon="Delete"
                  v-hasPermi="['ai:algorithm:delete']"
                >删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </ContentWrap>

    <!-- 添加或修改算法对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="algorithmFormRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="form.name" placeholder="支持字母、数字、汉字、英文横杠和下划线" />
        </el-form-item>
        <el-form-item label="模型框架" prop="framework">
          <el-select v-model="form.framework" placeholder="请选择模型框架">
            <el-option v-for="item in frameworkOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型格式" prop="format">
          <el-select v-model="form.format" placeholder="请选择模型格式">
            <el-option v-for="item in formatOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型类别" prop="category">
          <el-select v-model="form.category" placeholder="请选择模型类别" allow-create filterable>
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入模型描述"
          />
        </el-form-item>
        <el-form-item label="模型文件" prop="modelFile">
          <UploadFile
            v-model="form.modelFile"
            :file-type="['pt', 'pth', 'onnx', 'pb', 'h5', 'caffemodel', 'params']"
            :file-size="500"
            :limit="1"
            :drag="true"
          >
            <template #tip>
              <div class="el-upload__tip">
                支持 pt/pth/onnx/pb/h5/caffemodel/params 格式，文件大小不超过 500MB
              </div>
            </template>
          </UploadFile>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'
import { getAccessToken } from '@/utils/auth'
import { dateFormatter } from '@/utils/formatTime'
import { UploadFile } from '@/components/UploadFile'
import { 
  listAlgorithm,
  getAlgorithm,
  addAlgorithm,
  updateAlgorithm,
  delAlgorithm,
  downloadModelFile
} from '@/api/ai/algorithm'

const { proxy } = getCurrentInstance()

// 模型框架选项
const frameworkOptions = [
  { label: 'OneFlow', value: 'ONEFLOW' },
  { label: 'TensorFlow', value: 'TENSORFLOW' },
  { label: 'Pytorch', value: 'PYTORCH' },
  { label: 'Keras', value: 'KERAS' },
  { label: 'Caffe', value: 'CAFFE' },
  { label: 'Blade', value: 'BLADE' },
  { label: 'Mxnet', value: 'MXNET' }
]

// 模型格式选项
const formatOptions = [
  { label: 'SaveModel', value: 'SAVEMODEL' },
  { label: 'FrozenPb', value: 'FROZENPB' },
  { label: 'KerasH5', value: 'KERASH5' },
  { label: 'CaffePrototxt', value: 'CAFFEPROTOTXT' },
  { label: 'ONNX', value: 'ONNX' },
  { label: 'BladeModel', value: 'BLADEMODEL' },
  { label: 'PMML', value: 'PMML' },
  { label: 'PTH', value: 'PTH' },
  { label: 'pb', value: 'PB' },
  { label: 'ckpt', value: 'CKPT' },
  { label: 'pkt', value: 'PKT' },
  { label: 'pt', value: 'PT' },
  { label: 'HDF5', value: 'HDF5' },
  { label: 'caffemodel', value: 'CAFFEMODEL' },
  { label: 'params', value: 'PARAMS' },
  { label: 'json', value: 'JSON' },
  { label: 'Directory', value: 'DIRECTORY' }
]

// 模型类别选项
const categoryOptions = [
  { label: '图像分类', value: 'IMAGE_CLASSIFICATION' },
  { label: '目标检测', value: 'OBJECT_DETECTION' },
  { label: '语义分割', value: 'SEMANTIC_SEGMENTATION' },
  { label: '目标跟踪', value: 'OBJECT_TRACKING' },
  { label: '文本分类', value: 'TEXT_CLASSIFICATION' },
  { label: '中文分词', value: 'CHINESE_WORD_SEGMENTATION' },
  { label: '命名实体识别', value: 'NAMED_ENTITY_RECOGNITION' },
  { label: '音频分类', value: 'AUDIO_CLASSIFICATION' },
  { label: '语音识别', value: 'SPEECH_RECOGNITION' },
  { label: '器官分割', value: 'ORGAN_SEGMENTATION' },
  { label: '肺结节检测', value: 'LUNG_NODULE_DETECTION' },
  { label: '其他病灶识别', value: 'OTHER_LESION_DETECTION' },
  { label: '模型优化', value: 'MODEL_OPTIMIZATION' }
]

const loading = ref(false)
const total = ref(0)
const open = ref(false)
const title = ref('')
const algorithmList = ref([])
const uploadFileUrl = import.meta.env.VITE_APP_BASE_API + '/ai/algorithm/upload'
const headers = { Authorization: 'Bearer ' + getAccessToken() }

// 查询参数扩展
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  framework: '',
  category: ''
})

// 重置查询参数
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 搜索按钮操作
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const form = reactive({
  algorithmId: undefined,
  name: '',
  framework: '',
  format: '',
  category: '',
  description: '',
  modelFile: ''
})

const rules = {
  name: [
    { required: true, message: '模型名称不能为空', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5a-zA-Z0-9_-]+$/, message: '模型名称仅支持字母、数字、汉字、英文横杠和下划线', trigger: 'blur' }
  ],
  framework: [
    { required: true, message: '请选择模型框架', trigger: 'change' }
  ],
  format: [
    { required: true, message: '请选择模型格式', trigger: 'change' }
  ],
  category: [
    { required: true, message: '请选择或输入模型类别', trigger: 'change' }
  ],
  modelFile: [
    { required: true, message: '请上传模型文件', trigger: 'blur' }
  ]
}

// 获取算法列表
const getList = () => {
  loading.value = true
  listAlgorithm(queryParams).then(response => {
    algorithmList.value = response.list
    total.value = response.total
    loading.value = false
  })
}

// 表单重置
const reset = () => {
  form.algorithmId = undefined
  form.name = ''
  form.framework = ''
  form.format = ''
  form.category = ''
  form.description = ''
  form.modelFile = ''
}

// 文件上传前的处理
const handleBeforeUpload = (file) => {
  const isLt500M = file.size / 1024 / 1024 < 500
  if (!isLt500M) {
    ElMessage.error('上传文件大小不能超过 500MB!')
    return false
  }
  return true
}

// 文件上传成功的处理
const handleUploadSuccess = (response) => {
  form.modelFile = response.data
  ElMessage.success('上传成功')
}

// 文件上传失败的处理
const handleUploadError = () => {
  ElMessage.error('上传失败')
}

// 取消按钮
const cancel = () => {
  open.value = false
  reset()
}

// 提交按钮
const submitForm = async () => {
  proxy.$refs["algorithmFormRef"].validate((valid) => {
    if (valid) {
      if (form.algorithmId != undefined) {
        updateAlgorithm(form).then(response => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addAlgorithm(form).then(response => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

// 新增按钮操作
const handleAdd = () => {
  reset()
  open.value = true
  title.value = "添加模型"
}

// 修改按钮操作
const handleUpdate = (row) => {
  reset()
  const algorithmId = row.algorithmId || row.id
  getAlgorithm(algorithmId).then(response => {
    Object.assign(form, response.data)
    open.value = true
    title.value = "修改模型"
  })
}

// 删除按钮操作
const handleDelete = async (row) => {
  const algorithmId = row.algorithmId || row.id
  delAlgorithm(algorithmId).then(() => {
    getList()
    ElMessage.success('删除成功')
  })
}

// 下载模型文件
const downloadModel = (fileName) => {
  downloadModelFile(fileName)
}

getList()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-select {
  width: 100%;
}
</style>
