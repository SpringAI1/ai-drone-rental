import { get } from './request'

export const getPublicStats = () => {
  return get('/public/stats')
}