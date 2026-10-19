<script setup>
import { ref, onMounted } from 'vue'
import { useHeritageStore } from '@/stores/heritage'
import HeritageCard from '@/components/HeritageCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const heritageStore = useHeritageStore()

const loading = ref(false)
const categories = ref([])
const projects = ref([])
const total = ref(0)

const filters = ref({
  categoryId: null,
  region: '',
  keyword: '',
  pageNum: 1,
  pageSize: 12,
})

async function loadCategories() {
  categories.value = await heritageStore.loadCategories()
}

async function loadProjects() {
  loading.value = true
  try {
    const page = await heritageStore.fetchProjects(filters.value)
    projects.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function selectCategory(id) {
  filters.value.categoryId = filters.value.categoryId === id ? null : id
  filters.value.pageNum = 1
  loadProjects()
}

function handleSearch() {
  filters.value.pageNum = 1
  loadProjects()
}

function handlePageChange(page) {
  filters.value.pageNum = page
  loadProjects()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  loadCategories()
  loadProjects()
})
</script>

<template>
  <div class="gallery-view">
    <div class="gallery-view__inner">
      <!-- 页面标题区 -->
      <header class="gallery-header">
        <h1 class="gallery-header__title">数字展馆</h1>
        <p class="gallery-header__desc">
          浏览国家级非物质文化遗产代表性项目，探索千年技艺的文化魅力
        </p>
      </header>

      <div class="gallery-layout">
        <!-- 左侧筛选 -->
        <aside class="gallery-sidebar">
          <div class="sidebar-section">
            <h3 class="sidebar-section__title">非遗分类</h3>
            <ul class="category-list">
              <li
                class="category-item"
                :class="{ active: !filters.categoryId }"
                @click="selectCategory(null)"
              >
                全部
              </li>
              <li
                v-for="cat in categories"
                :key="cat.id"
                class="category-item"
                :class="{ active: filters.categoryId === cat.id }"
                @click="selectCategory(cat.id)"
              >
                {{ cat.name }}
              </li>
            </ul>
          </div>
        </aside>

        <!-- 右侧内容 -->
        <div class="gallery-content">
          <!-- 搜索栏 -->
          <div class="gallery-toolbar">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索非遗项目或地区"
              size="large"
              clearable
              class="gallery-toolbar__search"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleSearch"
            >
              搜索
            </el-button>
          </div>

          <!-- 结果统计 -->
          <div class="gallery-meta">
            <span>共找到 <strong>{{ total }}</strong> 个非遗项目</span>
          </div>

          <!-- 卡片网格 -->
          <div v-loading="loading" class="gallery-grid">
            <HeritageCard
              v-for="project in projects"
              :key="project.id"
              :project="project"
            />
          </div>

          <!-- 空状态 -->
          <EmptyState
            v-if="!loading && projects.length === 0"
            title="暂无非遗项目"
            description="试试其他分类或关键词"
          />

          <!-- 分页 -->
          <div v-if="total > filters.pageSize" class="gallery-pagination">
            <el-pagination
              :current-page="filters.pageNum"
              :page-size="filters.pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gallery-view {
  padding: 40px 0 60px;
}

.gallery-view__inner {
  max-width: var(--yy-content-width);
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
}

.gallery-header {
  margin-bottom: 32px;
}

.gallery-header__title {
  font-family: var(--yy-font-serif);
  font-size: 32px;
  color: var(--yy-color-text);
  margin: 0 0 8px;
}

.gallery-header__desc {
  font-size: 15px;
  color: var(--yy-color-muted);
  margin: 0;
}

.gallery-layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

/* ===== 侧边筛选 ===== */
.gallery-sidebar {
  flex: 0 0 200px;
  position: sticky;
  top: 80px;
}

.sidebar-section {
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  padding: 20px;
}

.sidebar-section__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--yy-color-border);
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.category-item {
  padding: 10px 12px;
  font-size: 14px;
  color: var(--yy-color-text-secondary);
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--yy-transition);
}

.category-item:hover {
  background: var(--yy-color-bg);
  color: var(--yy-color-primary);
}

.category-item.active {
  background: var(--yy-color-primary);
  color: #fff;
}

/* ===== 主内容区 ===== */
.gallery-content {
  flex: 1;
  min-width: 0;
}

.gallery-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.gallery-toolbar__search {
  flex: 1;
}

.gallery-meta {
  font-size: 14px;
  color: var(--yy-color-muted);
  margin-bottom: 20px;
}

.gallery-meta strong {
  color: var(--yy-color-primary);
  font-weight: 600;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  min-height: 200px;
}

.gallery-pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

/* ===== 响应式 ===== */
@media (max-width: 1100px) {
  .gallery-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .gallery-layout {
    flex-direction: column;
  }

  .gallery-sidebar {
    flex: none;
    position: static;
    width: 100%;
  }

  .category-list {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
  }

  .category-item {
    padding: 6px 14px;
  }

  .gallery-grid {
    grid-template-columns: 1fr;
  }
}
</style>