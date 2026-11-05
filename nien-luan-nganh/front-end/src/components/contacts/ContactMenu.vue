<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

const emit = defineEmits(['block', 'remove'])

const open = ref(false)
const root = ref(null)

function toggle() {
  open.value = !open.value
}

function choose(action) {
  open.value = false
  emit(action)
}

function onClickOutside(e) {
  if (root.value && !root.value.contains(e.target)) open.value = false
}

onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<template>
  <div ref="root" class="contact-menu" @click.stop>
    <button class="dots-btn" title="Tùy chọn" @click="toggle">
      <font-awesome-icon icon="ellipsis-vertical" />
    </button>

    <div v-if="open" class="dropdown">
      <button class="item" @click="choose('block')">
        <font-awesome-icon icon="ban" />
        Chặn người này
      </button>
      <button class="item danger" @click="choose('remove')">
        <font-awesome-icon icon="user-xmark" />
        Xóa bạn
      </button>
    </div>
  </div>
</template>

<style scoped>
.contact-menu {
  position: relative;
}

.dots-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #2f5fd0;
  border-radius: 50%;
  cursor: pointer;
}

.dots-btn:hover {
  background: rgba(47, 95, 208, 0.12);
}

.dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 50;
  min-width: 170px;
  padding: 6px;
  background: #fff;
  border-radius: 0.7rem;
  box-shadow: 0 6px 20px rgba(15, 37, 71, 0.18);
}

.item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: none;
  background: transparent;
  border-radius: 0.5rem;
  font: inherit;
  font-size: 14px;
  color: #1f2a44;
  cursor: pointer;
  text-align: left;
}

.item:hover {
  background: #eef3ff;
}

.item.danger {
  color: #d93025;
}

.item.danger:hover {
  background: #fdecea;
}
</style>
