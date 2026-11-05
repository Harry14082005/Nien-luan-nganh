<script setup>
import { computed, ref, watch } from 'vue';

const props = defineProps({
  name: { type: String, default: '' },
  src: { type: String, default: '' },
  size: { type: Number, default: 36 },
});

const imgError = ref(false);
watch(() => props.src, () => (imgError.value = false));

const showImage = computed(() => !!props.src && !imgError.value);

const avatarText = computed(() => {
  const trimmed = (props.name || '').trim();
  if (!trimmed) return '?';
  const words = trimmed.split(/\s+/).filter(Boolean);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[words.length - 1][0]).toUpperCase();
});

const palette = [
  { bg: '#ECE9FF', color: '#6B5BD2' }, // tím
  { bg: '#FFE9DC', color: '#D2693B' }, // cam
  { bg: '#E3EDFF', color: '#3B6FD2' }, // xanh dương
  { bg: '#DDF5EA', color: '#2E9A6B' }, // xanh lá
  { bg: '#FFE3EC', color: '#D23B73' }, // hồng
  { bg: '#FFF3D6', color: '#B8860B' }, // vàng
];

const avatarStyle = computed(() => {
  let hash = 0;
  const str = props.name || 'User';
  for (const ch of str) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  const c = palette[hash % palette.length];
  return {
    width: `${props.size}px`,
    height: `${props.size}px`,
    fontSize: `${Math.max(11, Math.round(props.size * 0.38))}px`,
    backgroundColor: c.bg,
    color: c.color,
  };
});
</script>

<template>
  <div class="user-avatar-root">
    <img
      v-if="showImage"
      :src="src"
      :alt="name"
      class="avatar-img"
      :style="{ width: `${size}px`, height: `${size}px` }"
      @error="imgError = true"
    />
    <div
      v-else
      class="avatar-initials"
      :style="avatarStyle"
    >
      {{ avatarText }}
    </div>
  </div>
</template>

<style scoped>
.user-avatar-root {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.avatar-initials {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Quicksand', sans-serif;
  font-weight: 700;
  user-select: none;
}
</style>

