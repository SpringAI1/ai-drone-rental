// 本地静态资源：uniapp 打包后会内置这些图，h5/小程序都能用，不依赖后端服务
const DEFAULT_DRONE_IMAGE = '/static/drones/mavic3_drone.png'
const DEFAULT_USER_AVATAR = '/static/icons/user.png'

// uniapp 内置的所有无人机图（这些图在 /static/drones/ 里有本地副本）
const LOCAL_DRONE_FILES = new Set([
  'mavic3_drone.png',
  'mini3pro_drone.png',
  'air2s_drone.png',
  'inspire2_drone.png',
  'phantom4pro_drone.png',
  'agrast40_drone.png',
  'mavic3pro_drone.png',
  'mini4pro_drone.png',
  'cert1.png',
  'cert_1.png',
  'cert_2.png',
  'cert_3.png'
])

// API 基础地址（仅在 Web/H5 端访问后端时使用）
// - 小程序端：不要用后端地址
// - H5 端：默认走后端 8080
// #ifdef H5
const API_BASE_URL = 'http://localhost:8080'
// #endif
// #ifndef H5
const API_BASE_URL = ''
// #endif

function getFilename(url: string): string {
  const idx = url.lastIndexOf('/')
  return idx >= 0 ? url.substring(idx + 1) : url
}

/**
 * 解析图片 URL
 * 优先级：
 *   1. 已是 http(s) 链接 → 原样返回
 *   2. /static/...      → 原样返回（uniapp 内置）
 *   3. /api/uploads/x 或 /uploads/x：
 *      - 文件名在本地副本列表里 → /static/drones/<filename> （H5/小程序都可用）
 *      - 否则 → 拼 API_BASE_URL 走后端
 *   4. 其他 → 当成裸文件名，优先本地，否则走后端
 */
export function resolveImageUrl(url: string | undefined | null): string {
  if (!url || url.length === 0) return DEFAULT_DRONE_IMAGE
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('//')) return 'https:' + url
  if (url.startsWith('/static/')) return url

  const filename = getFilename(url)

  // /api/uploads/x 或 /uploads/x
  if (url.startsWith('/api/uploads/') || url.startsWith('/uploads/')) {
    if (LOCAL_DRONE_FILES.has(filename)) {
      return '/static/drones/' + filename
    }
    return API_BASE_URL + url
  }

  // 裸文件名
  if (LOCAL_DRONE_FILES.has(filename)) {
    return '/static/drones/' + filename
  }
  return API_BASE_URL + '/uploads/' + url
}

export function resolveAvatarUrl(url: string | undefined | null): string {
  if (!url || url.length === 0) return DEFAULT_USER_AVATAR
  return resolveImageUrl(url)
}

export function getImageBaseUrl(): string {
  return '/static/drones/'
}

export default {
  resolveImageUrl,
  resolveAvatarUrl,
  getImageBaseUrl
}
