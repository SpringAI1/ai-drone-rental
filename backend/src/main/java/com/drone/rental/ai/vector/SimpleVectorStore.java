package com.drone.rental.ai.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自实现内存向量库 SimpleVectorStore
 *
 * 解决本地 Redis 不支持 redis-modules（JSON.SET）的问题：
 * - 内存中保存 Document + 它的 embedding
 * - 检索时计算 query 与所有 Document 的余弦相似度
 * - topK + 相似度阈值过滤
 *
 * 适合开发环境和小规模知识库。生产环境应替换为 Redis/Milvus 等。
 */
@Slf4j
@Component
public class SimpleVectorStore implements VectorStore {

    private final EmbeddingModel embeddingModel;
    /** documentId -> 文档 + embedding */
    private final Map<String, StoredDoc> store = new ConcurrentHashMap<>();

    public SimpleVectorStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        log.info("[SimpleVectorStore] 初始化完成，使用 EmbeddingModel: {}",
                embeddingModel.getClass().getSimpleName());
    }

    public static class StoredDoc {
        public final Document document;
        public final float[] embedding;
        public StoredDoc(Document d, float[] e) { this.document = d; this.embedding = e; }
    }

    @Override
    public void add(List<Document> documents) {
        for (Document doc : documents) {
            try {
                // 简单场景：embedding 由调用方预先生成并通过 metadata 传入
                // 否则使用 embeddingModel 现生成
                float[] emb = embeddingModel.embed(doc.getText());
                store.put(doc.getId(), new StoredDoc(doc, emb));
            } catch (Exception e) {
                log.warn("[SimpleVectorStore] 添加文档失败: {} - {}", doc.getId(), e.getMessage());
            }
        }
        log.info("[SimpleVectorStore] 已写入 {} 个文档，当前共 {} 个", documents.size(), store.size());
    }

    @Override
    public void delete(List<String> idList) {
        idList.forEach(store::remove);
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        // 简单场景不支持 filter，全量删除会破坏数据，所以仅警告
        log.warn("[SimpleVectorStore] 不支持 filter 删除");
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        String query = request.getQuery();
        int topK = request.getTopK() > 0 ? request.getTopK() : 4;
        double threshold = request.getSimilarityThreshold();

        float[] queryEmb;
        try {
            queryEmb = embeddingModel.embed(query);
        } catch (Exception e) {
            log.warn("[SimpleVectorStore] embedding 失败: {}", e.getMessage());
            return List.of();
        }

        // 计算所有文档的余弦相似度
        List<ScoredDoc> scored = new ArrayList<>();
        for (StoredDoc sd : store.values()) {
            double sim = cosineSimilarity(queryEmb, sd.embedding);
            if (sim >= threshold) {
                scored.add(new ScoredDoc(sd, sim));
            }
        }
        // 降序排，取 topK
        scored.sort(Comparator.comparingDouble((ScoredDoc s) -> s.score).reversed());
        if (scored.size() > topK) scored = scored.subList(0, topK);

        // 包装回 Document（带 score 元数据）
        return scored.stream().map(s -> {
            Document orig = s.stored.document;
            Map<String, Object> meta = new java.util.HashMap<>(orig.getMetadata());
            meta.put("score", s.score);
            return new Document(orig.getId(), orig.getText(), meta);
        }).toList();
    }

    private record ScoredDoc(StoredDoc stored, double score) {}

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
