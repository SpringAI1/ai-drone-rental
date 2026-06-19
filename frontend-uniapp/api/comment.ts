import { post, get, del, BASE_URL } from '../utils/request'

export interface AddCommentParams {
  droneId: number
  orderId?: number
  content: string
  rating: number
  images?: string[]
  imagesJson?: string
  parentId?: number | null
}

export function addComment(data: AddCommentParams) {
  // 后端接收 CommentDTO: droneId, orderId, content, rating, images(JSON字符串), parentId
  const payload: any = {
    droneId: data.droneId,
    content: data.content,
    rating: data.rating
  }
  if (data.orderId) payload.orderId = data.orderId
  if (data.parentId) payload.parentId = data.parentId
  if (data.imagesJson) {
    payload.images = data.imagesJson
  } else if (data.images && data.images.length > 0) {
    payload.images = JSON.stringify(data.images)
  }
  return post('/comment/add', payload)
}

export function getMyComments(params?: { pageNum?: number; pageSize?: number }) {
  return get<{ records: any[]; total: number }>('/comment/my', params)
}

export function deleteComment(id: number) {
  return del(`/comment/${id}`)
}

export function uploadFile(filePath: string, type: string = 'comment') {
  return new Promise<{ data: any }>((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
      url: BASE_URL + '/common/upload',
      filePath: filePath,
      name: 'file',
      formData: { type: type },
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data) as any
          if (data.code === 200) {
            resolve({ data: data.data })
          } else {
            uni.showToast({ title: data.message || '上传失败', icon: 'none' })
            reject(data)
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}