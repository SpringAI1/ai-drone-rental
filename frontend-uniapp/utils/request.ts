import { USE_MOCK, mockStats, mockDrones, mockOrders, mockOrderStats, mockUserInfo, mockLoginResult, mockAirspaceRecords, mockNotifications, mockFaultReports, mockComments, mockBrands, mockTypes } from './mock'

export const BASE_URL = 'http://localhost:8080/api'

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, any>
  header?: Record<string, string>
  loading?: boolean
}

interface ResponseData<T = any> {
  code: number
  message: string
  data: T
  success: boolean
}

function getMockData<T>(url: string, method: string, data?: Record<string, any>): ResponseData<T> | null {
  if (!USE_MOCK) return null
  
  console.log(`[Mock] ${method} ${url}`, data)
  
  if (url === '/public/stats') {
    return { code: 200, message: 'success', data: mockStats as T, success: true }
  }
  
  if (url === '/drone/list') {
    return { code: 200, message: 'success', data: { records: mockDrones, total: mockDrones.length, current: 1 } as T, success: true }
  }
  
  if (url.startsWith('/drone/detail/')) {
    const id = parseInt(url.split('/').pop() || '0')
    const drone = mockDrones.find(d => d.id === id)
    if (drone) {
      return { code: 200, message: 'success', data: { ...drone, images: [], rentalCount: 156, rating: 4.8 } as T, success: true }
    }
  }
  
  if (url.startsWith('/drone/') && url.includes('/comments')) {
    return { code: 200, message: 'success', data: { records: mockComments, total: mockComments.length } as T, success: true }
  }
  
  if (url === '/drone/brands') {
    return { code: 200, message: 'success', data: mockBrands as T, success: true }
  }
  
  if (url === '/drone/types') {
    return { code: 200, message: 'success', data: mockTypes as T, success: true }
  }
  
  if (url === '/auth/login' && method === 'POST') {
    return { code: 200, message: 'success', data: mockLoginResult as T, success: true }
  }
  
  if (url === '/auth/register' && method === 'POST') {
    return { code: 200, message: '注册成功', data: null as T, success: true }
  }
  
  if (url === '/user/info') {
    return { code: 200, message: 'success', data: mockUserInfo as T, success: true }
  }
  
  if (url === '/user/orders') {
    const status = data?.orderStatus
    let filteredOrders = mockOrders
    if (status !== undefined) {
      filteredOrders = mockOrders.filter(o => o.orderStatus === status)
    }
    return { code: 200, message: 'success', data: { records: filteredOrders, total: filteredOrders.length, current: 1, pages: 1 } as T, success: true }
  }
  
  if (url === '/user/order-stats') {
    return { code: 200, message: 'success', data: mockOrderStats as T, success: true }
  }
  
  if (url.startsWith('/order/') && !url.includes('/pay') && !url.includes('/cancel') && !url.includes('/receive') && !url.includes('/return') && !url.includes('/refund')) {
    const id = parseInt(url.split('/').pop() || '0')
    const order = mockOrders.find(o => o.id === id)
    if (order) {
      return { code: 200, message: 'success', data: { ...order, phone: '13800138000', airspaceRecordId: null, regionName: null, deliveryAddress: '北京市朝阳区', remark: null, cancelReason: null, refundReason: null, payTime: null, shipTime: null, receiveTime: null, returnTime: null, cancelTime: null } as T, success: true }
    }
  }
  
  if (url.includes('/pay') && method === 'POST') {
    return { code: 200, message: '支付成功', data: null as T, success: true }
  }
  
  if (url.includes('/cancel') && method === 'POST') {
    return { code: 200, message: '订单已取消', data: null as T, success: true }
  }
  
  if (url.includes('/receive') && method === 'POST') {
    return { code: 200, message: '确认收货成功', data: null as T, success: true }
  }
  
  if (url.includes('/return') && method === 'POST') {
    return { code: 200, message: '申请退租成功', data: null as T, success: true }
  }
  
  if (url === '/airspace/list') {
    return { code: 200, message: 'success', data: mockAirspaceRecords as T, success: true }
  }
  
  if (url === '/airspace/submit' && method === 'POST') {
    return { code: 200, message: '提交成功', data: null as T, success: true }
  }
  
  if (url === '/notification/list') {
    return { code: 200, message: 'success', data: { records: mockNotifications, total: mockNotifications.length } as T, success: true }
  }
  
  if (url === '/notification/unread-count') {
    return { code: 200, message: 'success', data: mockNotifications.filter(n => n.readStatus === 0).length as T, success: true }
  }
  
  if (url === '/user/fault/list') {
    return { code: 200, message: 'success', data: mockFaultReports as T, success: true }
  }
  
  if (url === '/user/fault/report' && method === 'POST') {
    return { code: 200, message: '提交成功', data: null as T, success: true }
  }
  
  if (url === '/ai/v2/chat' && method === 'POST') {
    return { code: 200, message: 'success', data: { reply: '您好！我是AI客服助手，有什么可以帮助您的吗？', conversationId: 'mock_conv_123', tools: [] } as T, success: true }
  }
  
  console.log(`[Mock] No mock data found for: ${url}`)
  return { code: 404, message: 'Mock data not found', data: null as T, success: false }
}

export function request<T = any>(options: RequestOptions): Promise<ResponseData<T>> {
  return new Promise((resolve, reject) => {
    const mockResponse = getMockData<T>(options.url, options.method || 'GET', options.data)
    
    if (mockResponse) {
      if (options.loading !== false) {
        setTimeout(() => {
          uni.hideLoading()
        }, 100)
      }
      
      setTimeout(() => {
        if (mockResponse.code === 200) {
          resolve(mockResponse)
        } else {
          uni.showToast({ title: mockResponse.message || '请求失败', icon: 'none' })
          reject(mockResponse)
        }
      }, 200)
      return
    }
    
    const token = uni.getStorageSync('token')
    const defaultHeader: Record<string, string> = {
      'Content-Type': 'application/json'
    }
    
    if (token) {
      defaultHeader['Authorization'] = `Bearer ${token}`
    }
    
    if (options.loading !== false) {
      uni.showLoading({ title: '加载中...', mask: true })
    }
    
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: { ...defaultHeader, ...options.header },
      success: (res) => {
        if (options.loading !== false) {
          uni.hideLoading()
        }
        
        const response = res.data as ResponseData<T>
        
        if (response.code === 200) {
          resolve(response)
        } else if (response.code === 401) {
          uni.showToast({ title: '登录已过期', icon: 'none' })
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.redirectTo({ url: '/pages/user/login' })
          reject(response)
        } else {
          uni.showToast({ title: response.message || '请求失败', icon: 'none' })
          reject(response)
        }
      },
      fail: (err) => {
        if (options.loading !== false) {
          uni.hideLoading()
        }
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function get<T = any>(url: string, data?: Record<string, any>, loading = true): Promise<ResponseData<T>> {
  return request({ url, method: 'GET', data, loading })
}

export function post<T = any>(url: string, data?: Record<string, any>, loading = true): Promise<ResponseData<T>> {
  return request({ url, method: 'POST', data, loading })
}

export function put<T = any>(url: string, data?: Record<string, any>, loading = true): Promise<ResponseData<T>> {
  return request({ url, method: 'PUT', data, loading })
}

export function del<T = any>(url: string, data?: Record<string, any>, loading = true): Promise<ResponseData<T>> {
  return request({ url, method: 'DELETE', data, loading })
}