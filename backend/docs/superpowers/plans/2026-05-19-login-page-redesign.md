# 智慧消防登录页重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于设计文档将智慧消防远程值守系统登录页完全重构为科技深蓝主题、左右分栏布局，包含消防设备节点网络、毛玻璃登录卡片、密码强度提示、响应式适配。

**Architecture:** 采用组件化拆分，左侧品牌区（BrandPanel + DeviceNetwork + DataStats）与右侧登录区（LoginForm + PasswordStrength + SafeInput）独立封装。动画全部使用 CSS keyframes 实现，避免 JS 计算。响应式通过 CSS media query + UnoCSS 工具类实现。

**Tech Stack:** Vue 3 (script setup) + TypeScript + Element Plus + UnoCSS + SCSS

---

## 文件结构

```
src/views/login/
├── Login.vue                           # 主容器 — 重写布局与响应式
├── components/
│   ├── LoginForm.vue                   # 登录表单 — 重写为深色主题
│   ├── LoginFormTitle.vue              # 表单标题 — 复用，适配深色
│   ├── BrandPanel.vue                  # [新增] 左侧品牌面板
│   ├── DeviceNetwork.vue               # [新增] 设备节点网络 SVG
│   ├── DataStats.vue                   # [新增] 底部数据卡片
│   ├── PasswordStrength.vue            # [新增] 密码强度指示器
│   ├── SafeInput.vue                   # [新增] 带聚焦动效的输入框
│   └── index.ts                        # [修改] 导出新增组件
```

---

## 前置准备

- [ ] **Step 0: 确认 dev 服务器可启动**

  Run: `cd anwei-fire-remote-monitor-vue ; pnpm dev`
  Expected: Vite dev server starts on localhost, existing login page accessible at `/login`

---

## Task 1: 创建密码强度指示器组件

**Files:**
- Create: `src/views/login/components/PasswordStrength.vue`

- [ ] **Step 1.1: 编写 PasswordStrength 组件**

  ```vue
  <template>
    <div class="password-strength">
      <div class="strength-bars">
        <div
          v-for="i in 4"
          :key="i"
          class="strength-bar"
          :class="{ active: i <= level, [`level-${level}`]: i <= level }"
        />
      </div>
      <div class="strength-text">
        <span :class="`text-level-${level}`">{{ strengthLabel }}</span>
        <span v-if="level <= 2" class="strength-tip">{{ tip }}</span>
      </div>
    </div>
  </template>

  <script lang="ts" setup>
  import { computed } from 'vue'

  const props = defineProps<{
    password: string
  }>()

  const level = computed(() => {
    const pwd = props.password
    if (!pwd) return 0
    let score = 0
    if (pwd.length >= 6) score++
    if (pwd.length >= 10) score++
    if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
    if (/\d/.test(pwd) && /[^a-zA-Z0-9]/.test(pwd)) score++
    return Math.min(score, 4)
  })

  const strengthLabel = computed(() => {
    const labels = ['', '密码强度：弱', '密码强度：弱', '密码强度：中', '密码强度：强']
    return labels[level.value]
  })

  const tip = '建议包含大小写字母和数字'
  </script>

  <style lang="scss" scoped>
  .password-strength {
    margin-bottom: 14px;
  }
  .strength-bars {
    display: flex;
    gap: 3px;
    margin-bottom: 4px;
  }
  .strength-bar {
    flex: 1;
    height: 3px;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 2px;
    transition: background 0.3s;
    &.active {
      &.level-1, &.level-2 { background: #ff5252; }
      &.level-3 { background: #ffb74d; }
      &.level-4 { background: #00e676; }
    }
  }
  .strength-text {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 9px;
  }
  .text-level-1, .text-level-2 { color: #ff5252; }
  .text-level-3 { color: #ffb74d; }
  .text-level-4 { color: #00e676; }
  .strength-tip {
    color: rgba(255, 255, 255, 0.2);
  }
  </style>
  ```

- [ ] **Step 1.2: 在 LoginForm 中引入并验证**

  暂不修改 LoginForm，先完成所有基础组件后再统一集成。本 Step 标记完成即可。

---

## Task 2: 创建安全输入框组件

**Files:**
- Create: `src/views/login/components/SafeInput.vue`

- [ ] **Step 2.1: 编写 SafeInput 组件**

  ```vue
  <template>
    <div class="safe-input-wrapper">
      <div class="input-label" v-if="label">{{ label }}</div>
      <div
        class="safe-input"
        :class="{ focused: isFocused, error: hasError }"
      >
        <span class="input-icon" v-if="prefixIcon">
          <Icon :icon="prefixIcon" />
        </span>
        <input
          :type="inputType"
          :value="modelValue"
          :placeholder="placeholder"
          @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
          @focus="isFocused = true"
          @blur="isFocused = false"
          @keyup.enter="$emit('enter')"
        />
        <span
          class="input-icon suffix"
          v-if="showPasswordToggle"
          @click="togglePassword"
        >
          <Icon :icon="showPassword ? 'ep:view' : 'ep:hide'" />
        </span>
      </div>
      <div class="input-error" v-if="hasError && errorMessage">{{ errorMessage }}</div>
      <div class="focus-line" :class="{ active: isFocused }"></div>
    </div>
  </template>

  <script lang="ts" setup>
  import { ref, computed } from 'vue'
  import { Icon } from '@iconify/vue'

  const props = defineProps<{
    modelValue: string
    label?: string
    placeholder?: string
    type?: string
    prefixIcon?: string
    showPasswordToggle?: boolean
    hasError?: boolean
    errorMessage?: string
  }>()

  defineEmits<{
    (e: 'update:modelValue', value: string): void
    (e: 'enter'): void
  }>()

  const isFocused = ref(false)
  const showPassword = ref(false)

  const inputType = computed(() => {
    if (props.type === 'password' && props.showPasswordToggle) {
      return showPassword.value ? 'text' : 'password'
    }
    return props.type || 'text'
  })

  const togglePassword = () => {
    showPassword.value = !showPassword.value
  }
  </script>

  <style lang="scss" scoped>
  .safe-input-wrapper {
    margin-bottom: 16px;
    position: relative;
  }
  .input-label {
    color: rgba(255, 255, 255, 0.45);
    font-size: 10px;
    margin-bottom: 5px;
  }
  .safe-input {
    width: 100%;
    height: 40px;
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 8px;
    display: flex;
    align-items: center;
    padding: 0 12px;
    gap: 8px;
    transition: border-color 0.3s;
    box-sizing: border-box;

    input {
      flex: 1;
      background: transparent;
      border: none;
      outline: none;
      color: #fff;
      font-size: 13px;
      &::placeholder {
        color: rgba(255, 255, 255, 0.18);
      }
    }

    &.focused {
      border-color: rgba(79, 195, 247, 0.3);
    }

    &.error {
      border-color: rgba(255, 82, 82, 0.4);
    }
  }
  .input-icon {
    color: rgba(255, 255, 255, 0.25);
    font-size: 14px;
    display: flex;
    align-items: center;
    &.suffix {
      cursor: pointer;
      &:hover {
        color: rgba(255, 255, 255, 0.45);
      }
    }
  }
  .input-error {
    color: #ff5252;
    font-size: 10px;
    margin-top: 4px;
  }
  .focus-line {
    position: absolute;
    bottom: 16px;
    left: 12px;
    right: 12px;
    height: 1px;
    background: #4fc3f7;
    transform: scaleX(0);
    transition: transform 0.3s ease-out;
    pointer-events: none;
    &.active {
      transform: scaleX(1);
    }
  }
  </style>
  ```

---

## Task 3: 创建设备节点网络组件

**Files:**
- Create: `src/views/login/components/DeviceNetwork.vue`

- [ ] **Step 3.1: 编写 DeviceNetwork 组件**

  ```vue
  <template>
    <div class="device-network">
      <svg class="network-svg" viewBox="0 0 300 220">
        <!-- 连接线 -->
        <line
          v-for="(line, idx) in connections"
          :key="`line-${idx}`"
          :x1="center.x" :y1="center.y"
          :x2="line.x" :y2="line.y"
          stroke="rgba(79,195,247,0.12)"
          stroke-width="1"
        />

        <!-- 中心节点 -->
        <g class="center-node">
          <circle :cx="center.x" :cy="center.y" r="14" fill="rgba(79,195,247,0.1)" stroke="#4fc3f7" stroke-width="1.5"/>
          <circle :cx="center.x" :cy="center.y" r="5" fill="#4fc3f7"/>
          <circle :cx="center.x" :cy="center.y" r="22" fill="none" stroke="rgba(79,195,247,0.2)" stroke-width="1" stroke-dasharray="4 4" class="rotating-ring"/>
        </g>

        <!-- 外围节点 -->
        <g
          v-for="(node, idx) in nodes"
          :key="`node-${idx}`"
          class="outer-node"
          :class="{ alarm: node.type === 'alarm' }"
        >
          <circle
            :cx="node.x" :cy="node.y" r="9"
            :fill="node.type === 'alarm' ? 'rgba(255,82,82,0.1)' : 'rgba(0,230,118,0.1)'"
            :stroke="node.type === 'alarm' ? '#ff5252' : '#00e676'"
            stroke-width="1"
          />
          <text
            :x="node.x" :y="node.y + 18"
            text-anchor="middle"
            :fill="node.type === 'alarm' ? '#ff5252' : '#00e676'"
            font-size="7"
          >{{ node.label }}</text>
        </g>
      </svg>
    </div>
  </template>

  <script lang="ts" setup>
  const center = { x: 150, y: 110 }

  const nodes = [
    { x: 90, y: 70, label: '烟感', type: 'normal' },
    { x: 210, y: 70, label: '消防栓', type: 'normal' },
    { x: 150, y: 50, label: '温感', type: 'normal' },
    { x: 70, y: 140, label: '告警', type: 'alarm' },
    { x: 230, y: 140, label: '监控', type: 'normal' },
    { x: 150, y: 170, label: '手报', type: 'normal' },
  ]

  const connections = nodes.map(n => ({ x: n.x, y: n.y }))
  </script>

  <style lang="scss" scoped>
  .device-network {
    width: 100%;
    max-width: 300px;
    margin: 0 auto;
  }
  .network-svg {
    width: 100%;
    height: auto;
  }
  .rotating-ring {
    transform-origin: 150px 110px;
    animation: rotate 20s linear infinite;
  }
  .outer-node.alarm circle:first-child {
    animation: alarmPulse 2s infinite ease-in-out;
  }
  @keyframes rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
  @keyframes alarmPulse {
    0%, 100% {
      filter: drop-shadow(0 0 0 rgba(255, 82, 82, 0.4));
    }
    50% {
      filter: drop-shadow(0 0 8px rgba(255, 82, 82, 0.4));
    }
  }
  </style>
  ```

---

## Task 4: 创建数据面板组件

**Files:**
- Create: `src/views/login/components/DataStats.vue`

- [ ] **Step 4.1: 编写 DataStats 组件**

  ```vue
  <template>
    <div class="data-stats">
      <div
        v-for="(item, idx) in stats"
        :key="idx"
        class="stat-card"
        :style="{ animationDelay: `${600 + idx * 100}ms` }"
      >
        <div class="stat-header">
          <Icon :icon="item.icon" class="stat-icon" />
          <span class="stat-label">{{ item.label }}</span>
        </div>
        <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
      </div>
    </div>
  </template>

  <script lang="ts" setup>
  import { Icon } from '@iconify/vue'

  const stats = [
    { icon: 'ep:circle-check', label: '在线设备', value: '1,247', color: '#00e676' },
    { icon: 'ep:warning', label: '实时告警', value: '3', color: '#ff5252' },
    { icon: 'ep:video-camera', label: '视频监控', value: '86', color: '#4fc3f7' },
    { icon: 'ep:map-location', label: 'GIS覆盖', value: '42', color: '#4fc3f7' },
  ]
  </script>

  <style lang="scss" scoped>
  .data-stats {
    display: flex;
    gap: 12px;
    width: 100%;
  }
  .stat-card {
    flex: 1;
    background: rgba(255, 255, 255, 0.02);
    border: 1px solid rgba(79, 195, 247, 0.08);
    border-radius: 8px;
    padding: 10px 12px;
    opacity: 0;
    transform: translateY(10px);
    animation: cardPopIn 0.5s ease-out forwards;
  }
  .stat-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 5px;
  }
  .stat-icon {
    font-size: 12px;
  }
  .stat-label {
    color: rgba(255, 255, 255, 0.35);
    font-size: 8px;
  }
  .stat-value {
    font-size: 15px;
    font-weight: 700;
  }
  @keyframes cardPopIn {
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  @media (max-width: 1279px) {
    .data-stats {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }
  }
  </style>
  ```

---

## Task 5: 创建左侧品牌面板组件

**Files:**
- Create: `src/views/login/components/BrandPanel.vue`

- [ ] **Step 5.1: 编写 BrandPanel 组件**

  ```vue
  <template>
    <div class="brand-panel">
      <!-- 背景层 -->
      <div class="bg-layer">
        <svg class="grid-bg" width="100%" height="100%">
          <defs>
            <pattern id="gisGrid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#4fc3f7" stroke-width="0.5" opacity="0.06"/>
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#gisGrid)"/>
        </svg>
        <div class="building-silhouette">
          <svg viewBox="0 0 400 120" preserveAspectRatio="none">
            <path d="M0,120 L0,80 L20,80 L20,60 L40,60 L40,50 L60,50 L60,70 L80,70 L80,40 L100,40 L100,55 L120,55 L120,35 L150,35 L150,65 L170,65 L170,45 L200,45 L200,70 L220,70 L220,50 L250,50 L250,30 L280,30 L280,60 L300,60 L300,40 L330,40 L330,75 L360,75 L360,55 L400,55 L400,120Z" fill="#4fc3f7" opacity="0.08"/>
          </svg>
        </div>
      </div>

      <!-- 内容层 -->
      <div class="brand-content">
        <!-- 顶部品牌栏 -->
        <div class="brand-header">
          <div class="brand-logo">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round">
              <path d="M12 2L12 8"/>
              <path d="M8 6C8 6 10 4 12 4C14 4 16 6 16 6"/>
              <path d="M12 8C8 8 5 11 5 15C5 18.87 8.13 22 12 22C15.87 22 19 18.87 19 15C19 11 16 8 12 8Z"/>
            </svg>
          </div>
          <div class="brand-text">
            <div class="brand-name">智慧消防</div>
            <div class="brand-sub">REMOTE FIRE MONITORING</div>
          </div>
          <div class="system-status">
            <span class="status-dot"></span>
            <span class="status-text">系统运行正常</span>
          </div>
        </div>

        <!-- 中部 -->
        <div class="brand-middle">
          <DeviceNetwork />
          <div class="brand-title-area">
            <div class="title-bar"></div>
            <div>
              <h1 class="main-title">智慧消防远程值守系统</h1>
              <p class="sub-title">
                物联网实时监控 · AI 智能预警 · 24 小时远程值守<br/>
                GIS 可视化 · 视频监控联动 · 安消一体化管理
              </p>
            </div>
          </div>
        </div>

        <!-- 底部数据 -->
        <DataStats />
      </div>
    </div>
  </template>

  <script lang="ts" setup>
  import DeviceNetwork from './DeviceNetwork.vue'
  import DataStats from './DataStats.vue'
  </script>

  <style lang="scss" scoped>
  .brand-panel {
    position: relative;
    width: 100%;
    height: 100%;
    background: linear-gradient(160deg, #060b14 0%, #0c192d 25%, #0f1f35 50%, #0a1420 100%);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: 30px 35px;
    box-sizing: border-box;
    overflow: hidden;
  }
  .bg-layer {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
  }
  .grid-bg {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    animation: drift 30s linear infinite;
  }
  .building-silhouette {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 35%;
  }
  .building-silhouette svg {
    width: 100%;
    height: 100%;
  }
  .brand-content {
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    height: 100%;
  }
  .brand-header {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .brand-logo {
    width: 38px;
    height: 38px;
    background: linear-gradient(135deg, #ff5252, #d32f2f 40%, #1976d2);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 0 15px rgba(255, 82, 82, 0.25);
    flex-shrink: 0;
  }
  .brand-text {
    flex: 1;
  }
  .brand-name {
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 1.5px;
  }
  .brand-sub {
    color: rgba(79, 195, 247, 0.5);
    font-size: 8px;
    letter-spacing: 3px;
  }
  .system-status {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .status-dot {
    width: 6px;
    height: 6px;
    background: #00e676;
    border-radius: 50%;
    box-shadow: 0 0 6px #00e676;
    animation: statusPulse 2s infinite ease-in-out;
  }
  .status-text {
    color: rgba(255, 255, 255, 0.4);
    font-size: 9px;
  }
  .brand-middle {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    position: relative;
  }
  .brand-title-area {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-top: 20px;
  }
  .title-bar {
    width: 3px;
    height: 40px;
    background: linear-gradient(180deg, #ff5252, #1976d2);
    border-radius: 2px;
    flex-shrink: 0;
    margin-top: 4px;
  }
  .main-title {
    color: #fff;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: 2px;
    margin: 0 0 10px;
  }
  .sub-title {
    color: rgba(255, 255, 255, 0.45);
    font-size: 10px;
    line-height: 1.9;
    margin: 0;
  }

  @keyframes drift {
    from { transform: translateY(0); }
    to { transform: translateY(-40px); }
  }
  @keyframes statusPulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }
  </style>
  ```

---

## Task 6: 重写登录表单组件（深色主题适配）

**Files:**
- Modify: `src/views/login/components/LoginForm.vue`

- [ ] **Step 6.1: 重写 LoginForm.vue 为深色主题**

  保留原有登录逻辑（useLogin、auth、permissionStore 等），仅重写模板和样式：

  ```vue
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
          <Icon icon="ep:user" class="avatar-icon" />
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

      <PasswordStrength :password="loginData.loginForm.password" />

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
        :imgSize="{ width: '400px', height: '200px' }"
        mode="pop"
        @success="handleLogin"
      />
    </el-form>
  </template>
  ```

  样式部分使用 SCSS，定义在 `<style lang="scss" scoped>` 中：

  ```scss
  .login-form-dark {
    width: 100%;
  }
  .form-header {
    text-align: center;
    margin-bottom: 26px;
  }
  .user-avatar {
    width: 44px;
    height: 44px;
    margin: 0 auto 12px;
    background: linear-gradient(135deg, rgba(79, 195, 247, 0.15), rgba(25, 118, 210, 0.1));
    border: 1px solid rgba(79, 195, 247, 0.2);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .avatar-icon {
    font-size: 22px;
    color: #4fc3f7;
  }
  .form-title {
    color: #fff;
    font-size: 17px;
    font-weight: 600;
    margin: 0 0 5px;
  }
  .form-subtitle {
    color: rgba(255, 255, 255, 0.3);
    font-size: 10px;
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
    font-size: 11px;
  }
  .forgot-link {
    color: #4fc3f7;
    font-size: 11px;
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
    font-size: 14px;
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
    font-size: 9px;
  }
  ```

  `<script setup>` 部分保留原有逻辑，仅新增 `usernameError` / `passwordError` ref 用于控制输入框错误状态显示。在 `validForm()` 调用失败时设置对应错误状态。

- [ ] **Step 6.2: 更新 components/index.ts 导出**

  ```typescript
  // src/views/login/components/index.ts
  export { default as LoginForm } from './LoginForm.vue'
  export { default as LoginFormTitle } from './LoginFormTitle.vue'
  export { default as MobileForm } from './MobileForm.vue'
  export { default as QrCodeForm } from './QrCodeForm.vue'
  export { default as RegisterForm } from './RegisterForm.vue'
  export { default as SSOLoginVue } from './SSOLogin.vue'
  export { default as ForgetPasswordForm } from './ForgetPasswordForm.vue'
  export { default as BrandPanel } from './BrandPanel.vue'
  export { default as PasswordStrength } from './PasswordStrength.vue'
  export { default as SafeInput } from './SafeInput.vue'
  ```

---

## Task 7: 重写登录页主容器

**Files:**
- Modify: `src/views/login/Login.vue`

- [ ] **Step 7.1: 重写 Login.vue 主容器**

  ```vue
  <template>
    <div :class="prefixCls" class="login-page">
      <!-- 左侧品牌区 -->
      <div class="login-left">
        <BrandPanel />
      </div>

      <!-- 右侧登录区 -->
      <div class="login-right">
        <div class="right-glow top"></div>
        <div class="right-glow bottom"></div>
        <div class="fire-deco">
          <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="#4fc3f7" stroke-width="1">
            <path d="M12 2L12 8"/>
            <path d="M8 6C8 6 10 4 12 4C14 4 16 6 16 6"/>
            <path d="M12 8C8 8 5 11 5 15C5 18.87 8.13 22 12 22C15.87 22 19 18.87 19 15C19 11 16 8 12 8Z"/>
          </svg>
        </div>

        <div class="login-card-wrapper">
          <LoginForm class="login-form-dark-card" />
        </div>
      </div>
    </div>
  </template>

  <script lang="ts" setup>
  import { useDesign } from '@/hooks/web/useDesign'
  import { BrandPanel, LoginForm } from './components'

  defineOptions({ name: 'Login' })

  const { getPrefixCls } = useDesign()
  const prefixCls = getPrefixCls('login')
  </script>

  <style lang="scss" scoped>
  $prefix-cls: #{$namespace}-login;

  .login-page {
    display: flex;
    width: 100vw;
    height: 100vh;
    overflow: hidden;
  }

  .login-left {
    width: 58%;
    height: 100%;
    position: relative;
  }

  .login-right {
    width: 42%;
    height: 100%;
    background: linear-gradient(180deg, #0a1625 0%, #0d1d2e 50%, #0a1625 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
  }

  .right-glow {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    &.top {
      top: 15%;
      right: 8%;
      width: 140px;
      height: 140px;
      background: radial-gradient(circle, rgba(79, 195, 247, 0.06), transparent 70%);
    }
    &.bottom {
      bottom: 12%;
      left: 3%;
      width: 100px;
      height: 100px;
      background: radial-gradient(circle, rgba(255, 82, 82, 0.04), transparent 70%);
    }
  }

  .fire-deco {
    position: absolute;
    top: 8%;
    right: 12%;
    opacity: 0.08;
    pointer-events: none;
  }

  .login-card-wrapper {
    width: 80%;
    max-width: 340px;
    z-index: 2;
  }

  .login-form-dark-card {
    background: rgba(255, 255, 255, 0.025);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(79, 195, 247, 0.1);
    border-radius: 16px;
    padding: 32px 28px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.04);
    animation: cardFadeIn 0.6s ease-out 0.5s both;
  }

  @keyframes cardFadeIn {
    from {
      opacity: 0;
      transform: translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  // 响应式：平板
  @media (max-width: 1279px) {
    .login-left { width: 50%; }
    .login-right { width: 50%; }
    .login-card-wrapper { width: 85%; }
  }

  // 响应式：移动端
  @media (max-width: 767px) {
    .login-page {
      flex-direction: column;
    }
    .login-left {
      width: 100%;
      height: 35vh;
      min-height: 220px;
    }
    .login-right {
      width: 100%;
      flex: 1;
    }
    .login-card-wrapper {
      width: 90%;
      max-width: 400px;
    }
    .login-form-dark-card {
      padding: 28px 24px;
    }
    .fire-deco { display: none; }
  }

  // 响应式：小屏移动端
  @media (max-width: 479px) {
    .login-left {
      height: 28vh;
      min-height: 180px;
    }
    .login-card-wrapper {
      width: 92%;
    }
    .login-form-dark-card {
      padding: 24px 20px;
    }
  }

  // backdrop-filter 降级
  @supports not (backdrop-filter: blur(20px)) {
    .login-form-dark-card {
      background: rgba(15, 30, 50, 0.95);
    }
  }
  </style>
  ```

---

## Task 8: 移动端品牌区适配

**Files:**
- Modify: `src/views/login/components/BrandPanel.vue`

- [ ] **Step 8.1: 在 BrandPanel 样式中添加移动端媒体查询**

  在 BrandPanel.vue 的 `<style>` 末尾追加：

  ```scss
  @media (max-width: 767px) {
    .brand-panel {
      padding: 20px;
      justify-content: center;
      align-items: center;
      text-align: center;
    }
    .brand-header {
      flex-direction: column;
      gap: 8px;
    }
    .brand-text {
      text-align: center;
    }
    .system-status {
      display: none;
    }
    .brand-middle {
      display: none;
    }
    .brand-title-area {
      margin-top: 0;
      flex-direction: column;
      align-items: center;
    }
    .title-bar {
      display: none;
    }
    .main-title {
      font-size: 16px;
    }
    .sub-title {
      display: none;
    }
    .data-stats {
      display: none;
    }
  }
  ```

---

## Task 9: 验证与测试

**Files:**
- N/A (dev server verification)

- [ ] **Step 9.1: 启动 dev 服务器验证桌面端效果**

  Run: `cd anwei-fire-remote-monitor-vue ; pnpm dev`
  Open: `http://localhost:端口号/login`
  Verify:
  - 左右分栏布局正常
  - 左侧品牌区显示 Logo、设备节点网络、标题、数据卡片
  - 右侧显示毛玻璃登录卡片
  - 输入框聚焦时有底部光带动效
  - 密码输入时显示强度指示器
  - 所有动画正常播放（节点旋转、状态脉冲、卡片入场）

- [ ] **Step 9.2: 验证响应式（浏览器 DevTools）**

  Verify at breakpoints:
  - 768px ~ 1279px: 左右各 50%，数据卡片 2x2 布局
  - 767px 以下: 上下堆叠，左侧品牌区压缩为顶部横幅
  - 479px 以下: 进一步简化，节点网络隐藏

- [ ] **Step 9.3: 验证登录功能**

  - 输入正确账号密码，点击登录
  - 验证 loading 状态显示 "登录中..."
  - 验证登录成功后正常跳转（值守端大屏/监控中心）
  - 验证验证码弹窗正常触发（如配置开启）

---

## 自审检查清单

### Spec 覆盖度

| Spec 章节 | 对应任务 |
|-----------|----------|
| 3.1 色彩体系 | Task 5, 6, 7 (各组件样式) |
| 5.1 背景层 | Task 5 (BrandPanel bg-layer) |
| 5.2 品牌栏 | Task 5 (brand-header) |
| 5.3 设备节点 | Task 3 (DeviceNetwork) |
| 5.4 系统文案 | Task 5 (brand-title-area) |
| 5.5 数据面板 | Task 4 (DataStats) |
| 6.2 登录卡片 | Task 7 (login-form-dark-card) |
| 6.3 表单元素 | Task 2 (SafeInput), Task 6 (LoginForm) |
| 7.1 ~ 7.3 动画 | Task 3 (keyframes), Task 5 (drift), Task 7 (cardFadeIn) |
| 8 响应式 | Task 7 (media queries), Task 8 (BrandPanel mobile) |
| 9 安全性 | Task 6 (保留 Verify 组件 + 密码强度) |
| 10 组件拆分 | 全部任务 |
| 11 交互状态 | Task 2 (error), Task 6 (loading) |
| 12 性能 | Task 7 (@supports backdrop-filter) |

### Placeholder 扫描

- [x] 无 TBD / TODO
- [x] 无 "add appropriate error handling" 类模糊描述
- [x] 每个代码步骤包含完整代码
- [x] 无 "Similar to Task N" 引用

### 类型一致性

- [x] `loginData.loginForm` 结构保持与原有代码一致
- [x] `useLogin` hook 接口未变更
- [x] `Verify` 组件 props 保持原有
