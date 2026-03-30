<template>
  <div class="tier-editor">
    <div class="tier-editor__toolbar">
      <el-button type="primary" plain icon="Plus" @click="handleAdd">新增阶梯</el-button>
      <span class="tier-editor__tip">保存时会自动校验连续性、重叠和空区间。</span>
    </div>
    <el-table :data="innerValue" size="small" border>
      <el-table-column label="序号" width="70" align="center">
        <template #default="scope">{{ scope.$index + 1 }}</template>
      </el-table-column>
      <el-table-column label="起始值" min-width="120" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.startValue" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="截止值" min-width="120" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.endValue" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="费率/单价" min-width="140" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.rateValue" controls-position="right" :precision="6" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="区间模式" min-width="170" align="center">
        <template #default="scope">
          <el-select v-model="scope.row.intervalMode" style="width: 100%">
            <el-option v-for="item in intervalModeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="180" align="center">
        <template #default="scope">
          <el-input v-model="scope.row.remark" placeholder="补充本档口径" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" @click="handleRemove(scope.$index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  intervalModeOptions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const innerValue = computed({
  get: () => props.modelValue || [],
  set: value => emit('update:modelValue', value)
})

function handleAdd() {
  innerValue.value = [...innerValue.value, { tierNo: innerValue.value.length + 1, intervalMode: 'LEFT_CLOSED_RIGHT_OPEN', status: '0' }]
}

function handleRemove(index) {
  const next = [...innerValue.value]
  next.splice(index, 1)
  next.forEach((item, idx) => {
    item.tierNo = idx + 1
  })
  innerValue.value = next
}
</script>

<style scoped lang="scss">
.tier-editor {
  display: grid;
  gap: 12px;
}

.tier-editor__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tier-editor__tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
