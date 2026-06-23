<template>
  <view class="comment-page">
    <!-- 我的评价列表模式 -->
    <view v-if="mode === 'my'" class="my-comments">
      <view class="page-header">
        <text class="page-title">我的评价</text>
        <text class="page-subtitle">共 {{ commentList.length }} 条评价</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-box" v-if="commentList.length === 0 && !loading">
        <text class="empty-icon">💬</text>
        <text class="empty-title">暂无评价</text>
        <text class="empty-desc">您还没有对已完成的订单发表评价</text>
        <view class="empty-action" @click="goToOrders">
          <text class="empty-action-text">去查看我的订单</text>
        </view>
      </view>

      <!-- 评价列表 -->
      <view class="comment-list" v-else>
        <view class="comment-card" v-for="item in commentList" :key="item.id">
          <view class="comment-header">
            <image class="comment-avatar" :src="resolveImageUrl(item.droneImage || '')" mode="aspectFill" />
            <view class="comment-title-col">
              <text class="comment-drone-name">{{ item.droneModel || '无人机' }}</text>
              <view class="comment-rating-row">
                <text
                  class="comment-star"
                  v-for="s in 5"
                  :key="s"
                  :class="{ active: s <= (item.rating || 0) }"
                >★</text>
                <text class="comment-time">{{ formatDateTime(item.createdTime) }}</text>
              </view>
            </view>
          </view>
          <text class="comment-content">{{ item.content }}</text>
          <view class="comment-images" v-if="item.images && item.images.length > 0">
            <image
              class="comment-img"
              v-for="(img, i) in item.images"
              :key="i"
              :src="resolveImageUrl(img)"
              mode="aspectFill"
            />
          </view>
        </view>
      </view>
    </view>

    <!-- 发表评价模式（原有功能） -->
    <view v-else>
      <view class="page-header">
        <text class="page-title">发表评价</text>
        <text class="page-subtitle">您的真实反馈将帮助更多用户</text>
      </view>

      <view class="drone-card" v-if="droneInfo.id > 0">
        <view class="drone-img-box">
          <image class="drone-img" :src="droneInfo.image" mode="aspectFit" />
        </view>
        <view class="drone-info">
          <text class="drone-name">{{ droneInfo.name }}</text>
          <text class="drone-meta">{{ droneInfo.brand }} · {{ droneInfo.type }}</text>
          <text class="drone-price">¥{{ droneInfo.price }}/天</text>
        </view>
      </view>

      <view class="form-card">
        <view class="form-item">
          <text class="form-label">设备评分</text>
          <view class="rating-row">
            <view
              class="star-item"
              v-for="star in 5"
              :key="star"
              @click="selectRating(star)"
            >
              <text class="star-icon" :class="{ active: star <= rating }">★</text>
            </view>
          </view>
          <text class="rating-tip">{{ ratingText }}</text>
        </view>
      </view>

      <view class="form-card">
        <view class="form-item">
          <text class="form-label">体验标签</text>
          <view class="tag-row">
            <view
              class="tag-item"
              v-for="tag in tagOptions"
              :key="tag"
              :class="{ active: selectedTags.includes(tag) }"
              @click="toggleTag(tag)"
            >
              <text class="tag-text">{{ tag }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="form-card">
        <view class="form-item">
          <text class="form-label">评价内容</text>
          <textarea
            class="comment-textarea"
            v-model="content"
            placeholder="请分享您对该设备的使用体验..."
            placeholder-class="placeholder"
            maxlength="500"
          />
          <view class="word-count">
            <text class="word-text">{{ content.length }} / 500</text>
          </view>
        </view>
      </view>

      <view class="form-card">
        <view class="form-item">
          <text class="form-label">上传图片(选填,最多9张)</text>
          <view class="image-row">
            <view class="image-item" v-for="(img, index) in imageList" :key="index">
              <image class="uploaded-img" :src="img" mode="aspectFill" />
              <view class="image-delete" @click.stop="removeImage(index)">
                <text class="delete-text">×</text>
              </view>
            </view>
            <view class="image-item image-add" v-if="imageList.length < 9" @click="handleChooseImage">
              <text class="add-icon">+</text>
            </view>
          </view>
        </view>
      </view>

      <view class="form-card">
        <view class="form-item">
          <view class="anonymous-row">
            <text class="form-label">匿名评价</text>
            <switch :checked="isAnonymous" color="#2563eb" @change="(e) => isAnonymous = e.detail.value" />
          </view>
        </view>
      </view>

      <view class="submit-box">
        <view class="submit-btn" :class="{ disabled: !canSubmit }" @click="handleSubmit">
          <text class="submit-text">{{ submitting ? '提交中...' : '提交评价' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { addComment, uploadFile, getMyComments } from '../../api/comment'
import { resolveImageUrl } from '../../utils/image'

interface DroneInfo {
  id: number
  name: string
  brand: string
  type: string
  price: number
  image: string
}

interface MyCommentItem {
  id: number
  droneId: number
  droneModel?: string
  droneImage?: string
  rating: number
  content: string
  images?: string[]
  createdTime?: string
}

const mode = ref<'my' | 'create'>('create')
const loading = ref(false)
const commentList = ref<MyCommentItem[]>([])

const droneInfo = ref<DroneInfo>({
  id: 0,
  name: '',
  brand: '',
  type: '',
  price: 0,
  image: ''
})

const orderId = ref<number>(0)

const rating = ref<number>(5)
const content = ref<string>('')
const selectedTags = ref<string[]>([])
const imageList = ref<string[]>([])
const isAnonymous = ref<boolean>(false)
const submitting = ref<boolean>(false)

const tagOptions = ref<string[]>(['性能稳定', '画质清晰', '操控简单', '续航优秀', '信号稳定', '服务到位', '性价比高'])

const ratingText = computed<string>(() => {
  if (rating.value === 5) return '非常满意'
  if (rating.value === 4) return '满意'
  if (rating.value === 3) return '一般'
  if (rating.value === 2) return '不满意'
  return '非常差'
})

const canSubmit = computed<boolean>(() => {
  if (submitting.value) return false
  if (rating.value <= 0) return false
  if (!content.value || content.value.trim().length < 5) return false
  return true
})

const selectRating = (star: number) => {
  rating.value = star
}

const toggleTag = (tag: string) => {
  const idx = selectedTags.value.indexOf(tag)
  if (idx >= 0) {
    selectedTags.value.splice(idx, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

const handleChooseImage = () => {
  uni.chooseImage({
    count: 9 - imageList.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const tempFiles = res.tempFilePaths
      imageList.value.push(...tempFiles)
    }
  })
}

const removeImage = (index: number) => {
  imageList.value.splice(index, 1)
}

const handleSubmit = async () => {
  if (!canSubmit.value) {
    if (!content.value || content.value.trim().length < 5) {
      uni.showToast({ title: '评价内容至少5个字', icon: 'none' })
    } else {
      uni.showToast({ title: '请先评分', icon: 'none' })
    }
    return
  }

  if (droneInfo.value.id <= 0) {
    uni.showToast({ title: '设备信息错误', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const uploadedImages: string[] = []
    for (let i = 0; i < imageList.value.length; i++) {
      const img = imageList.value[i]
      if (img.startsWith('http://') || img.startsWith('https://')) {
        uploadedImages.push(img)
        continue
      }
      try {
        const res = await uploadFile(img, 'comment')
        const url = (res.data as any)?.url || (res.data as any) || ''
        if (url) {
          uploadedImages.push(url)
        }
      } catch (err) {
        console.error('图片上传失败', err)
      }
    }

    let finalContent = content.value
    if (selectedTags.value.length > 0) {
      finalContent = '【' + selectedTags.value.join(' ') + '】' + finalContent
    }
    if (isAnonymous.value) {
      finalContent = '【匿名】' + finalContent
    }

    await addComment({
      droneId: droneInfo.value.id,
      orderId: orderId.value > 0 ? orderId.value : undefined,
      content: finalContent,
      rating: rating.value,
      images: uploadedImages.length > 0 ? uploadedImages : undefined
    })
    uni.showToast({ title: '评价成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } catch (err) {
    console.error('提交评价失败', err)
  } finally {
    submitting.value = false
  }
}

const loadMyComments = async () => {
  loading.value = true
  try {
    const res = await getMyComments({ pageNum: 1, pageSize: 50 })
    const records: any[] = (res.data as any)?.records || (Array.isArray(res.data) ? res.data : [])
    commentList.value = records.map((r) => {
      let images: string[] = []
      if (r.images) {
        try {
          images = typeof r.images === 'string' ? JSON.parse(r.images) : r.images
        } catch (e) {
          images = []
        }
      }
      return {
        id: r.id,
        droneId: r.droneId,
        droneModel: r.droneModel || r.model || r.droneName || '无人机',
        droneImage: r.droneImage || r.image || '',
        rating: Number(r.rating) || 5,
        content: r.content || '',
        images,
        createdTime: r.createdTime || r.createTime || ''
      }
    })
  } catch (err) {
    console.error('获取我的评价失败', err)
  } finally {
    loading.value = false
  }
}

const goToOrders = () => {
  uni.switchTab({ url: '/pages/orders/list' })
}

const formatDateTime = (dateStr: string | undefined) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr.substring(0, 10)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const initFromPageOptions = () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const opts = (currentPage as any)?.options || {}
  const typeParam = String(opts.type || '').toLowerCase()
  if (typeParam === 'my') {
    mode.value = 'my'
    return
  }
  mode.value = 'create'
  if (opts.id) droneInfo.value.id = Number(opts.id) || 0
  if (opts.name) droneInfo.value.name = decodeURIComponent(opts.name)
  if (opts.brand) droneInfo.value.brand = decodeURIComponent(opts.brand)
  if (opts.type) droneInfo.value.type = decodeURIComponent(opts.type)
  if (opts.price) droneInfo.value.price = Number(opts.price) || 0
  if (opts.image) droneInfo.value.image = decodeURIComponent(opts.image)
  if (opts.orderId) orderId.value = Number(opts.orderId) || 0
}

onMounted(() => {
  initFromPageOptions()
  if (mode.value === 'my') {
    loadMyComments()
  }
})
</script>

<style lang="scss">
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.comment-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding: 20rpx 20rpx 40rpx;
  box-sizing: border-box;
}

.page-header {
  padding: 16rpx 12rpx 28rpx;
}

.page-title {
  display: block;
  font-size: 40rpx;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 8rpx;
}

.page-subtitle {
  display: block;
  font-size: 24rpx;
  color: #64748b;
}

/* 我的评价 - 空状态 */
.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
  background: #ffffff;
  border-radius: 20rpx;
  margin: 20rpx 0;
}

.empty-icon {
  font-size: 96rpx;
  margin-bottom: 24rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: #94a3b8;
  margin-bottom: 32rpx;
}

.empty-action {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  border-radius: 999rpx;
  padding: 18rpx 48rpx;
}

.empty-action-text {
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 600;
}

/* 我的评价 - 列表 */
.comment-list {
  margin-top: 8rpx;
}

.comment-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.04);
}

.comment-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-bottom: 16rpx;
}

.comment-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 16rpx;
  background: #f1f5f9;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.comment-title-col {
  flex: 1;
}

.comment-drone-name {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8rpx;
}

.comment-rating-row {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.comment-star {
  font-size: 28rpx;
  color: #e2e8f0;
  margin-right: 4rpx;

  &.active {
    color: #fbbf24;
  }
}

.comment-time {
  font-size: 22rpx;
  color: #94a3b8;
  margin-left: 16rpx;
}

.comment-content {
  display: block;
  font-size: 26rpx;
  color: #334155;
  line-height: 1.8;
  margin-bottom: 16rpx;
}

.comment-images {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
}

.comment-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  margin-right: 12rpx;
  margin-bottom: 12rpx;
  background: #f1f5f9;
}

/* 设备卡片 */
.drone-card {
  display: flex;
  flex-direction: row;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.04);
}

.drone-img-box {
  width: 160rpx;
  height: 120rpx;
  background: #f1f5f9;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.drone-img {
  width: 140rpx;
  height: 100rpx;
}

.drone-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.drone-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8rpx;
}

.drone-meta {
  font-size: 22rpx;
  color: #94a3b8;
  margin-bottom: 8rpx;
}

.drone-price {
  font-size: 24rpx;
  color: #2563eb;
  font-weight: 600;
}

/* 表单卡片 */
.form-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx 28rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 26rpx;
  color: #475569;
  font-weight: 500;
  margin-bottom: 16rpx;
}

.rating-row {
  display: flex;
  flex-direction: row;
  margin-bottom: 12rpx;
}

.star-item {
  margin-right: 16rpx;
}

.star-icon {
  font-size: 56rpx;
  color: #e2e8f0;

  &.active {
    color: #fbbf24;
  }
}

.rating-tip {
  font-size: 24rpx;
  color: #94a3b8;
}

.tag-row {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
}

.tag-item {
  background: #f1f5f9;
  border-radius: 999rpx;
  padding: 10rpx 20rpx;
  margin-right: 12rpx;
  margin-bottom: 12rpx;
  border: 2rpx solid transparent;

  &.active {
    background: #eff6ff;
    border-color: #2563eb;
  }
}

.tag-item.active .tag-text {
  color: #2563eb;
}

.tag-text {
  font-size: 24rpx;
  color: #475569;
}

.comment-textarea {
  width: 100%;
  height: 240rpx;
  background: #f8fafc;
  border-radius: 12rpx;
  padding: 20rpx;
  box-sizing: border-box;
  font-size: 28rpx;
  color: #0f172a;
}

.placeholder {
  color: #cbd5e1;
}

.word-count {
  display: flex;
  justify-content: flex-end;
  margin-top: 8rpx;
}

.word-text {
  font-size: 22rpx;
  color: #94a3b8;
}

.image-row {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
}

.image-item {
  width: 160rpx;
  height: 160rpx;
  background: #f8fafc;
  border-radius: 12rpx;
  margin-right: 12rpx;
  margin-bottom: 12rpx;
  position: relative;
  overflow: hidden;
}

.uploaded-img {
  width: 100%;
  height: 100%;
}

.image-delete {
  position: absolute;
  top: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.5);
  width: 40rpx;
  height: 40rpx;
  border-radius: 0 0 0 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-text {
  color: #ffffff;
  font-size: 28rpx;
  line-height: 1;
}

.image-add {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed #cbd5e1;
  background: #ffffff;
}

.add-icon {
  font-size: 56rpx;
  color: #94a3b8;
  font-weight: 300;
}

.anonymous-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.submit-box {
  margin-top: 40rpx;
}

.submit-btn {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  border-radius: 999rpx;
  padding: 26rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(37, 99, 235, 0.25);

  &.disabled {
    background: #cbd5e1;
    box-shadow: none;
  }
}

.submit-text {
  font-size: 30rpx;
  color: #ffffff;
  font-weight: 600;
  letter-spacing: 4rpx;
}
</style>
