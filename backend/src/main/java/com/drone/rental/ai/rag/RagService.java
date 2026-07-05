package com.drone.rental.ai.rag;

import com.drone.rental.ai.vector.SimpleVectorStore;
import com.drone.rental.entity.KbChunk;
import com.drone.rental.entity.KbDocument;
import com.drone.rental.mapper.KbChunkMapper;
import com.drone.rental.mapper.KbDocumentMapper;
import com.drone.rental.security.UserContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG 知识库服务
 *
 * 1) 启动时把 classpath:knowledge-base/*.txt 写入 SQLite + SimpleVectorStore（scope=public, userId=0）
 *    重复启动会自动跳过已存在的文件
 * 2) 用户上传的文档（私有/公共）由 KbDocumentService 处理，本类只负责检索
 * 3) 检索时按当前 userId 过滤：公共 + 当前用户私有
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final SimpleVectorStore vectorStore;
    private final KbDocumentMapper kbDocumentMapper;
    private final KbChunkMapper kbChunkMapper;

    @org.springframework.beans.factory.annotation.Value("classpath:knowledge-base/*.txt")
    private Resource[] knowledgeFiles;

    private static final long KB_SYSTEM_USER_ID = 0L;     // 系统内置
    private static final int BATCH_SIZE = 10;             // Qwen embedding API 单次 ≤10

    @PostConstruct
    public void init() {
        try {
            if (knowledgeFiles == null || knowledgeFiles.length == 0) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                knowledgeFiles = resolver.getResources("classpath:knowledge-base/*.txt");
            }
            log.info("[RAG] 系统知识库文件数: {}", knowledgeFiles.length);

            for (Resource res : knowledgeFiles) {
                String filename = res.getFilename();
                if (filename == null) continue;

                // 重复启动：检查是否已存在
                KbDocument exist = kbDocumentMapper.findAllReady().stream()
                        .filter(d -> KB_SYSTEM_USER_ID == d.getUserId() && filename.equals(d.getFilename()))
                        .findFirst().orElse(null);
                if (exist != null) {
                    log.info("[RAG] 系统知识 {} 已存在（id={}），跳过", filename, exist.getId());
                    continue;
                }

                String text = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                KbDocument doc = new KbDocument();
                doc.setUserId(KB_SYSTEM_USER_ID);
                doc.setScope("public");
                doc.setFilename(filename);
                doc.setFileType("txt");
                doc.setFileSize((long) text.length());
                doc.setFilePath("classpath:knowledge-base/" + filename);
                doc.setStatus(0);
                kbDocumentMapper.insert(doc);

                // 切块
                List<Document> rawList = List.of(new Document(
                        "sys:" + doc.getId(), text,
                        java.util.Map.of("source", filename, "type", "drone-knowledge")));
                TokenTextSplitter splitter = new TokenTextSplitter(300, 50, 5, 10000, true);
                List<Document> chunks = splitter.apply(rawList);
                log.info("[RAG] 系统知识 {} 切块: {} 块", filename, chunks.size());

                // 转 KbChunk 并写入
                List<KbChunk> kchunks = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                    Document c = chunks.get(i);
                    KbChunk kc = new KbChunk();
                    kc.setDocumentId(doc.getId());
                    kc.setChunkIndex(i);
                    kc.setChunkText(c.getText());
                    kchunks.add(kc);
                }
                vectorStore.addWithPersistence(kchunks, doc);

                doc.setChunkCount(kchunks.size());
                doc.setCharCount(text.length());
                doc.setStatus(1);
                kbDocumentMapper.updateById(doc);
            }
            log.info("[RAG] 系统知识库初始化完成，stats: {}", vectorStore.stats());
        } catch (IOException e) {
            log.error("[RAG] 加载知识库失败", e);
        } catch (Exception e) {
            log.error("[RAG] 向量化失败（可能是 LLM 不可达）", e);
        }
    }

    /**
     * 语义检索：按当前用户可见性过滤
     * @param query 查询
     * @param topK  topK
     */
    public List<Document> search(String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        Long userId = null;
        try { userId = UserContext.getCurrentUserId(); } catch (Exception ignored) {}
        return vectorStore.search(query, topK > 0 ? topK : 4, userId);
    }

    /**
     * 把检索结果拼成可注入 prompt 的字符串
     */
    public String formatContext(List<Document> docs) {
        if (docs == null || docs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("【知识库参考】\n");
        int i = 1;
        for (Document d : docs) {
            sb.append("[").append(i++).append("] 来源：")
              .append(d.getMetadata().get("source"))
              .append("\n").append(d.getText()).append("\n\n");
        }
        return sb.toString();
    }

    public SimpleVectorStore getVectorStore() {
        return vectorStore;
    }
}
