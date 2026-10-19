<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHeritageStore } from '@/stores/heritage'
import EmptyState from '@/components/EmptyState.vue'

const props = defineProps({
  id: {
    type: [String, Number],
    required: true,
  },
})

const router = useRouter()
const heritageStore = useHeritageStore()

const loading = ref(false)
const detail = ref(null)
const expandedDocs = ref(new Set())

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await heritageStore.fetchProjectDetail(props.id)
  } finally {
    loading.value = false
  }
}

function toggleDoc(id) {
  if (expandedDocs.value.has(id)) {
    expandedDocs.value.delete(id)
  } else {
    expandedDocs.value.add(id)
  }
}

function goCreate() {
  heritageStore.setCurrentHeritage(detail.value)
  router.push({ name: 'Create' })
}

onMounted(loadDetail)

watch(() => props.id, loadDetail)
</script>

<template>
  <div v-loading="loading" class="detail-view">
    <template v-if="detail">
      <!-- 封面横幅 -->
      <section class="detail-hero">
        <div class="detail-hero__cover">
          <img
            v-if="detail.coverUrl"
            :src="detail.coverUrl"
            :alt="detail.name"
          />
          <div v-else class="detail-hero__cover-placeholder" />
          <div class="detail-hero__overlay" />
        </div>

        <div class="detail-hero__content">
          <div class="detail-hero__meta">
            <span v-if="detail.categoryName" class="detail-hero__tag">
              {{ detail.categoryName }}
            </span>
            <span v-if="detail.region" class="detail-hero__region">
              <el-icon><LocationFilled /></el-icon>
              {{ detail.region }}
            </span>
          </div>
          <h1 class="detail-hero__title">{{ detail.name }}</h1>
        </div>
      </section>

      <div class="detail-body">
        <!-- 项目简介 -->
        <section v-if="detail.description" class="detail-section">
          <h2 class="detail-section__title">项目简介</h2>
          <p class="detail-section__text">{{ detail.description }}</p>
        </section>

        <div class="yy-divider" />

        <!-- 历史背景 -->
        <section v-if="detail.history" class="detail-section">
          <h2 class="detail-section__title">历史背景</h2>
          <p class="detail-section__text">{{ detail.history }}</p>
        </section>

        <div v-if="detail.history && detail.culturalMeaning" class="yy-divider" />

        <!-- 文化内涵 -->
        <section v-if="detail.culturalMeaning" class="detail-section">
          <h2 class="detail-section__title">文化内涵</h2>
          <p class="detail-section__text">{{ detail.culturalMeaning }}</p>
        </section>

        <div v-if="(detail.culturalMeaning || detail.history) && detail.craft" class="yy-divider" />

        <!-- 工艺技法 -->
        <section v-if="detail.craft" class="detail-section">
          <h2 class="detail-section__title">工艺技法</h2>
          <p class="detail-section__text">{{ detail.craft }}</p>
        </section>

        <!-- 知识文档 -->
        <section v-if="detail.documents && detail.documents.length" class="detail-section">
          <h2 class="detail-section__title">相关知识与参考来源</h2>
          <div class="doc-list">
            <div
              v-for="doc in detail.documents"
              :key="doc.id"
              class="doc-card"
            >
              <div class="doc-card__header" @click="toggleDoc(doc.id)">
                <div class="doc-card__meta">
                  <span v-if="doc.docType" class="doc-card__type">{{ doc.docType }}</span>
                  <h3 class="doc-card__title">{{ doc.title }}</h3>
                </div>
                <el-icon
                  class="doc-card__arrow"
                  :class="{ expanded: expandedDocs.has(doc.id) }"
                >
                  <ArrowDown />
                </el-icon>
              </div>

              <div v-show="expandedDocs.has(doc.id)" class="doc-card__body">
                <p class="doc-card__content">{{ doc.content }}</p>
                <div class="doc-card__source">
                  <span v-if="doc.author">作者：{{ doc.author }}</span>
                  <span v-if="doc.source">来源：{{ doc.source }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- AI 创作入口 -->
        <section class="create-entry">
          <div class="create-entry__inner">
            <div class="create-entry__text">
              <h2 class="create-entry__title">让传统文化成为你的创作灵感</h2>
              <p class="create-entry__desc">
                基于当前非遗项目「{{ detail.name }}」，探索传统技艺的现代表达。
              </p>
              <p class="create-entry__hint">
                <el-icon><InfoFilled /></el-icon>
                文化背景将随创作请求一起传递
              </p>
            </div>
            <div class="create-entry__action">
              <el-button type="primary" size="large" @click="goCreate">
                开始创作
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </section>
      </div>
    </template>

    <EmptyState
      v-if="!loading && !detail"
      title="项目不存在"
      description="该非遗项目可能已被删除"
    />
  </div>
</template>

<style scoped>
.detail-view {
  background: var(--yy-color-surface);
}

/* ===== 封面横幅 ===== */
.detail-hero {
  position: relative;
  height: 420px;
  overflow: hidden;
}

.detail-hero__cover {
  position: absolute;
  inset: 0;
}

.detail-hero__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-hero__cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--yy-color-accent), var(--yy-color-border));
}

.detail-hero__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    rgba(39, 39, 34, 0.8) 0%,
    rgba(39, 39, 34, 0.2) 60%,
    rgba(39, 39, 34, 0.1) 100%
  );
}

.detail-hero__content {
  position: relative;
  z-index: 1;
  height: 100%;
  max-width: var(--yy-content-width);
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding-bottom: 40px;
  color: #fff;
}

.detail-hero__meta {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-hero__tag {
  background: var(--yy-color-primary);
  color: #fff;
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 4px;
}

.detail-hero__region {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  opacity: 0.9;
}

.detail-hero__title {
  font-family: var(--yy-font-serif);
  font-size: 40px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

/* ===== 正文 ===== */
.detail-body {
  max-width: 800px;
  margin: 0 auto;
  padding: 48px var(--yy-page-padding) 80px;
}

.detail-section {
  margin-bottom: 8px;
}

.detail-section__title {
  font-size: 20px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 16px;
  padding-left: 12px;
  border-left: 3px solid var(--yy-color-primary);
}

.detail-section__text {
  font-size: 15px;
  line-height: 1.9;
  color: var(--yy-color-text-secondary);
  margin: 0;
}

/* ===== 知识文档 ===== */
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-card {
  background: var(--yy-color-bg);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  overflow: hidden;
}

.doc-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  transition: background var(--yy-transition);
}

.doc-card__header:hover {
  background: rgba(120, 47, 53, 0.04);
}

.doc-card__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.doc-card__type {
  font-size: 12px;
  color: var(--yy-color-primary);
  background: rgba(120, 47, 53, 0.08);
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.doc-card__title {
  font-size: 15px;
  font-weight: 500;
  color: var(--yy-color-text);
  margin: 0;
}

.doc-card__arrow {
  color: var(--yy-color-muted);
  transition: transform var(--yy-transition);
  font-size: 14px;
}

.doc-card__arrow.expanded {
  transform: rotate(180deg);
}

.doc-card__body {
  padding: 0 20px 20px;
  border-top: 1px solid var(--yy-color-border);
}

.doc-card__content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--yy-color-text-secondary);
  margin: 16px 0 0;
}

.doc-card__source {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--yy-color-border);
  font-size: 12px;
  color: var(--yy-color-muted);
}

/* ===== AI 创作入口 ===== */
.create-entry {
  margin-top: 56px;
  background: var(--yy-color-bg);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  padding: 32px;
}

.create-entry__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.create-entry__text {
  flex: 1;
}

.create-entry__title {
  font-size: 20px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 8px;
}

.create-entry__desc {
  font-size: 14px;
  color: var(--yy-color-text-secondary);
  margin: 0 0 8px;
}

.create-entry__hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--yy-color-muted);
  margin: 0;
}

.create-entry__action .el-button {
  white-space: nowrap;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .detail-hero {
    height: 320px;
  }

  .detail-hero__title {
    font-size: 28px;
  }

  .create-entry__inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .create-entry__action {
    width: 100%;
  }

  .create-entry__action .el-button {
    width: 100%;
  }
}
</style>