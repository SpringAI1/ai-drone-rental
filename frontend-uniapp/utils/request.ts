import { mockStats, mockDrones, mockOrders, mockOrderStats, mockUserInfo, mockAirspaceRecords, mockNotifications, mockComments, mockBrands, mockTypes } from './mock'
import { encryptRequest, decryptResponse, ENCRYPTION_ENABLED } from './encryption'

// API 基础地址（支持 .env 配置，留空时使用相对路径）
//   HBuilderX 运行时：可配置 .env.* 文件中的 UNI_API_BASE_URL
//   CLI 构建时：可配置 .env.* 文件中的 VITE_API_BASE_URL
//   优先级：UNI_API_BASE_URL > VITE_API_BASE_URL > 'http://localhost:8080/api'
const API_BASE_URL =
  (typeof process !== 'undefined' && (process as any)?.env?.UNI_API_BASE_URL) ||
  (import.meta as any)?.env?.VITE_API_BASE_URL ||
  'http://localhost:8080/api'
export const BASE_URL = API_BASE_URL

// Mock 模式开关：仅在显式开启时拦截请求并返回 mock 数据
//   开启方式：.env.development / .env.production 中设置 VITE_USE_MOCK=true
//   默认：false（走真实后端）
//   警告：开启后所有接口都返回 mock，**不调用真实后端**
const USE_MOCK: boolean =
  String(
    (typeof process !== 'undefined' && (process as any)?.env?.UNI_USE_MOCK) ||
    (import.meta as any)?.env?.VITE_USE_MOCK ||
    'false'
  ).toLowerCase() === 'true'

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

// 判断是否需要加密（与 web 端逻辑一致）
function shouldEncrypt(options: RequestOptions): boolean {
  if (!ENCRYPTION_ENABLED) return false
  const method = (options.method || 'GET').toUpperCase()
  if (method === 'GET') return false

  const url = (options.url || '').trim()
  if (url.startsWith('/public/')) return false
  if (url.includes('/common/upload')) return false
  if (url.startsWith('/ws/')) return false
  if (options.data === undefined || options.data === null) return false
  return true
}

export function request<T = any>(options: RequestOptions): Promise<ResponseData<T>> {
  return new Promise(async (resolve, reject) => {
    // 仅在显式开启 mock 模式时拦截
    const mockResponse = USE_MOCK ? buildMockResponse<T>(options.url, options.method || 'GET', options.data) : null

    if (options.loading !== false) {
      uni.showLoading({ title: '加载中...', mask: true })
    }

    // 自动从本地存储读取 token，附加到请求头
    const token = uni.getStorageSync('token') || ''
    const authHeader = token ? { Authorization: `Bearer ${token}` } : {}

    // ========== RSA+AES 信封加密：请求 ==========
    let payload: any = options.data
    const extraHeaders: Record<string, string> = {}
    if (shouldEncrypt(options) && options.data && Object.keys(options.data).length > 0) {
      try {
        payload = await encryptRequest(options.data)
        extraHeaders['X-Encrypted'] = '1'
      } catch (err) {
        console.warn('[encryption] 请求加密失败，降级为明文', err)
        payload = options.data
      }
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: payload,
      header: {
        'Content-Type': 'application/json',
        ...authHeader,
        ...extraHeaders,
        ...options.header
      },
      success: (res) => {
        if (options.loading !== false) uni.hideLoading()

        let response = res.data as ResponseData<T>

        // ========== RSA+AES 信封解密：响应 ==========
        if (response && (response as any).encrypted === true) {
          try {
            response = decryptResponse(response as any)
          } catch (err) {
            console.error('[encryption] 响应解密失败', err)
            uni.showToast({ title: '响应解密失败', icon: 'none' })
            reject({ code: 500, message: '响应解密失败', data: null, success: false } as any)
            return
          }
        }

        if (response && response.code === 200) {
          resolve(response)
        } else if (response && response.code === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.redirectTo({ url: '/pages/user/login' })
          reject(response)
        } else {
          resolve(response)
        }
      },
      fail: () => {
        if (options.loading !== false) uni.hideLoading()
        if (mockResponse) {
          setTimeout(() => resolve(mockResponse), 100)
        } else {
          reject({ code: 500, message: '网络异常', data: null, success: false } as any)
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
