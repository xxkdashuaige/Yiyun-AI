<script setup>
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
</script>

<template>
  <div class="main-layout">
    <AppHeader />
    <main class="main-layout__content">
      <router-view />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
.main-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-layout__content {
  flex: 1;
}
</style>