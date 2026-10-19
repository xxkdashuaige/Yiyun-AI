<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { generateImage, getAiTaskDetail } from '@/api/ai'
import { saveWork } from '@/api/works'
import { useHeritageStore } from '@/stores/heritage'

const router = useRouter()
const heritageStore = useHeritageStore()

const mode = ref('text2img')
const prompt = ref('')
const style = ref('chinese-painting')
const size = ref('1:1')
const count = ref(1)

const generating = ref(false)
const resultImage = ref('')
const currentTaskId = ref(null)
const savedWorkId = ref(null)
const saving = ref(false)
let pollTimer = null

const heritageProjectId = computed(() => heritageStore.currentHeritage?.id || null)
const heritageName = computed(() => heritageStore.currentHeritage?.name || '')

const STYLE_OPTIONS = [
  { value: 'ink', label: '水墨', desc: '黑白写意' },
  { value: 'oil', label: '油画', desc: '色彩厚重' },
  { value: 'watercolor', label: '水彩', desc: '清透明快' },
  { value: 'chinese-painting', label: '国画', desc: '工笔重彩' },
  { value: 'realistic', label: '写实', desc: '照片级' },
]

const SIZE_OPTIONS = [
  { value: '1:1', label: '1:1 方形' },
  { value: '16:9', label: '16:9 横版' },
  { value: '9:16', label: '9:16 竖版' },
  { value: '4:3', label: '4:3 标准' },
]

const sizeClass = computed(() => {
  const map = {
    '1:1': 'aspect-square',
    '16:9': 'aspect-video',
    '9:16': 'aspect-[9/16]',
    '4:3': 'aspect-[4/3]',
  }
  return map[size.value] || 'aspect-square'
})

function startPolling(taskId) {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    try {
      const task = await getAiTaskDetail(taskId)
      if (task.status === 'SUCCESS') {
        stopPolling()
        resultImage.value = task.resultUrl
        generating.value = false
        ElMessage.success('生成完成')
      } else if (task.status === 'FAILED') {
        stopPolling()
        generating.value = false
        ElMessage.error(task.errorMessage || '生成失败，请重试')
      }
    } catch (e) {
      stopPolling()
      generating.value = false
      ElMessage.error('查询任务状态失败')
    }
  }, 1500)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function handleGenerate() {
  if (!prompt.value.trim()) {
    ElMessage.warning('请输入创作描述')
    return
  }
  generating.value = true
  resultImage.value = ''
  savedWorkId.value = null

  try {
    const task = await generateImage({
      prompt: prompt.value.trim(),
      style: style.value,
      size: size.value,
      count: count.value,
      heritageProjectId: heritageProjectId.value,
    })
    currentTaskId.value = task.id
    startPolling(task.id)
  } catch (e) {
    generating.value = false
    ElMessage.error(e.message || '提交生成失败')
  }
}

async function handleSave() {
  if (!currentTaskId.value) return
  saving.value = true
  try {
    const work = await saveWork({
      taskId: currentTaskId.value,
      title: prompt.value.trim().slice(0, 20) || '未命名作品',
      style: style.value,
      heritageProjectId: heritageProjectId.value,
    })
    savedWorkId.value = work.id
    ElMessage.success('已保存到我的作品')
    router.push(`/works/${work.id}`)
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleRegenerate() {
  handleGenerate()
}

function clearHeritageScope() {
  heritageStore.clearCurrentHeritage()
}

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <div class="create-view">
    <div class="container">
      <header class="page-header">
        <h1 class="page-title">AI 创作工作台</h1>
        <p class="page-subtitle">以非遗文化为灵感，输入文字描述，让 AI 生成专属艺术作品</p>
      </header>

      <div class="workspace">
        <aside class="param-panel">
          <div class="panel-section">
            <div class="mode-switch">
              <button
                class="mode-btn"
                :class="{ active: mode === 'text2img' }"
                @click="mode = 'text2img'"
              >
                <el-icon><EditPen /></el-icon>
                <span>文生图</span>
              </button>
              <button
                class="mode-btn"
                :class="{ active: mode === 'img2img' }"
                @click="mode = 'img2img'"
              >
                <el-icon><Picture /></el-icon>
                <span>图生图</span>
              </button>
            </div>
          </div>

          <div class="panel-section">
            <label class="field-label">
              <span>创作描述</span>
              <span class="hint">描述你想生成的画面内容</span>
            </label>
            <el-input
              v-model="prompt"
              type="textarea"
              :rows="5"
              resize="none"
              placeholder="例如：一幅苗族银饰锻造场景的国画，匠人专注敲打银片，炉火映红面庞..."
              maxlength="500"
              show-word-limit
            />
          </div>

          <div v-if="mode === 'img2img'" class="panel-section">
            <label class="field-label">参考图</label>
            <div class="upload-placeholder">
              <el-icon :size="32"><Upload /></el-icon>
              <p>点击或拖拽上传参考图</p>
              <span class="hint">支持 JPG / PNG，最大 5MB</span>
            </div>
          </div>

          <div class="panel-section">
            <label class="field-label">艺术风格</label>
            <div class="style-grid">
              <div
                v-for="opt in STYLE_OPTIONS"
                :key="opt.value"
                class="style-card"
                :class="{ active: style === opt.value }"
                @click="style = opt.value"
              >
                <div class="style-preview" :class="`style-${opt.value}`"></div>
                <div class="style-info">
                  <span class="style-name">{{ opt.label }}</span>
                  <span class="style-desc">{{ opt.desc }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="panel-section">
            <label class="field-label">画面尺寸</label>
            <div class="size-row">
              <div
                v-for="opt in SIZE_OPTIONS"
                :key="opt.value"
                class="size-chip"
                :class="{ active: size === opt.value }"
                @click="size = opt.value"
              >
                {{ opt.label }}
              </div>
            </div>
          </div>

          <div class="panel-section">
            <label class="field-label">生成数量</label>
            <el-input-number v-model="count" :min="1" :max="4" />
          </div>

          <div class="panel-section">
            <label class="field-label">非遗灵感来源</label>
            <div v-if="heritageProjectId" class="heritage-scope">
              <el-tag type="info" effect="plain" closable @close="clearHeritageScope">
                {{ heritageName }}
              </el-tag>
              <span class="hint">AI 将参考该非遗项目的知识进行创作</span>
            </div>
            <div v-else class="heritage-empty">
              <el-icon><Collection /></el-icon>
              <span>未限定，从非遗展馆选择项目后可带入</span>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="generate-btn"
            :loading="generating"
            @click="handleGenerate"
          >
            <el-icon v-if="!generating"><MagicStick /></el-icon>
            {{ generating ? '生成中...' : '开始创作' }}
          </el-button>
        </aside>

        <section class="result-panel">
          <div class="result-stage" :class="sizeClass">
            <div v-if="!resultImage && !generating" class="empty-state">
              <el-icon :size="48"><PictureFilled /></el-icon>
              <p class="empty-title">等待创作</p>
              <p class="empty-desc">在左侧输入描述，选择风格后点击「开始创作」</p>
            </div>

            <div v-else-if="generating" class="generating-state">
              <div class="gen-spinner">
                <el-icon :size="40" class="spin"><Loading /></el-icon>
              </div>
              <p class="gen-text">AI 正在以非遗技艺为灵感创作中...</p>
              <p class="gen-hint">通常需要 3-10 秒</p>
            </div>

            <img
              v-else
              :src="resultImage"
              alt="生成结果"
              class="result-image"
            />
          </div>

          <div v-if="resultImage" class="result-actions">
            <el-button
              type="primary"
              :loading="saving"
              @click="handleSave"
            >
              <el-icon><FolderChecked /></el-icon>
              保存到我的作品
            </el-button>
            <el-button @click="handleRegenerate">
              <el-icon><Refresh /></el-icon>
              重新生成
            </el-button>
            <el-button
              v-if="savedWorkId"
              type="success"
              @click="router.push(`/works/${savedWorkId}`)"
            >
              查看作品详情
            </el-button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.create-view {
  background: var(--yy-color-bg);
  min-height: calc(100vh - 64px);
  padding: 32px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--yy-page-padding);
}

.page-header {
  margin-bottom: 28px;
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

.workspace {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 24px;
  align-items: start;
}

.param-panel {
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  padding: 24px;
  box-shadow: var(--yy-shadow-card);
  position: sticky;
  top: 80px;
}

.panel-section {
  margin-bottom: 24px;
}

.panel-section:last-of-type {
  margin-bottom: 0;
}

.field-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin-bottom: 10px;
}

.hint {
  display: block;
  font-size: 12px;
  font-weight: 400;
  color: var(--yy-color-muted);
  margin-top: 2px;
}

.mode-switch {
  display: flex;
  gap: 8px;
  background: var(--yy-color-bg);
  padding: 4px;
  border-radius: 10px;
}

.mode-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  color: var(--yy-color-text-secondary);
  cursor: pointer;
  transition: all var(--yy-transition);
}

.mode-btn.active {
  background: var(--yy-color-surface);
  color: var(--yy-color-primary);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.style-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.style-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1.5px solid var(--yy-color-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--yy-transition);
}

.style-card:hover {
  border-color: var(--yy-color-primary);
}

.style-card.active {
  border-color: var(--yy-color-primary);
  background: var(--el-color-primary-light-9);
}

.style-preview {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  flex-shrink: 0;
}

.style-ink { background: linear-gradient(135deg, #2C2C2C, #555); }
.style-oil { background: linear-gradient(135deg, #5C4A3A, #8B6F47); }
.style-watercolor { background: linear-gradient(135deg, #A8C5DA, #D4E4F0); }
.style-chinese-painting { background: linear-gradient(135deg, #F0E6D2, #D4B896); }
.style-realistic { background: linear-gradient(135deg, #8B7355, #B8A89A); }

.style-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.style-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--yy-color-text);
}

.style-desc {
  font-size: 11px;
  color: var(--yy-color-muted);
}

.size-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.size-chip {
  padding: 8px 14px;
  border: 1.5px solid var(--yy-color-border);
  border-radius: 8px;
  font-size: 13px;
  color: var(--yy-color-text-secondary);
  cursor: pointer;
  transition: all var(--yy-transition);
}

.size-chip:hover {
  border-color: var(--yy-color-primary);
}

.size-chip.active {
  border-color: var(--yy-color-primary);
  background: var(--yy-color-primary);
  color: #fff;
}

.heritage-scope {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.heritage-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--yy-color-muted);
  font-size: 13px;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px;
  border: 1.5px dashed var(--yy-color-border);
  border-radius: 10px;
  color: var(--yy-color-muted);
  cursor: pointer;
  transition: all var(--yy-transition);
}

.upload-placeholder:hover {
  border-color: var(--yy-color-primary);
  color: var(--yy-color-primary);
}

.upload-placeholder p {
  margin: 0;
  font-size: 13px;
}

.generate-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.result-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-stage {
  width: 100%;
  background: var(--yy-color-surface);
  border: 1px solid var(--yy-color-border);
  border-radius: var(--yy-radius-card);
  box-shadow: var(--yy-shadow-card);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.result-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.empty-state {
  text-align: center;
  color: var(--yy-color-muted);
  padding: 40px;
}

.empty-state .el-icon {
  color: var(--yy-color-border);
}

.empty-title {
  font-size: 16px;
  color: var(--yy-color-text-secondary);
  margin: 16px 0 6px;
}

.empty-desc {
  font-size: 13px;
  margin: 0;
}

.generating-state {
  text-align: center;
  color: var(--yy-color-text-secondary);
}

.gen-spinner {
  margin-bottom: 16px;
}

.spin {
  color: var(--yy-color-primary);
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.gen-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--yy-color-text);
  margin: 0 0 6px;
}

.gen-hint {
  font-size: 12px;
  color: var(--yy-color-muted);
  margin: 0;
}

.result-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.result-actions .el-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 900px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .param-panel {
    position: static;
  }
}
</style>