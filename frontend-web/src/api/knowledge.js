import { get, post, del, upload } from './request'

/**
 * 知识库管理 API（管理端 Web）
 * 对应后端 KbController: /ai/v3/knowledge
 */

/** 文档列表（当前用户可见：公共 + 我的私有） */
export function listVisible() {
  return get('/ai/v3/knowledge/list')
}

/** 我上传的文档 */
export function listMine() {
  return get('/ai/v3/knowledge/mine')
}

/** 公共知识库 */
export function listPublic() {
  return get('/ai/v3/knowledge/public')
}

/** 文档详情 */
export function getDetail(id) {
  return get(`/ai/v3/knowledge/detail/${id}`)
}

/** 上传文档（multipart） */
export function uploadKb(file, scope = 'private') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scope', scope)
  return upload('/ai/v3/knowledge/upload', formData)
}

/** 删除文档 */
export function remove(id) {
  return del(`/ai/v3/knowledge/${id}`)
}

/** 语义检索 */
export function search(query, topK = 4) {
  return get('/ai/v3/knowledge/search', { query, topK })
}

/** 向量库统计 */
export function stats() {
  return get('/ai/v3/knowledge/stats')
}
