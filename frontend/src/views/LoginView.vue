<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import logoImg from '@/assets/logo.png'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref(null)
const mode = ref('login')

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度需在 4-20 位之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6-20 位之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  nickname: [
    { max: 20, message: '昵称长度不能超过 20 位', trigger: 'blur' },
  ],
}

function switchMode(target) {
  mode.value = target
  formRef.value?.clearValidate()
  form.username = ''
  form.password = ''
  form.confirmPassword = ''
  form.nickname = ''
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (mode.value === 'login') {
      await userStore.login(form.username, form.password)
      ElMessage.success('登录成功')
    } else {
      await userStore.register({
        username: form.username,
        password: form.password,
        confirmPassword: form.confirmPassword,
        nickname: form.nickname,
      })
      ElMessage.success('注册成功')
    }
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-view">
    <!-- 左侧：文化视觉区 -->
    <section class="login-visual">
      <div class="login-visual__overlay" />
      <div class="login-visual__content">
        <div class="login-visual__brand">
          <img :src="logoImg" alt="遗韵" class="login-visual__logo-img" />
          <div class="login-visual__brand-text">
            <span class="login-visual__logo">遗韵</span>
            <span class="login-visual__tag">非遗文化智能创作平台</span>
          </div>
        </div>

        <div class="login-visual__slogan">
          <h1 class="login-visual__title">
            让千年技艺，<br />焕发新生
          </h1>
          <p class="login-visual__desc">以 AI 理解传统，以创意延续文化。</p>
        </div>

        <div class="login-visual__keywords">
          <span>传承</span>
          <span class="dot">·</span>
          <span>探索</span>
          <span class="dot">·</span>
          <span>创造</span>
        </div>
      </div>
    </section>

    <!-- 右侧：登录/注册操作区 -->
    <section class="login-panel">
      <div class="login-panel__inner">
        <div class="login-panel__header">
          <h2 class="login-panel__title">
            {{ mode === 'login' ? '欢迎回来' : '加入遗韵' }}
          </h2>
          <p class="login-panel__subtitle">
            {{ mode === 'login' ? '登录遗韵，继续探索与创作' : '创建账号，开启非遗文化创作之旅' }}
          </p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          label-position="top"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="4-20 位字母、数字或下划线"
              size="large"
              clearable
              :prefix-icon="User"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="6-20 位密码"
              size="large"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'register'" label="昵称" prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="选填，不超过 20 位"
              size="large"
              clearable
              :prefix-icon="UserFilled"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-form__submit"
              :loading="loading"
              @click="handleSubmit"
            >
              {{ mode === 'login' ? '登 录' : '注 册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-panel__footer">
          <template v-if="mode === 'login'">
            <span class="login-panel__hint">还没有账号？</span>
            <el-link type="primary" :underline="false" @click="switchMode('register')">立即注册</el-link>
          </template>
          <template v-else>
            <span class="login-panel__hint">已有账号？</span>
            <el-link type="primary" :underline="false" @click="switchMode('login')">立即登录</el-link>
          </template>
        </div>

        <p v-if="mode === 'login'" class="login-panel__demo">演示账号：admin / 123456</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-view {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

/* ===== 左侧视觉区 ===== */
.login-visual {
  position: relative;
  flex: 0 0 55%;
  background-image: url('https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?w=1200&q=80');
  background-size: cover;
  background-position: center;
  overflow: hidden;
}

.login-visual__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    rgba(39, 39, 34, 0.75) 0%,
    rgba(39, 39, 34, 0.35) 50%,
    rgba(39, 39, 34, 0.15) 100%
  );
}

.login-visual__content {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px;
  color: #fff;
}

.login-visual__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.login-visual__logo-img {
  width: 44px;
  height: 44px;
  object-fit: contain;
  border-radius: 8px;
}

.login-visual__brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.login-visual__logo {
  font-family: var(--yy-font-serif);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 6px;
  line-height: 1;
}

.login-visual__tag {
  font-size: 13px;
  opacity: 0.8;
  letter-spacing: 2px;
}

.login-visual__slogan {
  margin-bottom: auto;
  margin-top: 120px;
}

.login-visual__title {
  font-family: var(--yy-font-serif);
  font-size: 48px;
  font-weight: 700;
  line-height: 1.4;
  color: #fff;
  margin: 0 0 20px;
}

.login-visual__desc {
  font-size: 17px;
  line-height: 1.8;
  opacity: 0.85;
  margin: 0;
}

.login-visual__keywords {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  letter-spacing: 4px;
  opacity: 0.75;
}

.login-visual__keywords .dot {
  opacity: 0.5;
}

/* ===== 右侧登录区 ===== */
.login-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--yy-color-bg);
  padding: 48px;
}

.login-panel__inner {
  width: 100%;
  max-width: 380px;
}

.login-panel__header {
  margin-bottom: 40px;
}

.login-panel__title {
  font-size: 28px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 8px;
}

.login-panel__subtitle {
  font-size: 14px;
  color: var(--yy-color-muted);
  margin: 0;
}

.login-form {
  margin-top: 8px;
}

.login-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: var(--yy-color-text-secondary);
  font-weight: 500;
}

.login-form__submit {
  width: 100%;
  height: 46px;
  font-size: 16px;
  letter-spacing: 8px;
  margin-top: 8px;
}

.login-panel__footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--yy-color-muted);
}

.login-panel__footer .el-link {
  margin-left: 4px;
}

.login-panel__demo {
  text-align: center;
  font-size: 13px;
  color: var(--yy-color-muted);
  margin-top: 20px;
  opacity: 0.7;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .login-visual {
    display: none;
  }

  .login-panel {
    padding: 32px 24px;
  }
}
</style>