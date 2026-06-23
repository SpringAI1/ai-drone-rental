<template>
  <view class="qual-page">
    <!-- 顶部状态卡片 -->
    <view class="status-card">
      <view class="status-left">
        <view class="status-icon-box" :class="statusClass">
          <text class="status-icon">{{ statusIcon }}</text>
        </view>
        <view class="status-text-box">
          <text class="status-title">{{ statusTitle }}</text>
          <text class="status-desc">{{ statusDesc }}</text>
        </view>
      </view>
      <view v-if="currentQualification && currentQualification.auditStatus === 1" class="status-badge-approved">
        <text class="badge-text">已通过</text>
      </view>
    </view>

    <!-- 已提交资质的信息展示 -->
    <view v-if="currentQualification && currentQualification.id" class="info-card">
      <view class="info-title-row">
        <text class="info-title">资质信息</text>
        <text v-if="currentQualification.auditStatus === 1" class="info-tag-approved">审核通过</text>
        <text v-else-if="currentQualification.auditStatus === 0" class="info-tag-pending">审核中</text>
        <text v-else class="info-tag-rejected">已拒绝</text>
      </view>

      <view class="info-row">
        <text class="info-label">证件类型</text>
        <text class="info-value">{{ currentQualification.certificateType || '身份证' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">证件号码</text>
        <text class="info-value">{{ maskCertificateNo(currentQualification.certificateNo) }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">有效期</text>
        <text class="info-value">{{ currentQualification.validStartDate }} 至 {{ currentQualification.validEndDate }}</text>
      </view>

      <view v-if="currentQualification.certificateImage" class="image-row">
        <text class="info-label">证件图片</text>
        <image
          class="cert-image"
          :src="resolveImageUrl(currentQualification.certificateImage)"
          mode="aspectFill"
          @click="previewImage(currentQualification.certificateImage)"
        />
      </view>

      <view v-if="currentQualification.auditRemark" class="remark-row">
        <text class="info-label">审核备注</text>
        <text class="info-value remark-text">{{ currentQualification.auditRemark }}</text>
      </view>
    </view>

    <!-- 资质提交表单（未提交 / 审核被拒绝时允许重新提交） -->
    <view v-if="showForm" class="form-card">
      <view class="form-title-row">
        <text class="form-title">{{ currentQualification && currentQualification.id ? '重新提交资质' : '提交飞行资质' }}</text>
      </view>

      <!-- 证件类型选择 -->
      <view class="form-item">
        <text class="form-label">证件类型</text>
        <view class="type-row">
          <view
            class="type-chip"
            :class="{ active: form.certificateType === type }"
            v-for="type in certificateTypes"
            :key="type"
            @click="form.certificateType = type"
          >
            <text class="type-text">{{ type }}</text>
          </view>
        </view>
      </view>

      <!-- 证件号码 -->
      <view class="form-item">
        <text class="form-label">证件号码</text>
        <input
          class="form-input"
          v-model="form.certificateNo"
          placeholder="请输入证件号码"
          placeholderClass="input-placeholder"
        />
      </view>

      <!-- 有效期 -->
      <view class="date-row">
        <view class="date-col">
          <text class="form-label">有效期开始</text>
          <picker mode="date" :value="form.validStartDate" @change="onStartDateChange">
            <view class="date-picker-box">
              <text class="date-value">{{ form.validStartDate || '请选择日期' }}</text>
              <text class="date-icon">📅</text>
            </view>
          </picker>
        </view>
        <view class="date-col">
          <text class="form-label">有效期结束</text>
          <picker mode="date" :value="form.validEndDate" @change="onEndDateChange">
            <view class="date-picker-box">
              <text class="date-value">{{ form.validEndDate || '请选择日期' }}</text>
              <text class="date-icon">📅</text>
            </view>
          </picker>
        </view>
      </view>

      <!-- 证件图片上传 -->
      <view class="form-item">
        <text class="form-label">证件图片</text>
        <view class="upload-row">
          <view class="upload-box" @click="chooseImage">
            <image
              v-if="form.certificateImage"
              class="upload-preview"
              :src="resolveImageUrl(form.certificateImage)"
              mode="aspectFill"
            />
            <view v-else class="upload-placeholder">
              <text class="upload-plus">+</text>
              <text class="upload-text">点击上传</text>
            </view>
          </view>
          <view v-if="form.certificateImage" class="remove-btn" @click="clearImage">
            <text class="remove-text">清除</text>
          </view>
        </view>
        <text class="form-tip">支持 jpg/png 格式，建议保持信息清晰可见</text>
      </view>

      <!-- 提交按钮 -->
      <view class="submit-btn" :class="{ loading: submitting }" @click="handleSubmit">
        <text class="submit-text">{{ submitting ? '提交中...' : '提交审核' }}</text>
      </view>

      <text class="footer-hint">提交后管理员将在 1-2 个工作日内完成审核</text>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getQualification, submitQualification, uploadFile } from '../../api/user'
import { resolveImageUrl } from '../../utils/image'

interface FormState {
  certificateType: string
  certificateNo: string
  certificateImage: string
  validStartDate: string
  validEndDate: string
}

const certificateTypes = ['身份证', '驾驶执照', '无人机驾驶员合格证', '民航局执照']

const currentQualification = ref<any | null>(null)
const submitting = ref<boolean>(false)
const form = ref<FormState>({
  certificateType: '身份证',
  certificateNo: '',
  certificateImage: '',
  validStartDate: '',
  validEndDate: ''
})

const showForm = computed<boolean>(() => {
  const q = currentQualification.value
  // 没有资质 或 审核被拒绝时允许重新提交
  if (!q || !q.id) return true
  if (q.auditStatus === 2) return true
  return false
})

const statusIcon = computed<string>(() => {
  const s = currentQualification.value?.auditStatus
  if (s === 1) return '✓'
  if (s === 0) return '⏳'
  if (s === 2) return '✕'
  return '✈'
})

const statusTitle = computed<string>(() => {
  const s = currentQualification.value?.auditStatus
  if (s === 1) return '资质已通过审核'
  if (s === 0) return '资质审核中'
  if (s === 2) return '资质审核被拒绝'
  return '尚未提交飞行资质'
})

const statusDesc = computed<string>(() => {
  const s = currentQualification.value?.auditStatus
  if (s === 1) return '您的飞行资质已审核通过，可正常租赁无人机'
  if (s === 0) return '您的飞行资质正在审核中，通过后可进行租赁'
  if (s === 2) return '您的飞行资质未通过审核，请重新提交'
  return '提交您的飞行资质后才能租赁无人机'
})

const statusClass = computed<string>(() => {
  const s = currentQualification.value?.auditStatus
  if (s === 1) return 'approved'
  if (s === 0) return 'pending'
  if (s === 2) return 'rejected'
  return 'none'
})

const maskCertificateNo = (no: string): string => {
  if (!no || no.length < 6) return no
  return no.substring(0, 4) + '****' + no.substring(no.length - 4)
}

const onStartDateChange = (e: any) => {
  form.value.validStartDate = e.detail.value
}

const onEndDateChange = (e: any) => {
  form.value.validEndDate = e.detail.value
}

const chooseImage = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempPath: string = res.tempFilePaths && res.tempFilePaths.length > 0 ? res.tempFilePaths[0] : ''
      if (!tempPath) return
      uni.showLoading({ title: '上传中' })
      try {
        const uploadRes = await uploadFile(tempPath)
        const imageUrl: string = typeof uploadRes.data === 'string' ? uploadRes.data : (uploadRes.data?.url || uploadRes.data?.file || '')
        form.value.certificateImage = imageUrl
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (err) {
        console.error('上传失败', err)
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    fail: () => {
      uni.showToast({ title: '已取消选择', icon: 'none' })
    }
  })
}

const clearImage = () => {
  form.value.certificateImage = ''
}

const previewImage = (url: string) => {
  const full = resolveImageUrl(url)
  uni.previewImage({
    urls: [full],
    current: full
  })
}

const validate = (): boolean => {
  if (!form.value.certificateType || form.value.certificateType.trim().length === 0) {
    uni.showToast({ title: '请选择证件类型', icon: 'none' })
    return false
  }
  if (!form.value.certificateNo || form.value.certificateNo.trim().length < 5) {
    uni.showToast({ title: '请输入有效的证件号码', icon: 'none' })
    return false
  }
  if (!form.value.validStartDate || form.value.validStartDate.length === 0) {
    uni.showToast({ title: '请选择有效期开始日期', icon: 'none' })
    return false
  }
  if (!form.value.validEndDate || form.value.validEndDate.length === 0) {
    uni.showToast({ title: '请选择有效期结束日期', icon: 'none' })
    return false
  }
  if (form.value.validStartDate >= form.value.validEndDate) {
    uni.showToast({ title: '结束日期必须晚于开始日期', icon: 'none' })
    return false
  }
  if (!form.value.certificateImage || form.value.certificateImage.trim().length === 0) {
    uni.showToast({ title: '请上传证件图片', icon: 'none' })
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (submitting.value) return
  if (!validate()) return

  submitting.value = true
  try {
    const res = await submitQualification({
      certificateType: form.value.certificateType,
      certificateNo: form.value.certificateNo.trim(),
      certificateImage: form.value.certificateImage,
      validStartDate: form.value.validStartDate,
      validEndDate: form.value.validEndDate
    })
    if ((res as any).code === 200) {
      uni.showToast({ title: '提交成功，等待审核', icon: 'success' })
      await loadQualification()
    } else {
      uni.showToast({ title: (res as any).message || '提交失败', icon: 'none' })
    }
  } catch (err) {
    console.error('提交失败', err)
    uni.showToast({ title: '提交失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const loadQualification = async () => {
  try {
    const res = await getQualification()
    const data = (res.data as any) || null
    if (data && data.id) {
      currentQualification.value = data
      form.value.certificateType = data.certificateType || '身份证'
      form.value.certificateNo = data.certificateNo || ''
      form.value.certificateImage = data.certificateImage || ''
      form.value.validStartDate = data.validStartDate || ''
      form.value.validEndDate = data.validEndDate || ''
    } else {
      currentQualification.value = null
    }
  } catch (err) {
    console.error('获取资质信息失败', err)
  }
}

onShow(() => {
  loadQualification()
})

onMounted(() => {
  loadQualification()
})
</script>

<style lang="scss">
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.qual-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding: 20rpx;
  box-sizing: border-box;
}

/* 状态卡片 */
.status-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.05);
  margin-bottom: 20rpx;
}

.status-left {
  display: flex;
  align-items: center;
}

.status-icon-box {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;

  &.approved { background: linear-gradient(135deg, #10b981, #059669); }
  &.pending { background: linear-gradient(135deg, #f59e0b, #d97706); }
  &.rejected { background: linear-gradient(135deg, #ef4444, #dc2626); }
  &.none { background: linear-gradient(135deg, #64748b, #475569); }
}

.status-icon {
  font-size: 48rpx;
  color: #ffffff;
  font-weight: 700;
}

.status-text-box {
  display: flex;
  flex-direction: column;
}

.status-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8rpx;
}

.status-desc {
  font-size: 24rpx;
  color: #64748b;
  max-width: 520rpx;
  line-height: 1.5;
}

.status-badge-approved {
  background: #dcfce7;
  padding: 10rpx 20rpx;
  border-radius: 20rpx;
}
.badge-text {
  font-size: 22rpx;
  color: #16a34a;
  font-weight: 600;
}

/* 信息卡片 */
.info-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.05);
  margin-bottom: 20rpx;
}

.info-title-row {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #f1f5f9;
}

.info-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  flex: 1;
}

.info-tag-approved {
  font-size: 22rpx;
  color: #16a34a;
  background: #dcfce7;
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-weight: 500;
}

.info-tag-pending {
  font-size: 22rpx;
  color: #d97706;
  background: #fef3c7;
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-weight: 500;
}

.info-tag-rejected {
  font-size: 22rpx;
  color: #dc2626;
  background: #fee2e2;
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-weight: 500;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
}

.info-label {
  font-size: 26rpx;
  color: #64748b;
  flex-shrink: 0;
}

.info-value {
  font-size: 26rpx;
  color: #0f172a;
  font-weight: 500;
  text-align: right;
  flex: 1;
  margin-left: 20rpx;
}

.remark-text {
  color: #dc2626;
}

.image-row {
  display: flex;
  align-items: flex-start;
  padding: 16rpx 0;
}

.cert-image {
  width: 260rpx;
  height: 180rpx;
  border-radius: 16rpx;
  background: #f8fafc;
  margin-left: 20rpx;
}

/* 表单卡片 */
.form-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.05);
}

.form-title-row {
  margin-bottom: 20rpx;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #f1f5f9;
}

.form-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.form-item {
  margin-bottom: 28rpx;
}

.form-label {
  font-size: 26rpx;
  color: #334155;
  font-weight: 500;
  margin-bottom: 12rpx;
  display: block;
}

.form-input {
  width: 100%;
  height: 88rpx;
  background: #f8fafc;
  border: 2rpx solid #e2e8f0;
  border-radius: 20rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #0f172a;
  box-sizing: border-box;
}

.input-placeholder {
  color: #94a3b8;
}

/* 证件类型 */
.type-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.type-chip {
  padding: 16rpx 24rpx;
  background: #f8fafc;
  border: 2rpx solid #e2e8f0;
  border-radius: 40rpx;

  &.active {
    background: linear-gradient(135deg, #2563eb, #3b82f6);
    border-color: #2563eb;
  }
}

.type-text {
  font-size: 24rpx;
  color: #334155;

  .type-chip.active & {
    color: #ffffff;
    font-weight: 600;
  }
}

/* 日期选择 */
.date-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 28rpx;
}

.date-col {
  flex: 1;
}

.date-picker-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 88rpx;
  background: #f8fafc;
  border: 2rpx solid #e2e8f0;
  border-radius: 20rpx;
  padding: 0 24rpx;
}

.date-value {
  font-size: 28rpx;
  color: #0f172a;
}

.date-icon {
  font-size: 28rpx;
  color: #94a3b8;
}

/* 上传 */
.upload-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.upload-box {
  width: 260rpx;
  height: 180rpx;
  background: #f8fafc;
  border: 2rpx dashed #cbd5e1;
  border-radius: 20rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-preview {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.upload-plus {
  font-size: 52rpx;
  color: #94a3b8;
  line-height: 1;
}

.upload-text {
  font-size: 22rpx;
  color: #94a3b8;
}

.remove-btn {
  padding: 12rpx 24rpx;
  background: #fee2e2;
  border-radius: 16rpx;
}

.remove-text {
  font-size: 22rpx;
  color: #dc2626;
}

.form-tip {
  display: block;
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 12rpx;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 100rpx;
  background: linear-gradient(135deg, #1e3a8a, #2563eb, #3b82f6);
  border-radius: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(37, 99, 235, 0.3);
  margin-top: 16rpx;
  margin-bottom: 20rpx;

  &.loading {
    opacity: 0.7;
  }
}

.submit-text {
  font-size: 30rpx;
  color: #ffffff;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.footer-hint {
  display: block;
  text-align: center;
  font-size: 22rpx;
  color: #94a3b8;
}

.bottom-space {
  height: 60rpx;
}
</style>
