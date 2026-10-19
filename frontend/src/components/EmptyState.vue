<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据',
  },
  description: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__icon">
      <el-icon :size="48" color="var(--yy-color-accent)"><Document /></el-icon>
    </div>
    <p class="empty-state__title">{{ title }}</p>
    <p v-if="description" class="empty-state__desc">{{ description }}</p>
  </div>
</template>

<style scoped>
.empty-state {
  text-align: center;
  padding: 64px 24px;
}

.empty-state__icon {
  margin-bottom: 16px;
}

.empty-state__title {
  font-size: 16px;
  color: var(--yy-color-text-secondary);
  margin: 0;
}

.empty-state__desc {
  font-size: 14px;
  color: var(--yy-color-muted);
  margin-top: 8px;
}
</style>