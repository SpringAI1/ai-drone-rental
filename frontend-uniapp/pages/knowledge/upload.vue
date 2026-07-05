<template>
  <view class="kb-upload-page">
    <!-- 顶部说明 -->
    <view class="tip-card">
      <text class="tip-title">上传知识库文档</text>
      <text class="tip-desc">
        支持 PDF / Word / PPT / Excel / TXT / MD 等常见格式
        （单个文件最大 50MB）
      </text>
    </view>

    <!-- 选中的文件 -->
    <view v-if="file" class="file-card">
      <view class="file-row">
        <text class="file-icon">{{ fileIcon }}</text>
        <view class="file-info">
          <text class="file-name">{{ file.name }}</text>
          <text class="file-meta">{{ fileSizeText }} · {{ fileExt }}</text>
        </view>
        <text class="file-remove" @click="clearFile">×</text>
      </view>
    </view>

    <!-- 选文件按钮 -->
    <view v-else class="picker-card" @click="chooseFile">
      <text class="picker-icon">+</text>
      <text class="picker-text">点击选择文件</text>
      <text class="picker-sub">支持 PDF / Word / PPT / Excel / TXT</text>
    </view>

    <!-- 可见性选择 -->
    <view class="form-card">
      <view class="form-row">
        <text class="form-label">可见性</text>
        <view class="scope-picker">
          <view
            class="scope-option"
            :class="{ active: scope === 'private' }"
            @click="scope = 'private'"
          >
            <text class="scope-text">🔒 私有（仅我可见）</text>
          </view>
          <view
            class="scope-option"
            :class="{ active: scope === 'public' }"
            @click="scope = 'public'"
          >
            <text class="scope-text">🌐 公共（所有人可见）</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 解析说明 -->
    <view class="info-card">
      <view class="info-row">
        <text class="info-icon">⚡</text>
        <text class="info-text">上传后将自动完成解析、切块、向量化，并写入知识库</text>
      </view>
      <view class="info-row">
        <text class="info-icon">🔍</text>
        <text class="info-text">可立即在「AI 知识库」中检索命中</text>
      </view>
      <view class="info-row">
        <text class="info-icon">🤖</text>
        <text class="info-text">AI 客服问答时会自动从知识库召回相关内容</text>
      </view>
    </view>

    <!-- 提交 -->
    <view class="bottom-bar">
      <view
        class="submit-btn"
        :class="{ disabled: !file || uploading }"
        @click="handleSubmit"
      >
        <text class="submit-text">{{ uploading ? '处理中...' : '上传并入库' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface PickedFile {
  path: string  // uni.chooseMessageFile 返回的 path
  name: string
  size: number
  ext: string
}

const file = ref<PickedFile | null>(null)
const scope = ref<'private' | 'public'>('private')
const uploading = ref(false)

const fileExt = computed(() => file.value?.ext.toLowerCase() || '')
const fileSizeText = computed(() => {
  if (!file.value) return ''
  const b = file.value.size
  if (b < 1024) return `${b} B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`
  return `${(b / 1024 / 1024).toFixed(2)} MB`
})
const fileIcon = computed(() => {
  const e = fileExt.value
  if (e === 'pdf') return '📕'
  if (e === 'doc' || e === 'docx') return '📘'
  if (e === 'ppt' || e === 'pptx') return '📙'
  if (e === 'xls' || e === 'xlsx') return '📗'
  if (e === 'txt' || e === 'md') return '📄'
  if (e === 'html' || e === 'htm') return '🌐'
  return '📎'
})

const ALLOWED_EXTS = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'md', 'html', 'htm', 'xml', 'json', 'csv']
const MAX_SIZE = 50 * 1024 * 1024

const chooseFile = () => {
  // #ifdef MP-WEIXIN
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    success: (res) => {
      const f = res.tempFiles?.[0]
      if (!f) return
      handlePicked(f.path, f.name, f.size)
    },
    fail: (err) => {
      console.warn('[KB] chooseMessageFile 失败，回退到 chooseFile', err)
      chooseFileFallback()
    }
  })
  // #endif

  // #ifndef MP-WEIXIN
  chooseFileFallback()
  // #endif
}

const chooseFileFallback = () => {
  uni.chooseFile({
    count: 1,
    extension: ALLOWED_EXTS,
    success: (res) => {
      const f = res.tempFiles?.[0]
      if (!f) return
      handlePicked(f.path, f.name, f.size)
    },
    fail: (err) => {
      console.error('[KB] chooseFile 失败', err)
      uni.showToast({ title: '选择文件失败', icon: 'none' })
    }
  })
}

const handlePicked = (path: string, name: string, size: number) => {
  const ext = (name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED_EXTS.includes(ext)) {
    uni.showToast({ title: `不支持的格式: .${ext}`, icon: 'none' })
    return
  }
  if (size > MAX_SIZE) {
    uni.showToast({ title: '文件超过 50MB', icon: 'none' })
    return
  }
  file.value = { path, name, size, ext }
}

const clearFile = () => {
  file.value = null
}

const handleSubmit = async () => {
  if (!file.value || uploading.value) return
  uploading.value = true
  uni.showLoading({ title: '上传并解析中...', mask: true })
  try {
    const { uploadKb } = await import('../../api/knowledge')
    const res = await uploadKb(file.value.path, scope.value)
    uni.hideLoading()
    if (res.data) {
      uni.showToast({ title: '上传成功', icon: 'success' })
      setTimeout(() => {
        uni.redirectTo({ url: `/pages/knowledge/detail?id=${res.data.id}` })
      }, 800)
    } else {
      uni.showToast({ title: '上传失败', icon: 'none' })
    }
  } catch (err: any) {
    uni.hideLoading()
    console.error('[KB] 上传失败', err)
    const msg = err?.message || err?.data?.message || '上传失败'
    uni.showToast({ title: msg, icon: 'none' })
  } finally {
    uploading.value = false
  }
}
</script>

<style lang="scss" scoped>
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.kb-upload-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding: 24rpx 20rpx 200rpx;
  box-sizing: border-box;
}

.tip-card {
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  border-radius: 20rpx;
  padding: 24rpx 24rpx;
  margin-bottom: 24rpx;
}

.tip-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1e40af;
  display: block;
}

.tip-desc {
  font-size: 24rpx;
  color: #1e3a8a;
  margin-top: 8rpx;
  line-height: 1.5;
  display: block;
}

/* 选文件区 */
.picker-card {
  background: #ffffff;
  border: 2rpx dashed #cbd5e1;
  border-radius: 20rpx;
  padding: 80rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.picker-icon {
  font-size: 80rpx;
  color: #2563eb;
  font-weight: 200;
  line-height: 1;
}

.picker-text {
  font-size: 30rpx;
  color: #0f172a;
  font-weight: 600;
  margin-top: 16rpx;
}

.picker-sub {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 8rpx;
}

/* 选中的文件 */
.file-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx 24rpx;
  margin-bottom: 24rpx;
  box-sizing: border-box;
}

.file-row {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.file-icon {
  font-size: 64rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.file-name {
  font-size: 28rpx;
  color: #0f172a;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 6rpx;
}

.file-remove {
  font-size: 48rpx;
  color: #94a3b8;
  padding: 0 12rpx;
  line-height: 1;
  flex-shrink: 0;
}

/* 表单 */
.form-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-sizing: border-box;
}

.form-row {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 28rpx;
  color: #0f172a;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.scope-picker {
  display: flex;
  flex-direction: row;
  gap: 16rpx;
}

.scope-option {
  flex: 1;
  border: 2rpx solid #e2e8f0;
  border-radius: 14rpx;
  padding: 20rpx 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;

  &.active {
    border-color: #2563eb;
    background: #dbeafe;
  }
}

.scope-text {
  font-size: 24rpx;
  color: #475569;

  .active & {
    color: #1d4ed8;
    font-weight: 600;
  }
}

/* 说明卡 */
.info-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-sizing: border-box;
}

.info-row {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 8rpx 0;
}

.info-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.info-text {
  font-size: 26rpx;
  color: #475569;
  line-height: 1.6;
  flex: 1;
}

/* 底部提交按钮 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  padding: 20rpx 20rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
  z-index: 10;
}

.submit-btn {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  border-radius: 16rpx;
  padding: 24rpx 0;
  text-align: center;

  &.disabled {
    opacity: 0.5;
  }
}

.submit-text {
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
}
</style>
