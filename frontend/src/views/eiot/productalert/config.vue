<template>
  <AlertConfig :product-id="productId" />
</template>

<script lang="ts" setup>
import AlertConfig from '@/components/AlertConfig/index.vue'

const props = defineProps({
  productId: {
    type: Number,
    required: true,
  },
})
</script>
