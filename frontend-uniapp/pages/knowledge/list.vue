<template>
  <view class="kb-page">
    <!-- 顶部统计 -->
    <view class="kb-stats">
      <view class="stats-item">
        <text class="stats-value">{{ stats.totalChunks || 0 }}</text>
        <text class="stats-label">总片段</text>
      </view>
      <view class="stats-divider"></view>
      <view class="stats-item">
        <text class="stats-value">{{ stats.publicChunks || 0 }}</text>
        <text class="stats-label">公共</text>
      </view>
      <view class="stats-divider"></view>
      <view class="stats-item">
        <text class="stats-value">{{ stats.privateChunks || 0 }}</text>
        <text class="stats-label">我的私有</text>
      </view>
    </view>

    <!-- 搜索栏 -->
    <view class="kb-search">
      <view class="search-input-wrap">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="query"
          placeholder="在知识库中搜索..."
          confirm-type="search"
          @confirm="handleSearch"
        />
        <text v-if="query" class="search-clear" @click="clearQuery">×</text>
      </view>
      <view class="search-btn" @click="handleSearch">
        <text class="search-btn-text">搜索</text>
      </view>
    </view>

    <!-- Tabs -->
    <view class="kb-tabs">
      <view
        class="kb-tab"
        v-for="t in tabs"
        :key="t.key"
        :class="{ active: activeTab === t.key }"
        @click="switchTab(t.key)"
      >
        <text class="kb-tab-text">{{ t.label }}</text>
        <text v-if="counts[t.key] > 0" class="kb-tab-count">{{ counts[t.key] }}</text>
      </view>
    </view>

    <!-- 搜索结果 -->
    <view v-if="searchMode" class="search-result-wrap">
      <view class="result-header">
        <text class="result-title">检索结果（top {{ searchHits.length }}）</text>
        <text class="result-back" @click="exitSearch">返回列表</text>
      </view>
      <view v-if="searchLoading" class="empty-tip">
        <text class="empty-text">检索中...</text>
      </view>
      <view v-else-if="searchHits.length === 0" class="empty-tip">
        <text class="empty-text">未找到相关内容</text>
      </view>
      <view
        v-else
        class="result-card"
        v-for="(hit, idx) in searchHits"
        :key="hit.id"
        @click="goToDetail(hit.documentId)"
      >
        <view class="result-card-head">
          <text class="result-idx">#{{ idx + 1 }}</text>
          <text class="result-source">{{ hit.source || 'unknown' }}</text>
          <text class="result-score">相似度 {{ formatScore(hit.score) }}</text>
        </view>
        <text class="result-text">{{ truncate(hit.text, 200) }}</text>
      </view>
    </view>

    <!-- 文档列表 -->
    <view v-else class="kb-list">
      <view v-if="loading" class="empty-tip">
        <text class="empty-text">加载中...</text>
      </view>
      <view v-else-if="filteredDocs.length === 0" class="empty-tip">
        <text class="empty-text">{{ emptyText }}</text>
      </view>
      <view
        v-else
        class="kb-doc-card"
        v-for="doc in filteredDocs"
        :key="doc.id"
        @click="goToDetail(doc.id)"
      >
        <view class="doc-card-row1">
          <text class="doc-icon">{{ fileIcon(doc.fileType) }}</text>
          <view class="doc-meta">
            <text class="doc-name">{{ doc.filename }}</text>
            <text class="doc-sub">
              <text class="scope-tag" :class="doc.scope === 'public' ? 'public' : 'private'">
                {{ doc.scope === 'public' ? '公共' : '私有' }}
              </text>
              <text class="doc-time">{{ formatTime(doc.createdTime) }}</text>
            </text>
          </view>
          <text v-if="doc.userId === currentUserId" class="doc-del" @click.stop="handleDelete(doc)">删除</text>
        </view>
        <view class="doc-card-row2">
          <text class="doc-stat">📄 {{ doc.chunkCount }} 块</text>
          <text class="doc-stat">📝 {{ doc.charCount }} 字</text>
          <text class="doc-stat">💾 {{ formatFileSize(doc.fileSize) }}</text>
        </view>
      </view>
    </view>

    <!-- 右下角悬浮 + 按钮 -->
    <view v-if="!searchMode" class="fab" @click="goToUpload">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  listVisible,
  listMine,
  listPublic,
  remove,
  stats as fetchStats,
  search as fetchSearch,
  formatFileSize,
  type KbDocument,
  type KbStats,
  type KbSearchHit
} from '../../api/knowledge'

type TabKey = 'visible' | 'mine' | 'public'

const tabs: { key: TabKey; label: string }[] = [
  { key: 'visible', label: '可见' },
  { key: 'mine', label: '我的' },
  { key: 'public', label: '公共' }
]

const activeTab = ref<TabKey>('visible')
const query = ref('')
const searchMode = ref(false)
const searchLoading = ref(false)
const searchHits = ref<KbSearchHit[]>([])

const loading = ref(false)
const docsVisible = ref<KbDocument[]>([])
const docsMine = ref<KbDocument[]>([])
const docsPublic = ref<KbDocument[]>([])
const stats = ref<KbStats>({ totalChunks: 0, publicChunks: 0, privateChunks: 0 })

const currentUserId = computed<number>(() => {
  const cached = uni.getStorageSync('userInfo') as any
  return cached?.id || 0
})

const counts = computed(() => ({
  visible: docsVisible.value.length,
  mine: docsMine.value.length,
  public: docsPublic.value.length
}))

const filteredDocs = computed<KbDocument[]>(() => {
  if (activeTab.value === 'mine') return docsMine.value
  if (activeTab.value === 'public') return docsPublic.value
  return docsVisible.value
})

const emptyText = computed(() => {
  if (activeTab.value === 'mine') return '还没上传过文档\n点击右下角 + 上传'
  if (activeTab.value === 'public') return '公共知识库为空'
  return '暂无可访问的知识库'
})

const loadAll = async () => {
  loading.value = true
  try {
    const [v, m, p, s] = await Promise.all([
      listVisible(),
      listMine(),
      listPublic(),
      fetchStats()
    ])
    if (v.data) docsVisible.value = v.data
    if (m.data) docsMine.value = m.data
    if (p.data) docsPublic.value = p.data
    if (s.data) stats.value = s.data
  } catch (err) {
    console.error('[KB] 加载列表失败', err)
  } finally {
    loading.value = false
  }
}

const switchTab = (key: TabKey) => {
  activeTab.value = key
}

const handleSearch = async () => {
  const q = query.value.trim()
  if (!q) {
    uni.showToast({ title: '请输入查询内容', icon: 'none' })
    return
  }
  searchMode.value = true
  searchLoading.value = true
  try {
    const res = await fetchSearch(q, 5)
    searchHits.value = res.data || []
  } catch (err) {
    console.error('[KB] 搜索失败', err)
    searchHits.value = []
  } finally {
    searchLoading.value = false
  }
}

const clearQuery = () => {
  query.value = ''
}

const exitSearch = () => {
  searchMode.value = false
  searchHits.value = []
  query.value = ''
}

const handleDelete = (doc: KbDocument) => {
  uni.showModal({
    title: '确认删除',
    content: `确定删除「${doc.filename}」吗？此操作不可恢复。`,
    success: async (modal) => {
      if (!modal.confirm) return
      try {
        const res = await remove(doc.id)
        if (res.code === 200) {
          uni.showToast({ title: '已删除', icon: 'success' })
          await loadAll()
        } else {
          uni.showToast({ title: res.message || '删除失败', icon: 'none' })
        }
      } catch (err) {
        console.error('[KB] 删除失败', err)
      }
    }
  })
}

const goToDetail = (id?: number) => {
  if (!id) return
  uni.navigateTo({ url: `/pages/knowledge/detail?id=${id}` })
}

const goToUpload = () => {
  uni.navigateTo({ url: '/pages/knowledge/upload' })
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

const truncate = (s: string, n: number) => {
  if (!s) return ''
  return s.length > n ? s.substring(0, n) + '...' : s
}

const formatScore = (s?: number) => {
  if (s == null) return 'N/A'
  return (s * 100).toFixed(1) + '%'
}

const formatTime = (s?: string) => {
  if (!s) return ''
  return s.replace('T', ' ').substring(0, 16)
}

onShow(() => {
  loadAll()
})

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.kb-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding-bottom: 160rpx;
  box-sizing: border-box;
}

/* 顶部统计 */
.kb-stats {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-around;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  padding: 24rpx 20rpx;
  color: #ffffff;
}

.stats-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.stats-value {
  font-size: 36rpx;
  font-weight: 700;
}

.stats-label {
  font-size: 22rpx;
  margin-top: 4rpx;
  opacity: 0.85;
}

.stats-divider {
  width: 2rpx;
  height: 60rpx;
  background: rgba(255, 255, 255, 0.3);
}

/* 搜索栏 */
.kb-search {
  display: flex;
  flex-direction: row;
  align-items: center;
  background: #ffffff;
  margin: 20rpx 20rpx 0;
  padding: 16rpx 20rpx;
  border-radius: 20rpx;
  gap: 16rpx;
}

.search-input-wrap {
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: center;
  background: #f1f5f9;
  border-radius: 999rpx;
  padding: 0 20rpx;
  height: 64rpx;
}

.search-icon {
  font-size: 28rpx;
  color: #94a3b8;
  margin-right: 8rpx;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #0f172a;
  height: 64rpx;
}

.search-clear {
  font-size: 36rpx;
  color: #94a3b8;
  padding: 0 8rpx;
}

.search-btn {
  background: #2563eb;
  border-radius: 999rpx;
  padding: 12rpx 24rpx;
}

.search-btn-text {
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 600;
}

/* Tabs */
.kb-tabs {
  display: flex;
  flex-direction: row;
  background: #ffffff;
  margin: 20rpx 20rpx 0;
  border-radius: 20rpx;
  padding: 8rpx 12rpx;
  gap: 8rpx;
}

.kb-tab {
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 16rpx 0;
  border-radius: 14rpx;

  &.active {
    background: #2563eb;
  }
}

.kb-tab-text {
  font-size: 26rpx;
  color: #475569;

  .active & {
    color: #ffffff;
    font-weight: 600;
  }
}

.kb-tab-count {
  font-size: 20rpx;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
  margin-left: 6rpx;

  .active & {
    background: rgba(255, 255, 255, 0.25);
    color: #ffffff;
  }
}

/* 文档列表 */
.kb-list {
  margin: 20rpx 20rpx 0;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.kb-doc-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  box-sizing: border-box;
}

.doc-card-row1 {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.doc-icon {
  font-size: 56rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.doc-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.doc-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-sub {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-top: 6rpx;
}

.scope-tag {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  margin-right: 12rpx;

  &.public {
    background: #dbeafe;
    color: #1d4ed8;
  }

  &.private {
    background: #fef3c7;
    color: #b45309;
  }
}

.doc-time {
  font-size: 22rpx;
  color: #94a3b8;
}

.doc-del {
  font-size: 24rpx;
  color: #ef4444;
  padding: 8rpx 12rpx;
  flex-shrink: 0;
}

.doc-card-row2 {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-top: 12rpx;
  gap: 24rpx;
}

.doc-stat {
  font-size: 22rpx;
  color: #64748b;
}

/* 搜索结果 */
.search-result-wrap {
  margin: 20rpx 20rpx 0;
}

.result-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.result-title {
  font-size: 26rpx;
  color: #0f172a;
  font-weight: 600;
}

.result-back {
  font-size: 24rpx;
  color: #2563eb;
  padding: 4rpx 12rpx;
}

.result-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  box-sizing: border-box;
}

.result-card-head {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-bottom: 10rpx;
}

.result-idx {
  font-size: 22rpx;
  color: #2563eb;
  font-weight: 700;
  background: #dbeafe;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
  margin-right: 12rpx;
}

.result-source {
  flex: 1;
  font-size: 22rpx;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-score {
  font-size: 22rpx;
  color: #10b981;
  font-weight: 600;
  flex-shrink: 0;
}

.result-text {
  font-size: 26rpx;
  color: #0f172a;
  line-height: 1.6;
}

/* 浮按钮 */
.fab {
  position: fixed;
  right: 32rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50rpx;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 20rpx rgba(37, 99, 235, 0.35);
  z-index: 99;
}

.fab-icon {
  font-size: 56rpx;
  color: #ffffff;
  line-height: 1;
  font-weight: 300;
}

/* 空状态 */
.empty-tip {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 60rpx 20rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-text {
  font-size: 26rpx;
  color: #94a3b8;
  white-space: pre-line;
  line-height: 1.6;
}
</style>
