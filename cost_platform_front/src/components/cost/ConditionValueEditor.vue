<template>
  <el-select
    v-if="usesDictSelect"
    :model-value="dictModelValue"
    clearable
    filterable
    :multiple="isMultiValueOperator"
    collapse-tags
    collapse-tags-tooltip
    placeholder="请选择条件值"
    style="width: 100%"
    @update:model-value="handleDictChange"
  >
    <el-option v-for="item in dictOptions" :key="item.value" :label="item.label" :value="item.value" />
  </el-select>
  <el-input-number
    v-else-if="isNumber"
    :model-value="numberValue"
    controls-position="right"
    style="width: 100%"
    @update:model-value="handleNumberChange"
  />
  <el-date-picker
    v-else-if="isDate"
    :model-value="modelValue || ''"
    type="date"
    value-format="YYYY-MM-DD"
    placeholder="请选择日期"
    style="width: 100%"
    @update:model-value="emit('update:modelValue', $event)"
  />
  <el-input
    v-else
    :model-value="modelValue"
    :placeholder="placeholder"
    @update:model-value="emit('update:modelValue', $event)"
  />
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: [String, Number, Array],
    default: ''
  },
  variableMeta: {
    type: Object,
    default: () => ({})
  },
  dictOptionsMap: {
    type: Object,
    default: () => ({})
  },
  operatorCode: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const usesDictSelect = computed(() => Boolean(props.variableMeta?.dictType))
const dictOptions = computed(() => props.dictOptionsMap?.[props.variableMeta?.dictType] || [])
const isMultiValueOperator = computed(() => ['IN', 'NOT_IN'].includes((props.operatorCode || '').toUpperCase()))
const isNumber = computed(() => ['NUMBER'].includes(props.variableMeta?.dataType) || props.variableMeta?.variableType === 'NUMBER')
const isDate = computed(() => props.variableMeta?.dataType === 'DATE' || props.variableMeta?.variableType === 'DATE')
const dictModelValue = computed(() => {
  if (!isMultiValueOperator.value) {
    return props.modelValue
  }
  if (Array.isArray(props.modelValue)) {
    return props.modelValue
  }
  if (props.modelValue === undefined || props.modelValue === null || props.modelValue === '') {
    return []
  }
  return String(props.modelValue)
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
})
const numberValue = computed(() => {
  if (props.modelValue === undefined || props.modelValue === null || props.modelValue === '') {
    return undefined
  }
  return Number(props.modelValue)
})

const placeholder = computed(() => {
  if (props.operatorCode === 'IN' || props.operatorCode === 'NOT_IN') {
    return '多个值请用英文逗号分隔'
  }
  if (props.operatorCode === 'BETWEEN') {
    return '请按 起始值,截止值 录入'
  }
  if (props.operatorCode === 'EXPR') {
    return '请输入表达式条件'
  }
  return '请输入条件值'
})

function handleNumberChange(value) {
  emit('update:modelValue', value === undefined || value === null ? '' : String(value))
}

function handleDictChange(value) {
  if (isMultiValueOperator.value) {
    const normalized = Array.isArray(value) ? value.filter(item => item !== undefined && item !== null && item !== '') : []
    emit('update:modelValue', normalized.join(','))
    return
  }
  emit('update:modelValue', value)
}
</script>
