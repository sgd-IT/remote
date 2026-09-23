<template>
    <el-dialog
        :title="$t('选择产品')"
        v-model="dialogVisible"
        width="600px"
        append-to-body
    >
        <div style="margin-top:-55px;">
            <el-divider style="margin-top:-30px;"/>
            <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
                <el-form-item :label="$t('产品名称')" prop="name">
                    <el-input
                        v-model="queryParams.name"
                        :placeholder="$t('产品名称')"
                        clearable
                        size="small"
                        @keyup.enter="handleQuery"
                    />
                </el-form-item>
              <el-form-item :label="$t('productKey')" prop="productKey">
                <el-input
                  v-model="queryParams.productKey"
                  :placeholder="$t('productKey')"
                  clearable
                  size="small"
                  @keyup.enter="handleQuery"
                />
              </el-form-item>
                <el-form-item>
                    <el-button
                        type="primary"
                        icon="Search"
                        size="small"
                        @click="handleQuery"
                    >{{ $t('search') }}</el-button>
                    <el-button
                        icon="Refresh"
                        size="small"
                        @click="resetQuery"
                    >{{ $t('reset') }}</el-button>
                </el-form-item>
            </el-form>

            <el-table
                v-loading="loading"
                ref="singleTableRef"
                :data="productList"
                @row-click="rowClick"
                highlight-current-row
                size="small"
            >
                <el-table-column :label="$t('选择')" width="50" align="center">
                    <template #default="scope">
                        <input type="radio" :checked="scope.row.isSelect" name="product" />
                    </template>
                </el-table-column>
                <el-table-column :label="$t('产品Key')" align="center" prop="productKey" />
                <el-table-column :label="$t('产品名称')" align="center" prop="name" />
                <el-table-column :label="$t('分类名称')" align="center" prop="categoryName" />

              <el-table-column
                label="创建时间"
                align="center"
                prop="createTime"
                :formatter="dateFormatter"
                width="180px"
              />
            </el-table>

            <pagination
                v-show="total > 0"
                :total="total"
                v-model:page="queryParams.pageNo"
                v-model:limit="queryParams.pageSize"
                @pagination="getList"
            />
        </div>
        <template #footer>
            <div class="dialog-footer">
                <el-button @click="confirmSelectProduct" type="primary">{{ $t('confirm') }}</el-button>
                <el-button @click="closeDialog" type="info">{{ $t('close') }}</el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup>
import { ref, reactive, defineEmits } from 'vue'
import { ProductApi } from "@/api/eiot/product"
import { ElMessage } from 'element-plus'
import {dateFormatter} from "@/utils/formatTime";

// 定义组件属性
const props = defineProps({
    productKey: {
        type: String,
        default: ""
    }
})

// 定义事件
const emit = defineEmits(['productEvent'])

// refs
const queryFormRef = ref(null)
const singleTableRef = ref(null)

// 响应式状态
const loading = ref(true)
const total = ref(0)
const dialogVisible = ref(false)
const productList = ref([])
const product = ref({})

// 查询参数
const queryParams = reactive({
    pageNo: 1,
    pageSize: 10,
    name: null,
    categoryId: null,
    categoryName: null,
    tenantId: null,
    tenantName: null,

})

// 字典数据
const dict = reactive({
    type: {
        iot_network_method: []
    }
})

const open = async ()=>{
  dialogVisible.value = true
  await getList()
}

// 方法
const getList = async () => {
    try {
        loading.value = true
        const response = await ProductApi.getProductPage(queryParams)
        //产品列表初始化isSelect值，用于单选
        productList.value = response.list.map(row => ({
            ...row,
            isSelect: false
        }))
        total.value = response.total
        if (props.productKey !== 0) {
            setRadioSelected(props.productKey)
        }
    } catch (error) {
        console.error('获取产品列表失败:', error)
        ElMessage.error('获取产品列表失败')
    } finally {
        loading.value = false
    }
}

const handleQuery = () => {
    queryParams.pageNo = 1
    getList()
}

const resetQuery = () => {
    queryFormRef.value?.resetFields()
    handleQuery()
}

const rowClick = (row) => {
    if (row) {
        setRadioSelected(row.productKey)
        product.value = row
    }
}

const setRadioSelected = (productKey) => {
    productList.value = productList.value.map(item => ({
        ...item,
        isSelect: item.productKey === productKey
    }))
}

const confirmSelectProduct = () => {
    emit('productEvent', product.value)
    dialogVisible.value = false
}

const closeDialog = () => {
    dialogVisible.value = false
}

// 暴露方法给父组件
defineExpose({
    open
})
</script>

<style lang="scss" scoped>
.dialog-footer {
    padding: 20px 0 0;
    text-align: right;
}
</style>
