// MOCK=false 模式下会优先走真实后端接口；网络失败时才退化到本地模拟数据
export const USE_MOCK = false

// 开发环境：后端地址（H5 端走 Vite proxy `/api` → `http://localhost:8080`
export const BASE_URL = '/api'
// #ifndef H5
// 小程序/App 端不走代理，必须写完整地址
export const BASE_URL_NATIVE = 'http://localhost:8080/api'
// #endif

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

// 简单的本地备用 mock（仅在真实后端不可达 / MOCK=true 时触发），避免页面直接崩掉
const defaultStats = {
  totalDrones: 12,
  totalUsers: 238,
  totalOrders: 514,
  positiveRate: 97
}
const defaultDrones = [
  { id: 1, model: 'DJI Mavic 3', brand: 'DJI', type: '消费级', image: '/static/drones/mavic3_drone.png', pricePerDay: 299, stock: 5, status: 1, description: '专业航拍无人机' },
  { id: 2, model: 'DJI Mini 4 Pro', brand: 'DJI', type: '消费级', image: '/static/drones/mini4pro_drone.png', pricePerDay: 199, stock: 8, status: 1, description: '轻巧便携无人机' },
  { id: 3, model: 'DJI Air 2S', brand: 'DJI', type: '消费级', image: '/static/drones/air2s_drone.png', pricePerDay: 249, stock: 6, status: 1, description: '全能航拍无人机' }
]

function getToken(): string {
  try {
    return uni.getStorageSync('token') || ''
  } catch (e) {
    return ''
  }
}

function getBase(): string {
  // #ifdef H5
  return BASE_URL
  // #endif
  // #ifndef H5
  return BASE_URL_NATIVE
  // #endif
}

function buildFallbackMock<T>(url: string, method: string): ResponseData<T> | null {
  if (url === '/public/stats') {
    return { code: 200, message: 'success', data: defaultStats as unknown as T, success: true }
  }
  if (url === '/drone/list') {
    return { code: 200, message: 'success', data: { records: defaultDrones, total: defaultDrones.length, current: 1 } as unknown as T, success: true }
  }
  if (url === '/drone/brands') {
    return { code: 200, message: 'success', data: ['DJI', 'Autel', 'Parrot', 'Yuneec'] as unknown as T, success: true }
  }
  if (url === '/drone/types') {
    return { code: 200, message: 'success', data: ['消费级', '专业级', '行业级', '竞速机'] as unknown as T, success: true }
  }
  return { code: 200, message: 'success', data: null as unknown as T, success: true }
}

export function request<T = any>(options: RequestOptions): Promise<ResponseData<T>> {
  return new Promise((resolve, reject) => {
    // MOCK=true 时直接返回本地模拟数据，绝不再发网络请求
    if (USE_MOCK) {
      if (options.loading !== false) {
        uni.showLoading({ title: '加载中...', mask: true })
        setTimeout(() => uni.hideLoading(), 200)
      }
      const mockResponse = buildFallbackMock<T>(options.url, options.method || 'GET')
      setTimeout(() => {
        resolve(mockResponse!)
      }, 150)
      return
    }

    if (options.loading !== false) {
      uni.showLoading({ title: '加载中...', mask: true })
    }

    uni.request({
      url: getBase() + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': getToken() ? 'Bearer ' + getToken() : '',
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
          uni.showToast({ title: response?.message || '请求失败', icon: 'none' })
          reject(response)
        }
      },
      fail: () => {
        if (options.loading !== false) uni.hideLoading()
        // 网络失败：MOCK模式下给点面子，给一个保底数据
        const fallback = buildFallbackMock<T>(options.url, options.method || 'GET')
        if (fallback) {
          resolve(fallback)
        } else {
          uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
          reject({ code: 500, message: '网络异常', data: null, success: false })
        }
      }
    })
  })
}

export function get<T = any>(url: string, data?: Record<string, any>, loading = true): Promise<ResponseData<T>> {
  return request<T>({ url, method: 'GET', data, loading })
}

export function post<T = any>(url: string, data?: Record<string, any>, loading = true): Promise<ResponseData<T>> {
  return request<T>({ url, method: 'POST', data, loading })
}

export function put<T = any>(url: string, data?: Record<string, any>, loading = true): Promise<ResponseData<T>> {
  return request<T>({ url, method: 'PUT', data, loading })
}

export function del<T = any>(url: string, data?: Record<string, any>, loading = true): Promise<ResponseData<T>> {
  return request<T>({ url, method: 'DELETE', data, loading })
}

// 上传文件（头像/故障图等）：后端 `/common/upload` form-data `file`
export function uploadFile(filePath: string, name = 'file'): Promise<ResponseData<string>> {
  return new Promise((resolve, reject) => {
    uni.showLoading({ title: '上传中...', mask: true })
    uni.uploadFile({
      url: getBase() + '/common/upload',
      filePath,
      name,
      header: getToken() ? { 'Authorization': 'Bearer ' + getToken() } : {},
      success: (res) => {
        uni.hideLoading()
        try {
          const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (data && data.code === 200) {
            // 后端返回 { code:200, message: 'success', data: '/uploads/xxx.png' }
            resolve({ code: 200, message: 'success', data: String(data.data || ''), success: true })
          } else {
            uni.showToast({ title: data?.message || '上传失败', icon: 'none' })
            reject(data)
          }
        } catch (e) {
          // 后端返回纯文本/非JSON路径
          if (res.statusCode === 200) {
            resolve({ code: 200, message: 'success', data: String(res.data), success: true })
          } else {
            uni.showToast({ title: '上传失败', icon: 'none' })
            reject({ code: 500, message: '上传失败', data: null as any, success: false })
          }
        }
      },
      fail: () => {
        uni.hideLoading()
        uni.showToast({ title: '网络异常，上传失败', icon: 'none' })
        reject({ code: 500, message: '上传失败', data: null as any, success: false })
      }
    })
  })
}
