import { get, put } from './request'

export const getNotifications = (params) => {
  return get('/notification/list', params)
}

export const getUnreadCount = () => {
  return get('/notification/unread-count')
}

export const markAsRead = (id) => {
  return put(`/notification/${id}/read`)
}

export const markAllAsRead = () => {
  return put('/notification/read-all')
}

// ========== 管理员 ==========
export const getAdminNotifications = (params) => {
  return get('/notification/admin/list', params)
}

export const getAdminUnreadCount = () => {
  return get('/notification/admin/unread-count')
}

export const adminMarkAsRead = (id) => {
  return put(`/notification/admin/${id}/read`)
}

export const adminMarkAllAsRead = () => {
  return put('/notification/admin/read-all')
}