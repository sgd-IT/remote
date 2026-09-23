<template>
  <el-dialog
    v-model="visible"
    :fullscreen="true"
    :show-close="true"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    class="remote-console-dialog"
    append-to-body
    destroy-on-close
  >
    <template #header>
      <div class="dialog-header">
        <span class="dialog-title">远程控制台</span>
      </div>
    </template>
    <div class="dialog-body">
      <RemoteConsole />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import RemoteConsole from '../index.vue'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})
</script>

<style lang="scss">
@import '../styles/common.scss';

/* 全屏弹窗外壳 */
.remote-console-dialog {
  background: $bg-primary;

  .el-dialog__header {
    margin: 0;
    padding: 0;
    border-bottom: 1px solid $border-color;
  }

  .el-dialog__headerbtn {
    top: 14px;
    right: 16px;
    z-index: 10;

    .el-dialog__close {
      color: $text-secondary;
      font-size: 20px;

      &:hover {
        color: $text-primary;
      }
    }
  }

  .el-dialog__body {
    padding: 0;
    height: calc(100vh - 46px);
    overflow: hidden;
    background: $bg-primary;
  }
}

.dialog-header {
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-panel;
  border-bottom: 1px solid $border-color;
}

.dialog-title {
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 3px;
  color: $text-primary;
}

.dialog-body {
  width: 100%;
  height: 100%;

  .remote-console {
    height: 100% !important;
  }
}

/* 确保全屏弹窗本身也铺满 */
.remote-console-dialog.el-dialog--fullscreen {
  height: 100vh;
  display: flex;
  flex-direction: column;

  .el-dialog__body {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }
}

/* 远程控制台内部子弹窗浅色主题（append-to-body 场景） */
.remote-console-modal {
  background: #ffffff !important;
  border: 1px solid #c5dfde;

  .el-dialog__header {
    border-bottom: 1px solid #e2eded;
    margin-right: 0;
    padding: 16px 24px;
    background: #f7fbfb;
  }

  .el-dialog__title {
    color: #293b3d;
    font-size: 16px;
    font-weight: bold;
    letter-spacing: 1px;
  }

  .el-dialog__headerbtn .el-dialog__close {
    color: #7b8e90;
    &:hover { color: #293b3d; }
  }

  .el-dialog__body {
    color: #293b3d;
  }

  .el-dialog__footer {
    border-top: 1px solid #e2eded;
    padding: 16px 24px;
  }

  /* Element Plus 表单在浅色背景下的适配 */
  .el-form-item__label {
    color: #52686a;
  }

  .el-input__wrapper {
    background: #ffffff;
    box-shadow: 0 0 0 1px #cbdedd inset;

    .el-input__inner {
      color: #293b3d;
    }
  }

  .el-select .el-input__wrapper {
    background: #ffffff;
  }

  .el-table {
    --el-table-bg-color: #ffffff;
    --el-table-tr-bg-color: transparent;
    --el-table-header-bg-color: #eef8f7;
    --el-table-row-hover-bg-color: #f3faf9;
    --el-table-border-color: #e2eded;
    --el-table-text-color: #293b3d;
    --el-table-header-text-color: #52686a;
  }

  .el-empty__description {
    color: #7b8e90;
  }
}
</style>
