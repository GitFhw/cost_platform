<template>
  <div
    ref="rootRef"
    class="json-editor"
    :class="{
      'is-readonly': readonly,
      'is-disabled': disabled,
      'is-compact': compact,
      'is-fullscreen': isFullscreen
    }"
  >
    <div v-if="toolbar" class="json-editor__toolbar">
      <div class="json-editor__title">
        <span>{{ titleText }}</span>
        <el-tag v-if="statusText" :type="statusType" size="small">{{ statusText }}</el-tag>
        <el-tag size="small" effect="plain">{{ languageLabel }}</el-tag>
      </div>
      <div class="json-editor__actions">
        <el-button v-if="!isReadOnly && isJsonLanguage" link type="primary" @click="handleFormat">格式化</el-button>
        <el-button v-if="!isReadOnly && isJsonLanguage" link type="primary" @click="handleMinify">压缩</el-button>
        <el-button v-if="!isReadOnly && isJsonLanguage" link type="primary" @click="handleValidate">校验</el-button>
        <el-button link type="primary" @click="handleCopy">复制</el-button>
        <el-button v-if="allowFullscreen" link type="primary" @click="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </el-button>
        <el-button v-if="!isReadOnly && clearable" link type="danger" @click="handleClear">清空</el-button>
      </div>
    </div>

    <div class="json-editor__body" :class="{ 'has-toolbar': toolbar }">
      <div v-show="placeholder && !innerValue" class="json-editor__placeholder">{{ placeholder }}</div>
      <div ref="editorRef" class="json-editor__ace" :style="editorStyle"></div>
    </div>

    <div v-if="visibleMessage || counterText" class="json-editor__footer">
      <div v-if="visibleMessage" class="json-editor__message" :class="`is-${statusType}`">
        {{ visibleMessage }}
      </div>
      <div v-if="counterText" class="json-editor__counter" :class="{ 'is-overflow': isOverflow }">
        {{ counterText }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import ace from 'ace-builds/src-noconflict/ace'
import 'ace-builds/src-noconflict/ext-language_tools'
import 'ace-builds/src-noconflict/mode-json'
import 'ace-builds/src-noconflict/mode-javascript'
import 'ace-builds/src-noconflict/mode-java'
import 'ace-builds/src-noconflict/mode-text'
import 'ace-builds/src-noconflict/theme-chrome'
import 'ace-builds/src-noconflict/theme-tomorrow_night_eighties'
import { formatJsonText, minifyJsonText, normalizeJsonText, parseJsonText } from '@/utils/jsonTools'
import useSettingsStore from '@/store/modules/settings'

const props = defineProps({
  modelValue: {
    type: [String, Object, Array, Number, Boolean],
    default: ''
  },
  title: {
    type: String,
    default: ''
  },
  lang: {
    type: String,
    default: 'json'
  },
  theme: {
    type: String,
    default: ''
  },
  width: {
    type: [String, Number],
    default: '100%'
  },
  height: {
    type: [String, Number],
    default: ''
  },
  rows: {
    type: Number,
    default: 8
  },
  fontSize: {
    type: [String, Number],
    default: 13
  },
  maxLength: {
    type: Number,
    default: undefined
  },
  truncateOnOverflow: {
    type: Boolean,
    default: true
  },
  placeholder: {
    type: String,
    default: '请输入 JSON'
  },
  readonly: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  allowEmpty: {
    type: Boolean,
    default: true
  },
  showWordLimit: {
    type: Boolean,
    default: true
  },
  toolbar: {
    type: Boolean,
    default: true
  },
  clearable: {
    type: Boolean,
    default: true
  },
  compact: {
    type: Boolean,
    default: false
  },
  showStatus: {
    type: Boolean,
    default: true
  },
  validateOnBlur: {
    type: Boolean,
    default: true
  },
  autosize: {
    type: [Boolean, Object],
    default: false
  },
  allowFullscreen: {
    type: Boolean,
    default: true
  },
  options: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'valid', 'parsed'])
const settingsStore = useSettingsStore()

const rootRef = ref()
const editorRef = ref()
const editor = ref()
const innerValue = ref('')
const statusType = ref('info')
const statusText = ref('')
const statusMessage = ref('')
const isFullscreen = ref(false)
let resizeObserver
let syncingEditor = false

const isReadOnly = computed(() => props.readonly || props.disabled)
const isJsonLanguage = computed(() => normalizeLanguage(props.lang) === 'json')
const effectiveTheme = computed(() => props.theme || (settingsStore.isDark ? 'tomorrow_night_eighties' : 'chrome'))
const titleText = computed(() => props.title || (isJsonLanguage.value ? 'JSON 编辑器' : '代码编辑器'))
const languageLabel = computed(() => normalizeLanguage(props.lang).toUpperCase())
const isOverflow = computed(() => Boolean(props.maxLength && innerValue.value.length > props.maxLength))
const counterText = computed(() => {
  if (!props.showWordLimit || !props.maxLength) {
    return ''
  }
  return `${innerValue.value.length} / ${props.maxLength}`
})

const visibleMessage = computed(() => {
  if (!props.showStatus) {
    return ''
  }
  if (props.readonly && statusType.value !== 'danger') {
    return ''
  }
  return statusMessage.value
})

const editorStyle = computed(() => ({
  width: normalizeSize(props.width),
  height: isFullscreen.value ? 'calc(100vh - 178px)' : resolveHeight()
}))

watch(
  () => props.modelValue,
  value => {
    const normalized = limitText(normalizeJsonText(value), false)
    if (normalized !== innerValue.value) {
      setEditorText(normalized, { emitChange: false, reset: true })
    }
  },
  { immediate: true }
)

watch(() => props.lang, () => {
  if (editor.value) {
    editor.value.session.setMode(resolveMode(props.lang))
    clearAnnotations()
  }
})

watch(effectiveTheme, value => {
  editor.value?.setTheme(resolveTheme(value))
})

watch(isReadOnly, value => {
  if (!editor.value) {
    return
  }
  editor.value.setReadOnly(value)
  editor.value.setOptions({
    highlightActiveLine: !value,
    enableBasicAutocompletion: !value,
    enableLiveAutocompletion: !value && Boolean(props.options.enableLiveAutocompletion)
  })
})

watch(
  () => props.options,
  () => {
    applyEditorOptions()
  },
  { deep: true }
)

onMounted(() => {
  initEditor()
  initResizeObserver()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  editor.value?.destroy()
  editor.value?.container?.remove()
})

function initEditor() {
  if (!editorRef.value) {
    return
  }
  editor.value = ace.edit(editorRef.value)
  editor.value.setTheme(resolveTheme(effectiveTheme.value))
  editor.value.session.setMode(resolveMode(props.lang))
  editor.value.session.setUseWorker(false)
  editor.value.session.setUseWrapMode(true)
  editor.value.session.setTabSize(2)
  editor.value.session.setUseSoftTabs(true)
  editor.value.session.setValue(innerValue.value || '')
  editor.value.session.on('change', handleEditorChange)
  editor.value.on('blur', handleBlur)
  editor.value.renderer.setScrollMargin(8, 8, 0, 0)
  editor.value.setReadOnly(isReadOnly.value)
  applyEditorOptions()
  resizeEditor()
}

function applyEditorOptions() {
  if (!editor.value) {
    return
  }
  editor.value.setOptions({
    showPrintMargin: false,
    fontSize: props.fontSize,
    tabSize: 2,
    useSoftTabs: true,
    wrap: true,
    highlightActiveLine: !isReadOnly.value,
    highlightSelectedWord: true,
    displayIndentGuides: true,
    showGutter: true,
    scrollPastEnd: 0.15,
    enableBasicAutocompletion: !isReadOnly.value,
    enableLiveAutocompletion: false,
    enableSnippets: false,
    ...props.options
  })
}

function handleEditorChange() {
  if (syncingEditor || !editor.value) {
    return
  }
  const currentText = editor.value.getValue()
  const nextText = limitText(currentText)
  if (nextText !== currentText) {
    setEditorText(nextText, { emitChange: true, keepStatus: true })
    ElMessage.warning(`内容已超过 ${props.maxLength} 字，已自动截断`)
    return
  }
  innerValue.value = currentText
  if (props.maxLength && currentText.length > props.maxLength) {
    statusType.value = 'warning'
    statusText.value = '超出建议长度'
    statusMessage.value = `当前内容已超过 ${props.maxLength} 字，未自动截断，请确认浏览器与接口处理能力。`
  } else {
    resetStatus()
  }
  clearAnnotations()
  emit('update:modelValue', currentText)
}

function handleFormat() {
  const result = formatJsonText(getCurrentText(), { allowEmpty: props.allowEmpty })
  if (!applyJsonResult(result, '已格式化')) {
    return
  }
  setEditorText(result.text, { emitChange: true, keepStatus: true })
}

function handleMinify() {
  const result = minifyJsonText(getCurrentText(), { allowEmpty: props.allowEmpty })
  if (!applyJsonResult(result, '已压缩')) {
    return
  }
  setEditorText(result.text, { emitChange: true, keepStatus: true })
}

function handleValidate() {
  validateCurrent(true)
}

function handleBlur() {
  if (props.validateOnBlur && !isReadOnly.value && isJsonLanguage.value) {
    validateCurrent(false)
  }
}

function validateCurrent(showToast) {
  const text = getCurrentText()
  const result = parseJsonText(text, { allowEmpty: props.allowEmpty })
  if (!applyJsonResult(result, '格式正确', text)) {
    return false
  }
  if (showToast) {
    ElMessage.success(result.empty ? 'JSON 为空，已按允许为空处理' : 'JSON 格式正确')
  }
  return true
}

function applyJsonResult(result, successText, sourceText = getCurrentText()) {
  if (!result.valid) {
    statusType.value = 'danger'
    statusText.value = '校验失败'
    statusMessage.value = result.detailMessage || result.message
    emit('valid', false)
    applyErrorAnnotation(sourceText, result)
    ElMessage.error(result.message)
    return false
  }
  statusType.value = 'success'
  statusText.value = successText
  statusMessage.value = result.empty ? '当前 JSON 为空。' : 'JSON 格式正确。'
  clearAnnotations()
  emit('valid', true)
  emit('parsed', result.value)
  return true
}

async function handleCopy() {
  const text = getCurrentText()
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      fallbackCopy(text)
    }
    ElMessage.success('内容已复制')
  } catch {
    fallbackCopy(text)
    ElMessage.success('内容已复制')
  }
}

function fallbackCopy(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

function handleClear() {
  setEditorText('', { emitChange: true })
  resetStatus()
  clearAnnotations()
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  nextTick(resizeEditor)
}

function setEditorText(value, options = {}) {
  const { emitChange = true, reset = false, keepStatus = false } = options
  const nextText = limitText(normalizeJsonText(value), false)
  innerValue.value = nextText
  if (editor.value && editor.value.getValue() !== nextText) {
    syncingEditor = true
    editor.value.setValue(nextText, -1)
    syncingEditor = false
    resizeEditor()
  }
  if (reset) {
    resetStatus()
    clearAnnotations()
  } else if (!keepStatus) {
    resetStatus()
  }
  if (emitChange) {
    emit('update:modelValue', nextText)
  }
}

function getCurrentText() {
  return editor.value ? editor.value.getValue() : innerValue.value
}

function limitText(value, notify = true) {
  const text = value || ''
  if (!props.maxLength || text.length <= props.maxLength) {
    return text
  }
  if (notify) {
    statusType.value = props.truncateOnOverflow ? 'danger' : 'warning'
    statusText.value = props.truncateOnOverflow ? '超出长度' : '超出建议长度'
    statusMessage.value = props.truncateOnOverflow
      ? `内容长度不能超过 ${props.maxLength} 字。`
      : `当前内容已超过 ${props.maxLength} 字，未自动截断，请确认浏览器与接口处理能力。`
  }
  return props.truncateOnOverflow ? text.slice(0, props.maxLength) : text
}

function resetStatus() {
  statusType.value = 'info'
  statusText.value = ''
  statusMessage.value = ''
}

function resizeEditor() {
  nextTick(() => {
    editor.value?.resize(true)
  })
}

function initResizeObserver() {
  if (!rootRef.value || typeof ResizeObserver === 'undefined') {
    return
  }
  resizeObserver?.disconnect()
  resizeObserver = new ResizeObserver(() => {
    resizeEditor()
  })
  resizeObserver.observe(rootRef.value)
}

function clearAnnotations() {
  editor.value?.session.clearAnnotations()
}

function applyErrorAnnotation(text, result) {
  if (!editor.value) {
    return
  }
  const location = result?.annotationLocation || result?.location || resolveJsonErrorLocation(text, result?.message)
  editor.value.session.setAnnotations([
    {
      row: location.row,
      column: location.columnIndex ?? location.column,
      type: 'error',
      text: result?.hint || result?.message || 'JSON 格式不合法'
    }
  ])
  editor.value.gotoLine(location.row + 1, location.columnIndex ?? location.column, true)
}

function resolveJsonErrorLocation(text, message) {
  const match = String(message || '').match(/position\s+(\d+)/i)
  const position = match ? Number(match[1]) : 0
  const safePosition = Number.isFinite(position) ? Math.max(position, 0) : 0
  const before = String(text || '').slice(0, safePosition)
  const lines = before.split(/\r\n|\r|\n/)
  return {
    row: Math.max(lines.length - 1, 0),
    column: Math.max(lines[lines.length - 1]?.length || 0, 0)
  }
}

function resolveMode(lang) {
  const normalized = normalizeLanguage(lang)
  const modeMap = {
    json: 'ace/mode/json',
    javascript: 'ace/mode/javascript',
    js: 'ace/mode/javascript',
    java: 'ace/mode/java',
    text: 'ace/mode/text'
  }
  return modeMap[normalized] || 'ace/mode/text'
}

function resolveTheme(theme) {
  return `ace/theme/${theme || 'chrome'}`
}

function normalizeLanguage(lang) {
  return String(lang || 'json').trim().toLowerCase()
}

function normalizeSize(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return typeof value === 'number' ? `${value}px` : value
}

function resolveHeight() {
  if (props.height) {
    return normalizeSize(props.height)
  }
  const minRows = typeof props.autosize === 'object' ? props.autosize.minRows : undefined
  const rows = Math.max(minRows || props.rows || 8, 4)
  return `${rows * 24 + 34}px`
}
</script>

<style scoped lang="scss">
.json-editor {
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.json-editor__toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color);
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  background: var(--el-fill-color-lighter);
}

.json-editor__title {
  display: flex;
  align-items: center;
  flex: 1 1 240px;
  gap: 8px;
  min-width: 0;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.json-editor__actions {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 2px 8px;
  min-width: 0;
}

.json-editor__body {
  position: relative;
  min-width: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color-overlay);
}

.json-editor__body.has-toolbar {
  border-radius: 0 0 8px 8px;
}

.json-editor__ace {
  width: 100%;
  min-width: 0;
  line-height: 1.55;
}

.json-editor__placeholder {
  position: absolute;
  z-index: 2;
  top: 10px;
  left: 52px;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  pointer-events: none;
}

.json-editor__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
  min-width: 0;
}

.json-editor__message {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.json-editor__message.is-success {
  color: var(--el-color-success);
}

.json-editor__message.is-danger {
  color: var(--el-color-danger);
}

.json-editor__message.is-warning {
  color: var(--el-color-warning);
}

.json-editor__message.is-info {
  color: var(--el-text-color-secondary);
}

.json-editor__counter {
  flex: none;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.json-editor__counter.is-overflow {
  color: var(--el-color-danger);
}

.json-editor.is-compact {
  .json-editor__toolbar {
    padding: 6px 8px;
    gap: 6px 10px;
  }

  .json-editor__title {
    font-size: 12px;
  }

  .json-editor__actions {
    gap: 0 6px;
  }

  .json-editor__footer {
    flex-wrap: wrap;
    gap: 4px 12px;
  }
}

.json-editor.is-disabled {
  opacity: 0.72;
}

.json-editor.is-fullscreen {
  position: fixed;
  z-index: 3000;
  inset: 24px;
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: 12px;
  background: var(--el-bg-color-overlay);
  box-shadow: var(--el-box-shadow-dark);

  .json-editor__body {
    flex: 1;
  }
}
</style>
