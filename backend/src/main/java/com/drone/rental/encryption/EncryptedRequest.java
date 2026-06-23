package com.drone.rental.encryption;

import lombok.Data;

/**
 * 加密请求体。格式（前端发送）：
 * {
 *   "encryptedKey": "Base64(RSA公钥加密AES密钥)",
 *   "encryptedData": "Base64(IV(16字节) + AES加密后的JSON参数)"
 * }
 * <p>
 * 加密响应体格式（后端返回）：
 * {
 *   "encrypted": true,
 *   "data": "Base64(IV(16字节) + AES加密后的JSON响应)"
 * }
 */
@Data
public class EncryptedRequest {
    /**
     * 密钥密文：用后端公钥加密的 AES 密钥 (Base64)
     */
    private String encryptedKey;

    /**
     * 请求数据密文：用 AES 加密后的 JSON 业务参数 (Base64)
     * 前 16 字节为 IV，之后才是真实密文
     */
    private String encryptedData;
}
