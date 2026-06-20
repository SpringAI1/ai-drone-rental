import { post, get, del, uploadFile as doUploadFile } from '../utils/request'

export interface AddCommentParams {
  droneId: number
  orderId?: number
  content: string
  rating: number
  images?: string[]
  parentId?: number | null
}

export function addComment(data: AddCommentParams) {
  const payload: any = {
    droneId: data.droneId,
    content: data.content,
    rating: data.rating
  }
  if (data.orderId) payload.orderId = data.orderId
  if (data.parentId) payload.parentId = data.parentId
  if (data.images && data.images.length > 0) {
    payload.images = data.images
  }
  return post<void>('/comment/add', payload)
}

export function getMyComments(params?: { pageNum?: number; pageSize?: number }) {
  return get<any>('/comment/my', params)
}

export function deleteComment(id: number) {
  return del<void>(`/comment/${id}`)
}

// 统一的图片上传：后端返回 /uploads/xxx.png
export async function uploadFile(filePath: string, _type: string = 'comment'): Promise<{ data: string }> {
  const res = await doUploadFile(filePath, 'file')
  return { data: String(res.data || '') }
}

export async function uploadCommentImage(filePath: string): Promise<string> {
  const res = await uploadFile(filePath, 'comment')
  return res.data
}
