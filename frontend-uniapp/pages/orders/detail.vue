<template>
  <view class="order-detail-page">
    <view class="detail-section">
      <view class="section-header">
        <text class="section-title">订单信息</text>
        <text class="order-status" :class="getStatusClass(order?.orderStatus)">{{ order?.orderStatusDesc }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">订单号</text>
        <text class="info-value">{{ order?.orderNo }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">创建时间</text>
        <text class="info-value">{{ formatDateTime(order?.createdTime) }}</text>
      </view>
    </view>

    <view class="detail-section">
      <view class="section-header">
        <text class="section-title">设备信息</text>
      </view>
      <view class="drone-info">
        <image class="drone-image" :src="order?.droneImage" mode="aspectFill" />
        <view class="drone-detail">
          <text class="drone-name">{{ order?.droneModel }}</text>
          <text class="drone-time">租赁时间: {{ formatDate(order?.rentalStartTime) }} - {{ formatDate(order?.rentalEndTime) }}</text>
          <text class="drone-days">租赁天数: {{ order?.rentalDays }}天</text>
        </view>
      </view>
    </view>

    <view class="detail-section">
      <view class="section-header">
        <text class="section-title">费用明细</text>
      </view>
      <view class="fee-row">
        <text class="fee-label">单价</text>
        <text class="fee-value">¥{{ order?.unitPrice }}/天</text>
      </view>
      <view class="fee-row">
        <text class="fee-label">租赁费用</text>
        <text class="fee-value">¥{{ order?.totalAmount }}</text>
      </view>
      <view class="fee-row" v-if="order?.depositAmount > 0">
        <text class="fee-label">押金</text>
        <text class="fee-value">¥{{ order?.depositAmount }}</text>
      </view>
      <view class="fee-row total">
        <text class="fee-label">合计</text>
        <text class="fee-value total">¥{{ order?.totalAmount + (order?.depositAmount || 0) }}</text>
      </view>
    </view>

    <view class="detail-section" v-if="order?.deliveryAddress">
      <view class="section-header">
        <text class="section-title">配送信息</text>
      </view>
      <view class="info-row">
        <text class="info-label">配送地址</text>
        <text class="info-value">{{ order?.deliveryAddress }}</text>
      </view>
      <view class="info-row" v-if="order?.remark">
        <text class="info-label">备注</text>
        <text class="info-value">{{ order?.remark }}</text>
      </view>
    </view>

    <view class="detail-section" v-if="order?.regionName">
      <view class="section-header">
        <text class="section-title">空域信息</text>
      </view>
      <view class="info-row">
        <text class="info-label">飞行区域</text>
        <text class="info-value">{{ order?.regionName }}</text>
      </view>
    </view>

    <view class="detail-section" v-if="order?.cancelReason || order?.refundReason">
      <view class="section-header">
        <text class="section-title">取消/退款原因</text>
      </view>
      <view class="info-row" v-if="order?.cancelReason">
        <text class="info-label">取消原因</text>
        <text class="info-value">{{ order?.cancelReason }}</text>
      </view>
      <view class="info-row" v-if="order?.refundReason">
        <text class="info-label">退款原因</text>
        <text class="info-value">{{ order?.refundReason }}</text>
      </view>
    </view>

    <view class="timeline-section">
      <text class="section-title">订单进度</text>
      <view class="timeline">
        <view class="timeline-item" :class="{ active: order?.createdTime }">
          <view class="timeline-dot"></view>
          <text class="timeline-text">创建订单</text>
          <text class="timeline-time" v-if="order?.createdTime">{{ formatDateTime(order?.createdTime) }}</text>
        </view>
        <view class="timeline-item" :class="{ active: order?.payTime }">
          <view class="timeline-dot"></view>
          <text class="timeline-text">支付成功</text>
          <text class="timeline-time" v-if="order?.payTime">{{ formatDateTime(order?.payTime) }}</text>
        </view>
        <view class="timeline-item" :class="{ active: order?.shipTime }">
          <view class="timeline-dot"></view>
          <text class="timeline-text">已发货</text>
          <text class="timeline-time" v-if="order?.shipTime">{{ formatDateTime(order?.shipTime) }}</text>
        </view>
        <view class="timeline-item" :class="{ active: order?.receiveTime }">
          <view class="timeline-dot"></view>
          <text class="timeline-text">已收货</text>
          <text class="timeline-time" v-if="order?.receiveTime">{{ formatDateTime(order?.receiveTime) }}</text>
        </view>
        <view class="timeline-item" :class="{ active: order?.returnTime }">
          <view class="timeline-dot"></view>
          <text class="timeline-text">已归还</text>
          <text class="timeline-time" v-if="order?.returnTime">{{ formatDateTime(order?.returnTime) }}</text>
        </view>
      </view>
    </view>

    <view class="action-bar">
      <button class="action-btn primary" v-if="order?.orderStatus === 0" @click="goToPay">立即支付</button>
      <button class="action-btn" v-if="order?.orderStatus === 0 || order?.orderStatus === 1 || order?.orderStatus === 2" @click="handleCancel">取消订单</button>
      <button class="action-btn primary" v-if="order?.orderStatus === 2" @click="handleReceive">确认收货</button>
      <button class="action-btn primary" v-if="order?.orderStatus === 3" @click="handleReturn">申请退租</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderDetail, cancelOrder, receiveOrder, returnOrder } from '../../api/order'
import type { OrderDetail } from '../../api/order'

const orderId = ref(0)
const order = ref<OrderDetail | null>(null)

const formatDate = (dateStr: string | undefined) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`
}

const formatDateTime = (dateStr: string | undefined) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`
}

const getStatusClass = (status: number | undefined) => {
  switch (status) {
    case 0: return 'warning'
    case 3: return 'success'
    case 4: return 'info'
    case 5: return 'error'
    default: return ''
  }
}

const loadOrderDetail = async () => {
  try {
    const res = await getOrderDetail(orderId.value)
    order.value = res.data
  } catch (err) {
    console.error('Load order detail failed:', err)
  }
}

const goToPay = () => {
  uni.navigateTo({ url: `/pages/orders/pay?orderId=${orderId.value}` })
}

const handleCancel = () => {
  uni.showModal({
    title: '提示',
    content: '确定要取消订单吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(orderId.value, { reason: '用户取消' })
          uni.showToast({ title: '订单已取消', icon: 'success' })
          loadOrderDetail()
        } catch (err) {
          console.error('Cancel order failed:', err)
        }
      }
    }
  })
}

const handleReceive = () => {
  uni.showModal({
    title: '提示',
    content: '确定要确认收货吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await receiveOrder(orderId.value)
          uni.showToast({ title: '已确认收货', icon: 'success' })
          loadOrderDetail()
        } catch (err) {
          console.error('Receive order failed:', err)
        }
      }
    }
  })
}

const handleReturn = () => {
  uni.showModal({
    title: '提示',
    content: '确定要申请退租吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await returnOrder(orderId.value, { returnAddress: '归还地址' })
          uni.showToast({ title: '已申请退租', icon: 'success' })
          loadOrderDetail()
        } catch (err) {
          console.error('Return order failed:', err)
        }
      }
    }
  })
}

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  orderId.value = parseInt(currentPage.options?.id || '0')
  
  if (orderId.value > 0) {
    loadOrderDetail()
  }
})
</script>

<style lang="scss">
.order-detail-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding-bottom: 140rpx;
}

.detail-section {
  background: #ffffff;
  margin-bottom: 24rpx;
  padding: 32rpx;
  border-radius: 28rpx;
  margin-left: 24rpx;
  margin-right: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.order-status {
  font-size: 28rpx;
  font-weight: 600;
  
  &.warning {
    color: #f59e0b;
  }
  &.success {
    color: #22c55e;
  }
  &.info {
    color: #3b82f6;
  }
  &.error {
    color: #ef4444;
  }
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 28rpx;
  color: #64748b;
}

.info-value {
  font-size: 28rpx;
  color: #0f172a;
  font-weight: 500;
}

.drone-info {
  display: flex;
}

.drone-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 20rpx;
  margin-right: 24rpx;
}

.drone-detail {
  flex: 1;
}

.drone-name {
  font-size: 32rpx;
  color: #0f172a;
  font-weight: 600;
  margin-bottom: 16rpx;
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

.fee-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.fee-row.total {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f1f5f9;
}

.fee-label {
  font-size: 28rpx;
  color: #64748b;
}

.fee-value {
  font-size: 28rpx;
  color: #0f172a;
}

.fee-value.total {
  font-size: 36rpx;
  color: #ef4444;
  font-weight: 700;
}

.timeline-section {
  background: #ffffff;
  padding: 32rpx;
  border-radius: 28rpx;
  margin-left: 24rpx;
  margin-right: 24rpx;
}

.timeline {
  padding-top: 20rpx;
}

.timeline-item {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
  opacity: 0.4;
  
  &:last-child {
    margin-bottom: 0;
  }
  
  &.active {
    opacity: 1;
  }
}

.timeline-dot {
  width: 24rpx;
  height: 24rpx;
  background: #cbd5e1;
  border-radius: 12rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.timeline-item.active .timeline-dot {
  background: #3b82f6;
}

.timeline-text {
  font-size: 28rpx;
  color: #64748b;
  flex: 1;
}

.timeline-item.active .timeline-text {
  color: #0f172a;
  font-weight: 500;
}

.timeline-time {
  font-size: 24rpx;
  color: #94a3b8;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24rpx 32rpx;
  background: #ffffff;
  border-top: 1rpx solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
}

.action-btn {
  margin-left: 20rpx;
  padding: 20rpx 40rpx;
  font-size: 30rpx;
  border-radius: 32rpx;
  background: #f1f5f9;
  color: #475569;
  font-weight: 500;
  border: none;
  
  &.primary {
    background: #3b82f6;
    color: #ffffff;
  }
}
</style>