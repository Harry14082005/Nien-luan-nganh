<script setup>
import { ref, computed, watch } from 'vue'
import { useChatStore } from '@/stores/chat'

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false,
  },
  roomId: {
    type: String,
    default: '',
  },
  initialData: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close', 'created'])

const chatStore = useChatStore()

const question = ref('')
const options = ref(['', '']) // Mặc định 2 phương án trống
const sourceMessageIds = ref([])
const isAiPrefilled = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

watch(
  () => props.isOpen,
  (open) => {
    if (open) {
      errorMessage.value = ''
      if (props.initialData) {
        question.value = props.initialData.question || ''
        const initOpts = Array.isArray(props.initialData.options) ? props.initialData.options : []
        if (initOpts.length >= 2) {
          options.value = [...initOpts]
        } else if (initOpts.length === 1) {
          options.value = [initOpts[0], '']
        } else {
          options.value = ['', '']
        }
        sourceMessageIds.value = props.initialData.sourceMessageIds || []
        isAiPrefilled.value = Boolean(props.initialData.isAiGenerated)
      } else {
        question.value = ''
        options.value = ['', '']
        sourceMessageIds.value = []
        isAiPrefilled.value = false
      }
    }
  },
  { immediate: true },
)

const canAddOption = computed(() => {
  return options.value.length < 10
})

const addOptionField = () => {
  if (canAddOption.value) {
    options.value.push('')
  }
}

const removeOptionField = (index) => {
  if (options.value.length > 2) {
    options.value.splice(index, 1)
  }
}

const handleClose = () => {
  question.value = ''
  options.value = ['', '']
  sourceMessageIds.value = []
  isAiPrefilled.value = false
  errorMessage.value = ''
  emit('close')
}

const handleSubmit = async () => {
  errorMessage.value = ''
  const trimmedQ = question.value.trim()
  if (!trimmedQ) {
    errorMessage.value = 'Vui lòng nhập câu hỏi bình chọn'
    return
  }

  const validOptions = options.value.map((o) => o.trim()).filter(Boolean)
  if (validOptions.length < 2) {
    errorMessage.value = 'Vui lòng nhập ít nhất 2 phương án lựa chọn'
    return
  }

  // Kiểm tra trùng lặp option
  const lowerSet = new Set(validOptions.map((o) => o.toLowerCase()))
  if (lowerSet.size !== validOptions.length) {
    errorMessage.value = 'Các phương án lựa chọn không được trùng lặp'
    return
  }

  const targetRoomId = props.roomId || chatStore.activeRoomId
  if (!targetRoomId) {
    errorMessage.value = 'Không xác định được phòng trò chuyện'
    return
  }

  try {
    submitting.value = true
    const res = await chatStore.createRoomPoll(targetRoomId, {
      question: trimmedQ,
      options: validOptions,
      sourceMessageIds:
        sourceMessageIds.value && sourceMessageIds.value.length > 0
          ? sourceMessageIds.value
          : null,
    })
    emit('created', res)
    handleClose()
  } catch (err) {
    console.error('Lỗi khi tạo bình chọn:', err)
    errorMessage.value = err?.response?.data?.message || 'Có lỗi xảy ra khi tạo bình chọn'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="isOpen" class="modal-backdrop" @click.self="handleClose">
    <div class="modal-container">
      <!-- Modal Header -->
      <div class="modal-header">
        <div class="header-title">
          <font-awesome-icon icon="chart-simple" class="header-icon" />
          <span>Tạo cuộc bình chọn</span>
        </div>
        <button class="btn-close" @click="handleClose">
          <font-awesome-icon icon="xmark" />
        </button>
      </div>

      <!-- Modal Body -->
      <div class="modal-body">
        <div v-if="errorMessage" class="error-banner">
          {{ errorMessage }}
        </div>

        <!-- Huy hiệu thông báo gợi ý bởi AI -->
        <div v-if="isAiPrefilled" class="poll-ai-badge">
          ✨ Gợi ý bởi AI từ nội dung thảo luận
        </div>

        <!-- Câu hỏi bình chọn -->
        <div class="form-group">
          <label class="form-label">
            Câu hỏi bình chọn <span class="required">*</span>
          </label>
          <input
            v-model="question"
            type="text"
            class="form-input"
            placeholder="Ví dụ: Cuối tuần này cả nhóm đi ăn ở đâu?"
            maxlength="255"
            autofocus
            @keydown.enter.prevent
          />
        </div>

        <!-- Các phương án lựa chọn -->
        <div class="form-group">
          <label class="form-label">
            Các phương án lựa chọn <span class="required">*</span>
          </label>
          <div class="options-wrapper">
            <div
              v-for="(opt, idx) in options"
              :key="idx"
              class="option-input-row"
            >
              <span class="option-index">{{ idx + 1 }}</span>
              <input
                v-model="options[idx]"
                type="text"
                class="form-input option-input"
                :placeholder="`Lựa chọn ${idx + 1}`"
                maxlength="100"
              />
              <button
                v-if="options.length > 2"
                type="button"
                class="btn-remove-opt"
                title="Xóa lựa chọn này"
                @click="removeOptionField(idx)"
              >
                <font-awesome-icon icon="trash-can" />
              </button>
            </div>
          </div>

          <button
            v-if="canAddOption"
            type="button"
            class="btn-add-opt-field"
            @click="addOptionField"
          >
            <font-awesome-icon icon="plus" class="btn-icon" />
            <span>Thêm phương án (tối đa 10)</span>
          </button>
        </div>
      </div>

      <!-- Modal Footer -->
      <div class="modal-footer">
        <button type="button" class="btn-cancel" @click="handleClose">
          Hủy
        </button>
        <button
          type="button"
          class="btn-submit"
          :disabled="submitting || !question.trim()"
          @click="handleSubmit"
        >
          <font-awesome-icon v-if="submitting" icon="spinner" spin class="btn-icon" />
          <span>Tạo bình chọn</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  animation: fadeIn 0.15s ease-out;
}

.modal-container {
  width: 100%;
  max-width: 480px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Quicksand', sans-serif;
  animation: popIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-container input,
.modal-container button {
  font-family: 'Quicksand', sans-serif;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
}

.header-icon {
  color: #2563eb;
  font-size: 18px;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 16px;
  color: #64748b;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: color 0.15s;
}

.btn-close:hover {
  color: #0f172a;
}

.modal-body {
  padding: 20px 24px;
  max-height: 70vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error-banner {
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fee2e2;
  color: #ef4444;
  font-size: 13px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
}

.required {
  color: #ef4444;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  outline: none;
  transition: border-color 0.15s;
}

.form-input:focus {
  border-color: #2563eb;
}

.options-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-index {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  width: 16px;
  text-align: center;
}

.option-input {
  flex: 1;
}

.btn-remove-opt {
  background: transparent;
  border: none;
  color: #94a3b8;
  padding: 6px;
  cursor: pointer;
  border-radius: 6px;
  transition: color 0.15s;
}

.btn-remove-opt:hover {
  color: #ef4444;
}

.btn-add-opt-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px dashed #bfdbfe;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 4px;
  transition: all 0.15s;
}

.btn-add-opt-field:hover {
  background: #dbeafe;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

.btn-cancel {
  padding: 8px 16px;
  font-size: 13.5px;
  font-weight: 500;
  color: #475569;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #f1f5f9;
}

.btn-submit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  font-size: 13.5px;
  font-weight: 600;
  color: #ffffff;
  background: #2563eb;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.15s;
}

.btn-submit:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.poll-ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #eff6ff 0%, #e0e7ff 100%);
  color: #3b82f6;
  border: 1px solid #bfdbfe;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
  margin-bottom: 3px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
</style>