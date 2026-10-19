<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useHeritageStore } from '@/stores/heritage'
import HeritageCard from '@/components/HeritageCard.vue'

const router = useRouter()
const heritageStore = useHeritageStore()

const featured = ref([])
const categories = ref([])
const loading = ref(false)

const aiQuestions = [
  '苗族银饰为什么具有独特的文化意义？',
  '传统刺绣工艺有哪些常见针法？',
  '景德镇青花瓷的历史渊源是什么？',
  '剪纸艺术在不同地区有什么差异？',
]

async function loadData() {
  loading.value = true
  try {
    const [page, cats] = await Promise.all([
      heritageStore.fetchProjects({ pageNum: 1, pageSize: 4 }),
      heritageStore.loadCategories(),
    ])
    featured.value = page.records || []
    categories.value = cats || []
  } catch {
    // 接口异常时静默处理，页面仍可展示静态内容
  } finally {
    loading.value = false
  }
}

function goGallery() {
  router.push({ name: 'Gallery' })
}

function goCreate() {
  router.push({ name: 'Create' })
}

function goAiGuide(question) {
  router.push({ name: 'AiGuide', query: question ? { q: question } : {} })
}

onMounted(loadData)
</script>

<template>
  <div class="home-view">
    <!-- ===== 01 Hero 首屏 ===== -->
    <section class="hero">
      <div class="hero__bg" />
      <div class="hero__overlay" />
      <div class="hero__inner">
        <p class="hero__eyebrow">非遗文化 · AI 创作平台</p>
        <h1 class="hero__title">
          让千年技艺，<br />在 AI 时代焕发新生
        </h1>
        <p class="hero__subtitle">
          基于 RAG 的非遗文化智能问答与 AI 图像创作平台，<br />
          以 AI 理解传统，以创意延续文化。
        </p>
        <div class="hero__actions">
          <el-button type="primary" size="large" @click="goGallery">
            探索非遗
            <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button size="large" class="hero__btn-ghost" @click="goCreate">
            开始创作
          </el-button>
        </div>
      </div>
    </section>

    <!-- ===== 02 精选非遗 ===== -->
    <section class="section">
      <div class="section__inner">
        <div class="section__header">
          <h2 class="section__title">精选非遗</h2>
          <p class="section__desc">探索国家级非物质文化遗产代表性项目</p>
        </div>

        <div v-loading="loading" class="featured-grid">
          <HeritageCard
            v-for="project in featured"
            :key="project.id"
            :project="project"
          />
        </div>

        <div class="section__more">
          <el-button @click="goGallery">
            查看全部非遗
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </section>

    <!-- ===== 03 非遗知识探索 ===== -->
    <section class="section section--alt">
      <div class="section__inner">
        <div class="section__header">
          <h2 class="section__title">非遗知识探索</h2>
          <p class="section__desc">按类别浏览不同类型的传统技艺与文化</p>
        </div>

        <div v-if="categories.length" class="category-grid">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="category-card"
            @click="goGallery"
          >
            <div class="category-card__icon">
              <el-icon :size="28"><Collection /></el-icon>
            </div>
            <h3 class="category-card__name">{{ cat.name }}</h3>
            <p v-if="cat.description" class="category-card__desc">
              {{ cat.description }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 04 AI 文化向导 ===== -->
    <section class="section">
      <div class="section__inner">
        <div class="section__header">
          <h2 class="section__title">AI 文化向导</h2>
          <p class="section__desc">基于非遗知识库的智能问答，回答均附参考来源</p>
        </div>

        <div class="guide-entry">
          <div class="guide-entry__text">
            <el-icon class="guide-entry__icon" :size="32" color="var(--yy-color-primary)">
              <ChatDotRound />
            </el-icon>
            <h3 class="guide-entry__title">有任何关于非遗的问题？</h3>
            <p class="guide-entry__desc">
              输入你的问题，AI 文化向导将基于非遗知识库为你解答，并标注参考来源。
            </p>
          </div>

          <div class="guide-entry__questions">
            <button
              v-for="(q, i) in aiQuestions"
              :key="i"
              class="question-chip"
              @click="goAiGuide(q)"
            >
              <el-icon><ChatDotRound /></el-icon>
              {{ q }}
            </button>
          </div>

          <div class="guide-entry__action">
            <el-button type="primary" size="large" @click="goAiGuide()">
              进入 AI 文化向导
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 05 AI 创作展示 ===== -->
    <section class="section section--alt">
      <div class="section__inner">
        <div class="section__header">
          <h2 class="section__title">AI 创作展示</h2>
          <p class="section__desc">基于非遗文化，让传统技艺焕发全新表达</p>
        </div>

        <div class="create-showcase">
          <div class="showcase-card">
            <div class="showcase-card__cover showcase-card__cover--1" />
            <div class="showcase-card__body">
              <p class="showcase-card__type">文生图</p>
              <h3 class="showcase-card__title">苗族银饰现代设计</h3>
              <p class="showcase-card__desc">融合传统银饰工艺与现代产品设计</p>
            </div>
          </div>

          <div class="showcase-card">
            <div class="showcase-card__cover showcase-card__cover--2" />
            <div class="showcase-card__body">
              <p class="showcase-card__type">AI 短视频</p>
              <h3 class="showcase-card__title">刺绣工艺动态展示</h3>
              <p class="showcase-card__desc">以动态影像呈现苏绣针法之美</p>
            </div>
          </div>

          <div class="showcase-card showcase-card--cta" @click="goCreate">
            <el-icon class="showcase-card__plus" :size="40"><Plus /></el-icon>
            <p class="showcase-card__cta-text">开始你的创作</p>
            <p class="showcase-card__cta-hint">基于非遗文化，生成独特作品</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-view {
  background: var(--yy-color-bg);
}

/* ===== 通用区块 ===== */
.section {
  padding: 80px 0;
}

.section--alt {
  background: var(--yy-color-surface);
}

.section__inner {
  max-width: var(--yy-content-width);
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
}

.section__header {
  text-align: center;
  margin-bottom: 48px;
}

.section__title {
  font-family: var(--yy-font-serif);
  font-size: 32px;
  font-weight: 700;
  color: var(--yy-color-text);
  margin: 0 0 8px;
}

.section__desc {
  font-size: 15px;
  color: var(--yy-color-muted);
  margin: 0;
}

.section__more {
  text-align: center;
  margin-top: 40px;
}

/* ===== Hero ===== */
.hero {
  position: relative;
  min-height: 560px;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.hero__bg {
  position: absolute;
  inset: 0;
  background-image: url('https://images.unsplash.com/photo-1528459801416-a9e53bbf4e17?w=1600&q=80');
  background-size: cover;
  background-position: center;
}

.hero__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(39, 39, 34, 0.82) 0%,
    rgba(39, 39, 34, 0.55) 50%,
    rgba(120, 47, 53, 0.45) 100%
  );
}

.hero__inner {
  position: relative;
  z-index: 1;
  max-width: var(--yy-content-width);
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
  text-align: center;
  color: #fff;
}

.hero__eyebrow {
  font-size: 14px;
  letter-spacing: 6px;
  opacity: 0.8;
  margin: 0 0 20px;
}

.hero__title {
  font-family: var(--yy-font-serif);
  font-size: 56px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 24px;
}

.hero__subtitle {
  font-size: 17px;
  line-height: 1.8;
  opacity: 0.9;
  margin: 0 0 40px;
}

.hero__actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.hero__btn-ghost {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.5);
  color: #fff;
}

.hero__btn-ghost:hover {
  background: rgba(255, 255, 255, 0.25) !important;
  border-color: #fff !important;
  color: #fff !important;
}

/* ===== 精选非遗 ===== */
.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  min-height: 200px;
}

/* ===== 分类探索 ===== */
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.category-card {
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  padding: 28px 24px;
  text-align: center;
  cursor: pointer;
  transition: all var(--yy-transition);
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--yy-shadow-hover);
  border-color: var(--yy-color-primary);
}

.category-card__icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  background: rgba(120, 47, 53, 0.08);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--yy-color-primary);
}

.category-card:hover .category-card__icon {
  background: var(--yy-color-primary);
  color: #fff;
}

.category-card__name {
  font-size: 17px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 8px;
}

.category-card__desc {
  font-size: 13px;
  color: var(--yy-color-muted);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== AI 向导入口 ===== */
.guide-entry {
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  padding: 48px;
  text-align: center;
}

.guide-entry__text {
  margin-bottom: 32px;
}

.guide-entry__icon {
  margin-bottom: 16px;
}

.guide-entry__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 8px;
}

.guide-entry__desc {
  font-size: 15px;
  color: var(--yy-color-muted);
  margin: 0;
}

.guide-entry__questions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-bottom: 32px;
}

.question-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: var(--yy-color-bg);
  border: 1px solid var(--yy-color-border);
  border-radius: 20px;
  font-size: 14px;
  color: var(--yy-color-text-secondary);
  cursor: pointer;
  transition: all var(--yy-transition);
}

.question-chip:hover {
  background: var(--yy-color-primary);
  color: #fff;
  border-color: var(--yy-color-primary);
}

.question-chip .el-icon {
  font-size: 14px;
}

/* ===== AI 创作展示 ===== */
.create-showcase {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.showcase-card {
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  overflow: hidden;
  transition: all var(--yy-transition);
}

.showcase-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--yy-shadow-hover);
}

.showcase-card__cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  background-size: cover;
  background-position: center;
}

.showcase-card__cover--1 {
  background: linear-gradient(135deg, #d7c8ad, #c9a96e);
}

.showcase-card__cover--2 {
  background: linear-gradient(135deg, #b87a7e, #782f35);
}

.showcase-card__body {
  padding: 20px;
}

.showcase-card__type {
  font-size: 12px;
  color: var(--yy-color-primary);
  background: rgba(120, 47, 53, 0.08);
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  margin: 0 0 10px;
}

.showcase-card__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 6px;
}

.showcase-card__desc {
  font-size: 13px;
  color: var(--yy-color-muted);
  margin: 0;
}

.showcase-card--cta {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: var(--yy-color-bg);
  cursor: pointer;
  min-height: 280px;
}

.showcase-card__plus {
  color: var(--yy-color-primary);
  margin-bottom: 12px;
}

.showcase-card__cta-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 6px;
}

.showcase-card__cta-hint {
  font-size: 13px;
  color: var(--yy-color-muted);
  margin: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 1100px) {
  .featured-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .section {
    padding: 48px 0;
  }

  .hero {
    min-height: 440px;
  }

  .hero__title {
    font-size: 32px;
  }

  .hero__subtitle {
    font-size: 15px;
  }

  .section__title {
    font-size: 24px;
  }

  .featured-grid {
    grid-template-columns: 1fr;
  }

  .category-grid {
    grid-template-columns: 1fr;
  }

  .create-showcase {
    grid-template-columns: 1fr;
  }

  .guide-entry {
    padding: 32px 20px;
  }
}
</style>