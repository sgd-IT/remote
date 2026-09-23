<template>
  <codemirror v-if="isLoad" class="code" ref="editScriptRef" v-model:value="codeModel" height="300" :extensions="extensions" v-bind="cmOption" :options="cmOption" @ready="handleReady" />
</template>

<script lang="ts" setup>
import 'codemirror/mode/javascript/javascript.js'
import Codemirror from 'codemirror-editor-vue3'
import 'codemirror/theme/darcula.css'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'
import { propTypes } from '@/utils/propTypes'

const props = defineProps({
  code: propTypes.string.def(''),
  model: propTypes.string.def('text/javascript'),
})
const emits = defineEmits(['update:code'])
const codeModel = ref('')
watch(() => props.code, (newV) => {
  codeModel.value = newV
}, {
  immediate: true,
})
watch(codeModel, (newV) => {
  emits('update:code', newV)
})
const extensions = [javascript(), oneDark]
const view = shallowRef()
const handleReady = (payload: any) => {
  view.value = payload.view
}
const editScriptRef = ref()
const isLoad = ref(false)
onMounted(() => {
  isLoad.value = true
})
onUnmounted(() => {
  editScriptRef.value?.destroy();
})
const cmOption = reactive({
  tabSize: 2,
  lineNumbers: true,
  line: true,
  indentWithTabs: true,
  smartIndent: true,
  autofocus: true,
  mode: props.model,
  theme: 'darcula',
  foldGutter: true,
  lineWrapping: true,
  gutters: [
    'CodeMirror-linenumbers',
    'CodeMirror-foldgutter',
    'CodeMirror-lint-markers',
  ],
})
</script>

<style lang="scss" scoped>
// ::v-deep(.cm-editor) {
//   min-height: 200px;
// }
</style>
<style lang="scss">
.CodeMirror-line {
  // padding-left: 28px!important;
}
</style>