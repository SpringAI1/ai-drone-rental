package com.drone.rental.controller;

import com.drone.rental.common.Result;
import com.drone.rental.encryption.EncryptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 加密相关接口：前端通过该接口获取后端 RSA 公钥
 * <p>
 * 按文档设计，这是"第一阶段：前端发起请求"的前置步骤，
 * 前端在应用启动时（或第一次需要加密请求时）获取公钥并缓存。
 * 之后所有加密请求均使用同一公钥加密 AES key。
 */
@Slf4j
@Tag(name = "加密公钥下发")
@RestController
@RequestMapping("/public")
public class EncryptionController {

    @Autowired
    private EncryptionService encryptionService;

    /**
     * 获取 RSA 公钥
     * 返回 Base64 字符串 + PEM 格式，前端可直接使用 jsencrypt 导入
     */
    @Operation(summary = "获取RSA公钥")
    @GetMapping("/rsa-key")
    public Result<Map<String, Object>> getPublicKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("publicKey", encryptionService.getPublicKey());
        map.put("publicKeyPem", encryptionService.getPublicKeyPem());
        map.put("keySize", 2048);
        map.put("algorithm", "RSA+AES-CBC (信封加密)");
        map.put("aesKeySize", 256);
        return Result.success(map);
    }
}
