const DEFAULT_DRONE_IMAGE = '/static/drones/mavic3_drone.png'
const DEFAULT_USER_AVATAR = '/static/icons/user.png'

export function resolveImageUrl(url: string | undefined | null): string {
  if (!url || url.length === 0) return DEFAULT_DRONE_IMAGE
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('//')) return 'http:' + url
  if (url.startsWith('/uploads/')) return '/static/drones/mavic3_drone.png'
  if (url.startsWith('/static/')) return url
  return url
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
