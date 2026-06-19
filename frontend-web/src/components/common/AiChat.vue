<template>
  <div
    v-if="authStore.isLoggedIn"
    class="chat-float"
    :class="{
      'chat-float--expanded': isExpanded,
      'chat-float--fullscreen': isFullscreen,
      'chat-float--dragging': isDragging
    }"
    :style="{
      width: chatWidth + 'px',
      height: isExpanded ? chatHeight + 'px' : '56px',
      left: chatLeft + 'px',
      top: chatTop + 'px'
    }"
    @mousedown="startDrag"
  >
    <div class="chat-float__header" @dblclick.stop="toggleExpand">
      <div class="chat-float__header-icon" @click.stop="toggleExpand" @mousedown.stop>
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2Z" stroke="currentColor" stroke-width="2"/>
          <path d="M8 14C8 14 9.5 16 12 16C14.5 16 16 14 16 14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <circle cx="9" cy="10" r="1" fill="currentColor"/>
          <circle cx="15" cy="10" r="1" fill="currentColor"/>
        </svg>
      </div>
      <span class="chat-float__header-title">AI咨询顾问</span>
      <div class="chat-float__header-status" :class="{ 'chat-float__header-status--maintenance': !aiEnabled }" @mousedown.stop>
        <span class="chat-float__header-status-dot"></span>
        <span>{{ aiEnabled ? '在线' : '维护中' }}</span>
      </div>
      <div class="chat-float__header-actions" @mousedown.stop>
        <el-button
          v-if="isExpanded && !isFullscreen"
          text
          size="small"
          class="chat-float__header-action"
          @click.stop="toggleFullscreen"
        >
          <el-icon><FullScreen /></el-icon>
          <span>全屏</span>
        </el-button>
        <el-button
          v-if="isFullscreen"
          text
          size="small"
          class="chat-float__header-action"
          @click.stop="toggleFullscreen"
        >
          <el-icon><Expand /></el-icon>
          <span>退出</span>
        </el-button>
        <el-button
          v-if="isExpanded"
          text
          size="small"
          class="chat-float__header-action"
          @click.stop="toggleExpand"
        >
          <el-icon><ArrowDown /></el-icon>
          <span>收起</span>
        </el-button>
      </div>
    </div>

    <Transition name="expand">
      <div v-if="isExpanded" class="chat-float__content">
        <div class="chat-messages" ref="messagesContainer">
          <div v-if="messages.length === 0" class="chat-messages__welcome">
            <div class="chat-messages__welcome-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 2L4 7V17L12 22L20 17V7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                <circle cx="12" cy="12" r="3" fill="currentColor"/>
              </svg>
            </div>
            <h3>AI咨询顾问</h3>
            <p>我可以帮你推荐无人机、解答租赁问题、说明订单流程等</p>
            <div v-if="!authStore.isLogin" class="chat-messages__welcome-login">
              <el-button type="primary" size="small" @click="handleGoLogin">
                登录查看更多服务
              </el-button>
            </div>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="chat-message"
            :class="`chat-message--${msg.role}`"
          >
            <div class="chat-message__avatar">
              <el-avatar v-if="msg.role === 'user'" :size="36">
                {{ userInitial }}
              </el-avatar>
              <div v-else class="chat-message__avatar-ai">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 2L4 7V17L12 22L20 17V7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                  <circle cx="12" cy="12" r="3" fill="currentColor"/>
                </svg>
              </div>
            </div>
            <div class="chat-message__content-wrapper">
              <div class="chat-message__content">
                <div class="chat-message__text" v-html="formatMessage(msg.content)"></div>
                <div class="chat-message__time">{{ msg.time }}</div>
              </div>
            </div>
          </div>

          <div v-if="isLoading" class="chat-message chat-message--ai chat-message--loading">
            <div class="chat-message__avatar">
              <div class="chat-message__avatar-ai">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 2L4 7V17L12 22L20 17V7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                  <circle cx="12" cy="12" r="3" fill="currentColor"/>
                </svg>
              </div>
            </div>
            <div class="chat-message__content">
              <div class="chat-message__loading">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputMessage"
            :placeholder="authStore.isLogin ? '输入你的问题，例如：我该选哪种机型做航拍？' : '输入你的问题，登录后可查看个人订单等服务'"
            :disabled="isLoading"
            @keyup.enter="sendMessage"
            class="chat-input__field"
          >
            <template #suffix>
              <el-button
                type="primary"
                :disabled="!inputMessage.trim() || isLoading"
                :loading="isLoading"
                @click="sendMessage"
                class="chat-input__send"
              >
                发送
              </el-button>
            </template>
          </el-input>
        </div>

        <div class="chat-footer">
          <el-button text size="small" @click="clearHistory" :disabled="!conversationId || isLoading">
            <el-icon><Delete /></el-icon>
            清空会话
          </el-button>
          <span v-if="authStore.isLogin && conversationId" class="chat-footer__tips">当前会话ID: {{ conversationId.substring(0, 8) }}...</span>
          <span v-if="!authStore.isLogin" class="chat-footer__tips">未登录状态，部分功能受限</span>
        </div>
      </div>
    </Transition>

    <div class="chat-float__drag-hint" v-if="isDragging">
      <span>拖动到合适位置</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { ArrowDown, Delete, FullScreen, Expand } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { sendChatMessage, clearConversation as clearConversationApi, getAiStatus } from '@/api/ai'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

const isExpanded = ref(false)
const isFullscreen = ref(false)
const isDragging = ref(false)
const inputMessage = ref('')
const messages = ref([])
const isLoading = ref(false)
const messagesContainer = ref(null)
const conversationId = ref('')
const aiEnabled = ref(true)
const maintenanceMessage = ref('')

const chatWidth = ref(380)
const chatHeight = ref(550)
const chatLeft = ref(0)
const chatTop = ref(0)

const userInitial = computed(() => {
  return authStore.user?.nickname?.charAt(0) || 'U'
})

const STORAGE_KEY = 'drone_rental_chat_history'

onMounted(() => {
  const containerWidth = window.innerWidth
  const containerHeight = window.innerHeight
  chatLeft.value = containerWidth - chatWidth.value - 24
  chatTop.value = containerHeight - 56 - 24
  fetchAiStatus()
  loadMessagesFromStorage()
})

const loadMessagesFromStorage = () => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored)
      messages.value = parsed.messages || []
      conversationId.value = parsed.conversationId || ''
    }
  } catch (error) {
    console.error('加载聊天记录失败', error)
  }
}

const saveMessagesToStorage = () => {
  try {
    const data = {
      messages: messages.value,
      conversationId: conversationId.value,
      timestamp: Date.now()
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  } catch (error) {
    console.error('保存聊天记录失败', error)
  }
}

const fetchAiStatus = async () => {
  try {
    const res = await getAiStatus()
    if (res.code === 200 && res.data) {
      aiEnabled.value = res.data.enabled !== false
      maintenanceMessage.value = res.data.maintenanceMessage || ''
    }
  } catch (error) {
    console.error('获取AI状态失败', error)
  }
}

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  if (isFullscreen.value) {
    chatWidth.value = window.innerWidth
    chatHeight.value = window.innerHeight
    chatLeft.value = 0
    chatTop.value = 0
  } else {
    chatWidth.value = 380
    chatHeight.value = 550
    const containerWidth = window.innerWidth
    const containerHeight = window.innerHeight
    chatLeft.value = containerWidth - chatWidth.value - 24
    chatTop.value = containerHeight - chatHeight.value - 24
  }
}

let dragStartX = 0
let dragStartY = 0
let dragStartLeft = 0
let dragStartTop = 0

const startDrag = (e) => {
  if (isFullscreen.value) return
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragStartLeft = chatLeft.value
  dragStartTop = chatTop.value
  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', stopDrag)
}

const handleDrag = (e) => {
  if (!isDragging.value) return
  const deltaX = e.clientX - dragStartX
  const deltaY = e.clientY - dragStartY
  const containerWidth = window.innerWidth
  const containerHeight = window.innerHeight
  const currentHeight = isExpanded.value ? chatHeight.value : 56
  chatLeft.value = Math.max(0, Math.min(containerWidth - chatWidth.value, dragStartLeft + deltaX))
  chatTop.value = Math.max(0, Math.min(containerHeight - currentHeight, dragStartTop + deltaY))
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const handleGoLogin = () => {
  router.push('/login')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatTime = () => {
  const now = new Date()
  return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatMessage = (content) => {
  if (!content) return ''
  let html = content
    .replace(/\n/g, '<br>')
    .replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
  return html
}

const sendMessage = async () => {
  const text = inputMessage.value.trim()
  if (!text || isLoading.value) return

  if (!aiEnabled.value) {
    messages.value.push({
      role: 'user',
      content: text,
      time: formatTime()
    })
    messages.value.push({
      role: 'ai',
      content: maintenanceMessage.value || 'AI助手正在维护中，请稍后再试',
      time: formatTime()
    })
    inputMessage.value = ''
    await scrollToBottom()
    saveMessagesToStorage()
    return
  }

  if (!authStore.isLogin && (text.includes('订单') || text.includes('我的') || text.includes('个人') || text.includes('租赁记录') || text.includes('支付'))) {
    ElMessage.warning('请先登录查看个人订单等服务')
    router.push('/login')
    return
  }

  messages.value.push({
    role: 'user',
    content: text,
    time: formatTime()
  })

  inputMessage.value = ''
  isLoading.value = true

  await scrollToBottom()

  try {
    const res = await sendChatMessage(text, conversationId.value)
    if (res.code === 200) {
      if (!conversationId.value && res.data?.conversationId) {
        conversationId.value = res.data.conversationId
      }
      messages.value.push({
        role: 'ai',
        content: res.data?.reply || '',
        time: formatTime()
      })
      saveMessagesToStorage()
    } else {
      ElMessage.error(res.message || 'AI回复失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

const clearHistory = async () => {
  if (!conversationId.value) return

  try {
    await clearConversationApi(conversationId.value)
    messages.value = []
    conversationId.value = ''
    localStorage.removeItem(STORAGE_KEY)
    ElMessage.success('会话已清空')
  } catch (error) {
    ElMessage.error('清空会话失败')
  }
}

onUnmounted(() => {
  stopDrag()
})
</script>

<style lang="scss" scoped>
.chat-float {
  position: fixed;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.6);
  z-index: 1000;
  overflow: hidden;
  transition: all 0.3s ease;
  min-width: 320px;
  min-height: 56px;
  cursor: grab;

  &:active {
    cursor: grabbing;
  }

  &--dragging {
    opacity: 0.95;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
  }

  &--fullscreen {
    left: 0 !important;
    top: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    border-radius: 0;
    box-shadow: none;
    border: none;
    cursor: default;
    background: rgba(255, 255, 255, 0.98);
  }
}

.chat-float__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(30, 136, 229, 0.9), rgba(21, 101, 192, 0.9));
  color: #ffffff;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: linear-gradient(135deg, rgba(25, 118, 210, 0.95), rgba(13, 71, 161, 0.95));
  }
}

.chat-float__header-icon {
  width: 30px;
  height: 30px;

  svg {
    width: 100%;
    height: 100%;
  }
}

.chat-float__header-title {
  font-size: 16px;
  font-weight: 600;
  flex: 1;
}

.chat-float__header-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  padding: 2px 8px;
  background: rgba(76, 175, 80, 0.3);
  border-radius: 10px;

  &-dot {
    width: 6px;
    height: 6px;
    background: #4caf50;
    border-radius: 50%;
    animation: pulse 2s infinite;
  }

  &--maintenance {
    background: rgba(255, 152, 0, 0.3);

    .chat-float__header-status-dot {
      background: #ff9800;
      animation: none;
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.2);
  }
}

.chat-float__header-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.chat-float__header-action {
  color: rgba(255, 255, 255, 0.85);
  transition: all 0.2s ease;
  font-size: 13px;

  &:hover {
    color: #ffffff;
    background: rgba(255, 255, 255, 0.15);
  }

  span {
    margin-left: 3px;
  }
}

.chat-float__content {
  display: flex;
  flex-direction: column;
  height: calc(100% - 56px);
}

.chat-float__drag-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  z-index: 10;
  pointer-events: none;
}

.chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: rgba(245, 247, 250, 0.8);

  &__welcome {
    text-align: center;
    padding: 32px 16px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 16px;
    margin-bottom: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    &-icon {
      width: 56px;
      height: 56px;
      margin: 0 auto 12px;
      color: #1e88e5;

      svg {
        width: 100%;
        height: 100%;
      }
    }

    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #1a237e;
      margin-bottom: 8px;
    }

    p {
      font-size: 14px;
      color: #64748b;
      line-height: 1.5;
    }

    &-login {
      margin-top: 16px;

      .el-button {
        border-radius: 20px;
        padding: 6px 20px;
      }
    }
  }
}

.chat-message {
  display: flex;
  gap: 12px;
  max-width: 90%;

  &--user {
    align-self: flex-end;
    flex-direction: row-reverse;

    .chat-message__content-wrapper {
      align-items: flex-end;
    }

    .chat-message__content {
      background: linear-gradient(135deg, rgba(30, 136, 229, 0.9), rgba(21, 101, 192, 0.9));
      color: #ffffff;
      border-radius: 16px 16px 4px 16px;
      box-shadow: 0 4px 12px rgba(30, 136, 229, 0.25);
    }

    .chat-message__time {
      color: rgba(255, 255, 255, 0.6);
      text-align: right;
    }
  }

  &--ai {
    align-self: flex-start;

    .chat-message__content-wrapper {
      align-items: flex-start;
    }

    .chat-message__content {
      background: rgba(255, 255, 255, 0.95);
      color: #1a237e;
      border-radius: 16px 16px 16px 4px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    }

    .chat-message__time {
      color: #94a3b8;
    }
  }

  &__avatar {
    flex-shrink: 0;

    &-ai {
      width: 34px;
      height: 34px;
      background: linear-gradient(135deg, rgba(30, 136, 229, 0.9), rgba(21, 101, 192, 0.9));
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #ffffff;

      svg {
        width: 20px;
        height: 20px;
      }
    }
  }

  &__content-wrapper {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-width: calc(100% - 46px);
  }

  &__content {
    padding: 12px 16px;
    max-width: 100%;
  }

  &__text {
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
    white-space: pre-wrap;

    :deep(pre) {
      background: rgba(241, 245, 249, 0.8);
      padding: 10px;
      border-radius: 6px;
      overflow-x: auto;
      margin: 6px 0;
      font-size: 12px;
      font-family: monospace;

      code {
        background: none;
        padding: 0;
        color: inherit;
      }
    }

    :deep(code) {
      background: rgba(224, 231, 255, 0.8);
      padding: 2px 5px;
      border-radius: 3px;
      font-size: 12px;
      font-family: monospace;
      color: #4338ca;
    }

    :deep(strong) {
      font-weight: 600;
      color: inherit;
    }
  }

  &__time {
    font-size: 11px;
  }

  &--loading {
    .chat-message__loading {
      display: flex;
      gap: 4px;
      padding: 8px 0;

      span {
        width: 6px;
        height: 6px;
        background: #1e88e5;
        border-radius: 50%;
        animation: bounce 1.4s infinite ease-in-out;

        &:nth-child(1) {
          animation-delay: -0.32s;
        }
        &:nth-child(2) {
          animation-delay: -0.16s;
        }
      }
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(255, 255, 255, 0.9);

  &__field {
    :deep(.el-input__wrapper) {
      border-radius: 16px;
      padding-right: 10px;
      border: 1px solid rgba(226, 232, 240, 0.8);
      box-shadow: none;

      &:hover {
        border-color: rgba(30, 136, 229, 0.5);
      }

      &.is-focus {
        border-color: #1e88e5;
        box-shadow: 0 0 0 2px rgba(30, 136, 229, 0.08);
      }
    }
  }

  &__send {
    border-radius: 14px;
    padding: 6px 16px;
    margin-left: 8px;
    background: linear-gradient(135deg, rgba(30, 136, 229, 0.9), rgba(21, 101, 192, 0.9));
    border: none;

    &:hover {
      background: linear-gradient(135deg, rgba(25, 118, 210, 0.95), rgba(13, 71, 161, 0.95));
    }
  }
}

.chat-footer {
  padding: 8px 16px;
  border-top: 1px solid rgba(241, 245, 249, 0.8);
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: space-between;
  align-items: center;

  &__tips {
    font-size: 11px;
    color: #94a3b8;
  }
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 640px) {
  .chat-float {
    left: 16px !important;
    width: calc(100vw - 32px) !important;
    cursor: default;

    &--expanded {
      height: 65vh !important;
    }

    &--fullscreen {
      height: 100vh !important;
    }
  }

  .chat-float__header-status {
    display: none;
  }

  .chat-message {
    max-width: 95%;
  }
}
</style>
