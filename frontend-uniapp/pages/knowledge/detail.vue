<template>
  <view class="kb-detail-page">
    <view v-if="loading" class="empty-tip">
      <text class="empty-text">加载中...</text>
    </view>

    <view v-else-if="!doc" class="empty-tip">
      <text class="empty-text">文档不存在或已删除</text>
    </view>

    <template v-else>
      <!-- 头部信息 -->
      <view class="header-card">
        <view class="header-row1">
          <text class="header-icon">{{ fileIcon(doc.fileType) }}</text>
          <view class="header-info">
            <text class="header-name">{{ doc.filename }}</text>
            <view class="header-meta">
              <text class="scope-tag" :class="doc.scope === 'public' ? 'public' : 'private'">
                {{ doc.scope === 'public' ? '公共' : '私有' }}
              </text>
              <text class="header-status" :class="doc.status === 1 ? 'ready' : 'pending'">
                {{ doc.status === 1 ? '✓ 已就绪' : '处理中' }}
              </text>
            </view>
          </view>
        </view>

        <view class="header-stats">
          <view class="stat-cell">
            <text class="stat-value">{{ doc.chunkCount }}</text>
            <text class="stat-label">分块数</text>
          </view>
          <view class="stat-cell">
            <text class="stat-value">{{ formatCount(doc.charCount) }}</text>
            <text class="stat-label">字符数</text>
          </view>
          <view class="stat-cell">
            <text class="stat-value">{{ formatFileSize(doc.fileSize) }}</text>
            <text class="stat-label">文件大小</text>
          </view>
        </view>

        <view class="header-time">
          <text class="time-label">上传时间：</text>
          <text class="time-value">{{ formatTime(doc.createdTime) }}</text>
        </view>
      </view>

      <!-- 错误信息 -->
      <view v-if="doc.status !== 1 && doc.errorMessage" class="error-card">
        <text class="error-title">⚠️ 处理失败</text>
        <text class="error-msg">{{ doc.errorMessage }}</text>
      </view>

      <!-- 操作按钮 -->
      <view v-if="doc.userId === currentUserId" class="action-bar">
        <view class="action-btn primary" @click="goToSearch">
          <text class="action-text light">测试检索</text>
        </view>
        <view class="action-btn danger" @click="handleDelete">
          <text class="action-text light">删除文档</text>
        </view>
      </view>
      <view v-else class="action-bar">
        <view class="action-btn primary" @click="goToSearch">
          <text class="action-text light">检索使用</text>
        </view>
      </view>

      <!-- 文档描述 -->
      <view class="section-card">
        <view class="section-title-row">
          <text class="section-title">文档信息</text>
        </view>
        <view class="kv-list">
          <view class="kv-row">
            <text class="kv-label">文件名</text>
            <text class="kv-value">{{ doc.filename }}</text>
          </view>
          <view class="kv-row">
            <text class="kv-label">类型</text>
            <text class="kv-value">{{ doc.fileType || '-' }}</text>
          </view>
          <view class="kv-row">
            <text class="kv-label">大小</text>
            <text class="kv-value">{{ formatFileSize(doc.fileSize) }}</text>
          </view>
          <view class="kv-row">
            <text class="kv-label">分块数</text>
            <text class="kv-value">{{ doc.chunkCount }}（每块独立检索）</text>
          </view>
          <view class="kv-row">
            <text class="kv-label">字符数</text>
            <text class="kv-value">{{ doc.charCount }}</text>
          </view>
          <view class="kv-row">
            <text class="kv-label">可见性</text>
            <text class="kv-value">
              {{ doc.scope === 'public' ? '🌐 公共（所有人可见）' : '🔒 私有（仅我可见）' }}
            </text>
          </view>
          <view v-if="doc.userId === 0" class="kv-row">
            <text class="kv-label">来源</text>
            <text class="kv-value">系统内置</text>
          </view>
        </view>
      </view>
    </template>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getDetail,
  remove,
  formatFileSize,
  type KbDocument
} from '../../api/knowledge'

const doc = ref<KbDocument | null>(null)
const loading = ref(true)
const docId = ref<number>(0)

const currentUserId = computed<number>(() => {
  const cached = uni.getStorageSync('userInfo') as any
  return cached?.id || 0
})

const loadDetail = async () => {
  if (!docId.value) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const res = await getDetail(docId.value)
    doc.value = (res.data as KbDocument) || null
  } catch (err) {
    console.error('[KB] 详情加载失败', err)
    doc.value = null
  } finally {
    loading.value = false
  }
}

const handleDelete = () => {
  if (!doc.value) return
  uni.showModal({
    title: '确认删除',
    content: `确定删除「${doc.value.filename}」吗？`,
    success: async (modal) => {
      if (!modal.confirm) return
      try {
        const res = await remove(doc.value!.id)
        if (res.code === 200) {
          uni.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 600)
        } else {
          uni.showToast({ title: res.message || '删除失败', icon: 'none' })
        }
      } catch (err) {
        console.error('[KB] 删除失败', err)
      }
    }
  })
}

const goToSearch = () => {
  uni.navigateTo({ url: '/pages/knowledge/list' })
}

const fileIcon = (ext?: string) => {
  const e = (ext || '').toLowerCase()
  if (e === 'pdf') return '📕'
  if (e === 'doc' || e === 'docx') return '📘'
  if (e === 'ppt' || e === 'pptx') return '📙'
  if (e === 'xls' || e === 'xlsx') return '📗'
  if (e === 'txt' || e === 'md') return '📄'
  if (e === 'html' || e === 'htm') return '🌐'
  return '📎'
}

const formatCount = (n: number) => {
  if (!n) return '0'
  return n.toLocaleString()
}

const formatTime = (s?: string) => {
  if (!s) return ''
  return s.replace('T', ' ').substring(0, 19)
}

onLoad((options: any) => {
  const id = Number(options?.id || 0)
  if (id) docId.value = id
})

onMounted(() => {
  loadDetail()
})
</script>

<style lang="scss" scoped>
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.kb-detail-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding: 20rpx 20rpx 200rpx;
  box-sizing: border-box;
}

.header-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
}

.header-row1 {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-bottom: 24rpx;
}

.header-icon {
  font-size: 80rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.header-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  word-break: break-all;
  margin-bottom: 8rpx;
}

.header-meta {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12rpx;
}

.scope-tag {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;

  &.public {
    background: #dbeafe;
    color: #1d4ed8;
  }

  &.private {
    background: #fef3c7;
    color: #b45309;
  }
}

.header-status {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;

  &.ready {
    background: #d1fae5;
    color: #047857;
  }

  &.pending {
    background: #fef3c7;
    color: #b45309;
  }
}

.header-stats {
  display: flex;
  flex-direction: row;
  background: #f8fafc;
  border-radius: 14rpx;
  padding: 20rpx 0;
  margin-bottom: 16rpx;
}

.stat-cell {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #2563eb;
}

.stat-label {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 4rpx;
}

.header-time {
  font-size: 24rpx;
  color: #64748b;
  display: flex;
  flex-direction: row;
}

.time-label {
  color: #94a3b8;
}

.time-value {
  color: #475569;
  flex: 1;
}

/* 错误卡 */
.error-card {
  background: #fef2f2;
  border: 2rpx solid #fecaca;
  border-radius: 20rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.error-title {
  font-size: 28rpx;
  color: #b91c1c;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.error-msg {
  font-size: 24rpx;
  color: #991b1b;
  line-height: 1.5;
}

/* 操作 */
.action-bar {
  display: flex;
  flex-direction: row;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.action-btn {
  flex: 1;
  border-radius: 16rpx;
  padding: 22rpx 0;
  text-align: center;

  &.primary {
    background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  }

  &.danger {
    background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
  }
}

.action-text {
  font-size: 28rpx;
  font-weight: 600;

  &.light {
    color: #ffffff;
  }
}

/* 文档信息 */
.section-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-sizing: border-box;
}

.section-title-row {
  margin-bottom: 16rpx;
  padding-bottom: 12rpx;
  border-bottom: 2rpx solid #f1f5f9;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

.kv-list {
  display: flex;
  flex-direction: column;
}

.kv-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16rpx 0;
  font-size: 26rpx;

  &:not(:last-child) {
    border-bottom: 2rpx solid #f8fafc;
  }
}

.kv-label {
  color: #94a3b8;
  flex-shrink: 0;
  width: 140rpx;
}

.kv-value {
  color: #0f172a;
  flex: 1;
  text-align: right;
  word-break: break-all;
}

/* 空状态 */
.empty-tip {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 80rpx 20rpx;
  text-align: center;
}

.empty-text {
  font-size: 26rpx;
  color: #94a3b8;
}

.bottom-space {
  height: 60rpx;
}
</style>
