<template>
  <div v-if="hasError" class="route-error-boundary">
    <el-result
      icon="warning"
      title="页面加载失败"
      sub-title="当前页面组件出现异常，系统已保留错误信息，请重新加载或返回上一页。"
    >
      <template #extra>
        <el-button type="primary" @click="handleRetry">重新加载</el-button>
        <el-button @click="handleBack">返回上一页</el-button>
      </template>
    </el-result>
    <div v-if="errorSummary" class="route-error-boundary__meta">
      错误摘要：{{ errorSummary }}
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
const props = defineProps({
  routeKey: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['retry'])
const router = useRouter()
const hasError = ref(false)
const errorSummary = ref('')

watch(
  () => props.routeKey,
  () => {
    resetError()
  }
)

onErrorCaptured((error, instance, info) => {
  hasError.value = true
  errorSummary.value = buildErrorSummary(error, info)
  console.error('[RouteErrorBoundary]', error, info, instance)
  return false
})

function buildErrorSummary(error, info) {
  const message = error?.message || String(error || '未知错误')
  return info ? `${message}（${info}）` : message
}

function resetError() {
  hasError.value = false
  errorSummary.value = ''
}

function handleRetry() {
  resetError()
  emit('retry')
}

function handleBack() {
  resetError()
  router.back()
}
</script>

<style lang="scss" scoped>
.route-error-boundary {
  min-height: calc(100vh - 132px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #fff;
}

.route-error-boundary__meta {
  max-width: 760px;
  padding: 10px 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  color: #606266;
  background: #fafafa;
  line-height: 1.6;
  word-break: break-word;
}
</style>
