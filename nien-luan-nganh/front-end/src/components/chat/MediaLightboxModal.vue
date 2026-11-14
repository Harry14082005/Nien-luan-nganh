<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { resolveFileUrl } from '@/api/storage.api'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  mediaList: {
    type: Array,
    default: () => [],
  },
  initialIndex: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(['close'])

const currentIndex = ref(props.initialIndex)
const isLoadError = ref(false)

watch(
  () => props.initialIndex,
  (val) => {
    currentIndex.value = Math.max(0, Math.min(val, (props.mediaList.length || 1) - 1))
    isLoadError.value = false
  },
)

watch(
  () => currentIndex.value,
  () => {
    isLoadError.value = false
  },
)

const currentItem = computed(() => {
  if (!props.mediaList || props.mediaList.length === 0) return null
  return props.mediaList[currentIndex.value] || null
})

const currentUrl = computed(() => {
  if (!currentItem.value) return ''
  const raw = currentItem.value.url || currentItem.value.content || ''
  return resolveFileUrl(raw)
})

const isVideo = computed(() => {
  if (!currentItem.value) return false
  if (currentItem.value.type === 'VIDEO' || currentItem.value.messageType === 'VIDEO') return true
  const url = currentItem.value.url || currentItem.value.content || ''
  return Boolean(url.match(/\.(mp4|mov|webm|avi|mkv|m4v)$/i))
})

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < props.mediaList.length - 1)

function prev() {
  if (hasPrev.value) {
    currentIndex.value--
  }
}

function next() {
  if (hasNext.value) {
    currentIndex.value++
  }
}

function handleKeydown(e) {
  if (!props.show) return
  if (e.key === 'Escape') {
    emit('close')
  } else if (e.key === 'ArrowLeft') {
    prev()
  } else if (e.key === 'ArrowRight') {
    next()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="lightbox-fade">
      <div v-if="show" class="lightbox-overlay" @click.self="emit('close')">
        <!-- 1. Top Bar: Nút thoát góc trên bên trái & Bộ đếm -->
        <div class="lightbox-topbar">
          <button class="lightbox-close-btn" title="Đóng (Esc)" @click="emit('close')">
            <font-awesome-icon icon="x" />
          </button>

          <div v-if="mediaList.length > 1" class="lightbox-counter">
            {{ currentIndex + 1 }} / {{ mediaList.length }}
          </div>
        </div>

        <!-- 2. Nút chuyển trước (Bên trái) -->
        <button
          v-if="mediaList.length > 1"
          class="lightbox-nav-btn btn-prev"
          :class="{ disabled: !hasPrev }"
          title="Xem ảnh trước (Phím mũi tên trái)"
          :disabled="!hasPrev"
          @click.stop="prev"
        >
          <font-awesome-icon icon="chevron-left" />
        </button>

        <!-- 3. Khu vực hiển thị Media chính -->
        <div class="lightbox-content-box" @click.self="emit('close')">
          <!-- Fallback khi ảnh load thất bại -->
          <div v-if="isLoadError" class="lightbox-fallback-card">
            <font-awesome-icon icon="image" class="fallback-icon" />
            <span class="fallback-text">Không thể tải phương tiện này</span>
          </div>

          <!-- Video Player -->
          <video
            v-else-if="isVideo"
            :key="currentUrl"
            :src="currentUrl"
            class="lightbox-video"
            controls
            autoplay
            playsinline
            @error="isLoadError = true"
          ></video>

          <!-- Image Viewer -->
          <img
            v-else
            :key="currentUrl"
            :src="currentUrl"
            alt="Hình ảnh phóng to"
            class="lightbox-image"
            @error="isLoadError = true"
          />
        </div>

        <!-- 4. Nút chuyển kế tiếp (Bên phải) -->
        <button
          v-if="mediaList.length > 1"
          class="lightbox-nav-btn btn-next"
          :class="{ disabled: !hasNext }"
          title="Xem ảnh sau (Phím mũi tên phải)"
          :disabled="!hasNext"
          @click.stop="next"
        >
          <font-awesome-icon icon="chevron-right" />
        </button>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');

.lightbox-overlay {
  font-family: 'Quicksand', sans-serif;
  position: fixed;
  inset: 0;
  z-index: 99999;
  background-color: rgba(15, 23, 42, 0.92);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

/* Topbar */
.lightbox-topbar {
  position: absolute;
  top: 16px;
  left: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 10;
  pointer-events: none;
}

.lightbox-close-btn {
  pointer-events: auto;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.lightbox-close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.08);
}

.lightbox-counter {
  pointer-events: auto;
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(0, 0, 0, 0.4);
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* Nav Buttons */
.lightbox-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 10;
}

.btn-prev {
  left: 24px;
}

.btn-next {
  right: 24px;
}

.lightbox-nav-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.35);
  transform: translateY(-50%) scale(1.1);
}

.lightbox-nav-btn.disabled,
.lightbox-nav-btn:disabled {
  opacity: 0.2;
  cursor: not-allowed;
  transform: translateY(-50%) scale(1);
}

/* Main Content Box */
.lightbox-content-box {
  max-width: 90vw;
  max-height: 85vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lightbox-image {
  max-width: 88vw;
  max-height: 84vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  animation: zoomIn 0.1s ease-out;
}

.lightbox-video {
  max-width: 88vw;
  max-height: 84vh;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  outline: none;
  animation: zoomIn 0.1s ease-out;
}

/* Fallback card khi lỗi ảnh */
.lightbox-fallback-card {
  width: 320px;
  height: 240px;
  background: #f1f5f9;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #94a3b8;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
}

.fallback-icon {
  font-size: 48px;
}

.fallback-text {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
}

/* Transitions & Animations */
@keyframes zoomIn {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.lightbox-fade-enter-active,
.lightbox-fade-leave-active {
  transition: opacity 0.1s ease;
}

.lightbox-fade-enter-from,
.lightbox-fade-leave-to {
  opacity: 0;
}
</style>

