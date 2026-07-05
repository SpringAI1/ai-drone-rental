<template>
  <el-popover
    placement="bottom-end"
    :width="360"
    trigger="click"
    @show="fetchNotifications"
  >
    <template #reference>
      <div class="notification-badge">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
      </div>
    </template>

    <div class="notification-panel">
      <div class="notification-header">
        <span class="notification-title">消息通知</span>
        <el-button v-if="unreadCount > 0" text type="primary" size="small" @click="handleMarkAllRead">
          全部已读
        </el-button>
      </div>

      <div v-loading="loading" class="notification-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ 'notification-item--unread': item.readStatus === 0 }"
          @click="handleClick(item)"
        >
          <div class="notification-icon">
            <el-icon :size="18" :color="getTypeColor(item.type)">
              <component :is="getTypeIcon(item.type)" />
            </el-icon>
          </div>
          <div class="notification-content">
            <div class="notification-item-title">{{ item.title }}</div>
            <div class="notification-item-desc">{{ item.content }}</div>
            <div class="notification-time">{{ formatTime(item.createdTime) }}</div>
          </div>
        </div>

        <div v-if="notifications.length === 0 && !loading" class="notification-empty">
          <el-icon :size="32"><Bell /></el-icon>
          <span>暂无消息</span>
        </div>
      </div>

      <div v-if="total > pageSize" class="notification-footer">
        <el-button text type="primary" @click="router.push('/notifications')">查看全部</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Document, CircleCheck, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, getAdminNotifications, getAdminUnreadCount, adminMarkAsRead, adminMarkAllAsRead } from '@/api/notification'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const total = ref(0)
const pageSize = 5

const getTypeIcon = (type) => {
  const map = {
    1: Document,
    2: CircleCheck,
    3: InfoFilled
  }
  return map[type] || InfoFilled
}

const getTypeColor = (type) => {
  const map = {
    1: '#3b82f6',
    2: '#22c55e',
    3: '#f59e0b'
  }
  return map[type] || '#64748b'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return time.substring(0, 10)
}

const fetchNotifications = async () => {
  loading.value = true
  try {
    const res = authStore.isAdmin
      ? await getAdminNotifications({ pageNum: 1, pageSize: pageSize })
      : await getNotifications({ pageNum: 1, pageSize: pageSize })
    notifications.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取通知失败:', error)
    ElMessage.error('获取通知失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = authStore.isAdmin
      ? await getAdminUnreadCount()
      : await getUnreadCount()
    unreadCount.value = res.data?.count || 0
  } catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

const handleClick = async (item) => {
  if (item.readStatus === 0) {
    if (authStore.isAdmin) {
      await adminMarkAsRead(item.id)
    } else {
      await markAsRead(item.id)
    }
    unreadCount.value--
  }

  if (item.type === 1 && item.businessId) {
    if (authStore.isAdmin) {
      router.push('/admin/orders')
    } else {
      router.push(`/orders/${item.businessId}`)
    }
  }
}

const handleMarkAllRead = async () => {
  try {
    // 后端 /notification/read-all 会根据当前用户角色自动判断：
    // - 管理员：标记 userId=-1 的所有通知
    // - 普通用户：标记自己的所有通知
    await markAllAsRead()
    unreadCount.value = 0
    notifications.value.forEach(n => n.readStatus = 1)
    ElMessage.success('已全部已读')
    window.dispatchEvent(new Event('notification-unread-updated'))
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

let timer = null

onMounted(() => {
  fetchUnreadCount()
  timer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.notification-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: rgba(59, 130, 246, 0.1);
    border-radius: 8px;
  }
}

.notification-panel {
  max-height: 400px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.notification-list {
  min-height: 200px;
  max-height: 300px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;

  &:hover {
    background: #f8fafc;
  }

  &--unread {
    background: rgba(59, 130, 246, 0.05);
  }
}

.notification-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border-radius: 8px;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
  margin-bottom: 4px;
}

.notification-item-desc {
  font-size: 13px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #94a3b8;
}

.notification-footer {
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
  text-align: center;
}
</style>