import { post, get } from '../utils/request'

export interface FaultReportParams {
  droneId: number
  orderId?: number
  faultType?: string
  faultDescription: string
  faultImages?: string
  faultTime?: string
}

export interface FaultReportItem {
  id: number
  reportNo: string
  droneId: number
  droneBrand?: string
  droneModel?: string
  orderId?: number
  orderNo?: string
  faultType: string
  faultDescription: string
  faultImages?: string
  faultTime?: string
  auditStatus: number
  auditRemark?: string
  auditTime?: string
  createdTime: string
}

export function reportFault(data: FaultReportParams) {
  return post('/fault/report', data)
}

export function getFaultList(params?: { pageNum?: number; pageSize?: number }) {
  return get<{ records: FaultReportItem[]; total: number }>('/fault/my', params)
}

export function getFaultByOrder(orderId: number) {
  return get<FaultReportItem[]>(`/fault/order/${orderId}`)
}
