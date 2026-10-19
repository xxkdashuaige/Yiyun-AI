<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  project: {
    type: Object,
    required: true,
  },
})

const router = useRouter()

function goDetail() {
  router.push({ name: 'HeritageDetail', params: { id: props.project.id } })
}
</script>

<template>
  <div class="heritage-card" @click="goDetail">
    <div class="heritage-card__cover">
      <img
        v-if="project.coverUrl"
        :src="project.coverUrl"
        :alt="project.name"
        loading="lazy"
      />
      <div v-else class="heritage-card__cover-placeholder">
        <span>{{ project.name?.charAt(0) }}</span>
      </div>
      <div v-if="project.categoryName" class="heritage-card__category">
        {{ project.categoryName }}
      </div>
    </div>

    <div class="heritage-card__body">
      <h3 class="heritage-card__name">{{ project.name }}</h3>
      <p v-if="project.region" class="heritage-card__region">
        <el-icon><LocationFilled /></el-icon>
        {{ project.region }}
      </p>
      <p v-if="project.description" class="heritage-card__desc">
        {{ project.description }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.heritage-card {
  background: var(--yy-color-surface);
  border-radius: var(--yy-radius-card);
  border: 1px solid var(--yy-color-border);
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--yy-transition), box-shadow var(--yy-transition);
  display: flex;
  flex-direction: column;
}

.heritage-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--yy-shadow-hover);
}

.heritage-card__cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--yy-color-accent);
}

.heritage-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.heritage-card:hover .heritage-card__cover img {
  transform: scale(1.05);
}

.heritage-card__cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--yy-color-accent), var(--yy-color-border));
  font-family: var(--yy-font-serif);
  font-size: 48px;
  color: var(--yy-color-primary);
  opacity: 0.6;
}

.heritage-card__category {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--yy-color-primary);
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
  font-weight: 500;
}

.heritage-card__body {
  padding: 16px 18px 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.heritage-card__name {
  font-size: 17px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 6px;
  line-height: 1.4;
}

.heritage-card__region {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--yy-color-muted);
  margin: 0 0 8px;
}

.heritage-card__region .el-icon {
  font-size: 14px;
}

.heritage-card__desc {
  font-size: 13px;
  color: var(--yy-color-text-secondary);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>