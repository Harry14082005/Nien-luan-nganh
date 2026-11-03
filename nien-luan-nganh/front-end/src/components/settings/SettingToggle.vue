<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

function toggle() {
  if (props.disabled) return
  emit('update:modelValue', !props.modelValue)
}
</script>

<template>
  <div
    class="setting-toggle-row"
    :class="{ disabled }"
    @click="toggle"
  >
    <div class="toggle-text">
      <div class="toggle-title">{{ title }}</div>
      <div v-if="description" class="toggle-description">{{ description }}</div>
    </div>

    <button
      type="button"
      role="switch"
      :aria-checked="modelValue"
      :disabled="disabled"
      class="switch-control"
      :class="{ active: modelValue }"
      @click.stop="toggle"
      @keydown.space.prevent="toggle"
      @keydown.enter.prevent="toggle"
    >
      <span class="switch-knob"></span>
    </button>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.setting-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  user-select: none;
  font-family: 'Quicksand', sans-serif;
  transition: background-color 0.15s ease;
  border-radius: 12px;
}

.setting-toggle-row:hover {
  background-color: #f8fafd;
}

.setting-toggle-row.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toggle-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-right: 16px;
}

.toggle-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.toggle-description {
  font-size: 13px;
  color: #64748b;
  line-height: 1.4;
}

.switch-control {
  position: relative;
  width: 46px;
  height: 26px;
  background-color: #cbd5e1;
  border-radius: 9999px;
  border: none;
  cursor: pointer;
  padding: 3px;
  display: flex;
  align-items: center;
  transition: background-color 0.2s ease;
  flex-shrink: 0;
  outline: none;
}

.switch-control:focus-visible {
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
}

.switch-control.active {
  background-color: #2f5fd0;
}

.switch-knob {
  width: 20px;
  height: 20px;
  background-color: #ffffff;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  transform: translateX(0);
}

.switch-control.active .switch-knob {
  transform: translateX(20px);
}
</style>

