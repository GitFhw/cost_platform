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
            <el-button v-if="resolveRoutePath(item)" link type="primary" :icon="Position" @click="handleOpenImpact(item)">去处理</el-button>
          </div>
        </div>
        <div class="governance-impact__body">
          <p v-if="item.deleteImpact"><span>删除影响：</span>{{ item.deleteImpact }}</p>
          <p v-if="item.disableImpact"><span>停用影响：</span>{{ item.disableImpact }}</p>
          <p v-if="item.actionAdvice"><span>处理建议：</span>{{ item.actionAdvice }}</p>
        </div>
        <div v-if="item.examples && item.examples.length" class="governance-impact__examples">
          <span class="governance-impact__examples-label">引用对象</span>
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
import { useRouter } from 'vue-router'
import { Position } from '@element-plus/icons-vue'
import { localizeCostTechnicalText } from '@/utils/costDisplayLabels'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'

const props = defineProps({
  impacts: {
    type: Array,
    default: () => []
  },
  context: {
    type: Object,
    default: () => ({})
  }
})

const router = useRouter()

const impactRouteMap = {
  SCENE_FEE: COST_MENU_ROUTES.fee,
  SCENE_VARIABLE_GROUP: COST_MENU_ROUTES.variable,
  SCENE_VARIABLE: COST_MENU_ROUTES.variable,
  SCENE_RULE: COST_MENU_ROUTES.rule,
  SCENE_PUBLISH_VERSION: COST_MENU_ROUTES.publish,
  SCENE_ACTIVE_VERSION: COST_MENU_ROUTES.publish,
  FEE_RULE: COST_MENU_ROUTES.rule,
  FEE_VARIABLE_CONTRACT: COST_MENU_ROUTES.variable,
  FEE_PUBLISH_SNAPSHOT: COST_MENU_ROUTES.publish,
  FEE_RESULT_LEDGER: COST_MENU_ROUTES.result,
  VARIABLE_FEE_CONTRACT: COST_MENU_ROUTES.fee,
  VARIABLE_RULE_CONDITION: COST_MENU_ROUTES.rule,
  VARIABLE_RULE_QUANTITY: COST_MENU_ROUTES.rule,
  VARIABLE_PUBLISH_SNAPSHOT: COST_MENU_ROUTES.publish,
  RULE_CONDITION: COST_MENU_ROUTES.rule,
  RULE_TIER: COST_MENU_ROUTES.rule,
  RULE_PUBLISH_SNAPSHOT: COST_MENU_ROUTES.publish,
  RULE_RESULT_TRACE: COST_MENU_ROUTES.result,
  FORMULA_VARIABLE_REF: COST_MENU_ROUTES.variable,
  FORMULA_RULE_REF: COST_MENU_ROUTES.rule,
  FORMULA_PUBLISH_SNAPSHOT: COST_MENU_ROUTES.publish
}

const normalizedImpacts = computed(() => props.impacts.filter(item => item && Number(item.count || 0) > 0))

function resolveRoutePath(item) {
  return item.routePath || impactRouteMap[item.impactType] || ''
}

function buildRouteQuery(item) {
  const query = { ...(item.routeQuery || {}) }
  if (props.context?.sceneId && !query.sceneId) {
    query.sceneId = props.context.sceneId
  }
  return Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}

function handleOpenImpact(item) {
  const path = resolveRoutePath(item)
  if (!path) {
    return
  }
  router.push({ path, query: buildRouteQuery(item) })
}
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
