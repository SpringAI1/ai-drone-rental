<template>
  <view class="edit-page">
    <!-- 头像卡片 -->
    <view class="avatar-card">
      <text class="card-label">头像</text>
      <view class="avatar-edit-row" @click="handleChooseAvatar">
        <view class="avatar-box">
          <image
            v-if="avatarLocalUrl"
            class="avatar-img"
            :src="avatarLocalUrl"
            mode="aspectFill"
          />
          <text v-else class="avatar-text">{{ avatarText }}</text>
        </view>
        <view class="edit-action">
          <text class="edit-text">{{ uploading ? '上传中...' : '点击修改' }}</text>
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 基本信息 -->
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">用户名</text>
        <view class="form-value-wrap">
          <text class="form-value-readonly">{{ form.username || '未设置' }}</text>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">昵称</text>
        <view class="form-value-wrap">
          <input
            class="form-input"
            v-model="form.nickname"
            placeholder="请输入昵称"
            placeholder-class="placeholder"
            maxlength="20"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">手机号</text>
        <view class="form-value-wrap">
          <input
            class="form-input"
            v-model="form.phone"
            placeholder="不修改请留空（11位）"
            placeholder-class="placeholder"
            type="number"
            maxlength="11"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">邮箱</text>
        <view class="form-value-wrap">
          <input
            class="form-input"
            v-model="form.email"
            placeholder="请输入邮箱（选填）"
            placeholder-class="placeholder"
            maxlength="50"
          />
        </view>
      </view>
    </view>

    <!-- 地址卡片 -->
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">常用地址</text>
        <view class="address-picker-row" @click="openAddressPicker">
          <text class="picker-text" :class="{ placeholder: !form.address }">
            {{ form.address || '点击选择常用地址' }}
          </text>
          <text class="picker-arrow">›</text>
        </view>
        <view class="quick-address-row">
          <view
            class="quick-address-chip"
            v-for="(addr, idx) in quickAddressList"
            :key="idx"
            @click="form.address = addr"
          >
            <text class="chip-text">{{ addr }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-box">
      <view class="submit-btn" :class="{ disabled: submitting }" @click="handleSave">
        <text class="submit-text">{{ submitting ? '保存中...' : '保存修改' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getUserInfo, updateUserInfo, uploadFile } from '../../api/user'
import { useAuth } from '../../stores/auth'
import { resolveAvatarUrl } from '../../utils/image'

interface UserForm {
  username: string
  nickname: string
  phone: string
  email: string
  avatar: string
  address: string
}

const auth = useAuth()

const form = ref<UserForm>({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  address: ''
})

const quickAddressList = ref<string[]>([
  '北京市朝阳区望京街道',
  '上海市浦东新区陆家嘴',
  '广州市天河区珠江新城',
  '深圳市南山区科技园',
  '杭州市西湖区文三路',
  '成都市高新区天府大道'
])

const avatarLocalUrl = ref<string>('')
const uploading = ref<boolean>(false)
const submitting = ref<boolean>(false)

const avatarText = computed<string>(() => {
  const nick = form.value.nickname
  if (nick && nick.length > 0) return nick.charAt(0)
  const user = form.value.username
  if (user && user.length > 0) return user.charAt(0)
  return '用'
})

const loadUserInfo = async () => {
  try {
    const res = await getUserInfo()
    if (res.data) {
      const u = res.data as any
      form.value.username = u.username || ''
      form.value.nickname = u.nickname || ''
      form.value.phone = u.phone || ''
      form.value.email = u.email || ''
      form.value.avatar = u.avatar || ''
      form.value.address = u.address || ''
      avatarLocalUrl.value = resolveAvatarUrl(u.avatar)
    }
  } catch (err) {
    console.error('获取用户信息失败', err)
    const cached = auth.userInfo.value
    if (cached) {
      form.value.username = cached.username || ''
      form.value.nickname = cached.nickname || ''
      form.value.phone = cached.phone || ''
      form.value.email = cached.email || ''
      form.value.avatar = cached.avatar || ''
      form.value.address = cached.address || ''
      avatarLocalUrl.value = resolveAvatarUrl(cached.avatar)
    }
  }
}

const handleChooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const tempFile = res.tempFilePaths[0]
      if (!tempFile) return
      uploadAvatar(tempFile)
    }
  })
}

const uploadAvatar = async (filePath: string) => {
  uploading.value = true
  try {
    const res = await uploadFile(filePath, 'avatar')
    const url = (res.data as any)?.url || (res.data as any) || ''
    if (url) {
      form.value.avatar = url
      avatarLocalUrl.value = resolveAvatarUrl(url)
      uni.showToast({ title: '头像上传成功', icon: 'success' })
    } else {
      uni.showToast({ title: '上传失败,请重试', icon: 'none' })
    }
  } catch (err) {
    console.error('头像上传失败', err)
    uni.showToast({ title: '上传失败', icon: 'none' })
  } finally {
    uploading.value = false
  }
}

const openAddressPicker = () => {
  uni.showActionSheet({
    itemList: quickAddressList.value,
    success: (res) => {
      form.value.address = quickAddressList.value[res.tapIndex]
    }
  })
}

const handleSave = async () => {
  // 手机号校验：如果填写了必须是 11 位数字，以 1 开头
  if (form.value.phone && form.value.phone.trim().length > 0) {
    const phone = form.value.phone.trim()
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      uni.showToast({ title: '请输入有效的 11 位手机号', icon: 'none' })
      return
    }
  }

  submitting.value = true
  try {
    // 构造提交参数 - phone 为空时不传
    const params: any = {
      nickname: form.value.nickname || '',
      email: form.value.email || '',
      avatar: form.value.avatar || '',
      address: form.value.address || ''
    }
    if (form.value.phone && form.value.phone.trim().length === 11) {
      params.phone = form.value.phone.trim()
    }

    await updateUserInfo(params)
    auth.updateUserInfo({
      nickname: params.nickname,
      phone: params.phone || '',
      email: params.email,
      avatar: params.avatar,
      address: params.address
    } as any)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } catch (err) {
    console.error('保存失败', err)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style lang="scss">
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.edit-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding: 20rpx 20rpx 40rpx;
  box-sizing: border-box;
}

/* 头像卡片 */
.avatar-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx 28rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
}

.card-label {
  display: block;
  font-size: 24rpx;
  color: #94a3b8;
  margin-bottom: 16rpx;
}

.avatar-edit-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.avatar-box {
  width: 128rpx;
  height: 128rpx;
  background: #f1f5f9;
  border-radius: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 64rpx;
}

.avatar-text {
  font-size: 52rpx;
  color: #2563eb;
  font-weight: 700;
}

.edit-action {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.edit-text {
  font-size: 26rpx;
  color: #2563eb;
  margin-right: 8rpx;
}

.arrow {
  font-size: 28rpx;
  color: #cbd5e1;
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
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f1f5f9;

  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  font-size: 26rpx;
  color: #475569;
  font-weight: 500;
  margin-bottom: 12rpx;
}

.form-value-wrap {
  display: flex;
  flex-direction: row;
  align-items: center;
  background: #f8fafc;
  border-radius: 12rpx;
  padding: 14rpx 16rpx;
  box-sizing: border-box;
}

.form-input {
  flex: 1;
  font-size: 28rpx;
  color: #0f172a;
  background: transparent;
  height: 44rpx;
}

.placeholder {
  color: #cbd5e1;
}

.form-value-readonly {
  flex: 1;
  font-size: 28rpx;
  color: #94a3b8;
}

/* 地址选择器 */
.address-picker-row {
  background: #f8fafc;
  border-radius: 12rpx;
  padding: 20rpx 16rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.picker-text {
  font-size: 28rpx;
  color: #0f172a;

  &.placeholder {
    color: #94a3b8;
  }
}

.picker-arrow {
  font-size: 32rpx;
  color: #94a3b8;
}

.quick-address-row {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  margin-top: 16rpx;
}

.quick-address-chip {
  background: #eff6ff;
  border: 2rpx solid #dbeafe;
  border-radius: 999rpx;
  padding: 10rpx 20rpx;
  margin-right: 12rpx;
  margin-bottom: 12rpx;
}

.chip-text {
  font-size: 22rpx;
  color: #2563eb;
}

/* 提交 */
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
