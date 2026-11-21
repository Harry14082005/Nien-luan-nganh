<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { useChatStore } from '@/stores/chat'

// Tải thư viện mammoth từ public/libs khi cần đọc file docx (tránh phình bundle build)
const loadMammoth = () => {
  if (typeof window !== 'undefined' && window.mammoth) return Promise.resolve(window.mammoth)
  return new Promise((resolve, reject) => {
    const existing = document.getElementById('mammoth-script')
    if (existing) {
      if (window.mammoth) return resolve(window.mammoth)
      existing.addEventListener('load', () => resolve(window.mammoth))
      existing.addEventListener('error', reject)
      return
    }
    const script = document.createElement('script')
    script.id = 'mammoth-script'
    script.src = '/libs/mammoth.browser.min.js'
    script.onload = () => resolve(window.mammoth)
    script.onerror = () => reject(new Error('Không thể tải thư viện đọc DOCX'))
    document.head.appendChild(script)
  })
}

// =============================================================================
// PROPS & EMITS
// =============================================================================
const props = defineProps({
  file: {
    type: Object,
    required: true,
  },
  roomId: {
    type: [String, Number],
    default: null,
  },
})

const emit = defineEmits(['close', 'file-resent'])
const chatStore = useChatStore()

// =============================================================================
// 1. STATE & ĐIỀU CHỈNH KÍCH THƯỚC (RESIZABLE & FULLSCREEN)
// =============================================================================
const sidebarWidth = ref(Number(localStorage.getItem('filePreviewSidebarWidth')) || 580)
const isResizing = ref(false)
const isFullscreen = ref(false)

const widthPercent = computed(() => {
  if (isFullscreen.value) return 100
  return Math.min(100, Math.round((sidebarWidth.value / window.innerWidth) * 100))
})

// Thu phóng cỡ chữ hiển thị nội dung (Content Text Zoom %)
const contentZoom = ref(Number(localStorage.getItem('filePreviewContentZoom')) || 100)
const showZoomMenu = ref(false)
const zoomControlRef = ref(null)

const setContentZoom = (zoom) => {
  contentZoom.value = Math.max(70, Math.min(220, zoom))
  localStorage.setItem('filePreviewContentZoom', String(contentZoom.value))
  showZoomMenu.value = false
}

const adjustContentZoom = (delta) => {
  const target = Math.max(70, Math.min(220, contentZoom.value + delta))
  setContentZoom(target)
}

const bodyZoomStyle = computed(() => {
  const factor = contentZoom.value / 100
  return {
    '--code-font-size': `${Math.round(13 * factor * 10) / 10}px`,
    '--code-line-height': `${Math.round(20 * factor * 10) / 10}px`,
    '--md-font-size': `${Math.round(14.5 * factor * 10) / 10}px`,
    '--csv-font-size': `${Math.round(13 * factor * 10) / 10}px`,
  }
})

const handleDocClick = (e) => {
  if (zoomControlRef.value && !zoomControlRef.value.contains(e.target)) {
    showZoomMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocClick)
})

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

const startResize = (e) => {
  if (isFullscreen.value) return
  e.preventDefault()
  isResizing.value = true
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'

  const onMouseMove = (moveEvent) => {
    const newWidth = window.innerWidth - moveEvent.clientX
    const minWidth = 380
    const maxWidth = Math.min(window.innerWidth * 0.85, 1200)
    if (newWidth >= minWidth && newWidth <= maxWidth) {
      sidebarWidth.value = Math.round(newWidth)
    }
  }

  const onMouseUp = () => {
    isResizing.value = false
    document.body.style.userSelect = ''
    document.body.style.cursor = ''
    window.removeEventListener('mousemove', onMouseMove)
    window.removeEventListener('mouseup', onMouseUp)
    localStorage.setItem('filePreviewSidebarWidth', String(sidebarWidth.value))
  }

  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
}

// =============================================================================
// 2. PHÂN LOẠI ĐỊNH DẠNG TỆP & KHẢ NĂNG CHỈNH SỬA
// =============================================================================
const fileName = computed(() => props.file?.fileName || 'Tài liệu')
const fileUrl = computed(() => props.file?.fileUrl || '')
const fileSize = computed(() => props.file?.fileSize || '')

const fileExtension = computed(() => {
  if (props.file?.fileExtension) {
    return String(props.file.fileExtension).toLowerCase().trim()
  }
  const storageKey = props.file?.storageKey || props.file?.fileUrl || ''
  const keyParts = storageKey.split('?')[0].split('/').pop()?.split('.') || []
  if (keyParts.length > 1) {
    return keyParts.pop().toLowerCase()
  }
  const nameParts = String(fileName.value).toLowerCase().split('.')
  return nameParts.length > 1 ? nameParts.pop() : ''
})

const isPdf = computed(() => fileExtension.value === 'pdf')
const isDocx = computed(() => fileExtension.value === 'docx')
const isLegacyDoc = computed(() => fileExtension.value === 'doc')
const isCsv = computed(() => fileExtension.value === 'csv')
const isMarkdown = computed(() => ['md', 'markdown'].includes(fileExtension.value))
const isJson = computed(() => fileExtension.value === 'json')
const isCode = computed(() =>
  [
    'js', 'ts', 'jsx', 'tsx', 'html', 'css', 'scss', 'vue',
    'java', 'py', 'c', 'cpp', 'cs', 'php', 'sql', 'sh', 'bat',
    'xml', 'yaml', 'yml'
  ].includes(fileExtension.value)
)

// PDF và DOCX là dạng xem tài liệu; các tệp văn bản / code / json / csv cho phép sửa và gửi lại
const isEditable = computed(() => !isPdf.value && !isDocx.value && !isLegacyDoc.value)

// Chế độ hiện tại: 'preview' (Xem) | 'edit' (Sửa)
const activeMode = ref('preview')

// State dữ liệu nội dung
const rawContent = ref('')
const editableContent = ref('')
const docxHtml = ref('')
const pdfBlobUrl = ref('')
const isLoading = ref(false)
const loadError = ref('')
const isResending = ref(false)
const resendNote = ref('')
const resendSuccess = ref(false)

// Ref hỗ trợ cuộn đồng bộ hàng số, highlight cú pháp và textarea
const editorTextareaRef = ref(null)
const editorHighlightRef = ref(null)
const lineGutterRef = ref(null)

// Tải nội dung tệp tin từ server
const loadFileContent = async () => {
  if (!fileUrl.value) return
  isLoading.value = true
  loadError.value = ''

  // Dọn dẹp URL Blob cũ nếu có
  if (pdfBlobUrl.value) {
    URL.revokeObjectURL(pdfBlobUrl.value)
    pdfBlobUrl.value = ''
  }

  try {
    // 1. Nếu là tệp PDF: Tải dạng Blob để tạo URL cục bộ blob:... (Tránh chặn X-Frame-Options trên mọi trình duyệt)
    if (isPdf.value) {
      const res = await fetch(fileUrl.value)
      if (!res.ok) throw new Error('Không thể tải tệp PDF')
      const blob = await res.blob()
      pdfBlobUrl.value = URL.createObjectURL(blob)
      return
    }

    // 2. Nếu là tệp Word DOCX: Sử dụng thư viện mammoth để parse thành HTML
    if (isDocx.value) {
      const mammothLib = await loadMammoth()
      const res = await fetch(fileUrl.value)
      if (!res.ok) throw new Error('Không thể tải tệp Word')
      const arrayBuffer = await res.arrayBuffer()
      const result = await mammothLib.convertToHtml({ arrayBuffer })
      docxHtml.value = result.value
      return
    }

    // 3. Nếu là tệp .doc cũ (Word 97-2003 nhị phân)
    if (isLegacyDoc.value) {
      loadError.value = 'Tệp .doc định dạng cũ không hỗ trợ xem trực tiếp trên trình duyệt. Vui lòng bấm Tải về để mở bằng Microsoft Word.'
      return
    }

    // 4. Các tệp văn bản, code, CSV, JSON, Markdown: Đọc raw text
    const res = await fetch(fileUrl.value)
    if (!res.ok) throw new Error('Không thể tải tệp tin')
    const text = await res.text()
    rawContent.value = text
    editableContent.value = text
  } catch (err) {
    console.error('Lỗi tải tệp xem trước:', err)
    loadError.value = 'Không thể tải nội dung tệp tin để xem trước.'
  } finally {
    isLoading.value = false
  }
}

watch(
  () => props.file?.fileUrl,
  () => {
    activeMode.value = 'preview'
    resendNote.value = ''
    resendSuccess.value = false
    loadFileContent()
  },
  { immediate: true }
)

onUnmounted(() => {
  document.removeEventListener('click', handleDocClick)
  if (pdfBlobUrl.value) {
    URL.revokeObjectURL(pdfBlobUrl.value)
  }
})

// =============================================================================
// 3. PARSER NỘI DUNG (CSV, JSON, MARKDOWN CHUẨN XÁC, CODE)
// =============================================================================

// Parser bảng CSV
const parsedCsvData = computed(() => {
  if (!isCsv.value || !rawContent.value) return { headers: [], rows: [] }
  const lines = rawContent.value
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean)
  if (!lines.length) return { headers: [], rows: [] }

  const parseRow = (line) => {
    const cells = []
    let current = ''
    let insideQuotes = false
    for (let i = 0; i < line.length; i++) {
      const char = line[i]
      if (char === '"') {
        insideQuotes = !insideQuotes
      } else if (char === ',' && !insideQuotes) {
        cells.push(current.trim())
        current = ''
      } else {
        current += char
      }
    }
    cells.push(current.trim())
    return cells
  }

  const headers = parseRow(lines[0])
  const rows = lines.slice(1).map((l) => parseRow(l))
  return { headers, rows }
})

// Định dạng JSON đẹp mắt
const formattedJson = computed(() => {
  if (!isJson.value || !rawContent.value) return rawContent.value
  try {
    const obj = JSON.parse(rawContent.value)
    return JSON.stringify(obj, null, 2)
  } catch {
    return rawContent.value
  }
})

// Bộ dòng hiển thị cho chế độ Code
const contentLines = computed(() => {
  const text = isJson.value ? formattedJson.value : rawContent.value
  return text.split('\n')
})

// =============================================================================
// TÔ MÀU CÚ PHÁP MÃ NGUỒN (SYNTAX HIGHLIGHTING THEO NGÔN NGỮ LẬP TRÌNH)
// =============================================================================
const getLanguageRules = (ext) => {
  let keywords = [
    'const', 'let', 'var', 'function', 'return', 'if', 'else', 'for', 'while', 'do',
    'switch', 'case', 'default', 'break', 'continue', 'try', 'catch', 'finally', 'throw',
    'new', 'this', 'typeof', 'instanceof', 'void', 'delete', 'in', 'of', 'class', 'extends',
    'super', 'import', 'export', 'from', 'as', 'async', 'await', 'yield'
  ]

  if (ext === 'py') {
    keywords = [
      'def', 'class', 'return', 'if', 'elif', 'else', 'for', 'while', 'try', 'except',
      'finally', 'raise', 'with', 'as', 'import', 'from', 'yield', 'lambda', 'pass',
      'break', 'continue', 'in', 'is', 'not', 'and', 'or', 'global', 'nonlocal', 'assert',
      'async', 'await'
    ]
  } else if (['java', 'c', 'cpp', 'cs'].includes(ext)) {
    keywords = [
      'public', 'private', 'protected', 'static', 'final', 'abstract', 'synchronized',
      'volatile', 'transient', 'native', 'class', 'interface', 'enum', 'extends',
      'implements', 'package', 'import', 'return', 'if', 'else', 'for', 'while', 'do',
      'switch', 'case', 'default', 'break', 'continue', 'try', 'catch', 'finally', 'throw',
      'throws', 'new', 'this', 'super', 'instanceof', 'void', 'int', 'short', 'long',
      'byte', 'float', 'double', 'char', 'boolean'
    ]
  } else if (ext === 'sql') {
    keywords = [
      'select', 'from', 'where', 'insert', 'into', 'values', 'update', 'set', 'delete',
      'join', 'inner', 'left', 'right', 'full', 'outer', 'on', 'group', 'by', 'order',
      'asc', 'desc', 'having', 'limit', 'offset', 'union', 'all', 'distinct', 'as',
      'create', 'table', 'alter', 'drop', 'index', 'view', 'and', 'or', 'not', 'in',
      'is', 'null', 'like', 'between', 'exists', 'case', 'when', 'then', 'end', 'primary',
      'key', 'foreign', 'references'
    ]
  }

  const booleans = ['true', 'false', 'null', 'undefined', 'True', 'False', 'None']
  return { keywords: new Set(keywords), booleans: new Set(booleans) }
}

const tokenizeCodeLine = (line, ext) => {
  if (!line || !line.trim()) return '&nbsp;'
  const escape = (str) =>
    str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // Nếu không phải là file mã nguồn hay JSON (ví dụ txt, log, env, md, csv...), không dùng syntax highlighting code
  if (!isCode.value && !isJson.value) {
    return escape(line) || '&nbsp;'
  }

  if (ext === 'json') {
    let esc = escape(line)
    esc = esc.replace(/"([^"\\]*(?:\\.[^"\\]*)*)"(?=\s*:)/g, '<span class="tok-json-key">"$1"</span>')
    esc = esc.replace(/:\s*("[^"\\]*(?:\\.[^"\\]*)*")/g, ': <span class="tok-string">$1</span>')
    esc = esc.replace(/\b(true|false|null)\b/g, '<span class="tok-boolean">$1</span>')
    esc = esc.replace(/:\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\b/g, ': <span class="tok-number">$1</span>')
    return esc
  }

  const { keywords, booleans } = getLanguageRules(ext)

  // Token regex nhận diện: Comment, String, Annotation, Number, Function call, Word, Operator, Whitespace, Other
  const tokenRegex =
    /(\/\/[^\r\n]*|#[^\r\n]*|--[^\r\n]*)|("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|`(?:[^`\\]|\\.)*`)|(@[a-zA-Z0-9_]+)|(\b\d+(?:\.\d+)?\b)|(\b[a-zA-Z_$][a-zA-Z0-9_$]*(?=\s*\())|([a-zA-Z_$][a-zA-Z0-9_$]*)|([+\-*/%=<>!&|^~?:;,.]+)|(\s+)|(.)/g

  let match
  let html = ''

  while ((match = tokenRegex.exec(line)) !== null) {
    const [, comment, str, annotation, num, func, word, operator, ws, other] = match

    if (comment) {
      html += `<span class="tok-comment">${escape(comment)}</span>`
    } else if (str) {
      html += `<span class="tok-string">${escape(str)}</span>`
    } else if (annotation) {
      html += `<span class="tok-annotation">${escape(annotation)}</span>`
    } else if (num) {
      html += `<span class="tok-number">${escape(num)}</span>`
    } else if (func) {
      const lower = func.toLowerCase()
      if (keywords.has(lower)) {
        html += `<span class="tok-keyword">${escape(func)}</span>`
      } else {
        html += `<span class="tok-function">${escape(func)}</span>`
      }
    } else if (word) {
      const lower = word.toLowerCase()
      if (keywords.has(lower)) {
        html += `<span class="tok-keyword">${escape(word)}</span>`
      } else if (booleans.has(word) || booleans.has(lower)) {
        html += `<span class="tok-boolean">${escape(word)}</span>`
      } else if (/^[A-Z][a-zA-Z0-9_$]*$/.test(word)) {
        html += `<span class="tok-class">${escape(word)}</span>`
      } else {
        html += `<span class="tok-variable">${escape(word)}</span>`
      }
    } else if (operator) {
      html += `<span class="tok-operator">${escape(operator)}</span>`
    } else if (ws) {
      html += escape(ws)
    } else if (other) {
      html += escape(other)
    }
  }

  return html || '&nbsp;'
}

const highlightedContentLines = computed(() => {
  const text = isJson.value ? formattedJson.value : rawContent.value
  const lines = text.split('\n')
  return lines.map((l) => tokenizeCodeLine(l, fileExtension.value))
})

// Bộ dòng tô màu cú pháp trong chế độ Edit
const highlightedEditLines = computed(() => {
  const text = editableContent.value || ''
  const lines = text.split('\n')
  return lines.map((l) => tokenizeCodeLine(l, fileExtension.value))
})

// Số dòng trong chế độ Edit
const editorLineCount = computed(() => {
  if (!editableContent.value) return 1
  return editableContent.value.split('\n').length
})

// Parser Markdown cải tiến, chuẩn xác và không lỗi layout
const renderedMarkdown = computed(() => {
  if (!isMarkdown.value || !rawContent.value) return ''
  const escapeHtml = (str) =>
    str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  const formatInline = (text) => {
    let t = escapeHtml(text)
    // Chuyển thẻ br được escape thành thẻ HTML thực tế để xuống dòng chính xác
    t = t.replace(/&lt;br\s*\/?&gt;/gi, '<br />')
    // Code inline `code`
    t = t.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>')
    // Image ![alt](url)
    t = t.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" class="md-img" />')
    // Link [text](url)
    t = t.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="md-link">$1</a>')
    // Bold **text** hoặc __text__
    t = t.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    t = t.replace(/__(.+?)__/g, '<strong>$1</strong>')
    // Italic *text* hoặc _text_
    t = t.replace(/\*([^*]+)\*/g, '<em>$1</em>')
    t = t.replace(/_([^_]+)_/g, '<em>$1</em>')
    // Strikethrough ~~text~~
    t = t.replace(/~~(.+?)~~/g, '<del>$1</del>')
    return t
  }

  const lines = rawContent.value.split(/\r?\n/)
  let html = ''
  let inCodeBlock = false
  let codeBuffer = []
  let inUl = false
  let inOl = false
  let inTable = false
  let tableHeaderParsed = false

  const closeLists = () => {
    if (inUl) {
      html += '</ul>'
      inUl = false
    }
    if (inOl) {
      html += '</ol>'
      inOl = false
    }
  }

  const closeTable = () => {
    if (inTable) {
      html += '</tbody></table></div>'
      inTable = false
      tableHeaderParsed = false
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]

    // Khối code ```
    if (line.trim().startsWith('```')) {
      closeLists()
      closeTable()
      if (inCodeBlock) {
        html += `<pre class="md-code-block"><code>${escapeHtml(codeBuffer.join('\n'))}</code></pre>`
        codeBuffer = []
        inCodeBlock = false
      } else {
        inCodeBlock = true
      }
      continue
    }

    if (inCodeBlock) {
      codeBuffer.push(line)
      continue
    }

    // Dòng trống
    if (!line.trim()) {
      closeLists()
      closeTable()
      continue
    }

    // Bảng Markdown (| col | col |)
    if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
      closeLists()
      const cells = line
        .trim()
        .slice(1, -1)
        .split('|')
        .map((c) => c.trim())

      // Dòng phân cách |---|---|
      if (cells.every((c) => /^:?-+:?$/.test(c))) {
        tableHeaderParsed = true
        continue
      }

      if (!inTable) {
        inTable = true
        html += '<div class="md-table-wrap"><table class="md-table"><thead><tr>'
        cells.forEach((c) => {
          html += `<th>${formatInline(c)}</th>`
        })
        html += '</tr></thead><tbody>'
      } else {
        html += '<tr>'
        cells.forEach((c) => {
          html += `<td>${formatInline(c)}</td>`
        })
        html += '</tr>'
      }
      continue
    } else {
      closeTable()
    }

    // Tiêu đề # từ h1 đến h6
    const headingMatch = line.match(/^(#{1,6})\s+(.+)$/)
    if (headingMatch) {
      closeLists()
      const level = headingMatch[1].length
      html += `<h${level} class="md-h${level}">${formatInline(headingMatch[2])}</h${level}>`
      continue
    }

    // Đường kẻ ngang ---
    if (/^(\*\*\*|---|___)$/.test(line.trim())) {
      closeLists()
      html += '<hr class="md-hr" />'
      continue
    }

    // Trích dẫn >
    if (line.startsWith('>')) {
      closeLists()
      html += `<blockquote class="md-quote">${formatInline(line.replace(/^>\s?/, ''))}</blockquote>`
      continue
    }

    // Danh sách không thứ tự (- hoặc *), hỗ trợ task list checklist [-] [x]
    const ulMatch = line.match(/^[-*]\s+(.+)$/)
    if (ulMatch) {
      if (inOl) {
        html += '</ol>'
        inOl = false
      }
      if (!inUl) {
        html += '<ul class="md-ul">'
        inUl = true
      }
      const itemContent = ulMatch[1]
      const taskMatch = itemContent.match(/^\[([ xX])\]\s*(.*)$/)
      if (taskMatch) {
        const isChecked = taskMatch[1].toLowerCase() === 'x'
        const taskText = taskMatch[2]
        html += `<li class="md-li md-task-item"><input type="checkbox" ${isChecked ? 'checked' : ''} disabled class="md-task-checkbox" /><span>${formatInline(taskText)}</span></li>`
      } else {
        html += `<li class="md-li">${formatInline(itemContent)}</li>`
      }
      continue
    }

    // Danh sách có thứ tự (1., 2.), hỗ trợ task list
    const olMatch = line.match(/^\d+\.\s+(.+)$/)
    if (olMatch) {
      if (inUl) {
        html += '</ul>'
        inUl = false
      }
      if (!inOl) {
        html += '<ol class="md-ol">'
        inOl = true
      }
      const itemContent = olMatch[1]
      const taskMatch = itemContent.match(/^\[([ xX])\]\s*(.*)$/)
      if (taskMatch) {
        const isChecked = taskMatch[1].toLowerCase() === 'x'
        const taskText = taskMatch[2]
        html += `<li class="md-li md-task-item"><input type="checkbox" ${isChecked ? 'checked' : ''} disabled class="md-task-checkbox" /><span>${formatInline(taskText)}</span></li>`
      } else {
        html += `<li class="md-li">${formatInline(itemContent)}</li>`
      }
      continue
    }

    closeLists()

    // Đoạn văn thông thường
    html += `<p class="md-p">${formatInline(line)}</p>`
  }

  closeLists()
  closeTable()

  if (inCodeBlock && codeBuffer.length) {
    html += `<pre class="md-code-block"><code>${escapeHtml(codeBuffer.join('\n'))}</code></pre>`
  }

  return html
})

// Cuộn đồng bộ hàng số và lớp highlight cú pháp với vùng soạn thảo
const handleEditorScroll = (e) => {
  const top = e.target.scrollTop
  const left = e.target.scrollLeft
  if (lineGutterRef.value) {
    lineGutterRef.value.scrollTop = top
  }
  if (editorHighlightRef.value) {
    editorHighlightRef.value.scrollTop = top
    editorHighlightRef.value.scrollLeft = left
  }
}

// Hỗ trợ thụt đầu dòng (2 dấu cách) khi nhấn phím Tab
const handleEditorKeyDown = (e) => {
  if (e.key === 'Tab') {
    e.preventDefault()
    const textarea = e.target
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const val = editableContent.value
    editableContent.value = val.substring(0, start) + '  ' + val.substring(end)
    nextTick(() => {
      textarea.selectionStart = textarea.selectionEnd = start + 2
    })
  }
}

// Khôi phục nội dung gốc khi bấm Hủy sửa
const handleCancelEdit = () => {
  editableContent.value = rawContent.value
  resendNote.value = ''
  activeMode.value = 'preview'
}

// =============================================================================
// 4. GỬI LẠI TỆP ĐÃ SỬA VÀO PHÒNG CHAT (RESEND VIA CHAT STORE)
// =============================================================================
const handleResendFile = async () => {
  const rId = props.roomId || chatStore.activeRoomId
  if (!rId || isResending.value) return

  isResending.value = true
  resendSuccess.value = false

  try {
    // 1. Tạo File Blob từ nội dung đã chỉnh sửa
    const blob = new Blob([editableContent.value], { type: 'text/plain;charset=utf-8' })
    const newFile = new File([blob], fileName.value, { type: 'text/plain;charset=utf-8' })

    // 2. Gửi tệp lên Backend kèm trích dẫn (repliedMessageId = message gốc)
    const originalMsgId = props.file?.messageId || null
    await chatStore.sendFileMessage(rId, newFile, originalMsgId)

    // 3. Nếu người dùng có nhập thêm ghi chú gửi kèm
    if (resendNote.value && resendNote.value.trim()) {
      await chatStore.sendChatMessage(resendNote.value.trim(), originalMsgId)
    }

    resendSuccess.value = true
    emit('file-resent')

    // Tự động đóng sidebar sau 0.8s
    setTimeout(() => {
      emit('close')
    }, 800)
  } catch (err) {
    console.error('Lỗi khi gửi lại tệp đã sửa:', err)
    alert('Không thể gửi lại tệp tin. Vui lòng thử lại.')
  } finally {
    isResending.value = false
  }
}
</script>

<template>
  <aside
    class="file-preview-sidebar"
    :class="{ 'file-preview-sidebar--fullscreen': isFullscreen }"
    :style="isFullscreen ? {} : { width: `${sidebarWidth}px` }"
  >
    <!-- Thanh kéo giãn kích thước bề ngang (Resize Handle) -->
    <div
      v-if="!isFullscreen"
      class="preview-resize-handle"
      :class="{ 'is-active': isResizing }"
      title="Kéo sang trái/phải để điều chỉnh độ rộng"
      @mousedown="startResize"
    >
      <div class="resize-handle-bar"></div>
    </div>

    <!-- Header của Sidebar -->
    <header class="preview-sidebar-header">
      <div class="header-file-info">
        <div class="file-icon-badge" :class="`icon--${fileExtension}`">
          <font-awesome-icon v-if="isPdf" icon="file-lines" />
          <font-awesome-icon v-else-if="isDocx || isLegacyDoc" icon="file-lines" />
          <font-awesome-icon v-else-if="isCsv" icon="table" />
          <font-awesome-icon v-else-if="isCode" icon="file-code" />
          <font-awesome-icon v-else icon="file-lines" />
        </div>
        <div class="file-text-meta">
          <div class="file-title-row">
            <span class="file-name" :title="fileName">{{ fileName }}</span>
            <span class="file-ext-pill">{{ fileExtension.toUpperCase() || 'FILE' }}</span>
          </div>
          <span v-if="fileSize && !fileSize.toLowerCase().includes('đính kèm')" class="file-size-sub">{{ fileSize }}</span>
        </div>
      </div>

      <!-- Nhóm nút điều khiển chế độ Xem / Sửa & Tùy chọn -->
      <div class="header-action-group">
        <!-- Chuyển đổi trực tiếp Xem / Sửa -->
        <div v-if="isEditable && !isLoading && !loadError" class="mode-toggle-pills">
          <button
            type="button"
            class="toggle-pill-btn"
            :class="{ active: activeMode === 'preview' }"
            title="Chế độ xem trước trực tiếp"
            @click="activeMode = 'preview'"
          >
            <font-awesome-icon icon="eye" class="pill-icon" />
            <span>Xem</span>
          </button>
          <button
            type="button"
            class="toggle-pill-btn"
            :class="{ active: activeMode === 'edit' }"
            title="Chế độ chỉnh sửa code/nội dung"
            @click="activeMode = 'edit'"
          >
            <font-awesome-icon icon="pen" class="pill-icon" />
            <span>Sửa</span>
          </button>
        </div>

        <!-- Chỉ số & Nút điều chỉnh Thu phóng % kích thước chữ nội dung -->
        <div ref="zoomControlRef" class="zoom-control-wrapper">
          <button
            type="button"
            class="preview-size-badge preview-size-badge--interactive"
            :class="{ active: showZoomMenu }"
            :title="`Cỡ chữ nội dung: ${contentZoom}% (Bấm để thu phóng)`"
            @click.stop="showZoomMenu = !showZoomMenu"
          >
            <span>{{ contentZoom }}%</span>
            <font-awesome-icon icon="chevron-down" class="zoom-badge-chevron" />
          </button>

          <!-- Dropdown bảng điều khiển thu phóng cỡ chữ -->
          <div v-if="showZoomMenu" class="zoom-dropdown-menu" @click.stop>
            <div class="zoom-stepper-row">
              <button
                type="button"
                class="zoom-step-btn"
                title="Thu nhỏ chữ 10%"
                :disabled="contentZoom <= 70"
                @click="adjustContentZoom(-10)"
              >
                <font-awesome-icon icon="minus" />
              </button>
              <span class="zoom-stepper-val">{{ contentZoom }}%</span>
              <button
                type="button"
                class="zoom-step-btn"
                title="Phóng to chữ 10%"
                :disabled="contentZoom >= 200"
                @click="adjustContentZoom(10)"
              >
                <font-awesome-icon icon="plus" />
              </button>
            </div>
            <div class="zoom-divider"></div>
            <div class="zoom-presets">
              <button
                v-for="preset in [80, 100, 120, 140, 160, 200]"
                :key="preset"
                type="button"
                class="zoom-preset-btn"
                :class="{ active: contentZoom === preset }"
                @click="setContentZoom(preset)"
              >
                {{ preset === 100 ? '100% (Mặc định)' : `${preset}%` }}
              </button>
            </div>
          </div>
        </div>

        <!-- Nút Mở Fullscreen / Thu nhỏ -->
        <button
          type="button"
          class="preview-tool-btn"
          :class="{ active: isFullscreen }"
          :title="isFullscreen ? 'Thu nhỏ về thanh bên' : 'Mở toàn màn hình (Full)'"
          @click="toggleFullscreen"
        >
          <font-awesome-icon :icon="isFullscreen ? 'compress' : 'expand'" />
        </button>

        <!-- Nút tải file về máy -->
        <a
          :href="fileUrl"
          download
          target="_blank"
          rel="noopener noreferrer"
          class="preview-tool-btn"
          title="Tải tệp về máy"
        >
          <font-awesome-icon icon="arrow-down" />
        </a>

        <!-- Nút đóng sidebar -->
        <button
          type="button"
          class="preview-tool-btn btn-close-preview"
          title="Đóng xem trước (Esc)"
          @click="$emit('close')"
        >
          <font-awesome-icon icon="xmark" />
        </button>
      </div>
    </header>

    <!-- Body hiển thị nội dung tệp -->
    <main class="preview-sidebar-body" :style="bodyZoomStyle">
      <!-- 1. Trạng thái đang tải -->
      <div v-if="isLoading" class="preview-state-box">
        <font-awesome-icon icon="spinner" spin class="state-icon spin-icon" />
        <span>Đang tải nội dung tệp tin...</span>
      </div>

      <!-- 2. Trạng thái lỗi tải -->
      <div v-else-if="loadError" class="preview-state-box state--error">
        <font-awesome-icon icon="circle-xmark" class="state-icon text-red" />
        <p class="error-msg">{{ loadError }}</p>
        <div class="error-actions">
          <a
            :href="fileUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="btn-open-fallback"
          >
            Mở trong tab mới
          </a>
          <button type="button" class="btn-retry" @click="loadFileContent">
            <font-awesome-icon icon="arrows-rotate" />
            <span>Thử lại</span>
          </button>
        </div>
      </div>

      <!-- 3. HIỂN THỊ FILE PDF (Sử dụng Blob URL tránh lỗi chặn X-Frame-Options) -->
      <div v-else-if="isPdf" class="pdf-viewer-wrap">
        <iframe
          v-if="pdfBlobUrl"
          :src="pdfBlobUrl"
          class="pdf-frame"
          title="PDF Viewer"
        ></iframe>
      </div>

      <!-- 4. HIỂN THỊ TỆP WORD .DOCX (Render qua thư viện Mammoth) -->
      <div v-else-if="isDocx" class="docx-viewer-wrap">
        <div class="docx-document-paper" v-html="docxHtml"></div>
      </div>

      <!-- 5. CHẾ ĐỘ XEM TRƯỚC (PREVIEW MODE) -->
      <div v-else-if="activeMode === 'preview'" class="preview-content-wrap">
        <!-- 5.1 Bảng CSV xem trực tiếp -->
        <div v-if="isCsv" class="csv-table-container">
          <table class="csv-table">
            <thead>
              <tr>
                <th class="th-index">#</th>
                <th v-for="(h, idx) in parsedCsvData.headers" :key="idx">{{ h }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rIdx) in parsedCsvData.rows" :key="rIdx">
                <td class="td-index">{{ rIdx + 1 }}</td>
                <td v-for="(cell, cIdx) in row" :key="cIdx">{{ cell }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 5.2 Markdown xem trực tiếp dạng định dạng -->
        <div
          v-else-if="isMarkdown"
          class="markdown-container"
          v-html="renderedMarkdown"
        ></div>

        <!-- 5.3 Code / JSON / Text xem trực tiếp có số dòng và tô màu cú pháp -->
        <div v-else class="code-viewer-container">
          <div class="code-gutter">
            <span v-for="lineNum in highlightedContentLines.length" :key="lineNum" class="line-number">{{ lineNum }}</span>
          </div>
          <pre class="code-pre"><code><span v-for="(lineHtml, idx) in highlightedContentLines" :key="idx" class="code-line"><span v-html="lineHtml"></span><br /></span></code></pre>
        </div>
      </div>

      <!-- 6. CHẾ ĐỘ CHỈNH SỬA TRỰC TIẾP (EDIT MODE VỚI CÚ PHÁP TÔ MÀU REAL-TIME) -->
      <div v-else-if="activeMode === 'edit'" class="edit-content-wrap">
        <div class="editor-workspace">
          <!-- Cột số dòng bên trái -->
          <div ref="lineGutterRef" class="code-gutter editor-gutter">
            <span v-for="l in editorLineCount" :key="l" class="line-number">{{ l }}</span>
          </div>

          <!-- Khu vực soạn thảo có lớp màu cú pháp và lớp textarea tương tác đồng bộ -->
          <div class="editor-stage">
            <pre
              ref="editorHighlightRef"
              class="editor-syntax-underlay"
              aria-hidden="true"
            ><code><span v-for="(lineHtml, idx) in highlightedEditLines" :key="idx" class="code-line"><span v-html="lineHtml"></span><br /></span></code></pre>
            <textarea
              ref="editorTextareaRef"
              v-model="editableContent"
              class="editor-textarea-overlay"
              spellcheck="false"
              autocomplete="off"
              autocorrect="off"
              autocapitalize="off"
              @scroll="handleEditorScroll"
              @keydown="handleEditorKeyDown"
            ></textarea>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer trong chế độ Edit-->
    <footer v-if="activeMode === 'edit'" class="preview-sidebar-footer">
      <div v-if="resendSuccess" class="resend-success-banner">
        <font-awesome-icon icon="circle-check" />
        <span>Đã gửi tệp mới vào cuộc trò chuyện thành công!</span>
      </div>
      <div v-else class="footer-chatinput-bar">
        <div class="chatinput-pill">
          <input
            v-model="resendNote"
            type="text"
            class="chatinput-field"
            placeholder="Ghi chú gửi kèm file (tùy chọn)..."
            :disabled="isResending"
            @keydown.enter="handleResendFile"
          />
          <button
            type="button"
            class="btn-chatinput-cancel"
            title="Hủy chỉnh sửa"
            :disabled="isResending"
            @click="handleCancelEdit"
          >
            Hủy
          </button>
          <button
            type="button"
            class="btn-chatinput-send"
            :title="isResending ? 'Đang gửi...' : 'Gửi lại vào nhóm'"
            :disabled="isResending"
            @click="handleResendFile"
          >
            <font-awesome-icon v-if="isResending" icon="spinner" spin />
            <font-awesome-icon v-else icon="paper-plane" />
          </button>
        </div>
      </div>
    </footer>
  </aside>
</template>

<style scoped>
/* =============================================================================
   SIDEBAR CHÍNH VÀ CƠ CHẾ RESIZE CO GIÃN / FULLSCREEN
   ============================================================================= */
.file-preview-sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #ffffff;
  border-left: 1px solid #e2e8f0;
  box-shadow: -4px 0 24px rgba(15, 23, 42, 0.06);
  z-index: 40;
  flex-shrink: 0;
  transition: width 0.05s ease-out;
}

/* Chế độ Fullscreen phủ toàn bộ màn hình */
.file-preview-sidebar.file-preview-sidebar--fullscreen {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 9999 !important;
  border-left: none !important;
}

.preview-resize-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  left: -4px;
  width: 8px;
  cursor: col-resize;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease;
}

.resize-handle-bar {
  width: 2px;
  height: 36px;
  background: #cbd5e1;
  border-radius: 2px;
  opacity: 0;
  transition: opacity 0.2s ease, background 0.2s ease;
}

.preview-resize-handle:hover,
.preview-resize-handle.is-active {
  background: rgba(37, 99, 235, 0.08);
}

.preview-resize-handle:hover .resize-handle-bar,
.preview-resize-handle.is-active .resize-handle-bar {
  opacity: 1;
  background: #2563eb;
}

/* =============================================================================
   HEADER SIDEBAR
   ============================================================================= */
.preview-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 70px;
  min-height: 70px;
  max-height: 70px;
  padding: 0 20px;
  border-bottom: 1px solid #e6eaf2;
  background: #ffffff;
  gap: 12px;
  flex-shrink: 0;
  box-sizing: border-box;
}

.header-file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.file-icon-badge {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #3b82f6;
  color: #ffffff;
  font-size: 16px;
  flex-shrink: 0;
}

.file-icon-badge.icon--pdf {
  background: #ef4444;
}

.file-icon-badge.icon--docx,
.file-icon-badge.icon--doc {
  background: #2563eb;
}

.file-icon-badge.icon--csv {
  background: #10b981;
}

.file-icon-badge.icon--json,
.file-icon-badge.icon--js,
.file-icon-badge.icon--java,
.file-icon-badge.icon--py {
  background: #8b5cf6;
}

.file-text-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.file-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.file-name {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-ext-pill {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  background: #e2e8f0;
  color: #475569;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.file-size-sub {
  font-size: 11.5px;
  color: #64748b;
}

/* =============================================================================
   HEADER ACTION GROUP & PILL BUTTONS
   ============================================================================= */
.header-action-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.mode-toggle-pills {
  display: flex;
  align-items: center;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  height: 34px;
  padding: 3px;
  gap: 3px;
  box-sizing: border-box;
}

.toggle-pill-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 10px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
  box-sizing: border-box;
}

.toggle-pill-btn:hover {
  color: #0f172a;
}

.toggle-pill-btn.active {
  background: #ffffff;
  color: #2563eb;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.pill-icon {
  font-size: 11.5px;
}

.preview-size-badge {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  background: #e2e8f0;
  padding: 3px 8px;
  border-radius: 6px;
  user-select: none;
}

/* =============================================================================
   BỘ ĐIỀU CHỈNH THU PHÓNG % (ZOOM CONTROLLER)
   ============================================================================= */
.zoom-control-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.preview-size-badge--interactive {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  padding: 0 10px;
  border-radius: 8px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
  box-sizing: border-box;
}

.preview-size-badge--interactive:hover,
.preview-size-badge--interactive.active {
  background: #e2e8f0;
  color: #0f172a;
  border-color: #cbd5e1;
}

.zoom-badge-chevron {
  font-size: 8.5px;
  transition: transform 0.15s ease;
}

.preview-size-badge--interactive.active .zoom-badge-chevron {
  transform: rotate(180deg);
}

.zoom-dropdown-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 170px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
  padding: 8px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 6px;
  animation: zoomDropFadeIn 0.15s ease;
}

@keyframes zoomDropFadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.zoom-stepper-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 2px 4px;
}

.zoom-step-btn {
  width: 26px;
  height: 26px;
  border: none;
  background: #ffffff;
  border-radius: 4px;
  color: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 11px;
  transition: all 0.15s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.zoom-step-btn:hover:not(:disabled) {
  background: #2563eb;
  color: #ffffff;
}

.zoom-step-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.zoom-stepper-val {
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
}

.zoom-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 2px 0;
}

.zoom-presets {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.zoom-preset-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 8px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 12px;
  color: #334155;
  cursor: pointer;
  transition: all 0.12s ease;
  text-align: left;
}

.zoom-preset-btn:hover {
  background: #f1f5f9;
  color: #2563eb;
}

.zoom-preset-btn.active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
}

.preview-tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.15s ease;
  box-sizing: border-box;
}

.preview-tool-btn:hover {
  background: #f1f5f9;
  color: #2563eb;
  border-color: #cbd5e1;
}

.preview-tool-btn.active {
  background: #eff6ff;
  color: #2563eb;
  border-color: #93c5fd;
}

.btn-close-preview:hover {
  color: #ef4444;
}

/* =============================================================================
   BODY & CÁC TRÌNH RENDER NỘI DUNG
   ============================================================================= */
.preview-sidebar-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.preview-state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: #64748b;
  font-size: 13.5px;
  padding: 24px;
  text-align: center;
}

.state-icon {
  font-size: 28px;
  color: #3b82f6;
}

.state-icon.spin-icon {
  color: #2563eb;
}

.state-icon.text-red {
  color: #ef4444;
}

.error-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.btn-open-fallback {
  padding: 7px 14px;
  border-radius: 8px;
  background: #2563eb;
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.15s;
}

.btn-open-fallback:hover {
  background: #1d4ed8;
}

.btn-retry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-retry:hover {
  background: #f1f5f9;
}

/* PDF Viewer */
.pdf-viewer-wrap {
  width: 100%;
  height: 100%;
}

.pdf-frame {
  width: 100%;
  height: 100%;
  border: none;
}

/* DOCX Viewer */
.docx-viewer-wrap {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  background: #f1f5f9;
  padding: 24px;
  display: flex;
  justify-content: center;
}

.docx-document-paper {
  width: 100%;
  max-width: 820px;
  background: #ffffff;
  padding: 40px 48px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  color: #1e293b;
  font-family: 'Calibri', 'Inter', -apple-system, sans-serif;
  font-size: 15px;
  line-height: 1.7;
}

.docx-document-paper :deep(h1) {
  font-size: 22px;
  font-weight: 700;
  margin: 18px 0 10px;
  color: #0f172a;
}

.docx-document-paper :deep(h2) {
  font-size: 18px;
  font-weight: 600;
  margin: 14px 0 8px;
  color: #0f172a;
}

.docx-document-paper :deep(p) {
  margin: 8px 0;
}

.docx-document-paper :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.docx-document-paper :deep(table td),
.docx-document-paper :deep(table th) {
  border: 1px solid #cbd5e1;
  padding: 8px 12px;
}

/* Preview Content Wrappers */
.preview-content-wrap {
  width: 100%;
  height: 100%;
  overflow: auto;
}

/* Bảng CSV */
.csv-table-container {
  width: 100%;
  height: 100%;
  overflow: auto;
  padding: 12px;
}

.csv-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--csv-font-size, 13px);
  border: 1px solid #e2e8f0;
}

.csv-table th,
.csv-table td {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  text-align: left;
  white-space: nowrap;
}

.csv-table thead th {
  position: sticky;
  top: 0;
  background: #f1f5f9;
  font-weight: 600;
  color: #0f172a;
  z-index: 2;
}

.th-index,
.td-index {
  width: 40px;
  text-align: center !important;
  color: #94a3b8;
  background: #f8fafc;
}

.csv-table tbody tr:hover {
  background: #f8fafc;
}

/* Markdown Container */
.markdown-container {
  padding: 24px 28px;
  font-size: var(--md-font-size, 14.5px);
  line-height: 1.7;
  color: #1e293b;
  overflow-y: auto;
  height: 100%;
}

:deep(.md-h1) {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 8px;
  margin: 20px 0 12px;
}

:deep(.md-h2) {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 16px 0 8px;
}

:deep(.md-h3) {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 14px 0 6px;
}

:deep(.md-h4) {
  font-size: 14.5px;
  font-weight: 600;
  color: #334155;
  margin: 10px 0 4px;
}

:deep(.md-h5) {
  font-size: 13.5px;
  font-weight: 600;
  color: #475569;
  margin: 8px 0 4px;
}

:deep(.md-h6) {
  font-size: 12.5px;
  font-weight: 600;
  color: #64748b;
  margin: 6px 0 4px;
}

:deep(.md-p) {
  margin: 8px 0;
}

:deep(.md-ul),
:deep(.md-ol) {
  padding-left: 24px;
  margin: 8px 0;
}

:deep(.md-li) {
  margin: 4px 0;
}

:deep(.md-task-item) {
  list-style-type: none;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: -18px;
}

:deep(.md-task-checkbox) {
  margin: 0;
  cursor: default;
  accent-color: #2563eb;
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}

:deep(.md-img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 8px 0;
  display: block;
}

:deep(.md-hr) {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 20px 0;
}

:deep(.md-quote) {
  border-left: 3px solid #3b82f6;
  padding-left: 14px;
  margin: 10px 0;
  color: #475569;
  background: #f8fafc;
  padding-top: 4px;
  padding-bottom: 4px;
}

:deep(.md-inline-code) {
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 12.5px;
  color: #2563eb;
}

:deep(.md-code-block) {
  background: #0f172a;
  color: #e2e8f0;
  padding: 14px 18px;
  border-radius: 8px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  overflow-x: auto;
  margin: 12px 0;
}

:deep(.md-link) {
  color: #2563eb;
  text-decoration: underline;
}

:deep(.md-table-wrap) {
  overflow-x: auto;
  margin: 14px 0;
}

:deep(.md-table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  border: 1px solid #cbd5e1;
}

:deep(.md-table th),
:deep(.md-table td) {
  border: 1px solid #cbd5e1;
  padding: 8px 12px;
  text-align: left;
}

:deep(.md-table th) {
  background: #f1f5f9;
  font-weight: 600;
  color: #0f172a;
}

/* Code Viewer Container (Gutter + Code) */
.code-viewer-container {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: auto;
  background: #0f172a;
  color: #e2e8f0;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: var(--code-font-size, 13px);
  line-height: var(--code-line-height, 20px);
}

.code-gutter {
  display: flex;
  flex-direction: column;
  padding: 12px 8px;
  background: #090d16;
  color: #475569;
  user-select: none;
  text-align: right;
  min-width: 44px;
  border-right: 1px solid #1e293b;
}

.line-number {
  height: var(--code-line-height, 20px);
  line-height: var(--code-line-height, 20px);
  font-size: calc(var(--code-font-size, 13px) * 0.9);
}

.code-pre {
  margin: 0;
  padding: 12px 16px;
  flex: 1;
  overflow-x: auto;
  white-space: pre;
}

.code-line {
  height: var(--code-line-height, 20px);
  line-height: var(--code-line-height, 20px);
  display: block;
}

/* =============================================================================
   CHẾ ĐỘ SỬA (EDITOR)
   ============================================================================= */
.edit-content-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0f172a;
}

.editor-workspace {
  display: flex;
  flex: 1;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.editor-gutter {
  overflow: hidden;
}

.editor-stage {
  position: relative;
  flex: 1;
  height: 100%;
  overflow: hidden;
  background: #0f172a;
}

.editor-syntax-underlay,
.editor-textarea-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 12px 16px;
  border: none;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: var(--code-font-size, 13px);
  line-height: var(--code-line-height, 20px);
  tab-size: 2;
  box-sizing: border-box;
  white-space: pre;
  word-wrap: normal;
}

.editor-syntax-underlay {
  pointer-events: none;
  background: transparent;
  color: #e2e8f0;
  z-index: 1;
  overflow: hidden;
}

.editor-syntax-underlay code {
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
}

.editor-textarea-overlay {
  background: transparent;
  color: transparent;
  caret-color: #38bdf8;
  resize: none;
  outline: none;
  overflow: auto;
  z-index: 2;
}

.editor-textarea-overlay::selection {
  background: rgba(56, 189, 248, 0.35);
  color: transparent;
}

/* =============================================================================
   FOOTER CHẾ ĐỘ SỬA: THANH GỬI LẠI PHONG CÁCH CHATINPUT
   ============================================================================= */
.preview-sidebar-footer {
  padding: 10px 16px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
  flex-shrink: 0;
}

.resend-success-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  background: #f0fdf4;
  color: #16a34a;
  font-size: 13px;
  font-weight: 600;
}

.footer-chatinput-bar {
  display: flex;
  align-items: center;
  width: 100%;
}

.chatinput-pill {
  display: flex;
  align-items: center;
  width: 100%;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 28px;
  padding: 4px 6px 4px 16px;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.chatinput-pill:focus-within {
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.chatinput-field {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: #0f172a;
  min-width: 0;
  padding: 6px 0;
}

.btn-chatinput-cancel {
  padding: 5px 12px;
  border-radius: 16px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.btn-chatinput-cancel:hover:not(:disabled) {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-chatinput-send {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: #2563eb;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
}

.btn-chatinput-send:hover:not(:disabled) {
  background: #1d4ed8;
  transform: scale(1.04);
}

.btn-chatinput-send:disabled,
.btn-chatinput-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* =============================================================================
   MÀU SẮC TÔ CÚ PHÁP MÃ NGUỒN (SYNTAX HIGHLIGHTING)
   ============================================================================= */
:deep(.tok-keyword) {
  color: #ff7b72;
  font-weight: 600;
}
:deep(.tok-string) {
  color: #a5d6ff;
}
:deep(.tok-number) {
  color: #79c0ff;
  font-weight: 500;
}
:deep(.tok-comment) {
  color: #8b949e;
  font-style: italic;
}
:deep(.tok-function) {
  color: #d2a8ff;
  font-weight: 600;
}
:deep(.tok-class) {
  color: #ffa657;
  font-weight: 600;
}
:deep(.tok-variable) {
  color: #e6edf3;
}
:deep(.tok-operator) {
  color: #79c0ff;
}
:deep(.tok-boolean) {
  color: #ff7b72;
  font-weight: 600;
}
:deep(.tok-annotation) {
  color: #e3b341;
  font-weight: 500;
}
:deep(.tok-json-key) {
  color: #7ee787;
  font-weight: 600;
}
</style>
