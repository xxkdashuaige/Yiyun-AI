<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import logoImg from '@/assets/logo.png'

const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { name: 'Home', label: '首页' },
  { name: 'Gallery', label: '数字展馆' },
  { name: 'AiGuide', label: 'AI文化向导' },
  { name: 'Create', label: 'AI创作' },
  { name: 'Works', label: '我的作品' },
]

function handleCommand(command) {
  if (command === 'profile') {
    router.push({ name: 'Profile' })
  } else if (command === 'logout') {
    userStore.logout()
    router.push({ name: 'Home' })
  }
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <router-link :to="{ name: 'Home' }" class="app-header__logo">
        <img :src="logoImg" alt="遗韵" class="app-header__logo-img" />
        <span class="app-header__logo-text">遗韵</span>
      </router-link>

      <nav class="app-header__nav">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="app-header__nav-item"
          active-class="is-active"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="app-header__user">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="app-header__username">
              {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <router-link v-else :to="{ name: 'Login' }" class="app-header__login">
          登录
        </router-link>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--yy-color-surface);
  border-bottom: 1px solid var(--yy-color-border);
}

.app-header__inner {
  max-width: var(--yy-content-width);
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
  height: 64px;
  display: flex;
  align-items: center;
  gap: 48px;
}

.app-header__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.app-header__logo-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: 6px;
}

.app-header__logo-text {
  font-family: var(--yy-font-serif);
  font-size: 24px;
  font-weight: 700;
  color: var(--yy-color-primary);
  letter-spacing: 4px;
}

.app-header__nav {
  display: flex;
  gap: 32px;
  flex: 1;
}

.app-header__nav-item {
  font-size: 15px;
  color: var(--yy-color-text-secondary);
  transition: color var(--yy-transition);
  padding: 4px 0;
  position: relative;
}

.app-header__nav-item:hover,
.app-header__nav-item.is-active {
  color: var(--yy-color-primary);
}

.app-header__nav-item.is-active::after {
  content: '';
  position: absolute;
  bottom: -22px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--yy-color-primary);
}

.app-header__user {
  display: flex;
  align-items: center;
}

.app-header__username {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  color: var(--yy-color-text);
  cursor: pointer;
}

.app-header__login {
  font-size: 15px;
  color: var(--yy-color-primary);
  padding: 6px 16px;
  border: 1px solid var(--yy-color-primary);
  border-radius: var(--yy-radius-button);
  transition: all var(--yy-transition);
}

.app-header__login:hover {
  background: var(--yy-color-primary);
  color: #fff;
}

@media (max-width: 768px) {
  .app-header__inner {
    padding: 0 16px;
    gap: 16px;
  }

  .app-header__nav {
    gap: 16px;
  }

  .app-header__nav-item {
    font-size: 14px;
  }
}
</style>