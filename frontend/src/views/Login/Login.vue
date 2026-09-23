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
