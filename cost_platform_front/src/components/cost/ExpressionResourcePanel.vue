<template>
  <div class="expression-resource-panel">
    <section
      v-for="section in normalizedSections"
      :key="section.key"
      class="expression-resource-panel__section"
    >
      <div class="expression-resource-panel__header">
        <h4>{{ section.title }}</h4>
        <p v-if="section.tip">{{ section.tip }}</p>
      </div>
      <div v-if="section.items.length">
        <div v-if="section.display === 'tag'" class="expression-resource-panel__tag-list">
          <el-tag
            v-for="item in section.items"
            :key="`${section.key}-${item.value}-${item.label}`"
            class="expression-resource-panel__tag"
            :type="item.type || section.type || 'info'"
            effect="plain"
            @click="handleAppend(section, item)"
          >
            {{ item.label }}
          </el-tag>
        </div>
        <div v-else class="expression-resource-panel__list">
          <button
            v-for="item in section.items"
            :key="`${section.key}-${item.value}-${item.label}`"
            type="button"
            class="expression-resource-panel__item"
            @click="handleAppend(section, item)"
          >
            <strong>{{ item.label }}</strong>
            <span>{{ item.desc || item.meta || item.value }}</span>
          </button>
        </div>
      </div>
      <el-empty v-else :image-size="72" :description="section.emptyText || '暂无可用项'" />
    </section>
  </div>
</template>

<script setup>
const props = defineProps({
  sections: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['append'])

const normalizedSections = computed(() => {
  return (props.sections || [])
    .filter(section => section && section.hidden !== true)
    .map((section, index) => ({
      key: section.key || `section-${index}`,
      title: section.title || '资源面板',
      tip: section.tip || '',
      display: section.display || 'tag',
      type: section.type || 'info',
      emptyText: section.emptyText || '',
      items: Array.isArray(section.items) ? section.items.filter(Boolean) : []
    }))
})

function handleAppend(section, item) {
  if (!item || item.disabled || typeof item.value === 'undefined' || item.value === null) {
    return
  }
  emit('append', {
    sectionKey: section.key,
    value: item.value,
    item
  })
}
</script>

<style scoped lang="scss">
.expression-resource-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.expression-resource-panel__section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.expression-resource-panel__header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.expression-resource-panel__header h4 {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.expression-resource-panel__header p {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.expression-resource-panel__tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.expression-resource-panel__tag {
  cursor: pointer;
}

.expression-resource-panel__list {
  display: grid;
  gap: 10px;
}

.expression-resource-panel__item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  background: var(--el-bg-color-overlay);
  padding: 12px 14px;
  text-align: left;
  display: grid;
  gap: 4px;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.expression-resource-panel__item:hover {
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.expression-resource-panel__item strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.expression-resource-panel__item span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
