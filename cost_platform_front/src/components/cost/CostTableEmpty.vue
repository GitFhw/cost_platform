<template>
  <div class="cost-table-empty">
    <el-empty :image-size="imageSize">
      <template #description>
        <div class="cost-table-empty__copy">
          <strong>{{ title }}</strong>
          <span>{{ description }}</span>
        </div>
      </template>
      <div v-if="$slots.default" class="cost-table-empty__actions">
        <slot />
      </div>
    </el-empty>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '可以调整筛选条件后重试，或从上方操作区创建第一条业务数据。'
  },
  imageSize: {
    type: Number,
    default: 86
  }
})
</script>

<style scoped>
.cost-table-empty {
  padding: 18px 0 22px;
}

.cost-table-empty__copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 520px;
  line-height: 1.6;
  color: #606266;
}

.cost-table-empty__copy strong {
  font-size: 14px;
  color: #303133;
}

.cost-table-empty__copy span {
  font-size: 13px;
}

.cost-table-empty__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
}
</style>
