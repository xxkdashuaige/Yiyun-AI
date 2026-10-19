<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkDetail, deleteWork } from '@/api/works'

const props = defineProps({
  id: {
    type: [String, Number],
    required: true,
  },
})

const router = useRouter()

const loading = ref(false)
const detail = ref(null)

const STYLE_MAP = {
  ink: '水墨',
  oil: '油画',
  watercolor: '水彩',
  'chinese-painting': '国画',
  realistic: '写实',
}

const hasSources = computed(() => detail.value?.sources?.length > 0)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getWorkDetail(props.id)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除作品「${detail.value.title}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteWork(props.id)
    ElMessage.success('已删除')
    router.push({ name: 'Works' })
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

function goCreate() {
  router.push({ name: 'Create' })
}

function goBack() {
  router.back()
}

onMounted(loadDetail)
</script>

<template>
  <div v-loading="loading" class="work-detail-view">
    <div class="container">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </button>

      <template v-if="detail">
        <div class="detail-layout">
          <section class="work-display">
            <div class="work-image-wrap">
              <img :src="detail.resultUrl" :alt="detail.title" />
            </div>
            <div class="work-actions">
              <el-button type="primary" @click="goCreate">
                <el-icon><MagicStick /></el-icon>
                继续创作
              </el-button>
              <el-button type="danger" plain @click="handleDelete">
                <el-icon><Delete /></el-icon>
                删除作品
              </el-button>
            </div>
          </section>

          <aside class="work-info-panel">
            <div class="info-section">
              <h1 class="work-title">{{ detail.title }}</h1>
              <div class="work-meta-row">
                <span class="meta-tag">{{ detail.workType === 'IMAGE' ? '图像作品' : '视频作品' }}</span>
                <span v-if="detail.style" class="meta-tag meta-tag--style">
                  {{ STYLE_MAP[detail.style] || detail.style }}
                </span>
              </div>
              <p class="work-date">
                <el-icon><Clock /></el-icon>
                创建于 {{ detail.createdAt }}
              </p>
            </div>

            <div v-if="detail.prompt" class="info-section">
              <h3 class="info-title">创作描述</h3>
              <p class="info-content">{{ detail.prompt }}</p>
            </div>

            <div v-if="detail.enhancedPrompt" class="info-section">
              <h3 class="info-title">
                AI 增强 Prompt
                <el-tooltip content="AI 根据非遗知识库优化后的提示词" placement="top">
                  <el-icon class="info-icon"><InfoFilled /></el-icon>
                </el-tooltip>
              </h3>
              <p class="info-content enhanced">{{ detail.enhancedPrompt }}</p>
            </div>

            <div class="info-section">
              <h3 class="info-title">
                文化来源溯源
                <el-tag v-if="hasSources" type="success" size="small" effect="plain">
                  {{ detail.sources.length }} 条
                </el-tag>
                <el-tag v-else type="info" size="small" effect="plain">暂无</el-tag>
              </h3>

              <div v-if="hasSources" class="source-list">
                <div
                  v-for="(src, idx) in detail.sources"
                  :key="src.chunkId || idx"
                  class="source-card"
                >
                  <div class="source-header">
                    <span class="source-index">{{ idx + 1 }}</span>
                    <span v-if="src.docType" class="source-type">{{ src.docType }}</span>
                    <span v-if="src.score" class="source-score">
                      相似度 {{ (Number(src.score) * 100).toFixed(0) }}%
                    </span>
                  </div>
                  <h4 v-if="src.title" class="source-title">{{ src.title }}</h4>
                  <p v-if="src.content" class="source-content">{{ src.content }}</p>
                </div>
              </div>

              <p v-else class="source-empty">
                该作品未关联非遗知识来源。在非遗展馆选择项目后再创作，可查看文化溯源信息。
              </p>
            </div>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.work-detail-view {
  background: var(--yy-color-bg);
  min-height: calc(100vh - 64px);
  padding: 24px 0 48px;
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  margin-bottom: 20px;
  border: none;
  background: transparent;
  color: var(--yy-color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  border-radius: 6px;
  transition: all var(--yy-transition);
}

.back-btn:hover {
  color: var(--yy-color-primary);
  background: var(--yy-color-surface);
}

.detail-layout {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 32px;
  align-items: start;
}

.work-display {
  position: sticky;
  top: 80px;
}

.work-image-wrap {
  width: 100%;
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  overflow: hidden;
  box-shadow: var(--yy-shadow-card);
  aspect-ratio: 1;
}

.work-image-wrap img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.work-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.work-info-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-section {
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  padding: 20px 22px;
  box-shadow: var(--yy-shadow-card);
}

.work-title {
  font-family: var(--yy-font-serif);
  font-size: 24px;
  color: var(--yy-color-text);
  margin: 0 0 12px;
}

.work-meta-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.meta-tag {
  padding: 4px 12px;
  font-size: 12px;
  color: var(--yy-color-text-secondary);
  background: var(--yy-color-bg);
  border-radius: 20px;
}

.meta-tag--style {
  color: #fff;
  background: var(--yy-color-primary);
}

.work-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--yy-color-muted);
  margin: 0;
}

.info-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 12px;
}

.info-icon {
  font-size: 14px;
  color: var(--yy-color-muted);
  cursor: help;
}

.info-content {
  font-size: 14px;
  color: var(--yy-color-text-secondary);
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

.info-content.enhanced {
  background: var(--yy-color-bg);
  padding: 12px 14px;
  border-radius: 8px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.source-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.source-card {
  padding: 14px;
  background: var(--yy-color-bg);
  border: 1px solid var(--yy-color-border);
  border-radius: 10px;
}

.source-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.source-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--yy-color-primary);
  border-radius: 50%;
}

.source-type {
  font-size: 12px;
  color: var(--yy-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 2px 8px;
  border-radius: 4px;
}

.source-score {
  font-size: 11px;
  color: var(--yy-color-muted);
  margin-left: auto;
}

.source-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 6px;
}

.source-content {
  font-size: 12px;
  color: var(--yy-color-text-secondary);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.source-empty {
  font-size: 13px;
  color: var(--yy-color-muted);
  line-height: 1.6;
  margin: 0;
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .work-display {
    position: static;
  }
}
</style>