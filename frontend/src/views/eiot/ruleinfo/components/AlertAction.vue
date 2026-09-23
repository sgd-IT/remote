<template>
  <div>
    <el-form label-width="120px" v-for="service in config.services" :key="service">
      <el-form-item label="告警触发脚本">
        <code-editor style="width: 100%" v-model:code="service.script" />
      </el-form-item>
      <el-form-item label="告警解除脚本">
        <code-editor style="width: 100%" v-model:code="service.recoverScript" />
        <div class="form-tips">留空则复用触发脚本</div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script lang="ts" setup>
import CodeEditor from '@/components/CodeEditor/index.vue'

const props = defineProps({
  config: {
    type: Object,
    default: () => {
      return {
        type: 'alert',
        services: [
          {
            script: '',
            recoverScript: '',
          },
        ],
      }
    },
  },
})

const configRef = ref<any>(props.config)
const init = (data) => {
  if (!configRef.value.services) configRef.value.services = data.services
  if (configRef.value.services.length > 1) {
    configRef.value.services.splice(1, configRef.value.services.length - 1)
  }
}
watch(
  () => props.config,
  (newV) => {
    init(newV)
  },
  {
    immediate: true,
    deep: true,
  }
)
</script>

<style scoped>
.CodeMirror {
  height: 300px !important;
}
</style>
<style>
.CodeMirror pre.CodeMirror-line,
.CodeMirror pre.CodeMirror-line-like {
  line-height: 21px !important;
}
</style>
