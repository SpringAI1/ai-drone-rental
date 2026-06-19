import { get, post, put } from './request'

// ============ 用户端 ============

// 创建订单
export const createOrder = (data) => {
  return post('/order/create', data)
}

// 获取我的订单列表（通过 UserController）
export const getMyOrders = (params) => {
  return get('/user/orders', params)
}

// 获取订单详情
export const getOrderDetail = (id) => {
  return get(`/order/${id}`)
}

// 支付订单（模拟）
export const payOrder = (id, data) => {
  return post(`/order/${id}/pay`, data)
}

// 取消订单
export const cancelOrder = (id, reason) => {
  return post(`/order/${id}/cancel`, { reason })
}

// 确认收货
export const confirmReceive = (id) => {
  return post(`/order/${id}/receive`)
}

// 申请退租
export const applyReturn = (id) => {
  return post(`/order/${id}/return`)
}

// 获取订单统计
export const getOrderStats = () => {
  return get('/user/order-stats')
}

// 申请退款
export const applyRefund = (id, reason) => {
  return post(`/order/${id}/refund`, { reason })
}

// 评价订单
export const commentOrder = (id, data) => {
  return post(`/comment/add`, { orderId: id, ...data })
}

// ============ 管理端 ============

// 获取订单列表（管理端）
export const getAdminOrders = (params) => {
  return get('/admin/order/list', params)
}

// 获取订单详情（管理端）
export const getAdminOrderDetail = (id) => {
  return get(`/admin/order/${id}`)
}

// 发货
export const shipOrder = (id, data) => {
  return post(`/admin/order/${id}/ship?expressCompany=${encodeURIComponent(data.expressCompany)}&expressNo=${encodeURIComponent(data.expressNo)}`)
}

// 确认退租
export const confirmReturn = (id, data) => {
  return post(`/admin/order/${id}/return`, data)
}

// 退款
export const refundOrder = (id, data) => {
  return post(`/admin/order/${id}/refund`, data)
}
