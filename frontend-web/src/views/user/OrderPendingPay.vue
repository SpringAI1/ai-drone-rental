<template>
  <div class="order-page">
    <div class="breadcrumb-wrapper">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>
          <router-link to="/orders">我的订单</router-link>
        </el-breadcrumb-item>
        <el-breadcrumb-item>待支付</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    
    <PageHeader title="待支付订单" subtitle="完成支付后商家将安排发货" />

    <div v-loading="loading" class="order-list">
      <GlassCard
        v-for="order in orderList"
        :key="order.id"
        class="order-card"
      >
        <div class="order-header">
          <div class="order-info">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-time">{{ formatDateTime(order.createdTime) }}</span>
          </div>
          <StatusTag text="待支付" type="warning" />
        </div>

        <div class="order-content" @click="goToDetail(order.id)">
          <div class="drone-info">
            <img :src="getImageUrl(order.droneImage) || defaultImage" :alt="order.droneModel" class="drone-image" />
            <div class="drone-detail">
              <h4 class="drone-model">{{ order.droneModel }}</h4>
              <p class="rent-period">
                租赁周期：{{ formatDate(order.rentalStartTime) }} 至 {{ formatDate(order.rentalEndTime) }}
                <span class="rent-days">（共{{ order.rentalDays }}天）</span>
              </p>
            </div>
          </div>
          <div class="order-price">
            <span class="price-label">订单金额</span>
            <span class="price-value">¥{{ order.totalAmount }}</span>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-actions">
            <el-button type="primary" @click="goToPay(order.id)">去支付</el-button>
            <el-button @click="handleCancel(order)">取消订单</el-button>
          </div>
        </div>
      </GlassCard>

      <EmptyState
        v-if="orderList.length === 0 && !loading"
        title="暂无待支付订单"
        description="快去浏览设备下单吧"
        :icon="Document"
      >
        <template #action>
          <el-button type="primary" @click="router.push('/drones')">浏览设备</el-button>
        </template>
      </EmptyState>
    </div>

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="total"
        :page-sizes="[10, 20, 30]"
        layout="total, prev, pager, next"
        background
        @current-change="fetchOrders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getMyOrders, cancelOrder } from '@/api/order'

const router = useRouter()
const loading = ref(false)
const orderList = ref([])
const total = ref(0)
const defaultImage = 'https://picsum.photos/120/80'

const pagination = reactive({
  page: 1,
  pageSize: 10
})

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ').substring(0, 19)
}

const formatDate = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.substring(0, 10)
}

const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api/uploads/')) return url
  if (url.startsWith('/uploads/')) return `/api${url}`
  if (!url.startsWith('/')) return `/api/uploads/${url}`
  return `/api${url}`
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getMyOrders({
      page: pagination.page,
      pageSize: pagination.pageSize,
      status: 0
    })
    orderList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取订单失败:', error)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/orders/${id}`)
}

const goToPay = (id) => {
  router.push(`/orders/${id}/pay`)
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '请输入取消原因'
    }).then(async ({ value }) => {
      await cancelOrder(order.id, value)
      ElMessage.success('订单已取消')
      fetchOrders()
    })
  } catch {
    // 取消操作
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.order-page {
  min-height: 100%;
}

.breadcrumb-wrapper {
  margin-bottom: 16px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 400px;
}

.order-card {
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
  }
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 16px;
}

.order-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-no {
  font-size: 14px;
  color: #64748b;
}

.order-time {
  font-size: 13px;
  color: #64748b;
}

.order-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  padding: 16px;
  margin: -16px;
  margin-bottom: 0;
  border-radius: 12px;
  transition: background 0.3s;

  &:hover {
    background: rgba(148, 163, 184, 0.05);
  }
}

.drone-info {
  display: flex;
  gap: 16px;
}

.drone-image {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 12px;
  background: #f5f7fa;
}

.drone-detail {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.drone-model {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.rent-period {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.rent-days {
  color: #3b82f6;
}

.order-price {
  text-align: right;
}

.price-label {
  display: block;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.price-value {
  font-size: 20px;
  font-weight: 700;
  color: #3b82f6;
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  margin-top: 16px;
}

.order-actions {
  display: flex;
  gap: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 768px) {
  .order-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .order-price {
    text-align: left;
    width: 100%;
    padding-top: 16px;
    border-top: 1px solid #e2e8f0;
  }

  .order-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .order-actions {
    flex-wrap: wrap;

    .el-button {
      flex: 1;
      min-width: 100px;
    }
  }
}
</style>