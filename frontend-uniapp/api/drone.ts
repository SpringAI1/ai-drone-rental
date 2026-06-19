import { get } from '../utils/request'

export interface DroneSpecifications {
  weight?: string
  maxSpeed?: string
  maxAltitude?: string
  batteryLife?: string
  flightTime?: string
  maxPayload?: string
  maxRange?: string
}

export interface DroneItem {
  id: number
  model: string
  brand: string
  type: string
  image: string
  images?: string[]
  price: number
  pricePerDay: number
  deposit: number
  stock: number
  status: number
  description: string
  specifications?: DroneSpecifications
  rentalCount?: number
  rating?: number
}

export interface DroneDetail extends DroneItem {
  images: string[]
  rentalCount: number
  rating: number
}

export interface CommentItem {
  id: number
  userId: number
  userNickname: string
  userAvatar: string
  content: string
  rating: number
  images: string[]
  children: {
    id: number
    userId: number
    userNickname: string
    userAvatar: string
    content: string
    parentId: number
    createTime: string
  }[]
  createTime: string
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
