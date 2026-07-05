package com.drone.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.entity.KbDocument;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface KbDocumentMapper extends BaseMapper<KbDocument> {

    /** 列出某用户可见的文档（公共 + 该用户私有），按时间倒序 */
    @Select("""
            SELECT id, user_id, scope, filename, file_type, file_size, file_path,
                   chunk_count, char_count, status, error_message, created_time, updated_time
            FROM kb_document
            WHERE deleted = 0
              AND (scope = 'public' OR user_id = #{userId})
            ORDER BY created_time DESC
            """)
    List<KbDocument> findVisible(@Param("userId") Long userId);

    /** 列出某用户上传的全部文档（含私有） */
    @Select("""
            SELECT * FROM kb_document
            WHERE deleted = 0 AND user_id = #{userId}
            ORDER BY created_time DESC
            """)
    List<KbDocument> findByUserId(@Param("userId") Long userId);

    /** 启动时加载所有 status=1 的文档（用于恢复向量库） */
    @Select("SELECT * FROM kb_document WHERE deleted = 0 AND status = 1")
    List<KbDocument> findAllReady();

    /** 公共文档（scope=public） */
    @Select("SELECT * FROM kb_document WHERE deleted = 0 AND scope = 'public' ORDER BY created_time DESC")
    List<KbDocument> findPublic();
}
