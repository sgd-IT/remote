<template>
  <AlertConfig :device-id="deviceId" :product-id="productId" />
</template>

<script lang="ts" setup>
import AlertConfig from '@/components/AlertConfig/index.vue'

const props = defineProps({
  deviceId: {
    type: Number,
    default: null,
  },
  productId: {
    type: Number,
    default: null,
  },
})
</script>
