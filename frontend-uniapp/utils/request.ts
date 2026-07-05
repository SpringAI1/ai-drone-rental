import { encryptRequest, decryptResponse, ENCRYPTION_ENABLED } from './encryption'

// API 基础地址（支持 .env 配置）
//   HBuilderX 运行时：可配置 .env.* 文件中的 UNI_API_BASE_URL
//   CLI 构建时：可配置 .env.* 文件中的 VITE_API_BASE_URL
//   优先级：UNI_API_BASE_URL > VITE_API_BASE_URL > 'http://localhost:8080/api'
const API_BASE_URL =
  (typeof process !== 'undefined' && (process as any)?.env?.UNI_API_BASE_URL) ||
  (import.meta as any)?.env?.VITE_API_BASE_URL ||
  'http://localhost:8080/api'
export const BASE_URL = API_BASE_URL

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
          // 非 200 响应统一 reject，让调用方处理错误
          reject(response)
        }
      },
      fail: (err) => {
        if (options.loading !== false) uni.hideLoading()
        console.error('[request] 请求失败', err, options.url)
        reject({ code: 500, message: '网络异常，请检查后端服务是否启动', data: null, success: false } as any)
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
