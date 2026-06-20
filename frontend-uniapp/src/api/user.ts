import { get, put, post, uploadFile as doUploadFile } from '../utils/request'

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  address?: string
  balance?: number
  creditScore?: number
  role?: number
  status?: number
  verificationStatus?: number
  qualificationStatus?: number
}

export interface UpdateUserParams {
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  address?: string
}

export function getUserInfo() {
  return get<UserInfo>('/user/info')
}

export function updateUserInfo(data: UpdateUserParams) {
  return put<UserInfo>('/user/info', data)
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return put<UserInfo>('/user/password', data)
}

export function recharge(amount: number) {
  return post<UserInfo>('/user/recharge', { amount })
}

export function getQualification() {
  return get<any>('/user/qualification')
}

export function submitQualification(data: { certificateNo?: string; certificateType?: string; certificateImage?: string; validStartDate?: string; validEndDate?: string }) {
  return post<any>('/user/qualification', data)
}

// 统一的上传方法（向后兼容：页面里有 uploadFile(filePath, 'avatar') 的调用）
export async function uploadFile(filePath: string, _type: string = 'common'): Promise<{ data: string }> {
  const res = await doUploadFile(filePath, 'file')
  const url = String(res.data || '')
  return { data: url }
}

export async function uploadAvatar(filePath: string): Promise<string> {
  const res = await uploadFile(filePath, 'avatar')
  return res.data
}
