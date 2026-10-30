<script setup>
defineProps({ size: { default: 16 }, strokeWidth: { default: 2 } })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none"
       stroke="currentColor" :stroke-width="strokeWidth"
       stroke-linecap="round" stroke-linejoin="round">
    <path d="M18 20V10M12 20V4M6 20v-6"/>
  </svg>
</template>
