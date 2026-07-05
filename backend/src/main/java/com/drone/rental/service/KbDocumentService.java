package com.drone.rental.service;

import com.drone.rental.ai.vector.SimpleVectorStore;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.entity.KbChunk;
import com.drone.rental.entity.KbDocument;
import com.drone.rental.mapper.KbChunkMapper;
import com.drone.rental.mapper.KbDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库文档服务
 *
 * 负责：
 * 1) 接收上传文件 → 保存到磁盘
 * 2) TikaDocumentReader 解析 PDF/Word/PPT/Excel
 * 3) TokenTextSplitter 切块
 * 4) 写入 KbDocument + KbChunk + SimpleVectorStore
 * 5) 列表（可见/我的）、详情、删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentService {

    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;
    private final SimpleVectorStore vectorStore;

    @Value("${file.upload-path:../uploads/}")
    private String uploadPath;

    @Value("${file.access-url:/uploads/}")
    private String accessUrl;

    /** 允许的文件类型（Tika 支持：pdf/doc/docx/ppt/pptx/xls/xlsx/txt/md/html...） */
    private static final Set<String> ALLOWED_EXT = Set.of(
            ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx",
            ".txt", ".md", ".html", ".htm", ".xml", ".json", ".csv"
    );
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;  // 50MB

    private Path kbDir;

    @PostConstruct
    public void init() throws IOException {
        String userDir = System.getProperty("user.dir");
        kbDir = Paths.get(userDir, uploadPath, "kb").toAbsolutePath().normalize();
        Files.createDirectories(kbDir);
        log.info("[KB] 知识库上传目录: {}", kbDir);
    }

    /**
     * 上传并向量化
     *
     * @param file   文档
     * @param userId 上传者
     * @param scope  private / public
     */
    public KbDocument upload(MultipartFile file, Long userId, String scope) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException("文件超过 50MB");
        if (!"private".equals(scope) && !"public".equals(scope)) scope = "private";

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) throw new BusinessException("文件名不能为空");

        int dotIdx = original.lastIndexOf('.');
        if (dotIdx <= 0) throw new BusinessException("文件必须有扩展名");
        String ext = original.substring(dotIdx).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) throw new BusinessException("不支持的文件类型: " + ext);

        // 1) 保存文件
        String newName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dest = kbDir.resolve(newName).normalize();
        if (!dest.startsWith(kbDir)) throw new BusinessException("非法的文件路径");
        try {
            file.transferTo(dest.toFile());
        } catch (IOException e) {
            throw new BusinessException("保存文件失败: " + e.getMessage());
        }

        // 2) 创建文档元数据（status=processing）
        KbDocument doc = new KbDocument();
        doc.setUserId(userId);
        doc.setScope(scope);
        doc.setFilename(original);
        doc.setFileType(ext.replace(".", ""));
        doc.setFileSize(file.getSize());
        doc.setFilePath(dest.toString());
        doc.setStatus(0);
        kbDocumentMapper.insert(doc);

        // 3) 同步解析（同步阻塞，前端可立即看到结果）
        try {
            ingestDocument(doc);
        } catch (Exception e) {
            log.error("[KB] 解析失败: {}", e.getMessage(), e);
            doc.setStatus(-1);
            doc.setErrorMessage(e.getMessage());
            kbDocumentMapper.updateById(doc);
            // 不抛异常，让前端看到 status=-1
        }
        return doc;
    }

    /**
     * 解析+切块+写入（同步）
     * 加 @Transactional 保证 chunks 与文档元数据在同一事务中提交
     */
    @org.springframework.transaction.annotation.Transactional
    public void ingestDocument(KbDocument doc) throws IOException {
        Path filePath = Paths.get(doc.getFilePath());
        byte[] bytes = Files.readAllBytes(filePath);

        // Tika 解析（基于 Spring AI）
        TikaDocumentReader reader = new TikaDocumentReader(new ByteArrayResource(bytes));
        List<Document> raw = reader.get();
        if (raw == null || raw.isEmpty()) {
            throw new RuntimeException("Tika 解析结果为空");
        }
        StringBuilder allText = new StringBuilder();
        for (Document d : raw) {
            if (d.getText() != null) allText.append(d.getText());
        }
        if (allText.length() == 0) {
            throw new RuntimeException("文档内容为空");
        }

        // 切块（每块 ~300 tokens）
        TokenTextSplitter splitter = new TokenTextSplitter(300, 50, 5, 10000, true);
        List<Document> chunks = splitter.apply(raw);
        log.info("[KB] 文档 {} 切块: {} 块, 总字符: {}", doc.getFilename(), chunks.size(), allText.length());

        // 转 KbChunk
        List<KbChunk> kchunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Document c = chunks.get(i);
            KbChunk kc = new KbChunk();
            kc.setDocumentId(doc.getId());
            kc.setChunkIndex(i);
            kc.setChunkText(c.getText());
            kchunks.add(kc);
        }
        // 写入向量库（持久化）
        vectorStore.addWithPersistence(kchunks, doc);

        // 更新文档元数据
        doc.setChunkCount(kchunks.size());
        doc.setCharCount(allText.length());
        doc.setStatus(1);
        kbDocumentMapper.updateById(doc);
    }

    /** 列出当前用户可见的全部文档（公共 + 我的私有） */
    public List<KbDocument> listVisible(Long userId) {
        return kbDocumentMapper.findVisible(userId);
    }

    /** 列出我上传的文档（仅私有 + 公共） */
    public List<KbDocument> listMine(Long userId) {
        return kbDocumentMapper.findByUserId(userId);
    }

    /** 公共文档列表 */
    public List<KbDocument> listPublic() {
        return kbDocumentMapper.findPublic();
    }

    public KbDocument getDetail(Long id, Long currentUserId) {
        KbDocument doc = kbDocumentMapper.selectById(id);
        if (doc == null || doc.getDeleted() == 1) throw new BusinessException("文档不存在");
        // 私有文档仅 owner 可见
        if ("private".equals(doc.getScope()) && !doc.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权查看该文档");
        }
        return doc;
    }

    /** 删除文档（仅 owner / admin） */
    public void delete(Long id, Long currentUserId) {
        KbDocument doc = kbDocumentMapper.selectById(id);
        if (doc == null || doc.getDeleted() == 1) throw new BusinessException("文档不存在");
        if (doc.getUserId() == 0L) {
            // 系统内置，禁止删除
            throw new BusinessException("系统内置文档禁止删除");
        }
        if (!doc.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权删除该文档");
        }
        // 物理删除 chunks（DB + 内存）
        vectorStore.deleteByDocumentId(id);
        // 软删除文档
        doc.setDeleted(1);
        kbDocumentMapper.updateById(doc);
        log.info("[KB] 删除文档 id={}, filename={}", id, doc.getFilename());
    }

    public Map<String, Object> stats() {
        return vectorStore.stats();
    }
}
