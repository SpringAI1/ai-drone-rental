import { get, post } from '../utils/request'

export interface OrderItem {
  id: number
  orderNo: string
  userId: number
  username: string
  droneId: number
  droneModel: string
  droneImage: string
  rentalStartTime: string
  rentalEndTime: string
  rentalDays: number
  unitPrice: number
  totalAmount: number
  depositAmount: number
  orderStatus: number
  orderStatusDesc: string
  hasComment: boolean
  createdTime: string
}

export interface OrderDetail extends OrderItem {
  phone: string
  airspaceRecordId: number | null
  regionName: string | null
  deliveryAddress: string | null
  remark: string | null
  cancelReason: string | null
  refundReason: string | null
  payTime: string | null
  shipTime: string | null
  receiveTime: string | null
  returnTime: string | null
  cancelTime: string | null
}

export interface OrderStats {
  pendingPay: number
  pendingShip: number
  pendingReceive: number
  renting: number
  returned: number
  canceled: number
}

export interface CreateOrderParams {
  droneId: number
  startDate: string
  endDate: string
  airspaceRecordId?: number | null
  deliveryAddress: string
  remark?: string
}

export interface CreateOrderResult {
  id: number
  orderNo: string
  totalAmount: number
}

export function getOrders(params?: { pageNum?: number; pageSize?: number; orderStatus?: number }) {
  return get<{ records: OrderItem[]; total: number; current: number; pages: number }>('/user/orders', params)
}

// 别名: getOrderList
export const getOrderList = getOrders

export function getOrderStats() {
  return get<OrderStats>('/user/order-stats')
}

export function getOrderDetail(orderId: number) {
  return get<OrderDetail>(`/order/${orderId}`)
}

export function createOrder(data: CreateOrderParams) {
  return post<CreateOrderResult>('/order/create', data)
}

export function payOrder(orderId: number, paymentMethod: number = 1) {
  // paymentMethod: 1-余额, 2-微信, 3-支付宝
  return post(`/order/${orderId}/pay`, { paymentMethod: paymentMethod, deliveryAddress: '' })
}

export function cancelOrder(orderId: number, reason: string = '用户主动取消') {
  return post(`/order/${orderId}/cancel`, { reason })
}

export function receiveOrder(orderId: number) {
  return post(`/order/${orderId}/receive`)
}

export function returnOrder(orderId: number, returnAddress: string = '系统默认地址') {
  return post(`/order/${orderId}/return`, { returnAddress })
}

export function refundOrder(orderId: number, reason: string = '用户申请退款') {
  return post(`/order/${orderId}/refund`, { reason })
}