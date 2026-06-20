<template>
  <view class="notif-page">
    <!-- 顶部操作栏 -->
    <view class="notif-header">
      <view class="header-left">
        <text class="header-title">消息通知</text>
        <text class="header-sub" v-if="unreadCount > 0">{{ unreadCount }} 条未读</text>
      </view>
      <view class="read-all-btn" @click="handleMarkAllRead" v-if="unreadCount > 0">
        <text class="read-all-text">全部已读</text>
      </view>
    </view>

    <!-- 通知列表 -->
    <view class="notif-list" v-if="notificationList.length > 0">
      <view
        class="notif-card"
        v-for="item in notificationList"
        :key="item.id"
        :class="{ unread: !item.isRead }"
        @click="handleClickNotification(item)"
      >
        <view class="notif-icon-box" :style="{ background: getTypeBg(item.type) }">
          <text class="notif-icon" :style="{ color: getTypeColor(item.type) }">{{ getTypeIcon(item.type) }}</text>
        </view>
        <view class="notif-content">
          <view class="notif-row">
            <text class="notif-title">{{ item.title || '系统通知' }}</text>
            <text class="notif-time">{{ formatTime(item.createTime) }}</text>
          </view>
          <text class="notif-desc">{{ item.content }}</text>
        </view>
        <view v-if="!item.isRead" class="unread-dot"></view>
      </view>
    </view>

    <!-- 空态 -->
    <view class="empty-state" v-else>
      <text class="empty-icon">🔔</text>
      <text class="empty-text">暂无消息通知</text>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getNotificationList, markRead, markAllRead, getUnreadCount } from '../../api/notification'

interface Notification {
  id: number
  type: number
  title: string
  content: string
  isRead: boolean
  createTime: string
  relatedId?: number
  relatedType?: string
}

const notificationList = ref<Notification[]>([])
const unreadCount = ref<number>(0)

const loadList = async () => {
  try {
    const res = await getNotificationList({ pageNum: 1, pageSize: 50 })
    const records = ((res.data as any)?.records) || []
    notificationList.value = records.map((n: any) => ({
      id: n.id,
      type: n.type !== undefined ? Number(n.type) : 0,
      title: n.title || '系统通知',
      content: n.content || '',
      isRead: n.isRead === true || n.isRead === 1,
      createTime: n.createTime || '',
      relatedId: n.relatedId,
      relatedType: n.relatedType
    }))
  } catch (err) {
    console.error('获取通知列表失败', err)
    notificationList.value = []
  }
}

const loadUnread = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = Number((res.data as any) || 0)
  } catch (err) {
    console.error('获取未读数量失败', err)
  }
}

const getTypeIcon = (type: number): string => {
  const map: Record<number, string> = {
    1: '📦',
    2: '💰',
    3: '✓',
    4: '✈',
    5: '🔧',
    6: '🔔'
  }
  return map[type] || '🔔'
}

const getTypeColor = (type: number): string => {
  const map: Record<number, string> = {
    1: '#2563eb',
    2: '#f59e0b',
    3: '#10b981',
    4: '#7c3aed',
    5: '#ef4444',
    6: '#64748b'
  }
  return map[type] || '#64748b'
}

const getTypeBg = (type: number): string => {
  const map: Record<number, string> = {
    1: '#eff6ff',
    2: '#fef3c7',
    3: '#d1fae5',
    4: '#ede9fe',
    5: '#fee2e2',
    6: '#f1f5f9'
  }
  return map[type] || '#f1f5f9'
}

const formatTime = (time: string): string => {
  if (!time) return ''
  try {
    const d = new Date(time.replace(/-/g, '/'))
    const now = new Date()
    const diff = Math.floor((now.getTime() - d.getTime()) / 1000)
    if (diff < 60) return '刚刚'
    if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
    if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
    if (diff < 604800) return Math.floor(diff / 86400) + '天前'
    return time.substring(0, 10)
  } catch (e) {
    return time
  }
}

const handleClickNotification = async (item: Notification) => {
  if (!item.isRead) {
    try {
      await markRead(item.id)
      item.isRead = true
      if (unreadCount.value > 0) unreadCount.value -= 1
    } catch (err) {
      console.error('标记已读失败', err)
    }
  }
  // 根据 relatedType 跳转到对应页面
  if (item.relatedType === 'order' && item.relatedId) {
    uni.switchTab({ url: '/pages/orders/list' })
  } else if (item.relatedType === 'airspace' && item.relatedId) {
    uni.navigateTo({ url: '/pages/airspace/record' })
  } else if (item.relatedType === 'fault' && item.relatedId) {
    uni.navigateTo({ url: '/pages/fault/report' })
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllRead()
    notificationList.value = notificationList.value.map((n) => ({ ...n, isRead: true }))
    unreadCount.value = 0
    uni.showToast({ title: '已全部标记为已读', icon: 'success' })
  } catch (err) {
    console.error('全部已读失败', err)
  }
}

onShow(() => {
  loadList()
  loadUnread()
})

onMounted(() => {
  loadList()
  loadUnread()
})
</script>

<style lang="scss">
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.notif-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* 顶部 */
.notif-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  padding: 24rpx 28rpx;
}

.header-left {
  display: flex;
  flex-direction: row;
  align-items: baseline;
}

.header-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
  margin-right: 12rpx;
}

.header-sub {
  font-size: 22rpx;
  color: #ef4444;
  font-weight: 500;
}

.read-all-btn {
  background: #eff6ff;
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
}

.read-all-text {
  font-size: 22rpx;
  color: #2563eb;
  font-weight: 500;
}

/* 通知列表 */
.notif-list {
  padding: 16rpx 20rpx;
}

.notif-card {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  background: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx 20rpx;
  margin-bottom: 16rpx;
  position: relative;
  box-shadow: 0 2rpx 6rpx rgba(15, 23, 42, 0.04);

  &.unread {
    background: #ffffff;
    border-left: 6rpx solid #2563eb;
  }
}

.notif-icon-box {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 20rpx;
}

.notif-icon {
  font-size: 40rpx;
}

.notif-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.notif-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.notif-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #0f172a;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-time {
  font-size: 20rpx;
  color: #94a3b8;
  flex-shrink: 0;
  margin-left: 12rpx;
}

.notif-desc {
  font-size: 24rpx;
  color: #64748b;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.unread-dot {
  position: absolute;
  top: 24rpx;
  right: 20rpx;
  width: 12rpx;
  height: 12rpx;
  background: #ef4444;
  border-radius: 6rpx;
}

/* 空态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
  opacity: 0.5;
}

.empty-text {
  font-size: 26rpx;
  color: #94a3b8;
}

.bottom-space {
  height: 60rpx;
}
</style>
