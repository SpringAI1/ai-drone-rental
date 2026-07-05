/**
 * 共享工具函数 - 从多个组件中提取，避免重复定义
 */

/** 订单状态 → 中文文本 */
export function getOrderStatusText(status) {
  const map = { 0: '待支付', 1: '待发货', 2: '待收货', 3: '租赁中', 4: '已归还', 5: '已取消', 6: '已退款' }
  return map[status] || '未知'
}

/** 订单状态 → Element Plus Tag 类型 */
export function getOrderStatusType(status) {
  const map = { 0: 'warning', 1: 'info', 2: 'info', 3: 'primary', 4: 'success', 5: 'default', 6: 'default' }
  return map[status] || 'default'
}

/** 支付方式 → 中文文本 */
export function getPaymentMethodText(method) {
  const map = { 1: '微信支付', 2: '支付宝', 3: '余额支付', alipay: '支付宝', wechat: '微信支付', balance: '余额支付' }
  return map[method] || '未知'
}

/** 图片 URL 标准化（处理相对路径、代理路径等） */
export function getImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/api/uploads/')) return url
  if (url.startsWith('/uploads/')) return '/api' + url
  return '/api' + (url.startsWith('/') ? '' : '/') + url
}

/** 默认占位图 */
export function getDefaultDroneImage() {
  return '/api/uploads/drone_placeholder.png'
}
