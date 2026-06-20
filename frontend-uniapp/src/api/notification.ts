import { get, put } from '../utils/request'

export interface NotificationItem {
  id: number
  type: number
  title: string
  content: string
  readStatus: number
  createdTime: string
  businessId?: number
  userId?: number
}

export function getNotificationList(params?: { pageNum?: number; pageSize?: number }) {
  return get<{ records: NotificationItem[]; total: number }>('/notification/list', params)
}

export function getUnreadCount() {
  return get<{ count: number }>('/notification/unread-count', {}, false)
}

export function markAsRead(id: number) {
  return put(`/notification/${id}/read`)
}

export function markAllAsRead() {
  return put('/notification/read-all')
}
