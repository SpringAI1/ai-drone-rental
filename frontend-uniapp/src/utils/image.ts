// 默认兜底图片（本地静态资源始终可用）
const DEFAULT_DRONE_IMAGE = '/static/drones/mavic3_drone.png'
const DEFAULT_USER_AVATAR = '/static/icons/user.png'

// H5 开发/生产时：后端 uploads 需要走 /api/uploads（由 Vite 代理转发到 http://localhost:8080）
// 非 H5（小程序/App）：直接拼接 BASE_URL_NATIVE 的 host（http://localhost:8080/uploads/xxx）
// 注意：这里通过字符串拼接的方式，保持运行期一致（不依赖编译常量）
const H5_UPLOADS_PREFIX = '/api'

function resolveUploadUrl(url: string): string {
  // #ifdef H5
  return H5_UPLOADS_PREFIX + url
  // #endif
  // #ifndef H5
  return 'http://localhost:8080/api' + url
  // #endif
}

/**
 * 统一处理图片 URL
 * - 空值 -> 返回默认无人机图片
 * - http(s):// 开头 -> 原样返回
 * - // 开头 -> 补 http:
 * - /uploads/ 开头 -> 通过代理（H5）或直连后端（非 H5）访问
 * - /static/ 开头 -> 原样返回（本地静态资源）
 * - 其他 -> 原样返回
 */
export function resolveImageUrl(url: string | undefined | null): string {
  if (!url || url.length === 0) return DEFAULT_DRONE_IMAGE
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('//')) return 'http:' + url
  if (url.startsWith('/uploads/')) return resolveUploadUrl(url)
  if (url.startsWith('/static/')) return url
  // 其它路径（如相对路径）：当作 /uploads/ 处理
  if (!url.startsWith('/')) return resolveUploadUrl('/uploads/' + url)
  return url
}

export function resolveAvatarUrl(url: string | undefined | null): string {
  if (!url || url.length === 0) return DEFAULT_USER_AVATAR
  return resolveImageUrl(url)
}

export function getImageBaseUrl(): string {
  // #ifdef H5
  return '/api/uploads/'
  // #endif
  // #ifndef H5
  return 'http://localhost:8080/api/uploads/'
  // #endif
}

export default {
  resolveImageUrl,
  resolveAvatarUrl,
  getImageBaseUrl
}
