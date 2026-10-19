<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorks, deleteWork } from '@/api/works'

const router = useRouter()

const loading = ref(false)
const works = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const activeType = ref('all')

const TYPE_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'IMAGE', label: '图像' },
  { value: 'VIDEO', label: '视频' },
]

const hasWorks = computed(() => works.value.length > 0)

async function loadWorks() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (activeType.value !== 'all') {
      params.workType = activeType.value
    }
    const res = await getWorks(params)
    works.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleTypeChange(type) {
  activeType.value = type
  pageNum.value = 1
  loadWorks()
}

function handlePageChange(page) {
  pageNum.value = page
  loadWorks()
}

function goDetail(work) {
  router.push({ name: 'WorkDetail', params: { id: work.id } })
}

async function handleDelete(work) {
  try {
    await ElMessageBox.confirm(
      `确定要删除作品「${work.title}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteWork(work.id)
    ElMessage.success('已删除')
    if (works.value.length === 1 && pageNum.value > 1) {
      pageNum.value--
    }
    loadWorks()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

function goCreate() {
  router.push({ name: 'Create' })
}

onMounted(loadWorks)
</script>

<template>
  <div v-loading="loading" class="works-view">
    <div class="container">
      <header class="page-header">
        <div>
          <h1 class="page-title">我的作品</h1>
          <p class="page-subtitle">你用 AI 创作的非遗文化作品都在这里</p>
        </div>
        <el-button type="primary" @click="goCreate">
          <el-icon><Plus /></el-icon>
          创作新作品
        </el-button>
      </header>

      <div class="filter-bar">
        <div class="type-tabs">
          <div
            v-for="opt in TYPE_OPTIONS"
            :key="opt.value"
            class="type-tab"
            :class="{ active: activeType === opt.value }"
            @click="handleTypeChange(opt.value)"
          >
            {{ opt.label }}
          </div>
        </div>
        <span v-if="total > 0" class="total-hint">共 {{ total }} 件作品</span>
      </div>

      <template v-if="hasWorks">
        <div class="work-grid">
          <div
            v-for="work in works"
            :key="work.id"
            class="work-card"
            @click="goDetail(work)"
          >
            <div class="work-thumb">
              <img :src="work.thumbnailUrl || work.resultUrl" :alt="work.title" />
              <div class="work-overlay">
                <span class="work-type-tag">{{ work.workType === 'IMAGE' ? '图像' : '视频' }}</span>
                <span v-if="work.style" class="work-style-tag">{{ work.style }}</span>
              </div>
            </div>
            <div class="work-info">
              <h3 class="work-title" :title="work.title">{{ work.title }}</h3>
              <p class="work-prompt" :title="work.prompt">{{ work.prompt }}</p>
              <div class="work-meta">
                <span class="work-date">{{ work.createdAt }}</span>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click.stop="handleDelete(work)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </template>

      <div v-else class="empty-state">
        <el-icon :size="56"><PictureFilled /></el-icon>
        <h3 class="empty-title">还没有作品</h3>
        <p class="empty-desc">去 AI 创作工作台，生成你的第一件非遗作品吧</p>
        <el-button type="primary" @click="goCreate">开始创作</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.works-view {
  background: var(--yy-color-bg);
  min-height: calc(100vh - 64px);
  padding: 32px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--yy-font-serif);
  font-size: 28px;
  color: var(--yy-color-text);
  margin: 0 0 6px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--yy-color-muted);
  margin: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.type-tabs {
  display: flex;
  gap: 4px;
  background: var(--yy-color-surface);
  padding: 4px;
  border-radius: 10px;
  border: 1px solid var(--yy-color-border);
}

.type-tab {
  padding: 8px 18px;
  font-size: 14px;
  color: var(--yy-color-text-secondary);
  border-radius: 7px;
  cursor: pointer;
  transition: all var(--yy-transition);
}

.type-tab:hover {
  color: var(--yy-color-primary);
}

.type-tab.active {
  background: var(--yy-color-primary);
  color: #fff;
  font-weight: 600;
}

.total-hint {
  font-size: 13px;
  color: var(--yy-color-muted);
}

.work-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.work-card {
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--yy-transition);
  box-shadow: var(--yy-shadow-card);
}

.work-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(39, 39, 34, 0.12);
  border-color: var(--yy-color-primary);
}

.work-thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: var(--yy-color-bg);
  overflow: hidden;
}

.work-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.work-overlay {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
}

.work-type-tag,
.work-style-tag {
  padding: 3px 10px;
  font-size: 11px;
  color: #fff;
  background: rgba(39, 39, 34, 0.7);
  backdrop-filter: blur(4px);
  border-radius: 20px;
}

.work-style-tag {
  background: rgba(120, 47, 53, 0.8);
}

.work-info {
  padding: 14px 16px 12px;
}

.work-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-prompt {
  font-size: 12px;
  color: var(--yy-color-muted);
  margin: 0 0 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}

.work-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.work-date {
  font-size: 12px;
  color: var(--yy-color-muted);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--yy-color-muted);
}

.empty-state .el-icon {
  color: var(--yy-color-border);
}

.empty-title {
  font-size: 18px;
  color: var(--yy-color-text-secondary);
  margin: 16px 0 6px;
}

.empty-desc {
  font-size: 14px;
  margin: 0 0 24px;
}
</style>