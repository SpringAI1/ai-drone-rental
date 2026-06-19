import { get, post, put } from './request'

// 用户端
export const submitAirspaceRecord = (data) => {
  return post('/airspace/submit', data)
}

export const getAirspaceList = () => {
  return get('/airspace/list')
}

export const getApprovedAirspace = () => {
  return get('/airspace/approved')
}

// 管理员端
export const getAdminAirspaceList = (params) => {
  return get('/admin/airspace/list', params)
}

export const getAdminAirspaceDetail = (id) => {
  return get(`/admin/airspace/${id}`)
}

export const approveAirspace = (id) => {
  return put(`/admin/airspace/${id}/audit`, { auditStatus: 1, auditRemark: '审核通过' })
}

export const rejectAirspace = (id, reason) => {
  return put(`/admin/airspace/${id}/audit`, { auditStatus: 2, auditRemark: reason })
}