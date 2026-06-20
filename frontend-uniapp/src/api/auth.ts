import { post } from '../utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  phone: string
  email?: string
}

export interface LoginResult {
  userId: number
  username: string
  nickname: string
  role: number
  token: string
}

export function login(data: LoginParams) {
  return post<LoginResult>('/auth/login', data)
}

export function register(data: RegisterParams) {
  return post('/auth/register', data)
}

export function adminLogin(data: LoginParams) {
  return post<LoginResult>('/auth/admin/login', data)
}