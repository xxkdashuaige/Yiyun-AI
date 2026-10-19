<script setup>
import { ref, nextTick, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { chat } from '@/api/ai'
import { useHeritageStore } from '@/stores/heritage'

const route = useRoute()
const heritageStore = useHeritageStore()

const messages = ref([])
const input = ref('')
const loading = ref(false)
const messagesEl = ref(null)

const heritageProjectId = computed(() => heritageStore.currentHeritage?.id || null)
const heritageName = computed(() => heritageStore.currentHeritage?.name || '')

const SUGGESTIONS = [
  '苗族银饰为什么具有独特的文化意义？',
  '昆曲的表演手法有哪些特点？',
  '介绍一下景德镇青花瓷的烧制工艺',
  '中国书法的主要书体有哪些？',
]

function scrollToBottom() {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

function buildHistory() {
  return messages.value
    .filter((m) => !m.loading && !m.error)
    .map((m) => ({ role: m.role, content: m.content }))
}

async function send(text) {
  const msg = (text || input.value).trim()
  if (!msg || loading.value) return

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: msg,
  })
  input.value = ''
  scrollToBottom()

  const placeholderId = Date.now() + 1
  messages.value.push({
    id: placeholderId,
    role: 'assistant',
    content: '',
    sources: [],
    loading: true,
    error: null,
  })
  loading.value = true
  scrollToBottom()

  try {
    const res = await chat(msg, heritageProjectId.value, buildHistory().slice(0, -1))
    const target = messages.value.find((m) => m.id === placeholderId)
    if (target) {
      target.content = res.reply || '（暂无回答内容）'
      target.sources = res.sources || []
      target.loading = false
    }
  } catch (e) {
    const target = messages.value.find((m) => m.id === placeholderId)
    if (target) {
      target.content = ''
      target.loading = false
      target.error = e.message || 'AI 服务暂时不可用，请稍后重试'
    }
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function clearHeritageScope() {
  heritageStore.clearCurrentHeritage()
}

onMounted(() => {
  const q = route.query.q
  if (q) {
    nextTick(() => send(q))
  }
})
</script>

<template>
  <div class="ai-guide">
    <div class="container">
      <header class="page-header">
        <h1 class="page-title">AI 文化向导</h1>
        <p class="page-subtitle">基于非遗知识库的智能问答，每个回答附带文献来源，可追溯可验证</p>
        <div v-if="heritageProjectId" class="scope-bar">
          <el-tag type="info" effect="plain" closable @close="clearHeritageScope">
            检索范围：{{ heritageName }}
          </el-tag>
        </div>
      </header>

      <div ref="messagesEl" class="chat-messages">
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-icon">
            <el-icon :size="40"><ChatDotRound /></el-icon>
          </div>
          <h3 class="welcome-title">你好，我是遗韵 AI 向导</h3>
          <p class="welcome-text">我可以为你解答关于非物质文化遗产的历史、工艺、文化内涵等问题。</p>
          <div class="suggestions">
            <el-tag
              v-for="s in SUGGESTIONS"
              :key="s"
              class="suggestion-chip"
              @click="send(s)"
            >
              {{ s }}
            </el-tag>
          </div>
        </div>

        <div
          v-for="m in messages"
          :key="m.id"
          class="message"
          :class="m.role"
        >
          <div v-if="m.role === 'assistant'" class="avatar ai-avatar">
            <el-icon><MagicStick /></el-icon>
          </div>
          <div class="bubble-wrap">
            <div
              v-if="m.role === 'user' || m.content"
              class="bubble"
              :class="{ 'bubble-user': m.role === 'user' }"
            >
              <template v-if="m.loading">
                <span class="typing">
                  <i></i><i></i><i></i>
                </span>
              </template>
              <template v-else-if="m.error">
                <span class="error-text">
                  <el-icon><Warning /></el-icon>
                  {{ m.error }}
                </span>
              </template>
              <template v-else>
                <p class="bubble-text">{{ m.content }}</p>
              </template>
            </div>

            <div v-if="m.role === 'assistant' && m.sources && m.sources.length > 0" class="sources">
              <el-collapse class="sources-collapse">
                <el-collapse-item title="引用来源" name="1">
                  <div
                    v-for="(src, idx) in m.sources"
                    :key="src.chunkId || idx"
                    class="source-item"
                  >
                    <div class="source-head">
                      <span class="source-title">{{ src.title || '未命名文档' }}</span>
                      <el-tag v-if="src.docType" type="info" size="small" effect="plain">{{ src.docType }}</el-tag>
                    </div>
                    <p class="source-content">{{ src.content }}</p>
                    <div v-if="src.source" class="source-meta">来源：{{ src.source }}</div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
          <div v-if="m.role === 'user'" class="avatar user-avatar">
            <el-icon><User /></el-icon>
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="输入你的问题，按 Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="send()"
          :disabled="loading"
        />
        <el-button
          type="primary"
          class="send-btn"
          :loading="loading"
          :disabled="!input.trim()"
          @click="send()"
        >
          <el-icon><Promotion /></el-icon>
          <span>发送</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-guide {
  background: var(--yy-color-bg);
  min-height: calc(100vh - 64px);
  padding: 40px 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 80px);
}

.page-header {
  text-align: center;
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--yy-font-serif);
  font-size: 32px;
  color: var(--yy-color-text);
  margin: 0 0 8px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--yy-color-muted);
  margin: 0 0 16px;
}

.scope-bar {
  display: flex;
  justify-content: center;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  box-shadow: var(--yy-shadow-card);
  scroll-behavior: smooth;
}

.welcome {
  text-align: center;
  padding: 48px 16px;
  color: var(--yy-color-text-secondary);
}

.welcome-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--yy-color-primary), #a0404a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.welcome-title {
  font-family: var(--yy-font-serif);
  font-size: 22px;
  color: var(--yy-color-text);
  margin: 0 0 8px;
}

.welcome-text {
  font-size: 14px;
  color: var(--yy-color-muted);
  margin: 0 0 24px;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 640px;
  margin: 0 auto;
}

.el-tag.suggestion-chip {
  cursor: pointer;
  padding: 8px 14px;
  font-size: 13px;
  border-radius: 20px;
  transition: all var(--yy-transition);
  color: #782f35 !important;
  background-color: #efdcde !important;
  border-color: #b87a7e !important;
}

.el-tag.suggestion-chip:hover {
  background: #782f35 !important;
  color: #fff !important;
  border-color: #782f35 !important;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.ai-avatar {
  background: linear-gradient(135deg, var(--yy-color-primary), #a0404a);
}

.user-avatar {
  background: var(--yy-color-text-secondary);
}

.bubble-wrap {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message.user .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  padding: 12px 16px;
  border-radius: var(--yy-radius-card);
  line-height: 1.7;
  font-size: 14px;
  color: var(--yy-color-text);
  background: var(--yy-color-bg);
  border: 1px solid var(--yy-color-border);
  border-top-left-radius: 4px;
}

.bubble-user {
  background: var(--yy-color-primary);
  color: #fff;
  border-color: var(--yy-color-primary);
  border-top-left-radius: var(--yy-radius-card);
  border-top-right-radius: 4px;
}

.bubble-user .bubble-text {
  color: #fff;
}

.bubble-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.typing {
  display: inline-flex;
  gap: 4px;
  padding: 4px 0;
}

.typing i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--yy-color-muted);
  animation: typing 1.4s infinite;
}

.typing i:nth-child(2) { animation-delay: 0.2s; }
.typing i:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-4px); }
}

.error-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--yy-color-danger);
  font-size: 13px;
}

.sources {
  width: 100%;
}

.sources-collapse {
  --el-collapse-border-color: var(--yy-color-border);
}

.source-item {
  padding: 12px;
  background: var(--yy-color-bg);
  border-radius: 8px;
  margin-bottom: 8px;
  border: 1px solid var(--el-border-color-light);
}

.source-item:last-child {
  margin-bottom: 0;
}

.source-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  gap: 8px;
}

.source-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--yy-color-primary);
}

.source-content {
  font-size: 12px;
  color: var(--yy-color-text-secondary);
  line-height: 1.6;
  margin: 0 0 4px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.source-meta {
  font-size: 11px;
  color: var(--yy-color-muted);
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-top: 20px;
  padding: 16px;
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  box-shadow: var(--yy-shadow-card);
}

.input-area :deep(.el-textarea__inner) {
  font-size: 14px;
}

.input-area :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--yy-color-primary) inset !important;
  outline: none !important;
}

.send-btn {
  height: auto;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .container {
    padding: 0 16px;
    height: calc(100vh - 64px - 32px);
  }

  .page-title {
    font-size: 24px;
  }

  .bubble-wrap {
    max-width: 85%;
  }

  .input-area {
    flex-direction: column;
    align-items: stretch;
  }

  .send-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>