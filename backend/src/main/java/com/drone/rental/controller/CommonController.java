package com.drone.rental.controller;

import com.drone.rental.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 通用接口控制器
 */
@Tag(name = "通用接口")
@RestController
@RequestMapping("/common")
public class CommonController {

    /**
     * 允许上传的文件扩展名白名单（仅图片，避免上传可执行脚本）
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    @Value("${file.access-url:/uploads/}")
    private String accessUrl;

    private Path uploadDir;

    @PostConstruct
    public void init() {
        String userDir = System.getProperty("user.dir");
        uploadDir = Paths.get(userDir, uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadDir, e);
        }
    }

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Operation(summary = "上传文件（仅支持图片）")
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 文件大小限制
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return Result.error("文件名不能为空");
        }

        // 1) 提取扩展名（统一转小写）
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return Result.error("文件必须带有扩展名");
        }
        String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);

        // 2) 白名单校验
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error("不支持的文件类型，仅允许：" + ALLOWED_EXTENSIONS);
        }

        // 3) 双重校验：检查 MIME（防止扩展名伪造）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return Result.error("文件内容类型必须为图片");
        }

        // 4) 生成新文件名（去掉原始名，只保留扩展名）
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

        // 5) 解析目标路径并校验：必须在 uploadDir 之下（防路径穿越）
        Path destPath = uploadDir.resolve(newFilename).normalize();
        if (!destPath.startsWith(uploadDir)) {
            return Result.error("非法的文件路径");
        }

        try {
            file.transferTo(destPath.toFile());
        } catch (IOException e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }

        return Result.success(accessUrl + newFilename);
    }
}
