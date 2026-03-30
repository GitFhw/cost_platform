<template>
  <el-select
    v-if="usesDictSelect"
    :model-value="modelValue"
    clearable
    filterable
    placeholder="请选择条件值"
    style="width: 100%"
    @update:model-value="emit('update:modelValue', $event)"
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
    type: [String, Number],
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
const isNumber = computed(() => ['NUMBER'].includes(props.variableMeta?.dataType) || props.variableMeta?.variableType === 'NUMBER')
const isDate = computed(() => props.variableMeta?.dataType === 'DATE' || props.variableMeta?.variableType === 'DATE')
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
</script>
