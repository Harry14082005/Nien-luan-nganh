<script setup>
defineProps({
  badgeText: {
    type: String,
    default: 'LỊCH SẮP TỚI',
  },
  title: {
    type: String,
    default: 'Review giao diện sản phẩm',
  },
  description: {
    type: String,
    default: 'Cùng xem lại các cập nhật mới nhất và thống nhất những bước tiếp theo.',
  },
  timeText: {
    type: String,
    default: 'Thứ Sáu tuần này · 14:00 - 16:00',
  },
  groupName: {
    type: String,
    default: 'Nhóm Thiết kế sản phẩm',
  },
  members: {
    type: Array,
    default: () => [
      { initials: 'MA', bg: '#ffe8d6', color: '#d97736' },
      { initials: 'QH', bg: '#dcfce7', color: '#0d9488' },
      { initials: 'IN', bg: '#e0f2fe', color: '#0284c7' },
    ],
  },
})

const emit = defineEmits(['click'])
</script>

<template>
  <div class="event-card" @click="emit('click')">
    <!-- Header -->
    <div class="card-header">
      <div class="badge-tag">
        <svg
          class="badge-icon"
          width="14"
          height="14"
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
        <span>{{ badgeText }}</span>
      </div>

      <button class="btn-more" title="Tùy chọn" @click.stop>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <circle cx="5" cy="12" r="2" />
          <circle cx="12" cy="12" r="2" />
          <circle cx="19" cy="12" r="2" />
        </svg>
      </button>
    </div>

    <!-- Content -->
    <div class="card-body">
      <h3 class="event-title">{{ title }}</h3>
      <p class="event-desc">{{ description }}</p>

      <div class="event-time-row">
        <svg
          class="time-icon"
          width="15"
          height="15"
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
        <span>{{ timeText }}</span>
      </div>
    </div>

    <div class="divider"></div>

    <!-- Footer -->
    <div class="card-footer">
      <div class="group-info">
        <div class="avatar-stack">
          <div
            v-for="(m, i) in members"
            :key="i"
            class="stacked-avatar"
            :style="{ backgroundColor: m.bg, color: m.color, zIndex: members.length - i }"
          >
            {{ m.initials }}
          </div>
        </div>
        <span class="group-name">{{ groupName }}</span>
      </div>

      <button class="btn-arrow" title="Chi tiết">
        <svg
          width="16"
          height="16"
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
      </button>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.event-card {
  background-color: #ffffff;
  border-radius: 20px;
  padding: 24px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.025);
  font-family: 'Quicksand', sans-serif;
  display: flex;
  flex-direction: column;
  transition: all 0.2s ease;
  cursor: pointer;
}

.event-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  border-color: #e2e8f0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.badge-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background-color: #f5f3ff;
  color: #7c3aed;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.btn-more {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
}

.btn-more:hover {
  color: #1e293b;
}

.card-body {
  flex: 1;
}

.event-title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px 0;
}

.event-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 14px 0;
}

.event-time-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.time-icon {
  color: #94a3b8;
}

.divider {
  height: 1px;
  background-color: #f1f5f9;
  margin: 20px 0 16px 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.group-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-stack {
  display: flex;
  align-items: center;
}

.stacked-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  margin-left: -6px;
}

.stacked-avatar:first-child {
  margin-left: 0;
}

.group-name {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.btn-arrow {
  background: none;
  border: none;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.event-card:hover .btn-arrow {
  transform: translateX(3px);
}
</style>

