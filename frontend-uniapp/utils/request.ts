import { mockStats, mockDrones, mockOrders, mockOrderStats, mockUserInfo, mockAirspaceRecords, mockNotifications, mockComments, mockBrands, mockTypes } from './mock'

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

function buildMockResponse<T>(url: string, method: string, data?: Record<string, any>): ResponseData<T> | null {
  if (url === '/public/stats') {
    return { code: 200, message: 'success', data: mockStats as unknown as T, success: true }
  }
  if (url === '/drone/list') {
    return { code: 200, message: 'success', data: { records: mockDrones, total: mockDrones.length, current: 1 } as unknown as T, success: true }
  }
  if (url.startsWith('/drone/detail/')) {
    const id = parseInt(url.split('/').pop() || '0')
    const drone = mockDrones.find(d => d.id === id) || mockDrones[0]
    return { code: 200, message: 'success', data: drone as unknown as T, success: true }
  }
  if (url.startsWith('/drone/') && url.includes('/comments')) {
    return { code: 200, message: 'success', data: { records: mockComments, total: mockComments.length } as unknown as T, success: true }
  }
  if (url === '/drone/brands') {
    return { code: 200, message: 'success', data: mockBrands as unknown as T, success: true }
  }
  if (url === '/drone/types') {
    return { code: 200, message: 'success', data: mockTypes as unknown as T, success: true }
  }
  if (url === '/auth/login' && method === 'POST') {
    return { code: 200, message: 'success', data: { token: 'mock-token-123', userInfo: mockUserInfo } as unknown as T, success: true }
  }
  if (url === '/auth/register' && method === 'POST') {
    return { code: 200, message: '注册成功', data: null as unknown as T, success: true }
  }
  if (url === '/user/info') {
    return { code: 200, message: 'success', data: mockUserInfo as unknown as T, success: true }
  }
  if (url === '/user/orders') {
    const status = data?.orderStatus
    let filtered = mockOrders
    if (status !== undefined) {
      filtered = mockOrders.filter(o => o.status === status)
    }
    return { code: 200, message: 'success', data: { records: filtered, total: filtered.length, current: 1, pages: 1 } as unknown as T, success: true }
  }
  if (url === '/user/order-stats') {
    return { code: 200, message: 'success', data: mockOrderStats as unknown as T, success: true }
  }
  if (url.startsWith('/order/') && !url.includes('/pay') && !url.includes('/cancel') && !url.includes('/receive') && !url.includes('/return') && !url.includes('/refund')) {
    const id = parseInt(url.split('/').pop() || '0')
    const order = mockOrders.find(o => o.id === id) || mockOrders[0]
    return { code: 200, message: 'success', data: order as unknown as T, success: true }
  }
  if (url.includes('/pay') && method === 'POST') {
    return { code: 200, message: '支付成功', data: null as unknown as T, success: true }
  }
  if (url.includes('/cancel') && method === 'POST') {
    return { code: 200, message: '订单已取消', data: null as unknown as T, success: true }
  }
  if (url.includes('/receive') && method === 'POST') {
    return { code: 200, message: '确认收货成功', data: null as unknown as T, success: true }
  }
  if (url.includes('/return') && method === 'POST') {
    return { code: 200, message: '申请退租成功', data: null as unknown as T, success: true }
  }
  if (url === '/airspace/list') {
    return { code: 200, message: 'success', data: mockAirspaceRecords as unknown as T, success: true }
  }
  if (url === '/airspace/submit' && method === 'POST') {
    return { code: 200, message: '提交成功', data: null as unknown as T, success: true }
  }
  if (url === '/notification/list') {
    return { code: 200, message: 'success', data: { records: mockNotifications, total: mockNotifications.length } as unknown as T, success: true }
  }
  if (url === '/notification/unread-count') {
    return { code: 200, message: 'success', data: mockNotifications.filter(n => n.readStatus === 0).length as unknown as T, success: true }
  }
  if (url.includes('/user/fault') && method === 'POST') {
    return { code: 200, message: '提交成功', data: null as unknown as T, success: true }
  }
  if (url.includes('/ai/') && method === 'POST') {
    return { code: 200, message: 'success', data: { reply: '您好！我是AI客服助手，请问有什么可以帮您？', conversationId: 'mock-conv', tools: [] } as unknown as T, success: true }
  }
  return { code: 200, message: 'success', data: null as unknown as T, success: true }
}

export function request<T = any>(options: RequestOptions): Promise<ResponseData<T>> {
  return new Promise((resolve, reject) => {
    const mockResponse = buildMockResponse<T>(options.url, options.method || 'GET', options.data)

    if (options.loading !== false) {
      uni.showLoading({ title: '加载中...', mask: true })
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...options.header
      },
      success: (res) => {
        if (options.loading !== false) uni.hideLoading()
        const response = res.data as ResponseData<T>
        if (response && response.code === 200) {
          resolve(response)
        } else if (response && response.code === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.redirectTo({ url: '/pages/user/login' })
          reject(response)
        } else {
          resolve({ code: 200, message: 'success', data: mockResponse?.data as T, success: true })
        }
      },
      fail: () => {
        if (options.loading !== false) uni.hideLoading()
        if (mockResponse) {
          setTimeout(() => resolve(mockResponse), 100)
        } else {
          reject({ code: 500, message: '网络异常', data: null, success: false })
        }
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
