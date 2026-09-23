<template>
  <div class="control-panels">
    <PanelSection title="多线盘" type="MULTI" :functions="multiFunctions" @action="(fn, actionType) => $emit('action', fn, 'MULTI', actionType)" />
    <PanelSection title="总线盘" type="BUS" :functions="busFunctions" @action="(fn, actionType) => $emit('action', fn, 'BUS', actionType)" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DeviceFunctionVO } from '@/api/eiot/remote/deviceRemote.api'
import PanelSection from './PanelSection.vue'

const props = defineProps<{
  functions: DeviceFunctionVO[]
}>()

const emit = defineEmits<{
  (e: 'action', fn: DeviceFunctionVO, panelType: 'MULTI' | 'BUS', actionType: 'start' | 'stop'): void
}>()

const multiFunctions = computed(() => props.functions.filter(f => f.functionType === 'MULTI'))
const busFunctions = computed(() => props.functions.filter(f => f.functionType === 'BUS'))
</script>

<style lang="scss" scoped>
@import '../styles/common.scss';
.control-panels {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
