<template>
  <div class="order-pay-page">
    <PageHeader title="订单支付" :breadcrumbs="breadcrumbs" />

    <div v-loading="loading" class="pay-content">
      <GlassCard v-if="order" class="pay-card">
        <!-- 订单信息 -->
        <div class="order-info-section">
          <h3 class="section-title">订单信息</h3>
          <div class="order-summary">
            <img :src="getImageUrl(order.droneImage) || defaultImage" :alt="order.droneModel" class="order-image" />
            <div class="order-detail">
              <h4 class="order-model">{{ order.droneModel }}</h4>
              <p class="order-period">
                租赁周期：{{ formatDate(order.rentalStartTime) }} 至 {{ formatDate(order.rentalEndTime) }}
                （共{{ order.rentalDays }}天）
              </p>
              <p class="order-no">订单号：{{ order.orderNo }}</p>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 支付金额 -->
        <div class="amount-section">
          <h3 class="section-title">支付金额</h3>
          <div class="amount-detail">
            <div class="amount-item">
              <span>租赁费用</span>
              <span>¥{{ (order.unitPrice * order.rentalDays).toFixed(2) }}</span>
            </div>
            <div class="amount-item">
              <span>押金</span>
              <span>¥{{ order.depositAmount }}</span>
            </div>
            <el-divider />
            <div class="amount-total">
              <span>应付总额</span>
              <span class="total-price">¥{{ order.totalAmount }}</span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 支付方式 -->
        <div class="payment-section">
          <h3 class="section-title">选择支付方式</h3>
          <div class="payment-methods">
            <div
              class="payment-method"
              :class="{ 'payment-method--active': selectedMethod === 'wechat' }"
              @click="selectedMethod = 'wechat'"
            >
              <div class="method-icon wechat-icon">
                <svg viewBox="0 0 24 24" width="32" height="32">
                  <path fill="#07C160" d="M8.691 2.188C3.891 2.188 0 5.476 0 9.49c0 2.372 1.523 4.523 3.867 5.845-.213.78-.547 2.048-.547 2.206 0 .18.107.195.24.12.12-.075 1.783-1.095 2.525-1.575.645.135 1.32.195 2.025.195 4.8 0 8.691-3.288 8.691-7.302 0-4.014-3.891-7.302-8.691-7.302zm-2.85 5.7c.63 0 1.14.51 1.14 1.14s-.51 1.14-1.14 1.14-1.14-.51-1.14-1.14.51-1.14 1.14-1.14zm5.7 0c.63 0 1.14.51 1.14 1.14s-.51 1.14-1.14 1.14-1.14-.51-1.14-1.14.51-1.14 1.14-1.14z"/>
                  <path fill="#07C160" d="M23.988 14.49c0-3.48-3.48-6.3-7.8-6.3-.48 0-.96.03-1.44.09.3.75.48 1.56.48 2.41 0 4.32-4.08 7.8-9.12 7.8-.18 0-.36-.01-.54-.02 1.38 2.28 4.5 3.84 8.1 3.84.54 0 1.08-.04 1.62-.12.66.42 2.28 1.38 2.4 1.44.12.06.24.04.24-.12 0-.18-.36-1.56-.54-2.28 2.16-1.32 3.6-3.42 3.6-5.76z"/>
                </svg>
              </div>
              <div class="method-info">
                <span class="method-name">微信支付</span>
                <span class="method-desc">沙盒模拟支付</span>
              </div>
              <div class="method-check">
                <el-icon v-if="selectedMethod === 'wechat'" :size="20"><Select /></el-icon>
              </div>
            </div>

            <div
              class="payment-method"
              :class="{ 'payment-method--active': selectedMethod === 'alipay' }"
              @click="selectedMethod = 'alipay'"
            >
              <div class="method-icon alipay-icon">
                <svg viewBox="0 0 24 24" width="32" height="32">
                  <path fill="#1677FF" d="M21.422 15.358c-1.773-.69-3.798-1.548-5.823-2.406.948-1.908 1.716-4.044 2.22-6.3H12.6V4.2h6.6V2.4H12.6V0H9.6v2.4H3v1.8h6.6v2.4H4.2v1.8h10.2c-.42 1.8-.99 3.48-1.71 5.04-2.88-1.08-5.64-2.04-7.56-2.04-2.94 0-4.8 1.56-4.8 3.72 0 2.16 1.86 3.72 4.8 3.72 2.52 0 5.28-1.2 7.92-2.88.9 1.44 1.8 2.88 2.7 4.32.3.48.6.96.9 1.44H21.6c-.18-.48-.36-.96-.54-1.44-.36-.72-.72-1.44-1.08-2.16 1.44-.6 2.88-1.2 4.32-1.8l-.54-1.8c-.72.3-1.44.6-2.16.9-.18-.36-.36-.72-.54-1.08z"/>
                </svg>
              </div>
              <div class="method-info">
                <span class="method-name">支付宝</span>
                <span class="method-desc">沙盒模拟支付</span>
              </div>
              <div class="method-check">
                <el-icon v-if="selectedMethod === 'alipay'" :size="20"><Select /></el-icon>
              </div>
            </div>

            <div
              class="payment-method"
              :class="{ 'payment-method--active': selectedMethod === 'balance' }"
              @click="selectedMethod = 'balance'"
            >
              <div class="method-icon balance-icon">
                <el-icon :size="32" color="#3b82f6"><Wallet /></el-icon>
              </div>
              <div class="method-info">
                <span class="method-name">账户余额</span>
                <span class="method-desc">可用余额：¥{{ userBalance }}</span>
              </div>
              <div class="method-check">
                <el-icon v-if="selectedMethod === 'balance'" :size="20"><Select /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- 收货地址 -->
        <div class="address-section">
          <h3 class="section-title">收货地址</h3>
          <el-input
            v-model="deliveryAddress"
            placeholder="请输入收货地址"
            :rows="2"
            type="textarea"
          />
        </div>

        <!-- 支付按钮 -->
        <div class="pay-actions">
          <el-button size="large" @click="router.back()">返回</el-button>
          <el-button
            type="primary"
            size="large"
            :loading="paying"
            :disabled="!deliveryAddress"
            @click="handlePay"
          >
            确认支付 ¥{{ order.totalAmount }}
          </el-button>
        </div>
      </GlassCard>
    </div>

    <!-- 支付成功弹窗 -->
    <el-dialog
      v-model="successDialogVisible"
      title="支付成功"
      width="400px"
      center
    >
      <div class="success-content">
        <el-icon :size="64" color="#22c55e"><CircleCheckFilled /></el-icon>
        <h3>支付成功！</h3>
        <p>订单号：{{ order?.orderNo }}</p>
        <p>支付方式：{{ getPaymentMethodText(selectedMethod) }}</p>
        <p>支付金额：¥{{ order?.totalAmount }}</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="goToOrderDetail">查看订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Select, Wallet, CircleCheckFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import { useAuthStore } from '@/stores/auth'
import { getOrderDetail, payOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const paying = ref(false)
const order = ref(null)
const selectedMethod = ref('wechat')
const deliveryAddress = ref('')
const successDialogVisible = ref(false)
const defaultImage = 'https://picsum.photos/120/80'

const breadcrumbs = [
  { title: '首页', path: '/' },
  { title: '我的订单', path: '/orders' },
  { title: '订单支付' }
]

const userBalance = computed(() => authStore.user?.balance || 0)

const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `/api${url}`
}

const formatDate = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.substring(0, 10)
}

const getPaymentMethodText = (method) => {
  const map = {
    wechat: '微信支付',
    alipay: '支付宝',
    balance: '账户余额'
  }
  return map[method] || method
}

const fetchOrderDetail = async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res.data
    if (order.value.orderStatus !== 0) {
      ElMessage.warning('该订单已支付或已取消')
      router.push(`/orders/${route.params.id}`)
    }
  } catch (error) {
    console.error('获取订单失败:', error)
    router.push('/orders')
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  if (!deliveryAddress.value) {
    ElMessage.warning('请填写收货地址')
    return
  }

  if (selectedMethod.value === 'balance' && userBalance.value < order.value.totalAmount) {
    ElMessage.error('账户余额不足')
    return
  }

  paying.value = true
  try {
    await payOrder(order.value.id, {
      paymentMethod: selectedMethod.value === 'wechat' ? 1 : selectedMethod.value === 'alipay' ? 2 : 3,
      deliveryAddress: deliveryAddress.value
    })
    
    successDialogVisible.value = true
    authStore.fetchUserInfo()
  } catch (error) {
    console.error('支付失败:', error)
  } finally {
    paying.value = false
  }
}

const goToOrderDetail = () => {
  successDialogVisible.value = false
  router.push(`/orders/${route.params.id}`)
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style lang="scss" scoped>
.order-pay-page {
  min-height: 100%;
}

.pay-content {
  max-width: 600px;
  margin: 0 auto;
}

.pay-card {
  padding: 32px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 16px;
}

.order-summary {
  display: flex;
  gap: 16px;
}

.order-image {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 12px;
}

.order-detail {
  flex: 1;
}

.order-model {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px;
}

.order-period,
.order-no {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 4px;
}

.amount-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.amount-item {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #64748b;
}

.amount-total {
  display: flex;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.total-price {
  color: #3b82f6;
  font-size: 24px;
}

.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.payment-method {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #3b82f6;
  }

  &--active {
    border-color: #3b82f6;
    background: rgba(59, 130, 246, 0.05);
  }
}

.method-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.wechat-icon {
  background: rgba(7, 193, 96, 0.1);
}

.alipay-icon {
  background: rgba(22, 119, 255, 0.1);
}

.balance-icon {
  background: rgba(59, 130, 246, 0.1);
}

.method-info {
  flex: 1;
}

.method-name {
  font-size: 16px;
  font-weight: 500;
  color: #0f172a;
}

.method-desc {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
  display: block;
}

.method-check {
  color: #3b82f6;
}

.address-section {
  margin-top: 24px;
}

.pay-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 32px;
}

.success-content {
  text-align: center;
  padding: 20px;

  h3 {
    font-size: 20px;
    color: #0f172a;
    margin: 16px 0 8px;
  }

  p {
    font-size: 14px;
    color: #64748b;
    margin: 4px 0;
  }
}

@media (max-width: 768px) {
  .pay-card {
    padding: 20px;
  }

  .order-summary {
    flex-direction: column;
  }

  .order-image {
    width: 100%;
    height: 120px;
  }

  .pay-actions {
    flex-direction: column;

    .el-button {
      width: 100%;
    }
  }
}
</style>