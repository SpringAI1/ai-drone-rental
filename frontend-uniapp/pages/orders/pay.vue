<template>
  <view class="pay-page">
    <view class="pay-header">
      <text class="pay-title">订单支付</text>
    </view>

    <scroll-view scroll-y class="pay-scroll">
      <view class="order-info-section">
        <view class="drone-card">
          <image class="drone-image" :src="order?.droneImage" mode="aspectFill" />
          <view class="drone-info">
            <text class="drone-name">{{ order?.droneModel }}</text>
            <text class="drone-time">{{ formatDate(order?.rentalStartTime) }} - {{ formatDate(order?.rentalEndTime) }}</text>
            <text class="drone-days">租赁{{ order?.rentalDays }}天</text>
          </view>
        </view>
      </view>

      <view class="amount-section">
        <text class="amount-label">支付金额</text>
        <view class="amount-main">
          <text class="amount-symbol">¥</text>
          <text class="amount-value">{{ order?.totalAmount }}</text>
        </view>
      </view>

      <view class="payment-section">
        <text class="section-title">选择支付方式</text>
        <view class="payment-options">
          <view class="payment-item" :class="{ active: paymentMethod === 'balance' }" @click="selectPayment('balance')">
            <view class="payment-icon-box balance-box">
              <text class="payment-icon-text">¥</text>
            </view>
            <view class="payment-info">
              <text class="payment-name">余额支付</text>
              <text class="payment-desc">当前余额: ¥{{ userInfo?.balance || 0 }}</text>
            </view>
            <view class="payment-check" v-if="paymentMethod === 'balance'">✓</view>
          </view>
          <view class="payment-item" :class="{ active: paymentMethod === 'wechat' }" @click="selectPayment('wechat')">
            <view class="payment-icon-box wechat-box">
              <text class="payment-icon-text">W</text>
            </view>
            <view class="payment-info">
              <text class="payment-name">微信支付</text>
              <text class="payment-desc">使用微信支付</text>
            </view>
            <view class="payment-check" v-if="paymentMethod === 'wechat'">✓</view>
          </view>
          <view class="payment-item" :class="{ active: paymentMethod === 'alipay' }" @click="selectPayment('alipay')">
            <view class="payment-icon-box alipay-box">
              <text class="payment-icon-text">A</text>
            </view>
            <view class="payment-info">
              <text class="payment-name">支付宝</text>
              <text class="payment-desc">使用支付宝支付</text>
            </view>
            <view class="payment-check" v-if="paymentMethod === 'alipay'">✓</view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="pay-bar">
      <view class="pay-info">
        <text class="pay-label">应付金额:</text>
        <text class="pay-amount">¥{{ order?.totalAmount }}</text>
      </view>
      <button class="pay-btn" @click="handlePay" :loading="loading">
        立即支付
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderDetail, payOrder } from '../../api/order'
import { getUserInfo } from '../../api/user'
import type { OrderDetail } from '../../api/order'
import type { UserInfo } from '../../api/user'

const orderId = ref(0)
const droneId = ref(0)
const order = ref<OrderDetail | null>(null)
const userInfo = ref<UserInfo | null>(null)
const paymentMethod = ref('balance')
const loading = ref(false)

const formatDate = (dateStr: string | undefined) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const loadOrderDetail = async () => {
  try {
    const res = await getOrderDetail(orderId.value)
    order.value = res.data
  } catch (err) {
    console.error('Load order detail failed:', err)
  }
}

const loadUserInfo = async () => {
  try {
    const res = await getUserInfo()
    userInfo.value = res.data
  } catch (err) {
    console.error('Load user info failed:', err)
  }
}

const selectPayment = (method: string) => {
  paymentMethod.value = method
}

const handlePay = async () => {
  if (!order.value) return

  if (paymentMethod.value === 'balance') {
    if (!userInfo.value || Number(userInfo.value.balance || 0) < Number(order.value.totalAmount)) {
      uni.showToast({ title: '余额不足，请更换支付方式', icon: 'none' })
      return
    }
  }

  loading.value = true
  try {
    // 映射支付方式: 1-余额, 2-微信, 3-支付宝
    const methodNum = paymentMethod.value === 'balance' ? 1 : paymentMethod.value === 'wechat' ? 2 : 3
    await payOrder(orderId.value, methodNum, order.value.deliveryAddress || '')
    uni.showToast({ title: '支付成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/orders/detail?id=${orderId.value}` })
    }, 1500)
  } catch (err) {
    console.error('Pay order failed:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const opts = (currentPage as any)?.options || {}
  // 兼容不同跳转：可能是 id 也可能是 orderId
  orderId.value = Number(opts.orderId || opts.id || '0')
  droneId.value = Number(opts.droneId || '0')

  if (orderId.value > 0) {
    loadOrderDetail()
    loadUserInfo()
  }
})
</script>

<style lang="scss">
.pay-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f1f5f9;
}

.pay-header {
  background: #ffffff;
  padding: 32rpx;
  text-align: center;
  flex-shrink: 0;
}

.pay-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #0f172a;
}

.pay-scroll {
  flex: 1;
  height: 0;
  padding: 20rpx;
  box-sizing: border-box;
}

.order-info-section {
  background: #ffffff;
  padding: 32rpx;
  margin: 4rpx 4rpx 24rpx;
  border-radius: 28rpx;
}

.drone-card {
  display: flex;
  padding: 24rpx;
  background: #f8fafc;
  border-radius: 24rpx;
}

.drone-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 20rpx;
  margin-right: 24rpx;
}

.drone-info {
  flex: 1;
}

.drone-name {
  font-size: 32rpx;
  color: #0f172a;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.drone-time {
  font-size: 26rpx;
  color: #64748b;
  margin-bottom: 8rpx;
}

.drone-days {
  font-size: 26rpx;
  color: #94a3b8;
}

.amount-section {
  background: #ffffff;
  padding: 48rpx;
  margin: 4rpx 4rpx 24rpx;
  border-radius: 28rpx;
  text-align: center;
}

.amount-label {
  font-size: 28rpx;
  color: #64748b;
  margin-bottom: 20rpx;
}

.amount-main {
  display: flex;
  align-items: baseline;
  justify-content: center;
}

.amount-symbol {
  font-size: 44rpx;
  color: #ef4444;
  font-weight: 600;
}

.amount-value {
  font-size: 80rpx;
  color: #ef4444;
  font-weight: 700;
}

.payment-section {
  background: #ffffff;
  padding: 32rpx;
  margin: 4rpx 4rpx 20rpx;
  border-radius: 28rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 24rpx;
}

.payment-options {
  padding: 0 8rpx;
}

.payment-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: #f8fafc;
  border-radius: 20rpx;
  margin-bottom: 16rpx;
  border: 3rpx solid transparent;
  
  &.active {
    border-color: #3b82f6;
    background: rgba(59, 130, 246, 0.08);
  }
}

.payment-icon {
  font-size: 44rpx;
  margin-right: 24rpx;
}

.payment-icon-box {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.payment-icon-box.balance-box {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.payment-icon-box.wechat-box {
  background: linear-gradient(135deg, #10b981 0%, #047857 100%);
}

.payment-icon-box.alipay-box {
  background: linear-gradient(135deg, #06b6d4 0%, #0e7490 100%);
}

.payment-icon-text {
  font-size: 32rpx;
  color: #ffffff;
  font-weight: 700;
}

.payment-info {
  flex: 1;
}

.payment-name {
  font-size: 30rpx;
  color: #0f172a;
  margin-bottom: 8rpx;
}

.payment-desc {
  font-size: 26rpx;
  color: #94a3b8;
}

.payment-check {
  width: 44rpx;
  height: 44rpx;
  background: #3b82f6;
  border-radius: 22rpx;
  color: #ffffff;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pay-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx;
  padding-bottom: calc(20rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  box-shadow: 0 -4rpx 16rpx rgba(15, 23, 42, 0.06);
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.pay-info {
  flex: 1;
  display: flex;
  align-items: center;
}

.pay-label {
  font-size: 28rpx;
  color: #64748b;
  margin-right: 12rpx;
}

.pay-amount {
  font-size: 44rpx;
  color: #ef4444;
  font-weight: 700;
}

.pay-btn {
  width: 240rpx;
  height: 96rpx;
  background: linear-gradient(135deg, #3b82f6 0%, #0ea5e9 100%);
  border-radius: 48rpx;
  font-size: 32rpx;
  color: #ffffff;
  font-weight: 600;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(59, 130, 246, 0.3);
}
</style>