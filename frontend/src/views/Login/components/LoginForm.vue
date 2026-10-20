<template>
  <el-form
    v-show="getShow"
    ref="formLogin"
    :model="loginData.loginForm"
    :rules="LoginRules"
    class="login-form-dark"
    label-position="top"
    size="large"
  >
    <div class="form-header">
      <div class="user-avatar">
        <Icon icon="ep:user" :size="22" color="#4fc3f7" class="avatar-icon" />
      </div>
      <h3 class="form-title">欢迎登录</h3>
      <p class="form-subtitle">智慧消防远程值守系统</p>
    </div>

    <SafeInput
      v-model="loginData.loginForm.username"
      label="账号"
      placeholder="请输入用户名"
      prefix-icon="ep:avatar"
      :has-error="usernameError"
      error-message="请输入账号"
      @enter="getCode()"
    />

    <SafeInput
      v-model="loginData.loginForm.password"
      label="密码"
      placeholder="请输入密码"
      type="password"
      prefix-icon="ep:lock"
      show-password-toggle
      :has-error="passwordError"
      error-message="请输入密码"
      @enter="getCode()"
    />

    <div class="form-options">
      <el-checkbox v-model="loginData.loginForm.rememberMe" class="remember-check">
        <span class="remember-text">记住密码</span>
      </el-checkbox>
      <span class="forgot-link" @click="handleForgot">忘记密码?</span>
    </div>

    <el-button
      :loading="loginLoading"
      class="login-btn"
      type="primary"
      @click="getCode()"
    >
      {{ loginLoading ? '登录中...' : '登 录' }}
    </el-button>

    <div class="form-footer">
      © 2025 智慧消防远程值守系统 · 安全可控 · 全面守护
    </div>

    <Verify
      v-if="loginData.captchaEnable === 'true'"
      ref="verify"
      :captchaType="captchaType"
      :imgSize="{ width: '310px', height: '155px' }"
      mode="pop"
      @success="handleLogin"
    />
  </el-form>
</template>

<script lang="ts" setup>
import { ElLoading } from 'element-plus'
import { Icon } from '@/components/Icon'
import type { RouteLocationNormalizedLoaded, RouteRecordRaw } from 'vue-router'

import * as authUtil from '@/utils/auth'
import { usePermissionStore } from '@/store/modules/permission'
import { useUserStoreWithOut } from '@/store/modules/user'
import * as LoginApi from '@/api/login'
import { LoginStateEnum, useFormValid, useLoginState } from './useLogin'
import SafeInput from './SafeInput.vue'

defineOptions({ name: 'LoginForm' })

const formLogin = ref()
const { validForm } = useFormValid(formLogin)
const { setLoginState, getLoginState } = useLoginState()
const { currentRoute, push } = useRouter()
const permissionStore = usePermissionStore()
const router = useRouter()
const redirect = ref<string>('')
const loginLoading = ref(false)
const verify = ref()
const captchaType = ref('blockPuzzle')
const usernameError = ref(false)
const passwordError = ref(false)

const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN)

const LoginRules = {
  username: [required],
  password: [required]
}
const loginData = reactive({
  isShowPassword: false,
  captchaEnable: import.meta.env.VITE_APP_CAPTCHA_ENABLE,
  loginForm: {
    username: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
    password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
    captchaVerification: '',
    loginNonce: '',
    rememberMe: true
  }
})

const handleForgot = () => {
  setLoginState(LoginStateEnum.RESET_PASSWORD)
}

const getCode = async () => {
  usernameError.value = !loginData.loginForm.username
  passwordError.value = !loginData.loginForm.password
  if (usernameError.value || passwordError.value) {
    return
  }
  if (loginData.captchaEnable === 'false') {
    await handleLogin({})
  } else {
    verify.value.show()
  }
}

const getLoginFormCache = () => {
  const loginForm = authUtil.getLoginForm()
  if (loginForm) {
    loginData.loginForm = {
      ...loginData.loginForm,
      username: loginForm.username ? loginForm.username : loginData.loginForm.username,
      password: loginForm.password ? loginForm.password : loginData.loginForm.password,
      rememberMe: loginForm.rememberMe
    }
  }
}
const loading = ref()

const handleLogin = async (params: any) => {
  loginLoading.value = true
  try {
    const data = await validForm()
    if (!data) {
      return
    }
    authUtil.setTenantId('1')
    // 携带一次性 nonce 获取租户ID（防重放攻击，用户不存在时返回 0）
    const tenantNonce = await LoginApi.getLoginNonce()
    const tenantId = await LoginApi.getTenantIdByUsername(loginData.loginForm.username, tenantNonce)
    if (tenantId) {
      authUtil.setTenantId(tenantId)
    }
    // 获取登录防重放 nonce
    const nonce = await LoginApi.getLoginNonce()
    const loginDataLoginForm = { ...loginData.loginForm }
    loginDataLoginForm.captchaVerification = params.captchaVerification
    loginDataLoginForm.loginNonce = nonce
    const res = await LoginApi.login(loginDataLoginForm)
    if (!res) {
      return
    }
    loading.value = ElLoading.service({
      lock: true,
      text: '正在加载系统中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    if (loginDataLoginForm.rememberMe) {
      authUtil.setLoginForm(loginDataLoginForm)
    } else {
      authUtil.removeLoginForm()
    }
    authUtil.setToken(res)
    const userStore = useUserStoreWithOut()
    await userStore.setUserInfoAction()
    await permissionStore.generateRoutes()
    permissionStore.getAddRouters.forEach((route) => {
      router.addRoute(route as unknown as RouteRecordRaw)
    })
    // 消控室值班员整天盯的就是值守大屏，登录后不要再绕一层监控中心
    let defaultPath = '/monitoring-center'
    if (
      userStore.getRoles?.some(
        (r) => r === 'super_admin' || r === 'monitor_role' || r === 'fire_watcher'
      )
    ) {
      defaultPath = '/duty-screen'
    }
    if (!redirect.value || redirect.value === '/' || redirect.value === '/index') {
      redirect.value = defaultPath
    }
    if (redirect.value.indexOf('sso') !== -1) {
      window.location.href = window.location.href.replace('/login?redirect=', '')
    } else {
      await push({ path: redirect.value || permissionStore.addRouters[0].path })
    }
  } finally {
    loginLoading.value = false
    loading.value?.close()
  }
}

watch(
  () => currentRoute.value,
  (route: RouteLocationNormalizedLoaded) => {
    redirect.value = route?.query?.redirect as string
  },
  {
    immediate: true
  }
)
onMounted(() => {
  authUtil.removeToken()
  getLoginFormCache()
})
</script>

<style lang="scss" scoped>
.login-form-dark {
  width: 100%;
}
.form-header {
  text-align: center;
  margin-bottom: 26px;
}
.user-avatar {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, rgba(79, 195, 247, 0.15), rgba(25, 118, 210, 0.1));
  border: 1px solid rgba(79, 195, 247, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-icon {
  font-size: 28px;
  color: #4fc3f7;
}
.form-title {
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 6px;
}
.form-subtitle {
  color: rgba(255, 255, 255, 0.3);
  font-size: 14px;
  margin: 0;
}
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.remember-check {
  :deep(.el-checkbox__input) {
    .el-checkbox__inner {
      background: rgba(79, 195, 247, 0.15);
      border-color: rgba(79, 195, 247, 0.4);
      width: 14px;
      height: 14px;
      &::after {
        border-color: #4fc3f7;
        border-width: 2px;
      }
    }
    &.is-checked .el-checkbox__inner {
      background: rgba(79, 195, 247, 0.15);
      border-color: #4fc3f7;
    }
  }
  :deep(.el-checkbox__label) {
    padding-left: 6px;
  }
}
.remember-text {
  color: rgba(255, 255, 255, 0.45);
  font-size: 13px;
}
.forgot-link {
  color: #4fc3f7;
  font-size: 13px;
  cursor: pointer;
  &:hover {
    text-decoration: underline;
  }
}
.login-btn {
  width: 100%;
  height: 42px;
  background: linear-gradient(135deg, #1565c0, #4fc3f7);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  box-shadow: 0 4px 18px rgba(21, 101, 192, 0.35);
  transition: filter 0.3s, box-shadow 0.3s;
  &:hover {
    filter: brightness(1.05);
    box-shadow: 0 6px 24px rgba(21, 101, 192, 0.45);
  }
  &:active {
    transform: scale(0.98);
  }
  :deep(.el-button__content) {
    color: #fff;
  }
}
.form-footer {
  text-align: center;
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.15);
  font-size: 12px;
}
</style>
