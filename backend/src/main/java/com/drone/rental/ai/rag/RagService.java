package com.drone.rental.ai.rag;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
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
 * 真实向量检索式 RAG：
 * 1) 启动时加载 classpath:knowledge-base/ 下的所有 .txt
 * 2) TokenTextSplitter 切块（每块约 300 tokens）
 * 3) 手动 embedding 分批写入（Qwen 限流 ≤ 10/批）
 * 4) similaritySearch 找到最相关的 top-K
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;

    @Value("classpath:knowledge-base/*.txt")
    private Resource[] knowledgeFiles;

    private static final String KNOWLEDGE_PREFIX = "drone-kb:";
    /** Qwen embedding API 单次请求最多 10 条 */
    private static final int BATCH_SIZE = 10;

    @PostConstruct
    public void init() {
        try {
            if (knowledgeFiles == null || knowledgeFiles.length == 0) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                knowledgeFiles = resolver.getResources("classpath:knowledge-base/*.txt");
            }
            log.info("[RAG] 知识库文件数: {}", knowledgeFiles.length);

            List<Document> all = new ArrayList<>();
            for (Resource res : knowledgeFiles) {
                String filename = res.getFilename();
                if (filename == null) continue;
                String text = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                Document doc = new Document(KNOWLEDGE_PREFIX + filename, text,
                        java.util.Map.of("source", filename, "type", "drone-knowledge"));
                all.add(doc);
                log.info("[RAG] 加载知识: {} ({} 字符)", filename, text.length());
            }
            // 切块
            TokenTextSplitter splitter = new TokenTextSplitter(300, 50, 5, 10000, true);
            List<Document> chunks = splitter.apply(all);
            log.info("[RAG] 切块完成: {} 个文档 → {} 个 chunks", all.size(), chunks.size());

            // 分批写入向量库（Qwen embedding API 限流 ≤ 10/批）
            if (!chunks.isEmpty()) {
                int total = 0;
                for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, chunks.size());
                    List<Document> batch = new ArrayList<>(chunks.subList(i, end));
                    try {
                        vectorStore.add(batch);  // 内部自动 embedding（每批 ≤ 10 避免限流）
                        total += batch.size();
                        log.info("[RAG] 批次 {}-{} 写入完成", i, end);
                    } catch (Exception e) {
                        log.warn("[RAG] 批次 {}-{} 失败: {}", i, end, e.getMessage());
                    }
                }
                log.info("[RAG] ✅ 知识库初始化完成，向量库共写入 {} 个 chunk", total);
            }
        } catch (IOException e) {
            log.error("[RAG] 加载知识库失败", e);
        } catch (Exception e) {
            log.error("[RAG] 向量化失败（可能是 LLM 不可达）", e);
        }
    }

    /**
     * 语义检索：根据用户问题从知识库找出最相关的 K 个片段
     */
    public List<Document> search(String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        try {
            return vectorStore.similaritySearch(
                    org.springframework.ai.vectorstore.SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .similarityThreshold(0.4)
                            .build()
            );
        } catch (Exception e) {
            log.warn("[RAG] 检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 把检索结果拼成可注入 prompt 的字符串
     */
    public String formatContext(List<Document> docs) {
        if (docs == null || docs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("【知识库参考】\n");
        int i = 1;
        for (Document d : docs) {
            sb.append("[").append(i++).append("] 来源：").append(d.getMetadata().get("source"))
              .append("\n").append(d.getText()).append("\n\n");
        }
        return sb.toString();
    }
}
