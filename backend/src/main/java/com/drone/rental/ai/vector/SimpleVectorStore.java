package com.drone.rental.ai.vector;

import com.drone.rental.entity.KbChunk;
import com.drone.rental.entity.KbDocument;
import com.drone.rental.mapper.KbDocumentMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 持久化版内存向量库
 *
 * 1) 启动时从 SQLite 加载所有 status=1 的文档及其 chunks + embedding
 * 2) 运行时 add/delete 通过 KbChunkMapper 同步持久化
 * 3) 支持 metadata.userId / scope 过滤：检索时仅返回 userId=当前用户或 scope=public 的 chunks
 *
 * 适合开发环境和小规模知识库。生产环境应替换为 Milvus / Qdrant 等。
 */
@Slf4j
@Component
public class SimpleVectorStore implements VectorStore {

    private final EmbeddingModel embeddingModel;
    private final KbDocumentMapper kbDocumentMapper;
    private final JdbcTemplate jdbc;

    /** chunkId -> chunk + embedding + metadata (userId/scope/source/...) */
    private final Map<String, StoredDoc> store = new ConcurrentHashMap<>();

    public SimpleVectorStore(EmbeddingModel embeddingModel,
                             KbDocumentMapper kbDocumentMapper,
                             JdbcTemplate jdbc) {
        this.embeddingModel = embeddingModel;
        this.kbDocumentMapper = kbDocumentMapper;
        this.jdbc = jdbc;
        log.info("[VectorStore] 初始化完成，使用 EmbeddingModel: {}",
                embeddingModel.getClass().getSimpleName());
    }

    public static class StoredDoc {
        public final String id;          // chunk id (kb_chunk.id 字符串)
        public final String text;
        public final float[] embedding;
        public final Map<String, Object> metadata;

        public StoredDoc(String id, String text, float[] embedding, Map<String, Object> metadata) {
            this.id = id;
            this.text = text;
            this.embedding = embedding;
            this.metadata = metadata == null ? new HashMap<>() : metadata;
        }
    }

    private record ScoredDoc(StoredDoc stored, double score) {}

    /** 启动时从 DB 恢复内存向量库 */
    @PostConstruct
    public void restoreFromDb() {
        try {
            List<KbDocument> docs = kbDocumentMapper.findAllReady();
            if (docs == null || docs.isEmpty()) {
                log.info("[VectorStore] 启动恢复：无 status=1 文档");
                return;
            }
            Map<Long, KbDocument> docMap = docs.stream()
                    .collect(Collectors.toMap(KbDocument::getId, d -> d));
            int total = 0;
            for (KbDocument doc : docs) {
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT id, chunk_index, chunk_text, embedding FROM kb_chunk WHERE document_id = ? AND deleted = 0 ORDER BY chunk_index ASC",
                        doc.getId());
                if (rows == null || rows.isEmpty()) continue;
                for (Map<String, Object> row : rows) {
                    String embStr = (String) row.get("embedding");
                    float[] emb = base64ToFloats(embStr);
                    if (emb == null || emb.length == 0) continue;
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("userId", doc.getUserId());
                    meta.put("scope", doc.getScope());
                    meta.put("source", doc.getFilename());
                    meta.put("documentId", doc.getId());
                    meta.put("chunkIndex", row.get("chunk_index"));
                    String chunkId = "kb:" + row.get("id");
                    store.put(chunkId, new StoredDoc(chunkId, (String) row.get("chunk_text"), emb, meta));
                    total++;
                }
            }
            log.info("[VectorStore] 启动恢复完成：{} 个文档, {} 个 chunks", docMap.size(), total);
        } catch (Exception e) {
            log.error("[VectorStore] 启动恢复失败", e);
        }
    }

    @Override
    public void add(List<Document> documents) {
        // 不直接调用；请使用 addWithPersistence(...)
        log.warn("[VectorStore] 直接 add() 调用未持久化，请使用 addWithPersistence()");
        for (Document doc : documents) {
            try {
                float[] emb = embeddingModel.embed(doc.getText());
                store.put(doc.getId(), new StoredDoc(doc.getId(), doc.getText(), emb, doc.getMetadata()));
            } catch (Exception e) {
                log.warn("[VectorStore] add 失败: {} - {}", doc.getId(), e.getMessage());
            }
        }
    }

    /**
     * 持久化版 add：同时写入内存 + SQLite
     * @param chunks    切块（已生成 embedding）
     * @param document  所属文档（用于填充 userId/scope/source metadata）
     */
    public void addWithPersistence(List<KbChunk> chunks, KbDocument document) {
        if (chunks == null || chunks.isEmpty()) return;
        Map<String, Object> baseMeta = new HashMap<>();
        baseMeta.put("userId", document.getUserId());
        baseMeta.put("scope", document.getScope());
        baseMeta.put("source", document.getFilename());
        baseMeta.put("documentId", document.getId());

        for (KbChunk c : chunks) {
            try {
                float[] emb = embeddingModel.embed(c.getChunkText());
                c.setEmbedding(floatsToBase64(emb));
                c.setCharCount(c.getChunkText() == null ? 0 : c.getChunkText().length());
                c.setDeleted(0);
                LocalDateTime now = LocalDateTime.now();
                c.setCreatedTime(now);
                // 用 JdbcTemplate 直接写库，绕开 MyBatis-Plus 事务问题
                int rows = jdbc.update(
                        "INSERT INTO kb_chunk (document_id, chunk_index, chunk_text, embedding, char_count, created_time, deleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        document.getId(),
                        c.getChunkIndex(),
                        c.getChunkText(),
                        c.getEmbedding(),
                        c.getCharCount(),
                        now,
                        0);
                Long newId = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
                c.setId(newId);
                log.info("[VectorStore] insert chunk idx={} doc={} rows={} id={}",
                        c.getChunkIndex(), document.getId(), rows, newId);

                Map<String, Object> meta = new HashMap<>(baseMeta);
                meta.put("chunkIndex", c.getChunkIndex());
                String chunkId = "kb:" + newId;
                store.put(chunkId, new StoredDoc(chunkId, c.getChunkText(), emb, meta));
            } catch (Exception e) {
                log.error("[VectorStore] addWithPersistence 失败 chunk: {} - {}", c.getChunkIndex(), e.getMessage(), e);
            }
        }
        log.info("[VectorStore] 写入 {} 个 chunks 到 document={}", chunks.size(), document.getId());
    }

    @Override
    public void delete(List<String> idList) {
        // 不直接调用；请使用 deleteByDocumentId(...)
        idList.forEach(store::remove);
    }

    /** 按 documentId 物理删除（内存 + DB） */
    public void deleteByDocumentId(Long documentId) {
        // 先把内存中对应 chunk 标 id
        List<String> toRemove = store.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue().metadata.get("documentId"), documentId))
                .map(Map.Entry::getKey)
                .toList();
        toRemove.forEach(store::remove);
        jdbc.update("DELETE FROM kb_chunk WHERE document_id = ?", documentId);
        log.info("[VectorStore] 删除 document={} 关联 {} 个 chunks", documentId, toRemove.size());
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        log.warn("[VectorStore] 不支持 filter 删除，请使用 deleteByDocumentId()");
    }

    /**
     * 公共/私有混合检索：
     *   - 公共文档（scope=public）所有用户可见
     *   - 私有文档仅 owner 可见
     *
     * @param query   查询
     * @param topK    取前 K
     * @param userId  当前用户（null 表示未登录，仅公共）
     */
    public List<Document> search(String query, int topK, Long userId) {
        if (query == null || query.isBlank()) return List.of();
        try {
            float[] queryEmb = embeddingModel.embed(query);

            List<ScoredDoc> scored = new ArrayList<>();
            int privateCnt = 0;
            for (StoredDoc sd : store.values()) {
                if (!isVisible(sd, userId)) continue;
                if (!"public".equals(sd.metadata.get("scope"))) privateCnt++;
                double sim = cosineSimilarity(queryEmb, sd.embedding);
                scored.add(new ScoredDoc(sd, sim));
            }
            scored.sort(Comparator.comparingDouble((ScoredDoc s) -> s.score).reversed());
            if (scored.size() > topK) scored = scored.subList(0, topK);
            log.info("[VectorStore.search] query='{}' userId={} totalScanned={} privateScanned={} topK={}",
                    query, userId, scored.size(), privateCnt, topK);

            return scored.stream().map(s -> {
                Map<String, Object> meta = new HashMap<>(s.stored.metadata);
                meta.put("score", s.score);
                return new Document(s.stored.id, s.stored.text, meta);
            }).toList();
        } catch (Exception e) {
            log.warn("[VectorStore] 检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** 是否对当前用户可见：公共全部可见；私有仅 owner */
    private boolean isVisible(StoredDoc sd, Long userId) {
        Object scope = sd.metadata.get("scope");
        if ("public".equals(scope)) return true;
        if (userId == null) return false;
        Object owner = sd.metadata.get("userId");
        return owner instanceof Number && ((Number) owner).longValue() == userId;
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        // 不使用此入口（filter 解析成本高，直接用 search() 简化）
        // 为了兼容 Spring AI 接口，这里走宽松检索（不过滤）
        if (request == null || request.getQuery() == null) return List.of();
        int topK = request.getTopK() > 0 ? request.getTopK() : 4;
        return search(request.getQuery(), topK, null);
    }

    /** 当前向量库统计 */
    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalChunks", store.size());
        long publicCnt = store.values().stream().filter(s -> "public".equals(s.metadata.get("scope"))).count();
        long privateCnt = store.size() - publicCnt;
        m.put("publicChunks", publicCnt);
        m.put("privateChunks", privateCnt);
        return m;
    }

    // ===== 工具方法 =====

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

    public static String floatsToBase64(float[] floats) {
        if (floats == null) return null;
        ByteBuffer bb = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(floats);
        return Base64.getEncoder().encodeToString(bb.array());
    }

    public static float[] base64ToFloats(String base64) {
        if (base64 == null || base64.isEmpty()) return new float[0];
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            if (bytes.length % 4 != 0) return new float[0];
            ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            FloatBuffer fb = bb.asFloatBuffer();
            float[] arr = new float[fb.remaining()];
            fb.get(arr);
            return arr;
        } catch (Exception e) {
            return new float[0];
        }
    }
}
