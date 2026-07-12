<template>
  <div class="my-orders-page">
    <PageHeader title="我的订单" subtitle="管理您的所有租赁订单" />

    <!-- 订单状态导航 -->
    <div class="order-stats">
      <router-link
        v-for="tab in orderTabs"
        :key="tab.value"
        :to="tab.path"
        class="order-stat-card"
      >
        <div class="stat-icon">
          <el-icon :size="24"><component :is="tab.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-count">{{ tab.count }}</span>
          <span class="stat-label">{{ tab.label }}</span>
        </div>
        <el-icon class="stat-arrow"><ArrowRight /></el-icon>
      </router-link>
    </div>

    <!-- 最近订单 -->
    <GlassCard title="最近订单" class="recent-orders-card">
      <div v-loading="loading" class="recent-order-list">
        <div
          v-for="order in recentOrders"
          :key="order.id"
          class="recent-order-item"
          @click="goToDetail(order.id)"
        >
          <img :src="getImageUrl(order.droneImage) || defaultImage" :alt="order.droneModel" class="recent-order-image" />
          <div class="recent-order-info">
            <h4 class="recent-order-model">{{ order.droneModel }}</h4>
            <p class="recent-order-time">{{ formatDateTime(order.createdTime) }}</p>
          </div>
          <div class="recent-order-right">
            <span class="recent-order-price">¥{{ order.totalAmount }}</span>
            <StatusTag :text="getStatusText(order.orderStatus)" :type="getStatusType(order.orderStatus)" />
          </div>
        </div>

        <EmptyState
          v-if="recentOrders.length === 0 && !loading"
          title="暂无订单"
          description="快去浏览设备下单吧"
          :icon="Document"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/drones')">浏览设备</el-button>
          </template>
        </EmptyState>
      </div>

      <div v-if="recentOrders.length > 0" class="view-all">
        <router-link to="/orders/pending-pay">查看全部订单</router-link>
      </div>
    </GlassCard>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Clock,
  Box,
  Van,
  Promotion,
  CircleCheckFilled,
  CircleCloseFilled,
  Document
} from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getMyOrders, getOrderStats } from '@/api/order'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

const router = useRouter()
const loading = ref(false)
const recentOrders = ref([])
const defaultImage = 'https://picsum.photos/80/60'

const orderTabs = reactive([
  { label: '待支付', value: 'pending-pay', path: '/orders/pending-pay', icon: markRaw(Clock), count: 0 },
  { label: '待发货', value: 'pending-ship', path: '/orders/pending-ship', icon: markRaw(Box), count: 0 },
  { label: '待收货', value: 'pending-receive', path: '/orders/pending-receive', icon: markRaw(Van), count: 0 },
  { label: '租赁中', value: 'renting', path: '/orders/renting', icon: markRaw(Promotion), count: 0 },
  { label: '已归还', value: 'returned', path: '/orders/returned', icon: markRaw(CircleCheckFilled), count: 0 },
  { label: '已取消', value: 'canceled', path: '/orders/canceled', icon: markRaw(CircleCloseFilled), count: 0 }
])

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ').substring(0, 16)
}

const formatDate = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.substring(0, 10)
}

const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api/uploads/')) return url
  if (url.startsWith('/uploads/')) return `${API_BASE}${url}`
  if (!url.startsWith('/')) return `${API_BASE}/uploads/${url}`
  return `${API_BASE}${url}`
}

const getStatusText = (status) => {
  const map = {
    0: '待支付',
    1: '待发货',
    2: '待收货',
    3: '租赁中',
    4: '已归还',
    5: '已取消'
  }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = {
    0: 'warning',
    1: 'info',
    2: 'primary',
    3: 'success',
    4: 'info',
    5: 'danger'
  }
  return map[status] || 'default'
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const [orderRes, statsRes] = await Promise.all([
      getMyOrders({
        page: 1,
        pageSize: 10
      }),
      getOrderStats()
    ])
    
    const orders = orderRes.data?.records || []
    recentOrders.value = orders.slice(0, 5)

    const stats = statsRes.data || {}
    orderTabs[0].count = stats.pendingPay || 0
    orderTabs[1].count = stats.pendingShip || 0
    orderTabs[2].count = stats.pendingReceive || 0
    orderTabs[3].count = stats.renting || 0
    orderTabs[4].count = stats.returned || 0
    orderTabs[5].count = stats.canceled || 0
  } catch (error) {
    console.error('获取订单失败:', error)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/orders/${id}`)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.my-orders-page {
  min-height: 100%;
}

.order-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.order-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  text-decoration: none;
  transition: all 0.3s;

  &:hover {
    border-color: #3b82f6;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
    transform: translateY(-2px);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-count {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
}

.stat-arrow {
  color: #94a3b8;
  transition: transform 0.3s;
}

.order-stat-card:hover .stat-arrow {
  transform: translateX(4px);
}

.recent-orders-card {
  padding: 24px;
}

.recent-order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recent-order-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: #f1f5f9;
  }
}

.recent-order-image {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  background: #e2e8f0;
}

.recent-order-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recent-order-model {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.recent-order-time {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.recent-order-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.recent-order-price {
  font-size: 16px;
  font-weight: 700;
  color: #3b82f6;
}

.view-all {
  text-align: center;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px solid #e2e8f0;

  a {
    font-size: 14px;
    color: #3b82f6;
    text-decoration: none;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}

@media (max-width: 768px) {
  .order-stats {
    grid-template-columns: repeat(3, 1fr);
  }

  .order-stat-card {
    flex-direction: column;
    text-align: center;
    padding: 16px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
  }

  .stat-count {
    font-size: 20px;
  }

  .stat-arrow {
    display: none;
  }

  .recent-order-item {
    flex-wrap: wrap;
  }

  .recent-order-right {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid #e2e8f0;
    margin-top: 8px;
  }
}

@media (max-width: 480px) {
  .order-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>