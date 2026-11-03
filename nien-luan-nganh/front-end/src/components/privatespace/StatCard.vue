<script setup>
defineProps({
  count: {
    type: [String, Number],
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  actionText: {
    type: String,
    default: '',
  },
  variant: {
    type: String,
    default: 'blue', // 'blue' | 'purple' | 'teal'
  },
})

const emit = defineEmits(['action'])
</script>

<template>
  <div class="stat-card" @click="emit('action')">
    <!-- Icon Container -->
    <div class="icon-wrap" :class="variant">
      <slot name="icon">
        <svg
          v-if="variant === 'blue'"
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
        <svg
          v-else-if="variant === 'purple'"
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
          <line x1="16" y1="2" x2="16" y2="6" />
          <line x1="8" y1="2" x2="8" y2="6" />
          <line x1="3" y1="10" x2="21" y2="10" />
        </svg>
        <svg
          v-else
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="4" y1="21" x2="4" y2="14" />
          <line x1="4" y1="10" x2="4" y2="3" />
          <line x1="12" y1="21" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12" y2="3" />
          <line x1="20" y1="21" x2="20" y2="16" />
          <line x1="20" y1="12" x2="20" y2="3" />
        </svg>
      </slot>
    </div>

    <!-- Stat Count -->
    <div class="stat-count">{{ count }}</div>

    <!-- Stat Label -->
    <div class="stat-label">{{ label }}</div>

    <!-- Action Link -->
    <div v-if="actionText" class="action-link">
      <span>{{ actionText }}</span>
      <svg
        class="arrow-icon"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.5"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <line x1="5" y1="12" x2="19" y2="12" />
        <polyline points="12 5 19 12 12 19" />
      </svg>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.stat-card {
  background-color: #ffffff;
  border-radius: 18px;
  padding: 22px 24px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.025);
  font-family: 'Quicksand', sans-serif;
  display: flex;
  flex-direction: column;
  transition: all 0.2s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: #e2e8f0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
}

.icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}

.icon-wrap.blue {
  background-color: #eff6ff;
  color: #2563eb;
}

.icon-wrap.purple {
  background-color: #f5f3ff;
  color: #7c3aed;
}

.icon-wrap.teal {
  background-color: #f0fdfa;
  color: #0d9488;
}

.stat-count {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.action-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  margin-top: 16px;
  transition: gap 0.15s ease, color 0.15s ease;
}

.stat-card:hover .action-link {
  color: #1d4ed8;
  gap: 8px;
}

.arrow-icon {
  transition: transform 0.15s ease;
}

.stat-card:hover .arrow-icon {
  transform: translateX(2px);
}
</style>

