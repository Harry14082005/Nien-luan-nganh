<script setup>
defineProps({ size: { default: 18 }, fill: { default: 'none' }, strokeWidth: { default: 2 } })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" :fill="fill"
       stroke="currentColor" :stroke-width="strokeWidth"
       stroke-linecap="round" stroke-linejoin="round">
    <path d="M12 2l2.4 7.4H22l-6 4.6 2.3 7.4-6.3-4.6-6.3 4.6 2.3-7.4-6-4.6h7.6z" />
  </svg>
</template>

