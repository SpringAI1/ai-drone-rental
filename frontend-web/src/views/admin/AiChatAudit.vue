<template>
  <div>
    <PageHeader title="AI对话审计" description="查看用户与 AI 的对话记录，用于反向优化本地知识库">
      <el-button :icon="Refresh" @click="refresh()">刷新</el-button>
    </PageHeader>

    <!-- 顶部统计卡片 -->
    <div class="stat-row">
      <StatTile label="会话总数" :value="stats.totalSessions" accent="linear-gradient(135deg, #667eea, #764ba2)" />
      <StatTile label="消息总数" :value="stats.totalMessages" accent="linear-gradient(135deg, #4facfe, #00f2fe)" />
      <StatTile label="活跃用户" :value="stats.activeUserCount" accent="linear-gradient(135deg, #43e97b, #38f9d7)" />
      <StatTile label="每会话平均消息" :value="stats.avgMessagesPerSession" accent="linear-gradient(135deg, #fa709a, #fee140)" />
    </div>

    <GlassCard>
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索消息内容..."
          clearable
          :prefix-icon="Search"
          style="width: 300px"
          @change="pageNum = 1; fetchConversations()"
        />
        <el-checkbox v-model="withUserOnly" @change="pageNum = 1; fetchConversations()">
          仅显示已登录用户会话
        </el-checkbox>
      </div>

      <!-- 会话列表 -->
      <el-table :data="conversations" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="会话ID" prop="conversationId" width="320" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="viewConversation(row.conversationId)">
              {{ row.conversationId }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="用户ID" prop="userId" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.userId" size="small" type="primary">{{ row.userId }}</el-tag>
            <el-tag v-else size="small" type="info">匿名</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首个问题" prop="firstMessage" min-width="300" show-overflow-tooltip />
        <el-table-column label="消息数" prop="messageCount" width="100" align="center">
          <template #default="{ row }">
            <el-badge :value="row.messageCount" class="item" :max="999" />
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.firstTime) }}</template>
        </el-table-column>
        <el-table-column label="最后活跃" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.lastTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewConversation(row.conversationId)">
              查看详情
            </el-button>
            <el-popconfirm title="确定删除此会话?" @confirm="deleteConversation(row.conversationId)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="pageNum = 1; fetchConversations()"
        @current-change="fetchConversations()"
        background
        style="margin-top: 16px; justify-content: flex-end; display: flex"
      />

      <EmptyState v-if="!loading && conversations.length === 0" description="暂无会话记录" />
    </GlassCard>

    <!-- 会话详情弹框 -->
    <el-drawer
      v-model="detailVisible"
      :title="'会话详情 - ' + (detail.conversationId || '')"
      direction="rtl"
      size="60%"
      :with-header="true"
    >
      <template v-if="detailLoading">
        <div style="text-align: center; padding: 60px;">加载中...</div>
      </template>

      <template v-else-if="detail && detail.messages && detail.messages.length > 0">
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="会话ID">
            <span style="font-family: monospace">{{ detail.conversationId }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">
            <el-tag v-if="detail.userId" size="small" type="primary">{{ detail.userId }}</el-tag>
            <el-tag v-else size="small" type="info">匿名</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="消息数">{{ detail.messageCount }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatTime(detail.firstTime) }}</el-descriptions-item>
          <el-descriptions-item label="最后活跃" :span="2">{{ formatTime(detail.lastTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">对话内容</el-divider>

        <div class="chat-detail">
          <div
            v-for="(msg, idx) in detail.messages"
            :key="msg.id || idx"
            class="chat-bubble-row"
            :class="{ 'chat-bubble-user': msg.role === 'user' }"
          >
            <div class="chat-avatar">
              <el-tag :type="msg.role === 'user' ? 'primary' : 'success'" size="small" round>
                {{ msg.role === 'user' ? '用户' : 'AI' }}
              </el-tag>
            </div>
            <div class="chat-bubble">
              <div class="chat-content">{{ msg.content }}</div>
              <div class="chat-meta">
                <span v-if="msg.model" style="margin-right: 12px">模型: {{ msg.model }}</span>
                <span>{{ formatTime(msg.time) }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <EmptyState v-else description="此会话无消息记录" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import StatTile from '@/components/common/StatTile.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import request from '@/utils/request'

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const conversations = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const withUserOnly = ref(false)

const stats = reactive({
  totalSessions: 0,
  totalMessages: 0,
  activeUserCount: 0,
  avgMessagesPerSession: 0
})

const detailVisible = ref(false)
const detail = ref({ messages: [] })
const detailLoading = ref(false)

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t).replace('T', ' ').substring(0, 19)
  const p = (n) => (n < 10 ? '0' + n : '' + n)
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const fetchStats = async () => {
  try {
    const res = await request.get('/admin/ai-chat/stats')
    if (res.code === 200 && res.data) {
      stats.totalSessions = res.data.totalSessions || 0
      stats.totalMessages = res.data.totalMessages || 0
      stats.activeUserCount = res.data.activeUserCount || 0
      stats.avgMessagesPerSession = res.data.avgMessagesPerSession || 0
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchConversations = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value && searchKeyword.value.trim().length > 0) {
      params.keyword = searchKeyword.value.trim()
    }
    if (withUserOnly.value) {
      params.withUserOnly = true
    }

    const res = await request.get('/admin/ai-chat/conversations', { params })
    if (res.code === 200 && res.data) {
      conversations.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      conversations.value = []
      total.value = 0
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载会话失败')
  } finally {
    loading.value = false
  }
}

const viewConversation = async (conversationId) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = { messages: [] }
  try {
    const res = await request.get(`/admin/ai-chat/conversation/${conversationId}`)
    if (res.code === 200 && res.data) {
      detail.value = res.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载会话详情失败')
  } finally {
    detailLoading.value = false
  }
}

const deleteConversation = async (conversationId) => {
  try {
    const res = await request.delete(`/admin/ai-chat/conversation/${conversationId}`)
    if (res.code === 200) {
      ElMessage.success('会话已删除')
      fetchConversations()
      fetchStats()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const refresh = () => {
  fetchStats()
  pageNum.value = 1
  fetchConversations()
}

onMounted(() => {
  fetchStats()
  fetchConversations()
})
</script>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.chat-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
}

.chat-bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.chat-bubble-row.chat-bubble-user {
  flex-direction: row-reverse;
}

.chat-avatar {
  flex-shrink: 0;
}

.chat-bubble {
  max-width: 70%;
  background: white;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.chat-bubble-user .chat-bubble {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: white;
}

.chat-bubble-user .chat-bubble .chat-meta {
  color: rgba(255, 255, 255, 0.8);
}

.chat-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
}

.chat-meta {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
  text-align: right;
}

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .chat-bubble {
    max-width: 85%;
  }
}
</style>
