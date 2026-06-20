import { get } from '../utils/request'

// 与后端 Drone 实体字段对齐：id, model, brand, type, description, image,
// pricePerDay, stock, flightTime, maxPayload, maxSpeed, maxRange, status, onShelf
export interface DroneItem {
  id: number
  model: string
  brand: string
  type: string
  image: string
  pricePerDay: number
  // 兼容字段（前端展示用，后端未返回时用默认值）
  price?: number
  deposit?: number
  stock?: number
  status?: number
  onShelf?: number
  description?: string
  flightTime?: number
  maxPayload?: number
  maxSpeed?: number
  maxRange?: number
  rentalCount?: number
  rating?: number
}

export interface DroneDetail extends DroneItem {
  // 预留多图展示（后端目前只有 image 一个字段）
  images?: string[]
}

export interface CommentItem {
  id: number
  userId?: number
  userNickname?: string
  userAvatar?: string
  content: string
  rating: number
  images?: string[]
  createTime?: string
}

export interface ListParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number
  brand?: string
  type?: string
  minPrice?: number
  maxPrice?: number
  sortBy?: string
  sortOrder?: string
}

export function getDroneList(params?: ListParams) {
  return get<{ records: DroneItem[]; total: number; current: number }>('/drone/list', params)
}

export function getDroneDetail(id: number) {
  return get<DroneDetail>(`/drone/detail/${id}`)
}

export function getDroneComments(droneId: number, pageNum = 1, pageSize = 10) {
  return get<{ records: CommentItem[]; total: number }>(`/drone/${droneId}/comments`, { pageNum, pageSize })
}

export function getBrands() {
  return get<string[]>('/drone/brands', {}, false)
}

export function getTypes() {
  return get<string[]>('/drone/types', {}, false)
}
