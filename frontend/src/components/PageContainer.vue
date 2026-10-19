<script setup>
</script>

<template>
  <div class="page-container">
    <slot />
  </div>
</template>

<style scoped>
.page-container {
  width: 100%;
  max-width: var(--yy-content-width);
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
}

@media (max-width: 768px) {
  .page-container {
    padding: 0 16px;
  }
}
</style>