/**
 * RSA + AES 混合加密工具（信封加密）
 * 按文档实现：
 *   1. 从后端获取 RSA 公钥（/api/public/rsa-key）并缓存
 *   2. 每次请求本地随机生成 AES-256 密钥
 *   3. AES 加密请求参数 JSON
 *   4. RSA 公钥加密 AES 密钥
 *   5. 发送 { encryptedKey, encryptedData }
 *   6. 后端响应 { encrypted: true, data: AES密文 }，用缓存的 AES 密钥解密
 *
 * 依赖：
 *   - crypto-js  —— AES-CBC / Base64 / SHA256
 *   - jsencrypt  —— RSA 公钥加密
 *   - axios       —— 用于请求公钥
 *
 * 使用方式：
 *   import { encryptRequest, decryptResponse } from '@/utils/encryption'
 *   const { encryptedKey, encryptedData } = await encryptRequest({ password: '123456' })
 */

import CryptoJS from 'crypto-js'
import { JSEncrypt } from 'jsencrypt'

// ============== 缓存 ==============

// 缓存 RSA 公钥（PEM 字符串）
let cachedPublicKey = null
// 当前正在使用的 AES 密钥（Base64），每发一个请求生成新的，后端用同一密钥加密响应
let currentAesKey = null
// 缓存 key 轮询获取 promise，防止并发下重复拉
let fetchingPromise = null

// ============== 1. 获取 RSA 公钥 ==============

/**
 * 从后端获取 RSA 公钥。只在第一次或缓存过期时真正请求。
 * @param {boolean} [force=false] 强制刷新
 */
export async function getPublicKey(force = false) {
  if (!force && cachedPublicKey) return cachedPublicKey
  if (fetchingPromise && !force) return fetchingPromise

  fetchingPromise = (async () => {
    try {
      const res = await fetch('/api/public/rsa-key')
      const json = await res.json()
      if (json.code === 200 && json.data) {
        // 优先使用 PEM 格式（jsencrypt 直接兼容），其次用 Base64 手动包裹
        cachedPublicKey = json.data.publicKeyPem
          ? json.data.publicKeyPem
          : wrapAsPem(json.data.publicKey)
        return cachedPublicKey
      }
      throw new Error('获取公钥失败')
    } catch (err) {
      console.error('[encryption] 获取 RSA 公钥失败', err)
      cachedPublicKey = null
      throw err
    }
  })()

  try {
    await fetchingPromise
  } finally {
    // 24 小时后失效，强制刷新一次
    setTimeout(() => { cachedPublicKey = null }, 24 * 60 * 60 * 1000)
  }
  return cachedPublicKey
}

function wrapAsPem(base64Key) {
  return '-----BEGIN PUBLIC KEY-----\n'
    + base64Key.replace(/(.{64})/g, '$1\n')
    + '\n-----END PUBLIC KEY-----'
}

// ============== 2. 生成 AES-256 密钥 ==============

/**
 * 生成 32 字节 (256位) 的随机 AES 密钥（返回 Base64）
 */
function generateAesKey() {
  const keyWords = CryptoJS.lib.WordArray.random(32) // 32 字节 = 256位
  const keyBase64 = CryptoJS.enc.Base64.stringify(keyWords)
  return keyBase64
}

/**
 * 生成 16 字节 (128位) 的随机 IV（返回 Base64）
 */
function generateIv() {
  const ivWords = CryptoJS.lib.WordArray.random(16)
  return CryptoJS.enc.Base64.stringify(ivWords)
}

// ============== 3. AES 加解密 ==============

/**
 * AES-256-CBC 加密明文 JSON 字符串
 * 输出格式（与后端 EncryptionService 对应）：
 *   Base64( IV(16字节) || AES_CIPHERTEXT )
 */
function aesEncrypt(plainText, keyBase64) {
  const key = CryptoJS.enc.Base64.parse(keyBase64)
  const iv = CryptoJS.lib.WordArray.random(16)
  const encrypted = CryptoJS.AES.encrypt(
    CryptoJS.enc.Utf8.parse(plainText),
    key,
    {
      iv: iv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  )

  // 把 IV + 密文合并，然后 Base64 编码
  const ivCiphertext = iv.concat(encrypted.ciphertext)
  return CryptoJS.enc.Base64.stringify(ivCiphertext)
}

/**
 * AES-256-CBC 解密
 * 输入：Base64(IV(16字节) + 密文)
 */
function aesDecrypt(cipherTextBase64, keyBase64) {
  const key = CryptoJS.enc.Base64.parse(keyBase64)
  const raw = CryptoJS.enc.Base64.parse(cipherTextBase64)

  // 前 16 字节 = IV
  const ivWords = CryptoJS.lib.WordArray.create(raw.words.slice(0, 4)) // 16B = 4 words
  // 剩余 = 密文
  const cipherWords = CryptoJS.lib.WordArray.create(raw.words.slice(4), raw.sigBytes - 16)

  const decrypted = CryptoJS.AES.decrypt(
    { ciphertext: cipherWords, salt: null },
    key,
    {
      iv: ivWords,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  )
  return CryptoJS.enc.Utf8.stringify(decrypted)
}

// ============== 4. RSA 加密 AES 密钥 ==============

/**
 * 用后端 RSA 公钥加密 AES 密钥（32 字节，Base64 -> 原始字节加密后再 Base64）
 */
function rsaEncrypt(aesKeyBase64, publicKeyPem) {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKeyPem)
  // AES 密钥长度仅 32 字节 (< RSA-2048 单次加密上限 245B)，一次搞定
  const encrypted = encrypt.encrypt(aesKeyBase64)
  if (!encrypted) {
    throw new Error('RSA 加密 AES 密钥失败（公钥可能无效或长度不匹配）')
  }
  return encrypted
}

// ============== 5. 对外接口：加密请求 / 解密响应 ==============

/**
 * 加密请求数据（核心）
 * @param {Object} params  业务参数对象（会被 JSON.stringify）
 * @returns {{ encryptedKey: string, encryptedData: string }}
 */
export async function encryptRequest(params) {
  // 1. 拿公钥
  const publicKey = await getPublicKey()

  // 2. 生成 AES 密钥
  const aesKey = generateAesKey()
  currentAesKey = aesKey

  // 3. AES 加密请求参数
  const plainText = typeof params === 'string' ? params : JSON.stringify(params)
  const encryptedData = aesEncrypt(plainText, aesKey)

  // 4. RSA 加密 AES 密钥
  const encryptedKey = rsaEncrypt(aesKey, publicKey)

  return { encryptedKey, encryptedData }
}

/**
 * 解密响应数据
 * @param {{ encrypted: boolean, data: string }} payload
 */
export function decryptResponse(payload) {
  if (!payload || !payload.encrypted || !payload.data) {
    // 非加密响应，直接返回 payload
    return payload
  }
  if (!currentAesKey) {
    console.warn('[encryption] 响应是加密的，但没有缓存的 AES 密钥')
    return payload
  }
  try {
    const plainText = aesDecrypt(payload.data, currentAesKey)
    return JSON.parse(plainText)
  } catch (err) {
    console.error('[encryption] 响应解密失败', err)
    return payload
  }
}

/**
 * 工具方法：仅加密特定字段（选择性加密）
 * 用法：const body = encryptFields({ username:'xxx', password:'123' }, ['password'])
 * @param {Object} obj 原始对象
 * @param {string[]} fields 需要加密的字段名
 */
export async function encryptFields(obj, fields) {
  if (!obj || !fields || fields.length === 0) return obj
  const publicKey = await getPublicKey()
  const result = { ...obj }
  for (const f of fields) {
    if (result[f] !== undefined && result[f] !== null && result[f] !== '') {
      const aesKey = generateAesKey()
      const ed = aesEncrypt(String(result[f]), aesKey)
      const ek = rsaEncrypt(aesKey, publicKey)
      result[f] = `ENC::${ek}::${ed}` // 前缀便于后端识别，不过本项目默认走请求级加密，不是字段级
    }
  }
  return result
}

/**
 * 手动清理当前 AES key（一般不需要，新请求会覆盖）
 */
export function clearAesKey() {
  currentAesKey = null
}

// 方便调试时开关
export const ENCRYPTION_ENABLED = true
