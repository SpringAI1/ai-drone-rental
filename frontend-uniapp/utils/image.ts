import { BASE_URL } from './request'

const IMAGE_BASE_URL = BASE_URL

const DEFAULT_DRONE_IMAGE = IMAGE_BASE_URL + '/uploads/mavic3_drone.png'
const DEFAULT_USER_AVATAR = IMAGE_BASE_URL + '/uploads/default_avatar.png'

export function resolveImageUrl(url: string | undefined | null): string {
  if (!url || url.length === 0) return DEFAULT_DRONE_IMAGE
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('//')) return 'http:' + url
  if (url.startsWith('/')) return IMAGE_BASE_URL + url
  return IMAGE_BASE_URL + '/' + url
}

export function resolveAvatarUrl(url: string | undefined | null): string {
  if (!url || url.length === 0) return DEFAULT_USER_AVATAR
  return resolveImageUrl(url)
}

export function getImageBaseUrl(): string {
  return IMAGE_BASE_URL
}

export default {
  resolveImageUrl,
  resolveAvatarUrl,
  getImageBaseUrl
}
