import { get } from '../utils/request'

export interface StatsResult {
  totalDrones: number
  totalOrders: number
  totalUsers: number
  totalComments?: number
  positiveRate?: number
}

export function getStats() {
  return get<StatsResult>('/public/stats', {}, false)
}
