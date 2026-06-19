import { get, put, post, BASE_URL } from '../utils/request'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  phone: string
  email: string
  avatar: string
  address?: string
  balance: number
  creditScore: number
  role: number
  status: number
  qualificationStatus?: number
}

export interface UpdateUserParams {
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  address?: string
}

export interface UserQualification {
  id: number
  userId: number
  certificateNo: string
  certificateType: string
  certificateImage: string
  validStartDate: string
  validEndDate: string
  auditStatus: number
  auditRemark?: string
  auditTime?: string
  createdTime?: string
  updatedTime?: string
}

export interface SubmitQualificationParams {
  certificateNo: string
  certificateType: string
  certificateImage: string
  validStartDate: string
  validEndDate: string
}

export function getUserInfo() {
  return get<UserInfo>('/user/info')
}

export function updateUserInfo(data: UpdateUserParams) {
  return put('/user/info', data)
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return put('/user/password', data)
}

export function recharge(amount: number) {
  return post('/user/recharge', {}, { amount: amount })
}

export function getQualification() {
  return get<UserQualification | null>('/user/qualification')
}

export function submitQualification(data: SubmitQualificationParams) {
  return post<UserQualification>('/user/qualification', data)
}

export function uploadFile(filePath: string, _type: string = 'common') {
  return new Promise<{ data: any }>((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
      url: BASE_URL + '/common/upload',
      filePath: filePath,
      name: 'file',
      header: {
        Authorization: token ? `Bearer ${token}` : ''
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
