/**
 * uniapp 端 RSA+AES 混合加密工具
 * 与 web 端加密逻辑完全一致，只是请求走 uni.request 而非 axios
 *
 * 依赖：
 *   crypto-js  (npm i crypto-js)
 *   jsencrypt   (npm i jsencrypt)
 *
 * 注意：微信小程序等环境对 crypto 的支持有限，
 * 请使用 "构建 npm" 或把这两个库拷贝到项目里。
 * H5 / App 可直接 npm install 使用。
 *
 * 若因小程序环境限制不能加密，调用方会自动降级为明文。
 */

import CryptoJS from 'crypto-js'
import { JSEncrypt } from 'jsencrypt'

import { BASE_URL } from './request'
// 降级默认值（与 request.ts 保持一致）
const FALLBACK_BASE_URL = 'http://localhost:8080/api'

let cachedPublicKey = null
let currentAesKey = null
let fetchingPromise = null

/**
 * 获取 API 基础地址
 */
function getBaseUrl() {
  try { return BASE_URL } catch (e) { return FALLBACK_BASE_URL }
}

/**
 * 获取 RSA 公钥（uniapp 专用，走 uni.request）
 */
export async function getPublicKey(force = false) {
  if (!force && cachedPublicKey) return cachedPublicKey
  if (fetchingPromise && !force) return fetchingPromise

  fetchingPromise = new Promise((resolve, reject) => {
    uni.request({
      url: getBaseUrl() + '/public/rsa-key',
      method: 'GET',
      success: (res) => {
        const json = res.data
        if (json && json.code === 200 && json.data) {
          cachedPublicKey = json.data.publicKeyPem
            ? json.data.publicKeyPem
            : wrapAsPem(json.data.publicKey)
          resolve(cachedPublicKey)
        } else {
          reject(new Error('获取公钥失败'))
        }
      },
      fail: (err) => {
        console.error('[encryption] 获取 RSA 公钥失败', err)
        cachedPublicKey = null
        reject(err)
      }
    })
  })

  try {
    await fetchingPromise
  } finally {
    setTimeout(() => { cachedPublicKey = null }, 24 * 60 * 60 * 1000)
  }
  return cachedPublicKey
}

function wrapAsPem(base64Key) {
  return '-----BEGIN PUBLIC KEY-----\n'
    + base64Key.replace(/(.{64})/g, '$1\n')
    + '\n-----END PUBLIC KEY-----'
}

function generateAesKey() {
  const keyWords = CryptoJS.lib.WordArray.random(32)
  return CryptoJS.enc.Base64.stringify(keyWords)
}

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
  const ivCiphertext = iv.concat(encrypted.ciphertext)
  return CryptoJS.enc.Base64.stringify(ivCiphertext)
}

function aesDecrypt(cipherTextBase64, keyBase64) {
  const key = CryptoJS.enc.Base64.parse(keyBase64)
  const raw = CryptoJS.enc.Base64.parse(cipherTextBase64)
  const ivWords = CryptoJS.lib.WordArray.create(raw.words.slice(0, 4))
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

function rsaEncrypt(aesKeyBase64, publicKeyPem) {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKeyPem)
  const encrypted = encrypt.encrypt(aesKeyBase64)
  if (!encrypted) {
    throw new Error('RSA 加密 AES 密钥失败')
  }
  return encrypted
}

export async function encryptRequest(params) {
  const publicKey = await getPublicKey()
  const aesKey = generateAesKey()
  currentAesKey = aesKey

  const plainText = typeof params === 'string' ? params : JSON.stringify(params)
  const encryptedData = aesEncrypt(plainText, aesKey)
  const encryptedKey = rsaEncrypt(aesKey, publicKey)

  return { encryptedKey, encryptedData }
}

export function decryptResponse(payload) {
  if (!payload || !payload.encrypted || !payload.data) {
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

export function clearAesKey() {
  currentAesKey = null
}

export const ENCRYPTION_ENABLED = true
