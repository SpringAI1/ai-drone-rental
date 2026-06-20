<template>
  <view class="chat-page">
    <!-- 顶部标题栏 -->
    <view class="chat-header">
      <view class="header-left" @click="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="header-center">
        <text class="header-title">AI 智能客服</text>
        <text class="header-sub">在线为您解答</text>
      </view>
      <view class="header-right">
        <view class="status-dot" :class="{ offline: !aiEnabled }"></view>
      </view>
    </view>

    <!-- 聊天区域 -->
    <scroll-view
      class="chat-scroll"
      scroll-y="true"
      :scroll-top="scrollTop"
      :scroll-with-animation="true"
    >
      <view class="message-list">
        <view
          class="message-item"
          v-for="(msg, index) in messages"
          :key="index"
          :class="{ user: msg.isUser }"
        >
          <view class="message-avatar" :class="{ user: msg.isUser }">
            <text class="avatar-text">{{ msg.isUser ? '我' : 'AI' }}</text>
          </view>

          <view class="message-content-wrap">
            <text v-if="!msg.isUser" class="message-name">AI 助手</text>
            <view class="message-bubble">
              <text class="message-text">{{ msg.content }}</text>
            </view>
            <text class="message-time">{{ formatTime(msg.time) }}</text>
          </view>
        </view>

        <!-- 加载状态 -->
        <view v-if="loading" class="message-item">
          <view class="message-avatar">
            <text class="avatar-text">AI</text>
          </view>
          <view class="message-content-wrap">
            <text class="message-name">AI 助手</text>
            <view class="message-bubble loading-bubble">
              <view class="typing-dot"></view>
              <view class="typing-dot"></view>
              <view class="typing-dot"></view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 底部输入框 -->
    <view class="input-bar">
      <view class="input-wrap">
        <input
          class="chat-input"
          v-model="inputMessage"
          placeholder="请输入您的问题..."
          :confirm-type="'send'"
          @confirm="sendMessage"
          adjust-position="true"
        />
      </view>
      <view
        class="send-btn"
        :class="{ active: inputMessage.trim().length > 0 && !loading }"
        @click="sendMessage"
      >
        <text class="send-icon">✈</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { chat } from '../../api/ai'

const STORAGE_KEY_CONVERSATION = 'ai_chat_conversation_id'
const STORAGE_KEY_MESSAGES = 'ai_chat_messages'

interface Message {
  id: number
  content: string
  isUser: boolean
  time: Date
}

const messages = ref<Message[]>([])
const inputMessage = ref('')
const conversationId = ref('')
const scrollTop = ref(0)
const loading = ref(false)
const aiEnabled = ref(true)

// 初始化：从本地存储恢复会话
onMounted(() => {
  const savedConvId = uni.getStorageSync(STORAGE_KEY_CONVERSATION)
  if (savedConvId && typeof savedConvId === 'string' && savedConvId.length > 0) {
    conversationId.value = savedConvId
  }
  const savedMessages = uni.getStorageSync(STORAGE_KEY_MESSAGES)
  if (savedMessages && Array.isArray(savedMessages) && savedMessages.length > 0) {
    messages.value = savedMessages.map((m: any) => ({
      id: m.id,
      content: m.content,
      isUser: m.isUser,
      time: new Date(m.time)
    }))
    scrollToBottom()
  } else {
    messages.value = [{
      id: 1,
      content: '您好！我是无人机租赁系统的 AI 客服助手 🛸\n\n我可以帮您：\n1. 查询设备信息和价格\n2. 了解租赁流程和规则\n3. 解答订单和支付问题\n4. 推荐适合的无人机\n\n请问有什么可以帮助您的吗？',
      isUser: false,
      time: new Date()
    }]
  }
})

const persistMessages = () => {
  uni.setStorageSync(STORAGE_KEY_MESSAGES, messages.value.map(m => ({
    id: m.id,
    content: m.content,
    isUser: m.isUser,
    time: m.time.toISOString()
  })))
}

const clearChat = () => {
  conversationId.value = ''
  messages.value = [{
    id: 1,
    content: '已开启新会话 🛸\n\n我可以帮您查询设备、解答流程问题，或者帮您推荐合适的机型。',
    isUser: false,
    time: new Date()
  }]
  uni.removeStorageSync(STORAGE_KEY_CONVERSATION)
  uni.removeStorageSync(STORAGE_KEY_MESSAGES)
  scrollToBottom()
}

const formatTime = (date: Date) => {
  const h = date.getHours().toString().padStart(2, '0')
  const m = date.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollTop.value = scrollTop.value + 10000
  })
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return

  const userMessage: Message = {
    id: messages.value.length + 1,
    content: content,
    isUser: true,
    time: new Date()
  }
  messages.value.push(userMessage)
  inputMessage.value = ''
  scrollToBottom()
  persistMessages()

  loading.value = true

  try {
    const res = await chat({
      message: content,
      conversationId: conversationId.value
    })

    let reply = ''
    if (res.data) {
      if (res.data.reply) {
        reply = res.data.reply
      } else if (res.data.content) {
        reply = res.data.content
      } else if (typeof res.data === 'string') {
        reply = res.data
      }
    }

    if (!reply || reply.length === 0) {
      reply = '抱歉，我暂时无法回答这个问题。您可以拨打人工客服：400-800-8888'
    }

    if (res.data && res.data.conversationId) {
      conversationId.value = res.data.conversationId
      uni.setStorageSync(STORAGE_KEY_CONVERSATION, res.data.conversationId)
    }

    const aiMessage: Message = {
      id: messages.value.length + 1,
      content: reply,
      isUser: false,
      time: new Date()
    }
    messages.value.push(aiMessage)
    scrollToBottom()
    persistMessages()
  } catch (err) {
    console.error('AI 对话失败', err)
    const errorMessage: Message = {
      id: messages.value.length + 1,
      content: '网络连接异常，请稍后重试。您也可以拨打人工客服：400-800-8888',
      isUser: false,
      time: new Date()
    }
    messages.value.push(errorMessage)
    scrollToBottom()
    persistMessages()
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  uni.navigateBack()
}
</script>

<style lang="scss">
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.chat-page {
  min-height: 100vh;
  background: #f1f5f9;
  display: flex;
  flex-direction: column;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ===== 顶部标题栏 ===== */
.chat-header {
  background: #ffffff;
  padding: 24rpx 20rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
  border-bottom: 2rpx solid #e2e8f0;
}

.header-left {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.back-icon {
  font-size: 48rpx;
  color: #0f172a;
  font-weight: 300;
  line-height: 1;
}

.header-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.header-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.header-sub {
  font-size: 22rpx;
  color: #64748b;
  margin-top: 4rpx;
}

.header-right {
  width: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-dot {
  width: 16rpx;
  height: 16rpx;
  background: #22c55e;
  border-radius: 8rpx;
}

.status-dot.offline {
  background: #94a3b8;
}

/* ===== 聊天滚动区 ===== */
.chat-scroll {
  flex: 1;
  height: 0;
  padding: 24rpx 20rpx;
  box-sizing: border-box;
}

.message-list {
  padding-bottom: 20rpx;
}

/* ===== 单条消息 ===== */
.message-item {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  margin-bottom: 28rpx;
}

.message-item.user {
  flex-direction: row-reverse;
}

/* 头像 */
.message-avatar {
  width: 64rpx;
  height: 64rpx;
  background: #e2e8f0;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14rpx;
  flex-shrink: 0;
}

.message-avatar.user {
  margin-right: 0;
  margin-left: 14rpx;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
}

.avatar-text {
  font-size: 22rpx;
  color: #475569;
  font-weight: 600;
}

.message-avatar.user .avatar-text {
  color: #ffffff;
}

/* 消息内容区 */
.message-content-wrap {
  max-width: 520rpx;
  display: flex;
  flex-direction: column;
}

.message-item.user .message-content-wrap {
  align-items: flex-end;
}

.message-name {
  font-size: 22rpx;
  color: #64748b;
  margin-bottom: 8rpx;
}

/* 气泡 */
.message-bubble {
  background: #ffffff;
  border-radius: 8rpx 20rpx 20rpx 20rpx;
  padding: 20rpx 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.04);
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  border-radius: 20rpx 8rpx 20rpx 20rpx;
}

.message-text {
  font-size: 28rpx;
  color: #334155;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.message-item.user .message-text {
  color: #ffffff;
}

.message-time {
  font-size: 20rpx;
  color: #94a3b8;
  margin-top: 8rpx;
}

/* ===== 加载中气泡 ===== */
.loading-bubble {
  padding: 20rpx 24rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
}

.typing-dot {
  width: 12rpx;
  height: 12rpx;
  background: #94a3b8;
  border-radius: 6rpx;
  margin: 0 4rpx;
}

.typing-dot:nth-child(1) {
  animation: bounce 1.2s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation: bounce 1.2s ease-in-out 0.2s infinite;
}

.typing-dot:nth-child(3) {
  animation: bounce 1.2s ease-in-out 0.4s infinite;
}

@keyframes bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-8rpx);
    opacity: 1;
  }
}

/* ===== 底部输入栏 ===== */
.input-bar {
  background: #ffffff;
  padding: 16rpx 20rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
  border-top: 2rpx solid #e2e8f0;
}

.input-wrap {
  flex: 1;
  background: #f1f5f9;
  border-radius: 40rpx;
  padding: 0 28rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
}

.chat-input {
  flex: 1;
  height: 80rpx;
  font-size: 28rpx;
  color: #0f172a;
  background: transparent;
}

.send-btn {
  width: 80rpx;
  height: 80rpx;
  background: #cbd5e1;
  border-radius: 40rpx;
  margin-left: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.send-btn.active {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
}

.send-icon {
  font-size: 30rpx;
  color: #ffffff;
  font-weight: 600;
}
</style>
