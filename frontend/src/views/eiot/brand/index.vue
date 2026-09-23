<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="品牌代码" prop="brandCode">
        <el-input
          v-model="queryParams.brandCode"
          placeholder="请输入品牌代码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="品牌名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入品牌名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="handleSync"
          :loading="syncLoading"
          v-hasPermi="['iot:brand:sync']"
        >
          <Icon icon="ep:refresh" class="mr-5px" /> 同步品牌数据
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="品牌id" align="center" prop="id" width="80" />
      <el-table-column label="品牌代码" align="center" prop="brandCode" />
      <el-table-column label="品牌名称" align="center" prop="name" />
      <el-table-column label="型号数量" align="center" width="100">
        <template #default="scope">
          <el-tag size="small" type="info">{{ getModelCount(scope.row.id) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180px"
      />
      <el-table-column
        label="更新时间"
        align="center"
        prop="updateTime"
        :formatter="dateFormatter"
        width="180px"
      />
      <el-table-column label="操作" align="center" min-width="150px" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleViewModels(scope.row)">
            <Icon icon="ep:view" class="mr-5px" /> 查看型号
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 型号列表弹窗 -->
  <el-dialog title="型号版本列表" v-model="modelDialogVisible" width="700px" destroy-on-close>
    <el-table :data="currentModels" border v-loading="modelLoading" size="small">
      <el-table-column prop="model" label="型号" width="180" />
      <el-table-column prop="versions" label="版本">
        <template #default="scope">
          <el-tag v-for="v in scope.row.versions" :key="v" size="small" class="mr-1 mb-1">{{ v }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleViewFunctions(scope.row)">
            查看功能
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <!-- 功能列表弹窗 -->
  <el-dialog title="功能列表" v-model="functionDialogVisible" width="600px" destroy-on-close>
    <el-table :data="currentFunctions" border v-loading="functionLoading" size="small">
      <el-table-column prop="functionId" label="功能编码" width="150" />
      <el-table-column prop="name" label="功能名称" />
      <el-table-column prop="ruleNum" label="规则号" width="80" />
      <el-table-column prop="awaitTime" label="等待时间" width="100">
        <template #default="scope">
          {{ scope.row.awaitTime ? scope.row.awaitTime + 'ms' : '-' }}
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { getBrandPage, syncBrandData, getBrandCascade, getBrandModelFunctions } from '@/api/eiot/brand'
import { ref, reactive, onMounted } from 'vue'

/** 品牌管理 列表 */
defineOptions({ name: 'Brand' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  brandCode: undefined,
  name: undefined
})
const queryFormRef = ref()
const syncLoading = ref(false)

// 级联数据缓存
const cascadeData = ref<any[]>([])

// 型号弹窗
const modelDialogVisible = ref(false)
const modelLoading = ref(false)
const currentBrand = ref<any>(null)
const currentModels = ref<any[]>([])

// 功能弹窗
const functionDialogVisible = ref(false)
const functionLoading = ref(false)
const currentFunctions = ref<any[]>([])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await getBrandPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

/** 加载级联数据 */
const loadCascadeData = async () => {
  if (cascadeData.value.length > 0) return
  try {
    const data = await getBrandCascade()
    cascadeData.value = data || []
  } catch (e) {
    console.error('加载级联数据失败', e)
  }
}

/** 获取型号数量 */
const getModelCount = (brandId: number) => {
  const brand = cascadeData.value.find((b: any) => b.id === brandId)
  return brand?.models?.length || 0
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 同步 */
const handleSync = async () => {
  try {
    await message.confirm('确认同步品牌数据？这会从四信API拉取最新数据', '提示')
    syncLoading.value = true
    await syncBrandData()
    message.success('同步成功')
    // 刷新级联缓存
    cascadeData.value = []
    await loadCascadeData()
    await getList()
  } catch (e) {
    // 取消或失败
  } finally {
    syncLoading.value = false
  }
}

/** 查看型号 */
const handleViewModels = async (row: any) => {
  currentBrand.value = row
  modelDialogVisible.value = true
  modelLoading.value = true
  try {
    await loadCascadeData()
    const brand = cascadeData.value.find((b: any) => b.id === row.id)
    currentModels.value = brand?.models || []
  } finally {
    modelLoading.value = false
  }
}

/** 查看功能 */
const handleViewFunctions = async (model: any) => {
  functionDialogVisible.value = true
  functionLoading.value = true
  try {
    const data = await getBrandModelFunctions(model.id)
    currentFunctions.value = data || []
  } catch (e) {
    message.error('加载功能列表失败')
  } finally {
    functionLoading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  getList()
  loadCascadeData()
})
</script>
