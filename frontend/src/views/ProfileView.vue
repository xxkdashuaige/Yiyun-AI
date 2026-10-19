<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/auth'
import { getWorks } from '@/api/works'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const workCount = ref(0)

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const passwordFormRef = ref(null)

async function loadStats() {
  loading.value = true
  try {
    const res = await getWorks({ pageNum: 1, pageSize: 1 })
    workCount.value = res.total || 0
  } catch (e) {
    workCount.value = 0
  } finally {
    loading.value = false
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  try {
    await changePassword(passwordForm.value.oldPassword, passwordForm.value.newPassword)
    ElMessage.success('密码修改成功')
    passwordFormRef.value.resetFields()
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  }
}

function handleLogout() {
  userStore.logout()
  router.push({ name: 'Home' })
}

function goWorks() {
  router.push({ name: 'Works' })
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  userStore.refreshUserInfo()
  loadStats()
})
</script>

<template>
  <div v-loading="loading" class="profile-view">
    <div class="container">
      <header class="page-header">
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理你的账号信息与安全设置</p>
      </header>

      <section class="profile-card">
        <div class="profile-left">
          <div class="avatar">
            <el-icon :size="40"><User /></el-icon>
          </div>
          <div class="profile-info">
            <h2 class="nickname">{{ userStore.userInfo?.nickname || '未设置昵称' }}</h2>
            <p class="username">@{{ userStore.userInfo?.username }}</p>
            <p class="register-time">
              <el-icon><Clock /></el-icon>
              注册于 {{ formatDate(userStore.userInfo?.createdAt) }}
            </p>
          </div>
        </div>

        <div class="profile-stats">
          <div class="stat-item" @click="goWorks">
            <span class="stat-num">{{ workCount }}</span>
            <span class="stat-label">我的作品</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <h3 class="panel-title">修改密码</h3>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="90px"
          class="password-form"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
              autocomplete="current-password"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码（至少 6 位）"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="panel danger-zone">
        <h3 class="panel-title">账号操作</h3>
        <p class="danger-hint">退出登录后需要重新登录才能继续使用创作功能。</p>
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.profile-view {
  background: var(--yy-color-bg);
  min-height: calc(100vh - 64px);
  padding: 32px 0 48px;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--yy-font-serif);
  font-size: 28px;
  color: var(--yy-color-text);
  margin: 0 0 6px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--yy-color-muted);
  margin: 0;
}

.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  padding: 28px 32px;
  margin-bottom: 24px;
  box-shadow: var(--yy-shadow-card);
}

.profile-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--yy-color-primary), #a0404a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.profile-info {
  min-width: 0;
}

.nickname {
  font-family: var(--yy-font-serif);
  font-size: 22px;
  color: var(--yy-color-text);
  margin: 0 0 4px;
}

.username {
  font-size: 14px;
  color: var(--yy-color-muted);
  margin: 0 0 6px;
}

.register-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--yy-color-text-secondary);
  margin: 0;
}

.profile-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  text-align: center;
  cursor: pointer;
  padding: 12px 20px;
  border-radius: 10px;
  background: var(--yy-color-bg);
  border: 1px solid var(--yy-color-border);
  transition: all var(--yy-transition);
}

.stat-item:hover {
  border-color: var(--yy-color-primary);
}

.stat-num {
  display: block;
  font-family: var(--yy-font-serif);
  font-size: 28px;
  font-weight: 700;
  color: var(--yy-color-primary);
  line-height: 1.2;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--yy-color-muted);
  margin-top: 4px;
}

.panel {
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  padding: 24px 28px;
  margin-bottom: 20px;
  box-shadow: var(--yy-shadow-card);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--yy-color-border);
}

.password-form {
  max-width: 480px;
}

.danger-zone {
  border-color: #e8c4c4;
}

.danger-hint {
  font-size: 13px;
  color: var(--yy-color-muted);
  margin: 0 0 16px;
}

@media (max-width: 768px) {
  .profile-card {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px;
  }

  .profile-stats {
    width: 100%;
  }

  .stat-item {
    flex: 1;
  }
}
</style>