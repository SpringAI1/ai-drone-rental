package com.drone.rental.controller;

import com.drone.rental.ai.rag.RagService;
import com.drone.rental.ai.vector.SimpleVectorStore;
import com.drone.rental.common.Result;
import com.drone.rental.entity.KbDocument;
import com.drone.rental.security.JwtUtil;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.KbDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI 知识库控制器
 *
 * 公共/私有混合模式：
 * - list     返回当前用户可见的文档（公共 + 我私有）
 * - mine     仅我上传的
 * - public   仅公共
 * - upload   上传（可选 scope=private/public）
 * - delete   仅 owner
 * - search   按当前用户可见性过滤检索
 */
@Tag(name = "AI 知识库")
@Slf4j
@RestController
@RequestMapping("/ai/v3/knowledge")
@RequiredArgsConstructor
public class KbController {

    private final KbDocumentService kbDocumentService;
    private final RagService ragService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "上传文档（PDF/Word/PPT/Excel/TXT...）")
    @PostMapping("/upload")
    public Result<KbDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "private") String scope) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        KbDocument doc = kbDocumentService.upload(file, userId, scope);
        return Result.success(doc);
    }

    @Operation(summary = "当前用户可见的全部知识库文档（公共 + 我的私有）")
    @GetMapping("/list")
    public Result<List<KbDocument>> list() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        return Result.success(kbDocumentService.listVisible(userId));
    }

    @Operation(summary = "我上传的文档")
    @GetMapping("/mine")
    public Result<List<KbDocument>> mine() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        return Result.success(kbDocumentService.listMine(userId));
    }

    @Operation(summary = "公共知识库（所有人可访问）")
    @GetMapping("/public")
    public Result<List<KbDocument>> publicList() {
        return Result.success(kbDocumentService.listPublic());
    }

    @Operation(summary = "文档详情")
    @GetMapping("/detail/{id}")
    public Result<KbDocument> detail(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(kbDocumentService.getDetail(id, userId));
    }

    @Operation(summary = "删除文档（仅 owner）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        kbDocumentService.delete(id, userId);
        return Result.success();
    }

    @Operation(summary = "语义检索（按当前用户可见性过滤）")
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "4") int topK,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // search 走白名单（JwtInterceptor 不解析），这里手动 setCurrentUser 让私有文档可被检索
        boolean bound = false;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7).trim();
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    UserContext.setCurrentUser(new UserContext.UserInfo(userId, null, null));
                    bound = true;
                }
            } catch (Exception ignored) {}
        }
        try {
            List<Document> docs = ragService.search(query, topK);
            List<Map<String, Object>> result = docs.stream().map(d -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", d.getId());
                m.put("text", d.getText());
                m.put("source", d.getMetadata().get("source"));
                m.put("scope", d.getMetadata().get("scope"));
                m.put("documentId", d.getMetadata().get("documentId"));
                m.put("userId", d.getMetadata().get("userId"));
                m.put("score", d.getMetadata().get("score"));
                return m;
            }).toList();
            return Result.success(result);
        } finally {
            if (bound) UserContext.clear();
        }
    }

    @Operation(summary = "向量库统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(kbDocumentService.stats());
    }
}
