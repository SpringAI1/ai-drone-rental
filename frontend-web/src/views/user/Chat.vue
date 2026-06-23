<template>
  <div class="chat-page">
    <PageHeader title="AI智能助手" subtitle="专业的无人机租赁咨询服务" />
    
    <div class="chat-container">
      <GlassCard class="chat-card">
        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="chat-empty">
            <el-icon :size="48" class="chat-empty-icon"><ChatDotRound /></el-icon>
            <p class="chat-empty-text">开始与AI助手对话吧</p>
            <p class="chat-empty-tip">我可以帮你推荐无人机、解答租赁问题</p>
          </div>
          
          <div 
            v-for="(msg, index) in messages" 
            :key="index" 
            class="chat-message"
            :class="{ 'chat-message--user': msg.role === 'user' }"
          >
            <div class="chat-message-avatar">
              <el-avatar :size="36" :class="msg.role === 'user' ? 'avatar-user' : 'avatar-ai'">
                <el-icon v-if="msg.role === 'ai'"><Monitor /></el-icon>
                <span v-else>{{ userInitial }}</span>
              </el-avatar>
            </div>
            <div class="chat-message-content">
              <div class="chat-message-role">{{ msg.role === 'user' ? '我' : 'AI助手' }}</div>
              <div class="chat-message-text">{{ msg.content }}</div>
            </div>
          </div>
          
          <div v-if="loading" class="chat-message chat-message--ai">
            <div class="chat-message-avatar">
              <el-avatar :size="36" class="avatar-ai">
                <el-icon><Monitor /></el-icon>
              </el-avatar>
            </div>
            <div class="chat-message-content">
              <div class="chat-message-role">AI助手</div>
              <div class="chat-loading">
                <span class="chat-loading-dot"></span>
                <span class="chat-loading-dot"></span>
                <span class="chat-loading-dot"></span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="chat-input-area">
          <el-input
            v-model="inputMessage"
            placeholder="输入消息，按Enter发送..."
            :disabled="loading"
            @keyup.enter="sendMessage"
            class="chat-input"
          >
            <template #suffix>
              <el-button 
                type="primary" 
                :icon="loading ? '' : 'Promotion'"
                :loading="loading"
                @click="sendMessage"
                circle
              />
            </template>
          </el-input>
        </div>
      </GlassCard>
    </div>
    
    <div class="chat-quick-actions">
      <GlassCard class="quick-card">
        <div class="quick-title">快捷问题</div>
        <div class="quick-buttons">
          <el-button 
            v-for="question in quickQuestions" 
            :key="question"
            size="small"
            @click="askQuickQuestion(question)"
            :disabled="loading"
          >
            {{ question }}
          </el-button>
        </div>
      </GlassCard>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { streamChatMessage, getAiStatus } from '@/api/ai'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Monitor, Promotion } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'

const authStore = useAuthStore()
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref(null)
const aiOnline = ref(true)
let currentAbort = null

const userInitial = computed(() => {
  return authStore.user?.nickname?.charAt(0) || 'U'
})

const quickQuestions = [
  '推荐航拍无人机',
  '租赁资质要求',
  '订单流程说明',
  '故障报修流程',
  '空域备案要求',
  '价格费用咨询'
]

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message || loading.value) return

  messages.value.push({ role: 'user', content: message })
  inputMessage.value = ''
  // 立即放一个空的 AI 气泡，等待流式逐字填充
  const aiIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '' })
  loading.value = true
  await scrollToBottom()

  if (currentAbort) currentAbort.abort()
  const ac = new AbortController()
  currentAbort = ac

  await streamChatMessage(
    { message, conversationId: '' },
    {
      signal: ac.signal,
      onChunk: (chunk) => {
        messages.value[aiIndex].content += chunk
        scrollToBottom()
      },
      onDone: (full) => {
        messages.value[aiIndex].content = full || messages.value[aiIndex].content
        loading.value = false
        currentAbort = null
        scrollToBottom()
      },
      onError: (err) => {
        console.error('SSE error', err)
        messages.value[aiIndex].content =
          (messages.value[aiIndex].content || '') +
          (messages.value[aiIndex].content ? '\n\n' : '') +
          '⚠️ 抱歉，AI 服务连接异常，请稍后重试或拨打人工客服 400-800-8888'
        loading.value = false
        currentAbort = null
        scrollToBottom()
      }
    }
  )
}

const askQuickQuestion = (question) => {
  inputMessage.value = question
  sendMessage()
}

const loadStatus = async () => {
  try {
    const res = await getAiStatus()
    aiOnline.value = !!res?.data?.enabled
  } catch (e) {
    aiOnline.value = false
  }
}

onMounted(() => {
  scrollToBottom()
  loadStatus()
})
</script>

<style lang="scss" scoped>
.chat-page {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.chat-container {
  margin-top: 24px;
}

.chat-card {
  min-height: 500px;
  display: flex;
  flex-direction: column;
}

.chat-message