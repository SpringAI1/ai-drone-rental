package com.drone.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.entity.KbChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface KbChunkMapper extends BaseMapper<KbChunk> {

    /** 加载某个文档的所有切块（用于 SimpleVectorStore 启动恢复） */
    @Select("SELECT * FROM kb_chunk WHERE document_id = #{documentId} AND deleted = 0 ORDER BY chunk_index ASC")
    List<KbChunk> findByDocumentId(@Param("documentId") Long documentId);

    /** 删除某文档的所有切块 */
    @Delete("DELETE FROM kb_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /** 加载所有切块（启动时一次性恢复向量库） */
    @Select("SELECT * FROM kb_chunk WHERE deleted = 0")
    List<KbChunk> findAll();
}
