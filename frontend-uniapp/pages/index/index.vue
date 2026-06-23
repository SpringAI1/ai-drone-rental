<template>
  <view class="home-page">
    <!-- Hero 区域 -->
    <view class="home-hero">
      <view class="hero-header">
        <view class="logo-box">
          <text class="logo-text">AI</text>
        </view>
        <view class="hero-title-group">
          <text class="hero-title-sm">AI智能体无人机租赁</text>
          <text class="hero-sub-sm">低空设备租赁平台</text>
        </view>
      </view>

      <!-- 整行 Hero 横幅 -->
      <view class="hero-banner">
        <view class="banner-content">
          <text class="banner-title">专业无人机租赁</text>
          <text class="banner-desc">高品质设备 · 快速响应 · 24h服务</text>
          <view class="banner-btns">
            <view class="btn-primary" @click="goToDroneList">
              <text class="btn-text">浏览设备</text>
            </view>
            <view class="btn-ghost" @click="handleLearnMore">
              <text class="btn-ghost-text">了解更多</text>
            </view>
          </view>
        </view>
        <view class="banner-image">
          <image class="banner-drone-img" :src="heroImage" mode="aspectFit" />
        </view>
      </view>

      <!-- 横向统计栏 -->
      <view class="stats-bar">
        <view class="stat-item">
          <text class="stat-num">{{ stats.totalDrones }}+</text>
          <text class="stat-tag">设备型号</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num">{{ stats.totalOrders }}+</text>
          <text class="stat-tag">累计订单</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num">{{ stats.totalUsers }}+</text>
          <text class="stat-tag">注册用户</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-num">{{ stats.positiveRate }}%</text>
          <text class="stat-tag">好评率</text>
        </view>
      </view>
    </view>

    <!-- 核心优势 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">核心优势</text>
        <text class="section-more" @click="handleLearnMore">查看 ›</text>
      </view>
      <view class="adv-row">
        <view class="adv-item" v-for="adv in advList" :key="adv.title">
          <view class="adv-icon" :style="{ background: adv.bg }">
            <text class="adv-icon-text">{{ adv.icon }}</text>
          </view>
          <text class="adv-title">{{ adv.title }}</text>
          <text class="adv-desc">{{ adv.desc }}</text>
        </view>
      </view>
    </view>

    <!-- 推荐机型 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">推荐机型</text>
        <text class="section-more" @click="goToDroneList">全部 ›</text>
      </view>
      <view class="drone-grid">
        <view class="drone-item" v-for="item in droneList" :key="item.id" @click="goToDetail(item.id)">
          <view class="drone-img-box">
            <image class="drone-img" :src="item.image" mode="aspectFit" />
            <view class="drone-tag">
              <text class="tag-text">{{ item.tagLeft }}</text>
            </view>
          </view>
          <view class="drone-info">
            <text class="drone-name">{{ item.name }}</text>
            <view class="drone-bottom">
              <view class="drone-price-col">
                <text class="drone-price">¥{{ item.price }}<text class="price-unit">/天</text></text>
                <text class="drone-spec">{{ item.spec }}</text>
              </view>
              <view class="rent-btn" @click.stop="handleRent(item)">
                <text class="rent-btn-text">租</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 解决方案 -->
    <view class="solution-card">
      <view class="solution-left">
        <text class="solution-title">定制化行业方案</text>
        <text class="solution-sub">专业技术团队1对1服务，提供全套设备建议</text>
      </view>
      <view class="solution-right">
        <view class="chat-fab" @click="goToChat">
          <text class="chat-icon">✦</text>
        </view>
        <view class="solution-btn" @click="handleConsult">
          <text class="solution-btn-text">立即咨询</text>
        </view>
      </view>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStats } from '../../api/public'
import { getDroneList } from '../../api/drone'
import { resolveImageUrl } from '../../utils/image'

interface DroneDisplayItem {
  id: number
  name: string
  spec: string
  price: number
  image: string
  tagLeft: string
}

interface AdvItem {
  icon: string
  title: string
  desc: string
  bg: string
}

interface StatsData {
  totalDrones: number
  totalOrders: number
  totalUsers: number
  positiveRate: number
}

const stats = ref<StatsData>({
  totalDrones: 0,
  totalOrders: 0,
  totalUsers: 0,
  positiveRate: 98
})

const heroImage = ref<string>(resolveImageUrl('/uploads/mavic3_drone.png'))

const droneList = ref<DroneDisplayItem[]>([])

const advList = ref<AdvItem[]>([
  { icon: '✈', title: 'AI智能驾驶', desc: '全自研视觉避障', bg: '#dbeafe' },
  { icon: '⚡', title: '飞行安全保障', desc: '百万保险覆盖', bg: '#fef3c7' },
  { icon: '◆', title: '专业数据支撑', desc: '多维分析赋能', bg: '#ede9fe' }
])

const fallbackDrones: DroneDisplayItem[] = [
  { id: 1, name: 'DJI Mavic 3', spec: '专业航拍', price: 299, image: resolveImageUrl('/uploads/mavic3_drone.png'), tagLeft: '热门推荐' },
  { id: 2, name: 'DJI Mini 4 Pro', spec: '轻便航拍', price: 199, image: resolveImageUrl('/uploads/mini4pro_drone.png'), tagLeft: '轻便灵活' },
  { id: 3, name: 'DJI Air 3', spec: '双主摄航拍', price: 249, image: resolveImageUrl('/uploads/air2s_drone.png'), tagLeft: '航拍摄影' },
  { id: 4, name: 'DJI Matrice 300 RTK', spec: '企业级', price: 899, image: resolveImageUrl('/uploads/mavic3_drone.png'), tagLeft: '行业应用' }
]

const fetchStats = async () => {
  try {
    const res = await getStats()
    const data = res.data
    stats.value = {
      totalDrones: data?.totalDrones ?? 10,
      totalOrders: data?.totalOrders ?? 100,
      totalUsers: data?.totalUsers ?? 1000,
      positiveRate: data?.positiveRate ?? 98
    }
  } catch (err) {
    console.error('获取统计数据失败', err)
  }
}

const fetchDrones = async () => {
  try {
    const res = await getDroneList({ pageNum: 1, pageSize: 10, status: 1 })
    const records = (res.data as any)?.records || []
    if (records.length === 0) {
      droneList.value = fallbackDrones
      return
    }
    droneList.value = records.map((d: any) => {
      const brand = d.brand || 'DJI'
      const type = d.type || '航拍'
      const spec = brand + ' · ' + type
      const rawImage = d.image || '/uploads/mavic3_drone.png'
      return {
        id: d.id,
        name: d.model || d.name || '无人机',
        spec: spec,
        price: d.pricePerDay ?? d.price ?? 0,
        image: resolveImageUrl(rawImage),
        tagLeft: brand
      }
    })
    if (droneList.value.length > 0) {
      heroImage.value = droneList.value[0].image
    }
  } catch (err) {
    console.error('获取设备列表失败', err)
    droneList.value = fallbackDrones
  }
}

const goToDroneList = () => {
  uni.switchTab({ url: '/pages/drones/list' })
}

const goToDetail = (id: number) => {
  uni.navigateTo({ url: '/pages/drone-detail/index?id=' + id })
}

const goToChat = () => {
  uni.navigateTo({ url: '/pages/chat/index' })
}

const handleLearnMore = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

const handleRent = (item: DroneDisplayItem) => {
  uni.navigateTo({ url: '/pages/drone-detail/index?id=' + item.id })
}

const handleConsult = () => {
  uni.navigateTo({ url: '/pages/chat/index' })
}

onMounted(() => {
  fetchStats()
  fetchDrones()
})
</script>

<style lang="scss">
page {
  min-height: 100vh;
  background: #f1f5f9;
}

.home-page {
  min-height: 100vh;
  background: #f1f5f9;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ===== Hero 顶部 ===== */
.home-hero {
  background: linear-gradient(180deg, #ffffff 0%, #eaf4ff 70%, #f1f5f9 100%);
  padding: 48rpx 0 0;
  box-sizing: border-box;
}

.hero-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 0 28rpx 20rpx;
}

.logo-box {
  width: 56rpx;
  height: 56rpx;
  background: #2563eb;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.logo-text {
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 700;
}

.hero-title-group {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.hero-title-sm {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.hero-sub-sm {
  font-size: 22rpx;
  color: #64748b;
  margin-top: 4rpx;
}

/* 整行 Hero 横幅 */
.hero-banner {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  margin: 16rpx 0;
  padding: 28rpx 28rpx;
  box-sizing: border-box;
  overflow: hidden;
}

.banner-content {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.banner-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #ffffff;
  line-height: 1.2;
  margin-bottom: 8rpx;
}

.banner-desc {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 18rpx;
}

.banner-btns {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.btn-primary {
  background: #ffffff;
  border-radius: 999rpx;
  padding: 12rpx 24rpx;
  display: flex;
  align-items: center;
}

.btn-text {
  color: #2563eb;
  font-size: 24rpx;
  font-weight: 700;
}

.btn-ghost {
  margin-left: 12rpx;
  padding: 12rpx 16rpx;
}

.btn-ghost-text {
  font-size: 24rpx;
  color: #ffffff;
  font-weight: 500;
}

.banner-image {
  width: 220rpx;
  height: 160rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.banner-drone-img {
  width: 200rpx;
  height: 140rpx;
}

/* 横向统计栏 */
.stats-bar {
  margin: 0 20rpx;
  background: #ffffff;
  border-radius: 16rpx;
  padding: 20rpx 12rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(15, 23, 42, 0.05);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rpx 0;
}

.stat-num {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4rpx;
  line-height: 1.1;
}

.stat-tag {
  font-size: 20rpx;
  color: #64748b;
}

.stat-divider {
  width: 2rpx;
  height: 40rpx;
  background: #e2e8f0;
}

/* ===== 通用 section ===== */
.section {
  margin: 20rpx 20rpx 0;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  box-sizing: border-box;
}

.section-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

.section-more {
  font-size: 22rpx;
  color: #3b82f6;
  font-weight: 500;
}

/* ===== 核心优势 ===== */
.adv-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}

.adv-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 4rpx;
  margin-right: 8rpx;

  &:last-child {
    margin-right: 0;
  }
}

.adv-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
}

.adv-icon-text {
  font-size: 28rpx;
}

.adv-title {
  font-size: 24rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4rpx;
}

.adv-desc {
  font-size: 20rpx;
  color: #64748b;
  text-align: center;
  line-height: 1.4;
}

/* ===== 推荐机型：网格平铺 ===== */
.drone-grid {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: space-between;
  margin: 0 -4rpx;
}

.drone-item {
  width: calc(50% - 10rpx);
  margin: 0 4rpx 14rpx;
  background: #fafbfc;
  border-radius: 16rpx;
  border: 2rpx solid #e2e8f0;
  overflow: hidden;
  box-sizing: border-box;
}

.drone-img-box {
  position: relative;
  width: 100%;
  height: 160rpx;
  background: linear-gradient(180deg, #f1f5f9 0%, #e2e8f0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.drone-img {
  width: 200rpx;
  height: 130rpx;
}

.drone-tag {
  position: absolute;
  top: 10rpx;
  left: 10rpx;
  background: rgba(37, 99, 235, 0.95);
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}

.tag-text {
  font-size: 18rpx;
  color: #ffffff;
  font-weight: 500;
}

.drone-info {
  padding: 12rpx 14rpx 14rpx;
}

.drone-name {
  font-size: 24rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drone-bottom {
  display: flex;
  flex-direction: row;
  align-items: flex-end;
  justify-content: space-between;
}

.drone-price-col {
  display: flex;
  flex-direction: column;
}

.drone-price {
  font-size: 28rpx;
  font-weight: 700;
  color: #ef4444;
  line-height: 1.1;
}

.price-unit {
  font-size: 20rpx;
  font-weight: 400;
}

.drone-spec {
  font-size: 20rpx;
  color: #64748b;
  margin-top: 4rpx;
}

.rent-btn {
  width: 52rpx;
  height: 52rpx;
  background: #2563eb;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: 8rpx;
}

.rent-btn-text {
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 600;
}

/* ===== 解决方案 ===== */
.solution-card {
  margin: 20rpx;
  background: #0f172a;
  border-radius: 20rpx;
  padding: 20rpx 24rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}

.solution-left {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding-right: 16rpx;
}

.solution-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 6rpx;
}

.solution-sub {
  font-size: 22rpx;
  color: #cbd5e1;
  line-height: 1.4;
}

.solution-right {
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-shrink: 0;
}

.chat-fab {
  width: 64rpx;
  height: 64rpx;
  background: #2563eb;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
}

.chat-icon {
  font-size: 32rpx;
}

.solution-btn {
  background: #ffffff;
  border-radius: 999rpx;
  padding: 12rpx 22rpx;
  display: flex;
}

.solution-btn-text {
  color: #1e40af;
  font-size: 22rpx;
  font-weight: 600;
}

.bottom-space {
  height: 60rpx;
}
</style>
