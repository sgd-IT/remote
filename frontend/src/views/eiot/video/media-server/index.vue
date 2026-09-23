<template>
  <ContentWrap>
    <el-card class="media-server-card">
      <template #header>
        <div class="card-header">
          <span>ICC 平台配置</span>
          <el-tag v-if="hasConfig" type="success">已配置</el-tag>
          <el-tag v-else type="info">未配置</el-tag>
        </div>
      </template>

      <el-form
        :model="form"
        label-width="140px"
        :rules="rules"
        ref="formRef"
        v-loading="loading"
      >
        <el-form-item label="配置名称" prop="serverName">
          <el-input v-model="form.serverName" placeholder="如：大华ICC主平台" />
        </el-form-item>
        <el-form-item label="平台地址" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="如：https://icc.example.com" />
        </el-form-item>
        <el-form-item label="Client ID" prop="clientId">
          <el-input v-model="form.clientId" placeholder="合作方凭证ID" />
        </el-form-item>
        <el-form-item label="Client Secret" prop="clientSecret">
          <el-input v-model="form.clientSecret" placeholder="合作方凭证密钥" show-password />
        </el-form-item>
        <el-form-item label="登录用户名" prop="username">
          <el-input v-model="form.username" placeholder="ICC 登录用户名" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" placeholder="ICC 登录密码" show-password />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <el-button type="primary" @click="handleSave">保存配置</el-button>
        <el-button v-if="hasConfig && form.id" type="danger" @click="handleDelete">删除配置</el-button>
      </div>
    </el-card>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ContentWrap } from '@/components/ContentWrap'
import {
  getMediaServerConfig,
  saveMediaServerConfig,
  deleteMediaServerConfig
} from '@/api/eiot/video/mediaServer.api'

const loading = ref(false)
const hasConfig = ref(false)
const formRef = ref<any>(null)
const form = ref<any>({
  id: null,
  serverName: '',
  baseUrl: '',
  clientId: '',
  clientSecret: '',
  username: '',
  password: '',
  enabled: true
})

const rules = {
  serverName: [{ required: true, message: '配置名称不能为空', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '平台地址不能为空', trigger: 'blur' }],
  clientId: [{ required: true, message: 'Client ID 不能为空', trigger: 'blur' }],
  clientSecret: [{ required: true, message: 'Client Secret 不能为空', trigger: 'blur' }],
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMediaServerConfig()
    if (res) {
      form.value = { ...form.value, ...res }
      hasConfig.value = true
    }
  } catch (e: any) {
    if (e?.response?.status !== 404) {
      ElMessage.error(e.message || '获取配置失败')
    }
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const res = await saveMediaServerConfig(form.value)
    if (res) {
      form.value.id = res.id
      hasConfig.value = true
    }
    ElMessage.success('保存成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确认删除该 ICC 平台配置？', '提示', { type: 'warning' })
    if (!form.value.id) return
    await deleteMediaServerConfig(form.value.id)
    ElMessage.success('删除成功')
    hasConfig.value = false
    form.value = {
      id: null,
      serverName: '',
      baseUrl: '',
      clientId: '',
      clientSecret: '',
      username: '',
      password: '',
      enabled: true
    }
  } catch (e) {}
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.media-server-card {
  max-width: 720px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
}
.form-actions {
  margin-top: 20px;
  padding-left: 140px;
}
</style>
