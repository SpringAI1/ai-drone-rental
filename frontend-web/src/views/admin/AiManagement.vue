<template>
  <div class="ai-management-page">
    <h1 class="page-title">AI助手管理</h1>

    <div class="ai-content">
      <div class="ai-status-card">
        <div class="ai-status-header">
          <div class="ai-status-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2Z" stroke="currentColor" stroke-width="2"/>
              <path d="M8 14C8 14 9.5 16 12 16C14.5 16 16 14 16 14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <circle cx="9" cy="10" r="1" fill="currentColor"/>
              <circle cx="15" cy="10" r="1" fill="currentColor"/>
            </svg>
          </div>
          <div class="ai-status-info">
            <h2>AI咨询顾问</h2>
            <p class="ai-status-description">智能客服系统，为用户提供无人机租赁相关咨询服务</p>
          </div>
        </div>

        <div class="ai-status-main">
          <div class="ai-status-indicator">
            <div :class="['ai-status-dot', aiStatus.enabled ? 'ai-status-dot--online' : 'ai-status-dot--offline']"></div>
            <span :class="['ai-status-text', aiStatus.enabled ? 'ai-status-text--online' : 'ai-status-text--offline']">
              {{ aiStatus.enabled ? '运行中' : '维护中' }}
            </span>
          </div>

          <div class="ai-status-toggle">
            <el-switch
              v-model="localEnabled"
              :active-value="true"
              :inactive-value="false"
              active-text="启用"
              inactive-text="维护"
              @change="handleStatusChange"
              :disabled="!isLoaded"
            />
          </div>
        </div>

        <div class="ai-maintenance-section">
          <el-form-item label="维护提示消息" label-width="120px">
            <el-input
              v-model="maintenanceMessage"
              type="textarea"
              :rows="3"
              placeholder="当AI助手处于维护状态时，用户将看到此消息"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-button
            type="primary"
            @click="saveMaintenanceMessage"
            :disabled="!maintenanceMessage.trim()"
          >
            保存维护消息
          </el-button>
        </div>
      </div>

      <div class="ai-info-grid">
        <div class="ai-info-card">
          <div class="ai-info-icon ai-info-icon--service">
            <el-icon :size="24"><Service /></el-icon>
          </div>
          <h3>服务状态</h3>
          <div :class="['ai-info-status', aiStatus.enabled ? 'ai-info-status--success' : 'ai-info-status--warning']">
            {{ aiStatus.enabled ? '正常运行' : '暂停服务' }}
          </div>
          <p>AI助手当前{{ aiStatus.enabled ? '可以' : '无法' }}响应用户请求</p>
        </div>

        <div class="ai-info-card">
          <div class="ai-info-icon ai-info-icon--cache">
            <el-icon :size="24"><Cpu /></el-icon>
          </div>
          <h3>缓存状态</h3>
          <div class="ai-info-status ai-info-status--success">Redis缓存</div>
          <p>AI状态配置已启用Redis缓存，减少数据库查询</p>
        </div>

        <div class="ai-info-card">
          <div class="ai-info-icon ai-info-icon--message">
            <el-icon :size="24"><ChatLineSquare /></el-icon>
          </div>
          <h3>功能说明</h3>
          <div class="ai-info-status ai-info-status--info">智能交互</div>
          <p>支持无人机推荐、订单查询、资质咨询等服务</p>
        </div>

        <div class="ai-info-card">
          <div class="ai-info-icon ai-info-icon--tip">
            <el-icon :size="24"><InfoFilled /></el-icon>
          </div>
          <h3>使用提示</h3>
          <div class="ai-info-status ai-info-status--primary">快捷操作</div>
          <p>用户未登录可咨询通用问题，登录后可查询个人订单</p>
        </div>
      </div>

      <div class="ai-actions">
        <h3>快捷操作</h3>
        <div class="ai-action-buttons">
          <el-button type="success" :disabled="aiStatus.enabled || !isLoaded" @click="enableAi">
            <el-icon><CircleCheck /></el-icon>
            启用AI助手
          </el-button>
          <el-button type="warning" :disabled="!aiStatus.enabled || !isLoaded" @click="disableAi">
            <el-icon><Clock /></el-icon>
            暂停服务（维护）
          </el-button>
          <el-button type="info" @click="resetStatus">
            <el-icon><Refresh /></el-icon>
            刷新状态
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Service, Cpu, ChatLineSquare, InfoFilled, CircleCheck, Clock, Refresh } from '@element-plus/icons-vue'
import { getAiStatus, updateAiStatus } from '@/api/ai'
import { ElMessage } from 'element-plus'

let fetchController = null

const aiStatus = ref({ enabled: false, maintenanceMessage: '' })
const localEnabled = ref(false)
const maintenanceMessage = ref('')
const isLoaded = ref(false)

const fetchAiStatus = async () => {
  if (fetchController) fetchController.abort()
  fetchController = new AbortController()
  isLoaded.value = false
  try {
    const res = await getAiStatus(fetchController.signal)
    if (res.code === 200 && res.data) {
      aiStatus.value.enabled = res.data.enabled !== false
      aiStatus.value.maintenanceMessage = res.data.maintenanceMessage || ''
      localEnabled.value = aiStatus.value.enabled
      maintenanceMessage.value = aiStatus.value.maintenanceMessage
    }
  } catch (error) {
    if (error.name !== 'CanceledError' && error.name !== 'AbortError') {
      console.error('获取AI状态失败', error)
      ElMessage.error('获取AI状态失败')
    }
  } finally {
    isLoaded.value = true
  }
}

const handleStatusChange = async (value) => {
  try {
    const res = await updateAiStatus({
      enabled: value,
      maintenanceMessage: maintenanceMessage.value || 'AI助手正在维护中，请稍后再试'
    })
    if (res.code === 200) {
      aiStatus.value.enabled = value
      ElMessage.success(value ? 'AI助手已启用' : 'AI助手已暂停服务')
    } else {
      localEnabled.value = !value
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    localEnabled.value = !value
    ElMessage.error('网络错误，操作失败')
  }
}

const saveMaintenanceMessage = async () => {
  try {
    const res = await updateAiStatus({
      enabled: aiStatus.value.enabled,
      maintenanceMessage: maintenanceMessage.value
    })
    if (res.code === 200) {
      aiStatus.value.maintenanceMessage = maintenanceMessage.value
      ElMessage.success('维护消息已保存')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，保存失败')
  }
}

const enableAi = async () => {
  try {
    const res = await updateAiStatus({ enabled: true })
    if (res.code === 200) {
      aiStatus.value.enabled = true
      localEnabled.value = true
      ElMessage.success('AI助手已启用')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，操作失败')
  }
}

const disableAi = async () => {
  try {
    const res = await updateAiStatus({
      enabled: false,
      maintenanceMessage: maintenanceMessage.value || 'AI助手正在维护中，请稍后再试'
    })
    if (res.code === 200) {
      aiStatus.value.enabled = false
      localEnabled.value = false
      ElMessage.success('AI助手已暂停服务')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，操作失败')
  }
}

const resetStatus = () => {
  fetchAiStatus()
}

onMounted(() => {
  fetchAiStatus()
})

onUnmounted(() => {
  if (fetchController) fetchController.abort()
})
</script>

<style lang="scss" scoped>
.ai-management-page { min-height: 100%; }

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 24px;
}

.ai-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.ai-status-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.ai-status-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.ai-status-icon {
  width: 56px;
  height: 56px;
  color: #3b82f6;
  flex-shrink: 0;
  svg { width: 100%; height: 100%; }
}

.ai-status-info h2 {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 6px;
}

.ai-status-description {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.ai-status-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.ai-status-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;

  &--online {
    background: #4ade80;
    animation: pulse 2s infinite;
  }

  &--offline {
    background: #f59e0b;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.2); }
}

.ai-status-text {
  font-size: 16px;
  font-weight: 600;

  &--online { color: #22c55e; }
  &--offline { color: #f59e0b; }
}

.ai-maintenance-section {
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;

  :deep(.el-form-item) { margin-bottom: 16px; }
  :deep(.el-textarea__inner) { border-radius: 10px; resize: none; }
}

.ai-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.ai-info-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.ai-info-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;

  &--service { background: rgba(34, 197, 94, 0.15); color: #22c55e; }
  &--cache { background: rgba(59, 130, 246, 0.15); color: #3b82f6; }
  &--message { background: rgba(139, 92, 246, 0.15); color: #8b5cf6; }
  &--tip { background: rgba(245, 158, 11, 0.15); color: #f59e0b; }
}

.ai-info-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px;
}

.ai-info-status {
  font-size: 13px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 6px;
  display: inline-block;
  margin-bottom: 8px;

  &--success { background: rgba(34, 197, 94, 0.15); color: #22c55e; }
  &--warning { background: rgba(245, 158, 11, 0.15); color: #f59e0b; }
  &--info { background: rgba(59, 130, 246, 0.15); color: #3b82f6; }
  &--primary { background: rgba(139, 92, 246, 0.15); color: #8b5cf6; }
}

.ai-info-card p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

.ai-actions {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #0f172a;
    margin: 0 0 16px;
  }
}

.ai-action-buttons {
  display: flex;
  gap: 12px;
}

@media (max-width: 1024px) {
  .ai-info-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .ai-info-grid { grid-template-columns: 1fr; }
  .ai-action-buttons { flex-direction: column; }
  .ai-status-main { flex-direction: column; gap: 16px; align-items: flex-start; }
}
</style>