<template>
  <view class="register-page">
    <!-- 顶部大图区 -->
    <view class="hero-section">
      <view class="hero-bg"></view>
      <image class="hero-image" src="/static/drones/inspire2_drone.png" mode="aspectFit" />
      <view class="hero-content">
        <text class="hero-title">注册账号</text>
        <text class="hero-subtitle">✦ 加入我们，开启您的飞行之旅 ✦</text>
      </view>
    </view>

    <!-- 注册表单卡片 -->
    <view class="register-card">
      <view class="card-title-box">
        <text class="card-title">创建新账号</text>
        <text class="card-subtitle">只需几秒，即可畅玩无限</text>
      </view>

      <!-- 用户名 -->
      <view class="form-item">
        <view class="input-box">
          <text class="input-icon">👤</text>
          <input
            class="form-input"
            v-model="username"
            placeholder="请输入用户名"
            placeholderClass="input-placeholder"
            :confirm-type="'next'"
          />
        </view>
      </view>

      <!-- 密码 -->
      <view class="form-item">
        <view class="input-box">
          <text class="input-icon">🔒</text>
          <input
            class="form-input"
            v-model="password"
            placeholder="请输入密码"
            placeholderClass="input-placeholder"
            :password="true"
            :confirm-type="'next'"
          />
        </view>
      </view>

      <!-- 确认密码 -->
      <view class="form-item">
        <view class="input-box">
          <text class="input-icon">🔐</text>
          <input
            class="form-input"
            v-model="confirmPassword"
            placeholder="请再次输入密码"
            placeholderClass="input-placeholder"
            :password="true"
            :confirm-type="'next'"
          />
        </view>
      </view>

      <!-- 手机号 -->
      <view class="form-item">
        <view class="input-box">
          <text class="input-icon">📱</text>
          <input
            class="form-input"
            v-model="phone"
            placeholder="请输入手机号"
            placeholderClass="input-placeholder"
            :type="'number'"
            :confirm-type="'done'"
          />
        </view>
      </view>

      <!-- 邮箱（选填） -->
      <view class="form-item">
        <view class="input-box">
          <text class="input-icon">📧</text>
          <input
            class="form-input"
            v-model="email"
            placeholder="请输入邮箱（选填）"
            placeholderClass="input-placeholder"
          />
        </view>
      </view>

      <!-- 注册按钮 -->
      <view class="submit-btn" :class="{ loading: loading }" @click="handleRegister">
        <text class="submit-icon">✈</text>
        <text class="submit-text">{{ loading ? '注册中...' : '立即注册' }}</text>
      </view>

      <!-- 分隔线 -->
      <view class="divider-box">
        <view class="divider-line"></view>
        <text class="divider-text">或</text>
        <view class="divider-line"></view>
      </view>

      <!-- 登录链接 -->
      <view class="login-link-box">
        <text class="link-ask">已有账号？</text>
        <text class="link-btn" @click="goToLogin">立即登录 →</text>
      </view>
    </view>

    <!-- 底部装饰 -->
    <view class="footer-decor">
      <text class="footer-text">探索 · 航拍 · 无限可能</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { register } from '../../api/auth'

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const phone = ref('')
const email = ref('')
const loading = ref(false)

const handleRegister = async () => {
  if (!username.value.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!password.value.trim()) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  if (!phone.value.trim()) {
    uni.showToast({ title: '请输入手机号', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await register({
      username: username.value,
      password: password.value,
      phone: phone.value,
      email: email.value || undefined
    })

    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } catch (err) {
    console.error('Register failed:', err)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  uni.navigateBack()
}
</script>

<style lang="scss">
page {
  min-height: 100vh;
  background: #ffffff;
}

.register-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background: #ffffff;
}

/* ===== 顶部大图区 ===== */
.hero-section {
  position: relative;
  height: 460rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, #312e81 0%, #4f46e5 40%, #6366f1 70%, #818cf8 100%);
}

.hero-image {
  position: relative;
  width: 340rpx;
  height: 260rpx;
  margin-top: 70rpx;
  z-index: 1;
  animation: floatPlane 3s ease-in-out infinite;
}

@keyframes floatPlane {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20rpx); }
}

.hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 10rpx;
}

.hero-title {
  font-size: 42rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 4rpx;
  text-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.25);
}

.hero-subtitle {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 12rpx;
  letter-spacing: 2rpx;
}

/* ===== 注册卡片 ===== */
.register-card {
  flex: 1;
  background: #ffffff;
  margin: -60rpx 40rpx 0;
  padding: 48rpx 40rpx 40rpx;
  border-radius: 40rpx 40rpx 24rpx 24rpx;
  box-shadow: 0 -20rpx 60rpx rgba(79, 70, 229, 0.15),
              0 20rpx 60rpx rgba(99, 102, 241, 0.08);
  position: relative;
  z-index: 2;
}

.card-title-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 40rpx;
}

.card-title {
  font-size: 38rpx;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 2rpx;
}

.card-subtitle {
  font-size: 24rpx;
  color: #64748b;
  margin-top: 8rpx;
}

.form-item {
  margin-bottom: 24rpx;
}

.input-box {
  display: flex;
  align-items: center;
  height: 96rpx;
  background: #f8fafc;
  border: 2rpx solid #e2e8f0;
  border-radius: 24rpx;
  padding: 0 28rpx;
  transition: all 0.2s;
}

.input-icon {
  font-size: 30rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  height: 96rpx;
  font-size: 28rpx;
  color: #0f172a;
  background: transparent;
}

.input-placeholder {
  color: #94a3b8;
}

/* ===== 注册按钮 ===== */
.submit-btn {
  width: 100%;
  height: 104rpx;
  background: linear-gradient(135deg, #312e81 0%, #4f46e5 40%, #6366f1 100%);
  border-radius: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 36rpx rgba(99, 102, 241, 0.35);
  margin-top: 36rpx;
  margin-bottom: 32rpx;
  transition: transform 0.2s, box-shadow 0.2s;
}

.submit-btn.loading {
  opacity: 0.7;
}

.submit-icon {
  font-size: 36rpx;
  color: #ffffff;
  margin-right: 12rpx;
}

.submit-text {
  font-size: 32rpx;
  color: #ffffff;
  font-weight: 600;
  letter-spacing: 2rpx;
}

/* ===== 分隔线 ===== */
.divider-box {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}

.divider-line {
  flex: 1;
  height: 1rpx;
  background: #e2e8f0;
}

.divider-text {
  font-size: 24rpx;
  color: #94a3b8;
  padding: 0 20rpx;
}

/* ===== 登录链接 ===== */
.login-link-box {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16rpx 0;
}

.link-ask {
  font-size: 26rpx;
  color: #64748b;
}

.link-btn {
  font-size: 26rpx;
  color: #4f46e5;
  font-weight: 600;
  margin-left: 12rpx;
}

/* ===== 底部装饰 ===== */
.footer-decor {
  padding: 20rpx 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.footer-text {
  font-size: 22rpx;
  color: #94a3b8;
  letter-spacing: 4rpx;
}
</style>
