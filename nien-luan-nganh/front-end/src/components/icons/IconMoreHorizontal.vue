<script setup>
defineProps({ size: { default: 18 } })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="currentColor">
    <circle cx="5" cy="12" r="2" />
    <circle cx="12" cy="12" r="2" />
    <circle cx="19" cy="12" r="2" />
  </svg>
</template>

