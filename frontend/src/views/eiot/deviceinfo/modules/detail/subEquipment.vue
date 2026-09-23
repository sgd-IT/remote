<template>
  <div>
    <div style="margin-bottom: 10px;">
      <el-button type="primary" @click="openAddDialog">添加子设备</el-button>
      <el-button type="danger" @click="unbindSelectedDevices" :disabled="selectedSubDevices.length === 0">解绑子设备</el-button>
    </div>
    <el-table 
      :data="list" 
      border 
      style="width: 95%" 
      @selection-change="handleSubDeviceSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="序号" type="index" align="center" width="80" />
      <el-table-column label="别名" property="name" align="center" width="180" />
      <el-table-column label="产品" property="productName" align="center" width="180" />
      <el-table-column label="设备标识" property="dn" align="center" />
      <el-table-column label="状态" align="center">
        <template v-slot="scope">
          <el-tag v-if="scope.row.state === 1" type="success" size="small">在线</el-tag>
          <el-tag v-else type="danger" size="small">离线</el-tag>
        </template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter"  label="创建时间" prop="createTime" align="center"/>
      <el-table-column label="操作" align="center">
        <template v-slot="scope">
          <el-popconfirm title="确认要解除与网关的关联吗？" @confirm="handleDelete(scope.row)">
            <template #reference>
              <el-button link type="danger" icon="Delete">解绑</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div class="mt-[20px] w-400px ml-auto">
      <el-pagination
        v-model:current-page="page.pageNo"
        v-model:page-size="page.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, pager, jumper"
        :total="page.total"
        @size-change="getList"
        @current-change="getList"
      />
    </div>

    <!-- 添加子设备对话框 -->
    <el-dialog v-model="addDeviceDialogVisible" title="添加子设备" width="800px">
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="别名">
          <el-input v-model="searchForm.name" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="searchForm.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchDevices">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table 
        ref="deviceTableRef" 
        :data="deviceList" 
        border 
        style="width: 100%" 
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="别名" prop="name" />
        <el-table-column label="产品名称" prop="productName" />
        <el-table-column label="设备标识" prop="dn" />
      </el-table>
      
      <div class="mt-[20px]">
        <el-pagination
          v-model:current-page="devicePage.pageNo"
          v-model:page-size="devicePage.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, pager, jumper"
          :total="devicePage.total"
          @size-change="getUnbindList"
          @current-change="getUnbindList"
        />
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDeviceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="bindDevices">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getChildrenDeviceList, getUnbindDeviceList, bindDevice, unbindDevice } from "@/api/eiot/deviceinfo/devices.api";
import {dateFormatter} from "@/utils/formatTime";
import {ElMessageBox, ElPopconfirm} from "element-plus";
import { ElMessage } from 'element-plus'

const props = defineProps({
  deviceInfo: {
    type: Object,
    default: () => {}
  }
})

const page = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const devicePage = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const list = ref([])
const deviceList = ref([])
const addDeviceDialogVisible = ref(false)
const deviceTableRef = ref()
const selectedDevices = ref([])
const selectedSubDevices = ref([])

const searchForm = reactive({
  name: '',
  dn: '',
  productName: '',
})

const getList = () => {
  getChildrenDeviceList({
    pageNo: page.pageNo,
    pageSize: page.pageSize,
    parentId: props.deviceInfo.deviceId,
  }).then((res) => {
    list.value = res.list
    page.total = res.total
  })
}

const getUnbindList = () => {
  getUnbindDeviceList({
    pageNo: devicePage.pageNo,
    pageSize: devicePage.pageSize,
    name: searchForm.name,
    dn: searchForm.dn,
    productName: searchForm.productName,
  }).then((res) => {
    deviceList.value = res.list
    devicePage.total = res.total
  })
}

const handleDelete = async (row: any) => {
  await unbindDevice({
    idList: [row.id],
  })
  ElMessage.success('解绑成功!')
  getList()
}

const openAddDialog = () => {
  addDeviceDialogVisible.value = true
  // 重置搜索条件
  searchForm.productName = ''
  // 获取设备列表
  getUnbindList()
}

const searchDevices = () => {
  devicePage.pageNo = 1
  getUnbindList()
}

const resetSearch = () => {
  searchForm.productName = ''
  devicePage.pageNo = 1
  getUnbindList()
}

const handleSelectionChange = (val) => {
  selectedDevices.value = val
}

const handleSubDeviceSelectionChange = (val) => {
  selectedSubDevices.value = val
}

const bindDevices = async () => {
  if (selectedDevices.value.length === 0) {
    ElMessage.warning('请至少选择一个设备')
    return
  }
  
  try {
    await bindDevice({
      idList: selectedDevices.value.map(item => item.id),
      parentId: props.deviceInfo.deviceId,
    })
    ElMessage.success('设备绑定成功!')
    addDeviceDialogVisible.value = false
    getList() // 重新加载子设备列表
  } catch (error) {
    ElMessage.error('设备绑定失败')
  }
}

const unbindSelectedDevices = () => {
  if (selectedSubDevices.value.length === 0) {
    ElMessage.warning('请至少选择一个子设备')
    return
  }

  ElMessageBox.confirm(
    `确定要解绑选中的 ${selectedSubDevices.value.length} 个子设备吗？`,
    '确认解绑',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      // 批量解绑设备
      await unbindDevice (
        {
          idList: selectedSubDevices.value.map(device => device.id),
        }
      )
      ElMessage.success('解绑成功!')
      getList()
      selectedSubDevices.value = []
    } catch (error) {
      ElMessage.error('解绑失败')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

onMounted(() => {
  console.log(props.deviceInfo)
  getList()
})
</script>

<style scoped></style>
