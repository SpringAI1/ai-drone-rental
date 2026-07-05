import { get, post, del, BASE_URL } from '../utils/request'

export interface KbDocument {
  id: number
  userId: number
  scope: 'public' | 'private'
  filename: string
  fileType: string
  fileSize: number
  filePath?: string
  chunkCount: number
  charCount: number
  status: number
  errorMessage?: string
  createdTime: string
  updatedTime: string
}

export interface KbSearchHit {
  id: string
  text: string
  source?: string
  scope?: string
  documentId?: number
  userId?: number
  score?: number
}

export interface KbStats {
  totalChunks: number
  publicChunks: number
  privateChunks: number
}

/** 列表 - 当前用户可见的文档（公共 + 我私有） */
export function listVisible() {
  return get<KbDocument[]>('/ai/v3/knowledge/list')
}

/** 我上传的 */
export function listMine() {
  return get<KbDocument[]>('/ai/v3/knowledge/mine')
}

/** 公共知识库 */
export function listPublic() {
  return get<KbDocument[]>('/ai/v3/knowledge/public')
}

/** 详情 */
export function getDetail(id: number) {
  return get<KbDocument>(`/ai/v3/knowledge/detail/${id}`)
}

/** 删除 */
export function remove(id: number) {
  return del<void>(`/ai/v3/knowledge/${id}`)
}

/** 向量库统计 */
export function stats() {
  return get<KbStats>('/ai/v3/knowledge/stats')
}

/** 语义检索 */
export function search(query: string, topK = 4) {
  return get<KbSearchHit[]>('/ai/v3/knowledge/search', { query, topK })
}

/**
 * 上传文档（multipart/form-data，scope 可选 private/public）
 * 后端 EncryptionFilter 已跳过 multipart
 */
export function uploadKb(filePath: string, scope: 'private' | 'public' = 'private') {
  return new Promise<{ data: KbDocument }>((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
      url: BASE_URL + '/ai/v3/knowledge/upload',
      filePath,
      name: 'file',
      formData: { scope },
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data) as any
          if (data.code === 200) {
            resolve({ data: data.data as KbDocument })
          } else {
            uni.showToast({ title: data.message || '上传失败', icon: 'none' })
            reject(data)
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: (err) => reject(err)
    })
  })
}

/** 文件大小格式化为可读字符串 */
export function formatFileSize(bytes: number): string {
  if (!bytes || bytes < 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
