package com.drone.rental.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA+AES 混合加密服务 (信封加密)
 * <p>
 * 核心流程（按文档）：
 * 1. 后端生成 RSA 密钥对，通过接口将公钥下发给前端
 * 2. 前端本地随机生成 AES 密钥，用 AES 加密请求参数得到请求密文
 * 3. 前端用后端公钥加密 AES 密钥，得到密钥密文
 * 4. 前端将 请求密文 + 密钥密文 一并发给后端
 * 5. 后端先用 RSA 私钥解密钥密文得到 AES 密钥
 * 6. 后端用 AES 密钥解密请求密文，执行业务逻辑
 * 7. 后端复用同一 AES 密钥加密响应数据，仅返回响应密文
 * 8. 前端用本地缓存的 AES 密钥解密响应
 */
@Slf4j
@Component
public class EncryptionService {

    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256; // AES-256
    private static final int IV_SIZE = 16;       // AES-CBC IV 固定16字节

    private PrivateKey rsaPrivateKey;
    private String rsaPublicKeyBase64;

    // 线程本地变量：存放当前请求-响应周期的 AES 密钥
    private static final ThreadLocal<String> currentAesKey = new ThreadLocal<>();

    public EncryptionService() {
        try {
            generateRsaKeyPair();
            log.info("加密服务初始化完成，RSA-2048 密钥对已生成");
        } catch (Exception e) {
            log.error("加密服务初始化失败", e);
            throw new RuntimeException("RSA 密钥生成失败", e);
        }
    }

    /**
     * 生成 RSA 密钥对
     */
    private synchronized void generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(RSA_KEY_SIZE, new SecureRandom());
        KeyPair keyPair = kpg.generateKeyPair();
        this.rsaPrivateKey = keyPair.getPrivate();
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        this.rsaPublicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyBytes);
    }

    /**
     * 获取 RSA 公钥 (Base64，X.509 格式)
     */
    public String getPublicKey() {
        return rsaPublicKeyBase64;
    }

    /**
     * 获取公钥 PEM 字符串（前端 jsencrypt 可直接使用）
     */
    public String getPublicKeyPem() {
        return "-----BEGIN PUBLIC KEY-----\n"
                + rsaPublicKeyBase64.replaceAll("(.{64})", "$1\n")
                + "\n-----END PUBLIC KEY-----";
    }

    // ========== RSA 解密：用私钥解 AES Key ==========

    /**
     * 用 RSA 私钥解密 AES 密钥密文
     * @param encryptedKeyBase64 Base64 编码的 AES 密钥密文
     * @return 明文 AES 密钥 (Base64 字符串)
     */
    public String rsaDecrypt(String encryptedKeyBase64) throws Exception {
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyBase64);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);

        // RSA 单次能解密的大小 = key_size / 8，2048 位可解 256 字节
        int maxDecryptBlock = RSA_KEY_SIZE / 8;
        if (encryptedKey.length <= maxDecryptBlock) {
            byte[] decrypted = cipher.doFinal(encryptedKey);
            // 前端 JSEncrypt 加密的是 AES 密钥的 Base64 字符串，
            // 解密后直接返回该字符串（不要再 Base64 编码一次）
            return new String(decrypted, StandardCharsets.UTF_8);
        }

        // 分片解密（前端一次只传 AES 密钥（32字节），一般不会触发）
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        int offset = 0;
        while (offset < encryptedKey.length) {
            int len = Math.min(maxDecryptBlock, encryptedKey.length - offset);
            os.write(cipher.doFinal(encryptedKey, offset, len));
            offset += len;
        }
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * 用 RSA 私钥加密（用于双向加密：响应数据）
     */
    public String rsaEncrypt(String plainText) throws Exception {
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, rsaPrivateKey);

        int maxEncryptBlock = (RSA_KEY_SIZE / 8) - 11; // PKCS1 padding 占11字节
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        int offset = 0;
        while (offset < plainBytes.length) {
            int len = Math.min(maxEncryptBlock, plainBytes.length - offset);
            os.write(cipher.doFinal(plainBytes, offset, len));
            offset += len;
        }
        return Base64.getEncoder().encodeToString(os.toByteArray());
    }

    // ========== AES 加解密 ==========

    /**
     * AES-256-CBC 解密：用 AES key 解密业务数据
     * @param cipherTextBase64 Base64 编码的密文
     * @param aesKeyBase64 Base64 编码的 AES 密钥（32字节 = 256位）
     * @return 明文 JSON 字符串
     */
    public String aesDecrypt(String cipherTextBase64, String aesKeyBase64) throws Exception {
        byte[] cipherBytes = Base64.getDecoder().decode(cipherTextBase64);
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);

        // 密文结构：前16字节 = IV，其余 = AES 密文
        byte[] iv = new byte[IV_SIZE];
        byte[] payload = new byte[cipherBytes.length - IV_SIZE];
        System.arraycopy(cipherBytes, 0, iv, 0, IV_SIZE);
        System.arraycopy(cipherBytes, IV_SIZE, payload, 0, payload.length);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] plain = cipher.doFinal(payload);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /**
     * AES-256-CBC 加密：加密响应数据
     * 每次加密随机生成新 IV，与密文拼接后返回
     */
    public String aesEncrypt(String plainText, String aesKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);

        // 生成随机 IV
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plainBytes);

        // 拼接：IV + 密文
        byte[] result = new byte[IV_SIZE + encrypted.length];
        System.arraycopy(iv, 0, result, 0, IV_SIZE);
        System.arraycopy(encrypted, 0, result, IV_SIZE, encrypted.length);
        return Base64.getEncoder().encodeToString(result);
    }

    // ========== 便捷接口：把当前请求的 AES Key 保存到线程变量 ==========

    public void bindAesKey(String aesKeyBase64) {
        currentAesKey.set(aesKeyBase64);
    }

    public String getCurrentAesKey() {
        return currentAesKey.get();
    }

    public void clearAesKey() {
        currentAesKey.remove();
    }

    /**
     * 工具方法：生成新的随机 AES 密钥（供后端自测或特殊场景使用）
     */
    public String generateAesKey() throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(AES_KEY_SIZE, new SecureRandom());
        SecretKey key = kg.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * 用 Base64 字符串直接构造 RSA 公钥（未来可支持从配置文件加载公钥）
     */
    public static PublicKey parseRsaPublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * 用 Base64 字符串直接构造 RSA 私钥
     */
    public static PrivateKey parseRsaPrivateKey(String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}
