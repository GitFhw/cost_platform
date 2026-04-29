<template>
  <div v-if="visible" class="cost-work-context">
    <el-tooltip :content="sceneTooltip" placement="bottom" effect="dark">
      <button class="cost-work-context__primary" type="button" @click="goTo(COST_MENU_ROUTES.scene)">
        <el-icon><Aim /></el-icon>
        <span class="cost-work-context__label">{{ sceneLabel }}</span>
      </button>
    </el-tooltip>

    <span class="cost-work-context__meta">
      <el-icon><PriceTag /></el-icon>
      <span>{{ domainLabel }}</span>
    </span>

    <span class="cost-work-context__meta">
      <el-icon><Calendar /></el-icon>
      <span>{{ billMonthLabel }}</span>
    </span>

    <span class="cost-work-context__meta">
      <el-icon><Tickets /></el-icon>
      <span>{{ versionLabel }}</span>
    </span>

    <div class="cost-work-context__steps" aria-label="成本核算链路">
      <el-tooltip v-for="item in stepItems" :key="item.path" :content="item.label" placement="bottom" effect="dark">
        <button
          class="cost-work-context__step"
          :class="{ 'is-active': route.path.startsWith(item.activePath) }"
          type="button"
          @click="goTo(item.path)"
        >
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
        </button>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { Aim, Calendar, Connection, Files, Finished, PriceTag, Promotion, Tickets } from '@element-plus/icons-vue'
import { getScene } from '@/api/cost/scene'
import {
  COST_WORK_CONTEXT_CHANGE_EVENT,
  getCostWorkContext,
  patchCostWorkContext,
  resolveCurrentBillMonth
} from '@/utils/costWorkContext'
import { COST_MENU_ROUTES } from '@/utils/costMenuRoutes'

const route = useRoute()
const router = useRouter()
const context = ref(getCostWorkContext())

const visible = computed(() => route.path.startsWith('/cost'))
const sceneLabel = computed(() => {
  if (context.value.sceneName || context.value.sceneCode) {
    return [context.value.sceneCode, context.value.sceneName].filter(Boolean).join(' / ')
  }
  return context.value.sceneId ? `场景 #${context.value.sceneId}` : '未设置工作场景'
})
const sceneTooltip = computed(() => context.value.sceneId ? '查看或切换当前工作场景' : '设置当前工作场景')
const domainLabel = computed(() => context.value.businessDomainName || context.value.businessDomain || '未设置业务域')
const billMonthLabel = computed(() => context.value.billMonth || resolveCurrentBillMonth())
const versionLabel = computed(() => context.value.versionNo || context.value.versionName || (context.value.versionId ? `版本 #${context.value.versionId}` : '未选择版本'))
const stepItems = [
  { label: '场景', path: COST_MENU_ROUTES.scene, activePath: '/cost/setup/scene', icon: Aim },
  { label: '费用', path: COST_MENU_ROUTES.fee, activePath: '/cost/model/fee', icon: Files },
  { label: '规则', path: COST_MENU_ROUTES.rule, activePath: '/cost/model/rule', icon: Connection },
  { label: '发布', path: COST_MENU_ROUTES.publish, activePath: '/cost/model/publish', icon: Tickets },
  { label: '试算/正式', path: COST_MENU_ROUTES.simulation, activePath: '/cost/execution', icon: Promotion },
  { label: '结果', path: COST_MENU_ROUTES.result, activePath: '/cost/execution/result', icon: Finished }
]

function refreshContext(event) {
  context.value = event?.detail || getCostWorkContext()
}

async function hydrateSceneInfo() {
  const sceneId = context.value.sceneId
  if (!sceneId || (context.value.sceneCode && context.value.sceneName)) {
    return
  }
  try {
    const response = await getScene(sceneId)
    const scene = response?.data || {}
    if (scene.sceneId) {
      context.value = patchCostWorkContext({
        sceneId: scene.sceneId,
        sceneCode: scene.sceneCode,
        sceneName: scene.sceneName,
        businessDomain: scene.businessDomain,
        businessDomainName: scene.businessDomainName || scene.businessDomainLabel
      })
    }
  } catch (error) {
    console.warn('Failed to hydrate cost scene context:', error)
  }
}

function goTo(path) {
  const query = {}
  if (context.value.sceneId) query.sceneId = context.value.sceneId
  if (context.value.versionId && path !== COST_MENU_ROUTES.publish) query.versionId = context.value.versionId
  if (context.value.billMonth) query.billMonth = context.value.billMonth
  router.push({ path, query })
}

onMounted(() => {
  window.addEventListener(COST_WORK_CONTEXT_CHANGE_EVENT, refreshContext)
  window.addEventListener('storage', refreshContext)
  hydrateSceneInfo()
})

onBeforeUnmount(() => {
  window.removeEventListener(COST_WORK_CONTEXT_CHANGE_EVENT, refreshContext)
  window.removeEventListener('storage', refreshContext)
})

watch(() => context.value.sceneId, () => {
  hydrateSceneInfo()
})
</script>

<style lang="scss" scoped>
.cost-work-context {
  display: flex;
  align-items: center;
  min-width: 0;
  height: 100%;
  gap: 8px;
  margin-left: 12px;
  color: #4b5563;
  font-size: 13px;
  white-space: nowrap;
}

.cost-work-context__primary,
.cost-work-context__meta,
.cost-work-context__step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #4b5563;
}

.cost-work-context__primary {
  max-width: 280px;
  gap: 6px;
  padding: 0 10px;
  border-radius: 4px;
  cursor: pointer;
}

.cost-work-context__label {
  overflow: hidden;
  text-overflow: ellipsis;
}

.cost-work-context__meta {
  gap: 5px;
  padding: 0 8px;
  border-radius: 4px;
}

.cost-work-context__steps {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.cost-work-context__step {
  width: 28px;
  padding: 0;
  border-radius: 4px;
  cursor: pointer;
}

.cost-work-context__primary:hover,
.cost-work-context__step:hover,
.cost-work-context__step.is-active {
  border-color: var(--current-color);
  color: var(--current-color);
  background: var(--current-color-light);
}

@media screen and (max-width: 1280px) {
  .cost-work-context__meta {
    display: none;
  }
}

@media screen and (max-width: 1080px) {
  .cost-work-context {
    display: none;
  }
}
</style>
