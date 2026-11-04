<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'

const props = defineProps({
  file: {
    type: File,
    default: null
  }
})

const emit = defineEmits(['save', 'cancel'])

const fileInput = ref(null)
const imageSrc = ref('')
const originalImage = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const containerSize = 280

const imgTransform = reactive({
  x: 0,
  y: 0,
  scale: 1,
  minScale: 1,
  maxScale: 4,
})

const dragging = ref(false)
const dragStart = ref({ mx: 0, my: 0, x: 0, y: 0 })

function triggerPick() {
  fileInput.value?.click()
}

function processFile(file) {
  if (!file || !file.type.startsWith('image/')) {
    errorMsg.value = 'Vui lòng chọn file ảnh hợp lệ.'
    return
  }
  errorMsg.value = ''
  
  const reader = new FileReader()
  reader.onload = (ev) => {
    const img = new Image()
    img.onload = () => {
      originalImage.value = img
      imageSrc.value = ev.target.result

      const minS = Math.max(containerSize / img.width, containerSize / img.height)
      imgTransform.minScale = minS
      imgTransform.maxScale = minS * 4
      imgTransform.scale = minS
      
      imgTransform.x = (containerSize - img.width * minS) / 2
      imgTransform.y = (containerSize - img.height * minS) / 2
    }
    img.src = ev.target.result
  }
  reader.readAsDataURL(file)
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) {
    processFile(file)
  }
  e.target.value = ''
}

// Khi modal vừa mở lên, nếu có prop file thì load luôn, nếu không thì tự động bật hộp thoại chọn file của máy tính
onMounted(() => {
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)

  if (props.file) {
    processFile(props.file)
  }
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', onMouseUp)
})

watch(() => props.file, (newFile) => {
  if (newFile) {
    processFile(newFile)
  }
})

function startDrag(e) {
  if (!imageSrc.value) return
  dragging.value = true
  dragStart.value = {
    mx: e.clientX,
    my: e.clientY,
    x: imgTransform.x,
    y: imgTransform.y,
  }
  e.preventDefault()
}

function onMouseMove(e) {
  if (!dragging.value || !originalImage.value) return
  const dx = e.clientX - dragStart.value.mx
  const dy = e.clientY - dragStart.value.my

  const img = originalImage.value
  const currentW = img.width * imgTransform.scale
  const currentH = img.height * imgTransform.scale

  let newX = dragStart.value.x + dx
  let newY = dragStart.value.y + dy

  const minX = containerSize - currentW
  const minY = containerSize - currentH
  const maxX = 0
  const maxY = 0

  imgTransform.x = Math.max(minX, Math.min(maxX, newX))
  imgTransform.y = Math.max(minY, Math.min(maxY, newY))
}

function onMouseUp() {
  dragging.value = false
}

function onScaleChange(e) {
  if (!originalImage.value) return
  const newScale = parseFloat(e.target.value)
  
  const img = originalImage.value
  const oldW = img.width * imgTransform.scale
  const oldH = img.height * imgTransform.scale
  const newW = img.width * newScale
  const newH = img.height * newScale

  const centerX = imgTransform.x + oldW / 2
  const centerY = imgTransform.y + oldH / 2

  let newX = centerX - newW / 2
  let newY = centerY - newH / 2

  const minX = containerSize - newW
  const minY = containerSize - newH
  
  imgTransform.x = Math.max(minX, Math.min(0, newX))
  imgTransform.y = Math.max(minY, Math.min(0, newY))
  imgTransform.scale = newScale
}

async function handleSave() {
  if (!originalImage.value || !imageSrc.value) return
  loading.value = true
  errorMsg.value = ''

  try {
    const img = originalImage.value
    const sx = -imgTransform.x / imgTransform.scale
    const sy = -imgTransform.y / imgTransform.scale
    const sw = containerSize / imgTransform.scale
    const sh = containerSize / imgTransform.scale

    const canvas = document.createElement('canvas')
    const OUTPUT = 400
    canvas.width = OUTPUT
    canvas.height = OUTPUT
    const ctx = canvas.getContext('2d')

    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, OUTPUT, OUTPUT)

    canvas.toBlob((blob) => {
      if (!blob) {
        errorMsg.value = 'Không thể xử lý ảnh.'
        loading.value = false
        return
      }
      const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
      emit('save', file)
    }, 'image/jpeg', 0.9)

  } catch (err) {
    errorMsg.value = 'Có lỗi xảy ra. Vui lòng thử lại.'
    loading.value = false
  }
}
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('cancel')">
    <div class="modal-box">
      <div class="modal-header">
        <span>Chọn ảnh đại diện</span>
        <button class="btn-close" @click="$emit('cancel')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <div class="modal-body">
        <div 
          class="crop-container" 
          :style="{ width: containerSize + 'px', height: containerSize + 'px' }"
        >
          <img
            v-if="imageSrc"
            :src="imageSrc"
            class="draggable-image"
            :style="{
              transform: `translate(${imgTransform.x}px, ${imgTransform.y}px) scale(${imgTransform.scale})`,
              cursor: dragging ? 'grabbing' : 'grab'
            }"
            @mousedown="startDrag"
            alt="Upload preview"
          />
          
          <div v-else class="crop-placeholder" @click="triggerPick">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/>
              <polyline points="21 15 16 10 5 21"/>
            </svg>
            <p>Bấm vào đây để chọn ảnh</p>
          </div>

          <div v-if="imageSrc" class="fixed-crop-frame" :style="{ width: containerSize + 'px', height: containerSize + 'px' }">
            <div class="grid-line-h1"></div>
            <div class="grid-line-h2"></div>
            <div class="grid-line-v1"></div>
            <div class="grid-line-v2"></div>
          </div>
        </div>

        <div v-if="imageSrc" class="zoom-control">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/>
          </svg>
          <input 
            type="range" 
            :min="imgTransform.minScale" 
            :max="imgTransform.maxScale" 
            step="0.01" 
            :value="imgTransform.scale"
            @input="onScaleChange"
            class="zoom-slider"
          />
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/>
          </svg>
        </div>

        <button class="btn-pick" @click="triggerPick">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          Chọn ảnh khác từ máy tính
        </button>
        <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
      </div>

      <div class="modal-footer">
        <button class="btn-secondary" @click="$emit('cancel')">Hủy</button>
        <button class="btn-primary" :disabled="!imageSrc || loading" @click="handleSave">
          {{ loading ? 'Đang lưu...' : 'Lưu ảnh' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  backdrop-filter: blur(2px);
}

.modal-box {
  background: #fff;
  border-radius: 18px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.15);
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.btn-close {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
}
.btn-close:hover { color: #1e293b; }

.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.crop-container {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: #0f172a;
  user-select: none;
}

.draggable-image {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: 0 0;
  user-select: none;
  pointer-events: auto;
}

.crop-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #94a3b8;
  background: #f8fafc;
  cursor: pointer;
  border: 2px dashed #cbd5e1;
  border-radius: 16px;
}
.crop-placeholder p { font-size: 13px; margin: 0; }

.fixed-crop-frame {
  position: absolute;
  top: 0;
  left: 0;
  border: 2px solid #ffffff;
  border-radius: 16px;
  pointer-events: none;
  box-sizing: border-box;
}

.grid-line-h1 { position: absolute; top: 33.33%; left: 0; right: 0; height: 1px; background: rgba(255,255,255,0.2); }
.grid-line-h2 { position: absolute; top: 66.66%; left: 0; right: 0; height: 1px; background: rgba(255,255,255,0.2); }
.grid-line-v1 { position: absolute; left: 33.33%; top: 0; bottom: 0; width: 1px; background: rgba(255,255,255,0.2); }
.grid-line-v2 { position: absolute; left: 66.66%; top: 0; bottom: 0; width: 1px; background: rgba(255,255,255,0.2); }

.zoom-control {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 0 10px;
  color: #64748b;
}

.zoom-slider {
  flex: 1;
  accent-color: #2f5fd0;
  cursor: pointer;
  height: 6px;
}

.btn-pick {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  border-radius: 10px;
  padding: 8px 16px;
  font-family: 'Quicksand', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-pick:hover { background: #f8fafc; }

.error-msg {
  font-size: 13px;
  color: #dc2626;
  margin: 0;
  text-align: center;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.btn-secondary {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}
.btn-secondary:hover { background: #f1f5f9; }

.btn-primary {
  padding: 8px 18px;
  background: #2f5fd0;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}
.btn-primary:hover:not(:disabled) { background: #254eb2; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
</style>