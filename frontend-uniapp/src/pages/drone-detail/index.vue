<template>
  <view class="detail-page">
    <!-- 顶部大图（整行） -->
    <view class="detail-hero">
      <image class="detail-hero-img" :src="drone.image" mode="aspectFit" />
    </view>

    <!-- 标题卡片 -->
    <view class="detail-card">
      <view class="detail-title-row">
        <text class="detail-title">{{ drone.name }}</text>
      </view>
      <view class="detail-meta-row">
        <text class="detail-meta">{{ drone.spec }}</text>
        <text class="detail-meta-dot">·</text>
        <text class="detail-meta">{{ drone.type }}</text>
        <text class="detail-meta-dot">·</text>
        <text class="detail-meta">库存 {{ drone.stock }} 台</text>
      </view>
      <view class="detail-price-row">
        <text class="detail-price">¥{{ drone.price }}<text class="price-unit">/天</text></text>
        <text class="detail-count" v-if="drone.rentalCount > 0">已出租 {{ drone.rentalCount }} 次</text>
      </view>
    </view>

    <!-- 参数规格 -->
    <view class="detail-card">
      <view class="card-header">
        <text class="card-header-title">设备参数</text>
      </view>
      <view class="spec-row">
        <view class="spec-item" v-for="spec in specList" :key="spec.label">
          <text class="spec-value">{{ spec.value }}</text>
          <text class="spec-label">{{ spec.label }}</text>
        </view>
      </view>
    </view>

    <!-- 描述亮点 -->
    <view class="detail-card" v-if="drone.description && drone.description.length > 0">
      <view class="card-header">
        <text class="card-header-title">设备描述</text>
      </view>
      <text class="description-text">{{ drone.description }}</text>
    </view>

    <!-- 租期选择卡片 -->
    <view class="detail-card">
      <view class="card-header">
        <text class="card-header-title">租期选择</text>
      </view>
      <view class="rental-options">
        <view
          class="rental-option"
          v-for="(opt, index) in rentalOptions"
          :key="opt.days"
          :class="{ active: selectedRental === index }"
          @click="selectRental(index)"
        >
          <text class="rental-days">{{ opt.days }}天</text>
          <text class="rental-price">¥{{ totalPrice }}</text>
        </view>
      </view>
      <view class="rental-summary">
        <view class="summary-row">
          <text class="summary-label">日租金</text>
          <text class="summary-value">¥{{ drone.price }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">租期</text>
          <text class="summary-value">{{ rentalOptions[selectedRental].days }} 天</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">总租金</text>
          <text class="summary-value highlight">¥{{ totalPrice }}</text>
        </view>
      </view>
    </view>

    <view class="bottom-space"></view>

    <!-- 底部操作栏（整行横排） -->
    <view class="action-bar">
      <view class="action-secondary" @click="handleChat">
        <text class="action-icon">🤖</text>
        <text class="action-text">咨询</text>
      </view>
      <view class="action-primary" @click="handleRent">
        <text class="action-primary-text">立即租赁 · ¥{{ totalPrice }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDroneDetail } from '../../api/drone'
import { createOrder } from '../../api/order'
import { getQualification } from '../../api/user'
import { resolveImageUrl } from '../../utils/image'

interface SpecItem {
  label: string
  value: string
}

interface RentalOption {
  days: number
}

interface DroneDetailData {
  id: number
  name: string
  spec: string
  type: string
  price: number
  image: string
  stock: number
  rentalCount: number
  rating: number
  description: string
}

const drone = ref<DroneDetailData>({
  id: 0,
  name: '加载中...',
  spec: '',
  type: '',
  price: 0,
  image: resolveImageUrl('/uploads/mavic3_drone.png'),
  stock: 1,
  rentalCount: 0,
  rating: 0,
  description: ''
})

const specList = ref<SpecItem[]>([
  { label: '续航(分)', value: '-' },
  { label: '载重(kg)', value: '-' },
  { label: '速度(km/h)', value: '-' },
  { label: '航程(km)', value: '-' }
])

const rentalOptions = ref<RentalOption[]>([
  { days: 1 },
  { days: 3 },
  { days: 7 },
  { days: 15 },
  { days: 30 }
])

const selectedRental = ref<number>(1)

const totalPrice = computed<number>(() => {
  return rentalOptions.value[selectedRental.value].days * drone.value.price
})

const selectRental = (index: number) => {
  selectedRental.value = index
}

const handleChat = () => {
  uni.navigateTo({ url: '/pages/chat/index' })
}

const handleRent = async () => {
  // 1. 先校验飞行资质
  try {
    const qualRes = await getQualification()
    const qual = (qualRes.data as any) || null
    if (!qual || !qual.id) {
      uni.showModal({
        title: '需要飞行资质',
        content: '您尚未提交飞行资质，请先完成飞行资质提交',
        confirmText: '去提交',
        cancelText: '稍后再说',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({ url: '/pages/qualification/index' })
          }
        }
      })
      return
    }
    const status: number = Number(qual.auditStatus)
    if (status === 0) {
      uni.showModal({
        title: '资质审核中',
        content: '您的飞行资质正在审核中，审核通过后即可租赁无人机',
        confirmText: '查看状态',
        cancelText: '知道了',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({ url: '/pages/qualification/index' })
          }
        }
      })
      return
    }
    if (status === 2) {
      uni.showModal({
        title: '资质未通过',
        content: '您的飞行资质未通过审核，请重新提交',
        confirmText: '去提交',
        cancelText: '知道了',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({ url: '/pages/qualification/index' })
          }
        }
      })
      return
    }
  } catch (err) {
    console.error('校验飞行资质失败', err)
    uni.showToast({ title: '资质校验失败，请重试', icon: 'none' })
    return
  }

  // 2. 正常租赁流程
  const days = rentalOptions.value[selectedRental.value].days
  const now = new Date()
  const end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000)

  const padZ = (n: number): string => (n < 10 ? '0' + n : '' + n)
  const startDate: string = now.getFullYear() + '-' + padZ(now.getMonth() + 1) + '-' + padZ(now.getDate())
  const endDate: string = end.getFullYear() + '-' + padZ(end.getMonth() + 1) + '-' + padZ(end.getDate())

  const presets: string[] = [
    '北京市朝阳区望京街道 xx 小区 3 号楼',
    '上海市浦东新区陆家嘴环路',
    '广州市天河区珠江新城花城大道',
    '深圳市南山区科技园高新南一道',
    '杭州市西湖区文三路',
    '成都市高新区天府大道'
  ]

  uni.showActionSheet({
    itemList: ['默认地址（北京朝阳）', '快捷地址选择...', '手动填写收货地址'],
    success: async (sheetRes) => {
      let finalAddress: string = ''

      if (sheetRes.tapIndex === 0) {
        finalAddress = '北京市朝阳区望京街道 xx 小区 3 号楼'
      } else if (sheetRes.tapIndex === 1) {
        uni.showActionSheet({
          itemList: presets,
          success: (innerRes) => {
            const picked: string = presets[innerRes.tapIndex] || ''
            uni.showModal({
              title: '确认地址',
              content: picked,
              editable: true,
              placeholderText: '如需修改请直接编辑',
              success: async (mRes) => {
                if (!mRes.confirm) return
                // H5: res.content  小程序/APP: res.userText
                const addr: string = ((mRes as any).content || (mRes as any).userText || picked) as string
                if (!addr || addr.trim().length < 3) {
                  uni.showToast({ title: '地址过短', icon: 'none' })
                  return
                }
                await doCreateOrder(addr.trim(), startDate, endDate)
              }
            })
          }
        })
        return
      } else {
        uni.showModal({
          title: '请填写收货地址',
          editable: true,
          placeholderText: '例如：北京市朝阳区 xx 小区 3 号楼',
          success: async (mRes) => {
            if (!mRes.confirm) return
            // H5: res.content  小程序/APP: res.userText
            const addr: string = ((mRes as any).content || (mRes as any).userText || '') as string
            if (!addr || addr.trim().length < 3) {
              uni.showToast({ title: '请填写有效地址', icon: 'none' })
              return
            }
            await doCreateOrder(addr.trim(), startDate, endDate)
          }
        })
        return
      }

      if (finalAddress && finalAddress.trim().length >= 3) {
        await doCreateOrder(finalAddress.trim(), startDate, endDate)
      }
    }
  })
}

const doCreateOrder = async (deliveryAddress: string, startDate: string, endDate: string) => {
  try {
    const res = await createOrder({
      droneId: drone.value.id,
      startDate: startDate,
      endDate: endDate,
      deliveryAddress: deliveryAddress,
      remark: ''
    })
    if (res && res.code === 200) {
      uni.showToast({ title: '订单创建成功', icon: 'success' })
      setTimeout(() => {
        uni.redirectTo({
          url: '/pages/orders/pay?id=' + (res.data as any).id
        })
      }, 1000)
    } else {
      uni.showToast({ title: (res && res.message) || '创建订单失败', icon: 'none' })
    }
  } catch (err) {
    console.error('创建订单失败', err)
  }
}

const fetchDetail = async (id: number) => {
  try {
    const res = await getDroneDetail(id)
    const data = res.data as any
    if (data) {
      const brand = data.brand || 'DJI'
      const type = data.type || '航拍'
      const priceVal = data.pricePerDay !== undefined ? data.pricePerDay : (data.price || 0)
      const imageUrl = resolveImageUrl(data.image || '/uploads/mavic3_drone.png')

      drone.value = {
        id: data.id,
        name: data.model || data.name || '无人机',
        spec: brand,
        type: type,
        price: priceVal,
        image: imageUrl,
        stock: data.stock !== undefined ? data.stock : 1,
        rentalCount: data.rentalCount || 0,
        rating: data.rating || 0,
        description: data.description || ''
      }

      const specsRaw: Array<{ label: string; key: string; unit: string }> = [
        { label: '续航(分)', key: 'flightTime', unit: '' },
        { label: '载重(kg)', key: 'maxPayload', unit: '' },
        { label: '速度(km/h)', key: 'maxSpeed', unit: '' },
        { label: '航程(km)', key: 'maxRange', unit: '' }
      ]
      specList.value = specsRaw.map((s) => {
        const rawVal = (data as any)[s.key]
        let displayVal = '-'
        if (rawVal !== null && rawVal !== undefined && String(rawVal).length > 0) {
          displayVal = String(rawVal)
          if (displayVal.length > 8) {
            displayVal = displayVal.substring(0, 8)
          }
        }
        return { label: s.label, value: displayVal }
      })
    }
  } catch (err) {
    console.error('获取设备详情失败', err)
  }
}

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  let droneId = 1
  if (currentPage && (currentPage as any).options && (currentPage as any).options.id) {
    droneId = Number((currentPage as any).options.id) || 1
  }
  fetchDetail(droneId)
})
</script>

<style lang="scss">
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.detail-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* 顶部大图 */
.detail-hero {
  position: relative;
  width: 100%;
  height: 400rpx;
  background: linear-gradient(180deg, #ffffff 0%, #e2e8f0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-hero-img {
  width: 520rpx;
  height: 360rpx;
}

/* 卡片通用 */
.detail-card {
  margin: 16rpx 20rpx 0;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  box-sizing: border-box;
}

.card-header {
  margin-bottom: 16rpx;
}

.card-header-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

/* 标题卡片 */
.detail-title-row {
  margin-bottom: 8rpx;
}

.detail-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #0f172a;
}

.detail-meta-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-bottom: 16rpx;
}

.detail-meta {
  font-size: 24rpx;
  color: #64748b;
}

.detail-meta-dot {
  font-size: 22rpx;
  color: #cbd5e1;
  margin: 0 10rpx;
}

.detail-price-row {
  display: flex;
  flex-direction: row;
  align-items: baseline;
}

.detail-price {
  font-size: 48rpx;
  font-weight: 800;
  color: #ef4444;
  margin-right: 16rpx;
}

.price-unit {
  font-size: 22rpx;
  font-weight: 400;
}

.detail-count {
  font-size: 22rpx;
  color: #94a3b8;
}

/* 参数规格 */
.spec-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}

.spec-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 8rpx;
  background: #f8fafc;
  border-radius: 14rpx;
  margin-right: 10rpx;

  &:last-child {
    margin-right: 0;
  }
}

.spec-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6rpx;
}

.spec-label {
  font-size: 20rpx;
  color: #64748b;
}

/* 描述 */
.description-text {
  font-size: 26rpx;
  color: #475569;
  line-height: 1.8;
}

/* 租期选择 */
.rental-options {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  margin-bottom: 20rpx;
}

.rental-option {
  flex: 1;
  min-width: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 8rpx;
  background: #f8fafc;
  border-radius: 14rpx;
  margin-right: 10rpx;
  border: 2rpx solid transparent;

  &:last-child {
    margin-right: 0;
  }

  &.active {
    background: #eff6ff;
    border-color: #2563eb;
  }

  &.active .rental-days {
    color: #2563eb;
  }

  &.active .rental-price {
    color: #2563eb;
  }
}

.rental-days {
  font-size: 26rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6rpx;
}

.rental-price {
  font-size: 22rpx;
  color: #64748b;
}

.rental-summary {
  background: #f8fafc;
  border-radius: 14rpx;
  padding: 16rpx 20rpx;
}

.summary-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 6rpx 0;
}

.summary-label {
  font-size: 24rpx;
  color: #475569;
}

.summary-value {
  font-size: 26rpx;
  font-weight: 600;
  color: #0f172a;

  &.highlight {
    color: #ef4444;
    font-size: 32rpx;
    font-weight: 800;
  }
}

.bottom-space {
  height: 200rpx;
}

/* 底部操作栏 */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: row;
  align-items: center;
  background: #ffffff;
  padding: 14rpx 20rpx;
  padding-bottom: calc(14rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(14rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 12rpx rgba(15, 23, 42, 0.06);
}

.action-secondary {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6rpx 20rpx;
  flex-shrink: 0;
}

.action-icon {
  font-size: 36rpx;
  margin-bottom: 4rpx;
}

.action-text {
  font-size: 20rpx;
  color: #475569;
}

.action-primary {
  flex: 1;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  border-radius: 999rpx;
  padding: 24rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 16rpx;
}

.action-primary-text {
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
}
</style>
