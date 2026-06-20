import { post, get } from '../utils/request'

export interface AirspaceRecord {
  id: number
  userId: number
  regionName: string
  regionAddress?: string
  longitude?: number
  latitude?: number
  radius?: number
  maxAltitude: number
  plannedStartTime: string
  plannedEndTime: string
  purpose?: string
  auditStatus: number
  auditRemark?: string
  createdTime: string
}

export interface SubmitAirspaceParams {
  regionName: string
  regionAddress?: string
  longitude?: number
  latitude?: number
  radius?: number
  maxAltitude: number
  plannedStartTime: string
  plannedEndTime: string
  purpose?: string
}

export function submitAirspace(data: SubmitAirspaceParams) {
  return post('/airspace/submit', data)
}

export function getAirspaceList() {
  return get<AirspaceRecord[]>('/airspace/list')
}

export function getApprovedAirspaces() {
  return get<AirspaceRecord[]>('/airspace/approved')
}
