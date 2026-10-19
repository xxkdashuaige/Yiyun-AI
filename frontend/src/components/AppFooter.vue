<script setup>
</script>

<template>
  <footer class="app-footer">
    <div class="app-footer__inner">
      <div class="app-footer__brand">
        <span class="app-footer__logo">遗韵</span>
        <p class="app-footer__desc">让千年技艺，在 AI 时代焕发新生</p>
      </div>
      <p class="app-footer__copy">© 2026 遗韵 · 非遗文化智能创作平台</p>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background: var(--yy-color-surface);
  border-top: 1px solid var(--yy-color-border);
  margin-top: 80px;
}

.app-footer__inner {
  max-width: var(--yy-content-width);
  margin: 0 auto;
  padding: 40px var(--yy-page-padding);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.app-footer__logo {
  font-family: var(--yy-font-serif);
  font-size: 20px;
  font-weight: 700;
  color: var(--yy-color-primary);
  letter-spacing: 4px;
}

.app-footer__desc {
  font-size: 13px;
  color: var(--yy-color-muted);
  margin-top: 4px;
}

.app-footer__copy {
  font-size: 13px;
  color: var(--yy-color-muted);
}
</style>