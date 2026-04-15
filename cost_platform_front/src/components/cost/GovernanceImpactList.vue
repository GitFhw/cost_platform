<template>
  <div class="governance-impact-list">
    <div class="governance-impact-list__title">关联影响明细</div>
    <el-empty v-if="!normalizedImpacts.length" description="当前没有关联影响" :image-size="72" />
    <div v-else class="governance-impact-list__items">
      <div v-for="item in normalizedImpacts" :key="item.impactType || item.title" class="governance-impact">
        <div class="governance-impact__head">
          <div>
            <div class="governance-impact__title">{{ item.title }}</div>
            <div class="governance-impact__module">{{ item.moduleName || '关联功能' }} · {{ item.count || 0 }} 项</div>
          </div>
          <div class="governance-impact__tags">
            <el-tag v-if="item.blocksDelete" type="danger" size="small">阻断删除</el-tag>
            <el-tag v-else type="info" size="small">不阻断删除</el-tag>
            <el-tag v-if="item.blocksDisable" type="warning" size="small">阻断停用</el-tag>
            <el-tag v-else type="info" size="small">不阻断停用</el-tag>
          </div>
        </div>
        <div class="governance-impact__body">
          <p v-if="item.deleteImpact"><span>删除影响：</span>{{ item.deleteImpact }}</p>
          <p v-if="item.disableImpact"><span>停用影响：</span>{{ item.disableImpact }}</p>
          <p v-if="item.actionAdvice"><span>处理建议：</span>{{ item.actionAdvice }}</p>
        </div>
        <div v-if="item.examples && item.examples.length" class="governance-impact__examples">
          <span class="governance-impact__examples-label">关联样例</span>
          <el-tag v-for="example in item.examples" :key="example" class="governance-impact__example" size="small">
            {{ localizeCostTechnicalText(example) }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { localizeCostTechnicalText } from '@/utils/costDisplayLabels'

const props = defineProps({
  impacts: {
    type: Array,
    default: () => []
  }
})

const normalizedImpacts = computed(() => props.impacts.filter(item => item && Number(item.count || 0) > 0))
</script>

<style scoped>
.governance-impact-list {
  margin-top: 14px;
}

.governance-impact-list__title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.governance-impact-list__items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.governance-impact {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.governance-impact__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.governance-impact__title {
  font-weight: 700;
  color: #1f2937;
}

.governance-impact__module {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.governance-impact__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.governance-impact__body {
  margin-top: 10px;
  color: #606266;
  line-height: 1.7;
}

.governance-impact__body p {
  margin: 4px 0;
}

.governance-impact__body span,
.governance-impact__examples-label {
  font-weight: 700;
  color: #374151;
}

.governance-impact__examples {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
}

.governance-impact__example {
  max-width: 100%;
  white-space: normal;
  height: auto;
  line-height: 1.4;
  padding-top: 4px;
  padding-bottom: 4px;
}
</style>
