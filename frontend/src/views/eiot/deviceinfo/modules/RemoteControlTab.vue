<template>
  <div class="remote-control-tab">
    <!-- 未配置提示 -->
    <el-empty v-if="!remoteConfig" description="该设备尚未配置远程控制">
      <template #default>
        <div class="text-gray-400 mb-4">请先设置设备对应的品牌型号版本</div>
        <el-button type="primary" @click="openConfigDialog" v-if="hasPermission('iot:device:remote:config')">
          <Icon icon="ep:setting" class="mr-1" />
          远程控制设置
        </el-button>
      </template>
    </el-empty>

    <!-- 已配置展示 -->
    <template v-else>
      <el-alert type="success" :closable="false" class="mb-4">
        <template #default>
          <div class="flex items-center justify-between">
            <div>
              <span class="font-bold">已配置：</span>
              <span>{{ remoteConfig.brandName }} / {{ remoteConfig.model }} / {{ remoteConfig.version }}</span>
              <el-tag v-if="remoteConfig.hasPassword" size="small" type="warning" class="ml-2">已设置二次确认密码</el-tag>
              <el-tag v-else size="small" type="info" class="ml-2">未设置二次确认密码</el-tag>
            </div>
            <el-button size="small" @click="openConfigDialog" v-if="hasPermission('iot:device:remote:config')">
              <Icon icon="ep:setting" class="mr-1" />
              重新设置
            </el-button>
          </div>
        </template>
      </el-alert>

      <!-- 功能列表 -->
      <div class="flex justify-between items-center mb-4">
        <h3 class="font-bold text-base">功能列表</h3>
        <el-button type="primary" @click="openAddFunctionDialog" v-if="hasPermission('iot:device:remote:config')">
          <Icon icon="ep:plus" class="mr-1" />
          新增功能
        </el-button>
      </div>

      <el-table :data="functionList" border v-loading="loading" style="width: 100%">
        <el-table-column prop="functionName" label="功能名称" width="180">
          <template #default="scope">
            <span class="font-bold">{{ scope.row.functionName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="buttonName" label="按钮名称" width="150">
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.buttonName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paramValues" label="参数值" width="250">
          <template #default="scope">
            <div v-if="formatParamValues(scope.row.paramValues)">
              <el-tag
                v-for="(val, key) in formatParamValues(scope.row.paramValues)"
                :key="key"
                size="small"
                class="mr-1 mb-1"
              >
                {{ key }}: {{ val }}
              </el-tag>
            </div>
            <span v-else class="text-gray-400">无参数</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <!-- 多线/总线开关：显示停止/启动两个按钮 -->
            <template v-if="isStopStart(scope.row.functionName)">
              <el-button
                type="danger"
                size="small"
                @click="handleSendInstruction(scope.row, '0')"
                :loading="sendingId === scope.row.id + '_0'"
                v-if="hasPermission('iot:device:instruction:send')"
              >
                <Icon icon="ep:video-pause" class="mr-1" />
                停止
              </el-button>
              <el-button
                type="success"
                size="small"
                @click="handleSendInstruction(scope.row, '1')"
                :loading="sendingId === scope.row.id + '_1'"
                v-if="hasPermission('iot:device:instruction:send')"
              >
                <Icon icon="ep:video-play" class="mr-1" />
                启动
              </el-button>
            </template>
            <!-- 开关/总线开关：显示关/开两个按钮 -->
            <template v-else-if="isSwitch(scope.row.functionName)">
              <el-button
                type="info"
                size="small"
                @click="handleSendInstruction(scope.row, '0')"
                :loading="sendingId === scope.row.id + '_0'"
                v-if="hasPermission('iot:device:instruction:send')"
              >
                <Icon icon="ep:switch-button" class="mr-1" />
                关
              </el-button>
              <el-button
                type="primary"
                size="small"
                @click="handleSendInstruction(scope.row, '1')"
                :loading="sendingId === scope.row.id + '_1'"
                v-if="hasPermission('iot:device:instruction:send')"
              >
                <Icon icon="ep:switch-button" class="mr-1" />
                开
              </el-button>
            </template>
            <!-- 复位/消音：单按钮，按钮名称使用功能名称 -->
            <template v-else>
              <el-button
                type="primary"
                size="small"
                @click="handleSendInstruction(scope.row, '1')"
                :loading="sendingId === scope.row.id + '_1'"
                v-if="hasPermission('iot:device:instruction:send')"
              >
                <Icon icon="ep:promotion" class="mr-1" />
                {{ scope.row.buttonName || scope.row.functionName }}
              </el-button>
            </template>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
              v-if="hasPermission('iot:device:remote:config')"
            >
              <Icon icon="ep:delete" />
            </el-button>
            <el-button
              type="warning"
              size="small"
              @click="handleEdit(scope.row)"
              v-if="hasPermission('iot:device:remote:config')"
            >
              <Icon icon="ep:edit" />
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && functionList.length === 0" description="暂无功能，请点击「新增功能」添加" :image-size="60" />
    </template>

    <!-- 配置弹窗 -->
    <RemoteConfigDialog
      ref="remoteConfigDialogRef"
      :deviceId="deviceId"
      :remoteConfig="remoteConfig"
      @success="handleConfigSuccess"
    />

    <!-- 新增功能弹窗 -->
    <AddFunctionDialog
      ref="addFunctionDialogRef"
      :deviceId="deviceId"
      :brandModelId="remoteConfig?.brandModelId || 0"
      @success="handleFunctionSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import RemoteConfigDialog from './RemoteConfigDialog.vue'
import AddFunctionDialog from './AddFunctionDialog.vue'
import {
  getDeviceRemoteConfig,
  getDeviceFunctions,
  deleteDeviceFunction,
  sendInstruction,
  type DeviceRemoteConfigVO,
  type DeviceFunctionVO
} from '@/api/eiot/remote/deviceRemote.api'

const props = defineProps({
  deviceId: {
    type: [Number, String],
    required: true
  }
})

const message = useMessage()
const loading = ref(false)
const sendingId = ref<string | null>(null)
const remoteConfig = ref<DeviceRemoteConfigVO | null>(null)
const functionList = ref<DeviceFunctionVO[]>([])
const remoteConfigDialogRef = ref()
const addFunctionDialogRef = ref()

const hasPermission = (_permission: string) => {
  // 简单实现：返回true，实际项目中应该从权限 store 获取
  return true
}

const formatParamValues = (val: any): Record<string, any> | null => {
  if (!val) return null
  if (typeof val === 'string') {
    try {
      const obj = JSON.parse(val)
      return Object.keys(obj).length > 0 ? obj : null
    } catch {
      return null
    }
  }
  if (typeof val === 'object') {
    return Object.keys(val).length > 0 ? val : null
  }
  return null
}

const loadRemoteConfig = async () => {
  try {
    const res = await getDeviceRemoteConfig(props.deviceId)
    if (res) {
      remoteConfig.value = res
      await loadFunctions()
    } else {
      remoteConfig.value = null
      functionList.value = []
    }
  } catch (e) {
    remoteConfig.value = null
    functionList.value = []
  }
}

const loadFunctions = async () => {
  loading.value = true
  try {
    const res = await getDeviceFunctions(props.deviceId)
    functionList.value = res || []
  } catch (e) {
    message.error('加载功能列表失败')
  } finally {
    loading.value = false
  }
}

const openConfigDialog = () => {
  remoteConfigDialogRef.value?.open()
}

const openAddFunctionDialog = () => {
  if (!remoteConfig.value?.brandModelId) {
    message.warning('请先配置远程控制设置')
    return
  }
  addFunctionDialogRef.value?.open()
}

const handleEdit = (row: DeviceFunctionVO) => {
  if (!remoteConfig.value?.brandModelId) {
    message.warning('请先配置远程控制设置')
    return
  }
  addFunctionDialogRef.value?.open(row)
}

const handleConfigSuccess = () => {
  loadRemoteConfig()
}

const handleFunctionSuccess = () => {
  loadFunctions()
}

const isStopStart = (name: string): boolean => {
  return name.includes('多线') || name.includes('总线')
}

const isSwitch = (name: string): boolean => {
  return name.includes('开关') && !name.includes('多线') && !name.includes('总线')
}

const handleSendInstruction = async (row: DeviceFunctionVO, commandType: string) => {
  const sendingKey = row.id + '_' + commandType
  sendingId.value = sendingKey
  try {
    let password: string | undefined = undefined

    // 如果设备设置了二次确认密码，先弹窗输入
    if (remoteConfig.value?.hasPassword) {
      const result = await ElMessageBox.prompt('该操作需要二次确认密码', '安全验证', {
        confirmButtonText: '确认下发',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '请输入二次确认密码',
        inputValidator: (val) => {
          if (!val || val.trim() === '') return '密码不能为空'
          return true
        }
      })
      if (!result.value) {
        sendingId.value = null
        return
      }
      password = result.value
    }

    const res = await sendInstruction({
      deviceId: props.deviceId,
      deviceFunctionId: row.id,
      password,
      commandType
    })

    if (res?.success) {
      message.success('指令下发成功')
    } else {
      message.warning(res?.message || '指令下发失败')
    }
  } catch (e: any) {
    if (e === 'cancel') return
    message.error(e?.message || '指令下发失败')
  } finally {
    sendingId.value = null
  }
}

const handleDelete = async (row: DeviceFunctionVO) => {
  try {
    await ElMessageBox.confirm('确认删除该功能吗？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDeviceFunction(props.deviceId, row.id)
    message.success('删除成功')
    loadFunctions()
  } catch (e) {
    // 取消删除不处理
  }
}

onMounted(() => {
  loadRemoteConfig()
})

defineExpose({ loadRemoteConfig })
</script>

<style lang="scss" scoped>
.remote-control-tab {
  padding: 10px;
}
</style>
