package com.drone.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.entity.AiChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    @Select("SELECT * FROM ai_chat_message WHERE conversation_id = #{conversationId} ORDER BY created_time DESC LIMIT #{limit}")
    List<AiChatMessage> findRecentMessages(@Param("conversationId") String conversationId, @Param("limit") int limit);

    /**
     * 分页查询会话摘要（按 conversation_id 聚合，按最后活跃时间倒序）
     * 替代全表加载到内存再分页的做法
     */
    @Select("<script>" +
            "SELECT c.conversation_id, MAX(c.user_id) AS user_id, " +
            "  COUNT(*) AS message_count, " +
            "  SUM(CASE WHEN c.role='user' THEN 1 ELSE 0 END) AS user_message_count, " +
            "  SUM(CASE WHEN c.role='ai' THEN 1 ELSE 0 END) AS ai_message_count, " +
            "  MIN(c.created_time) AS first_time, " +
            "  MAX(c.created_time) AS last_time " +
            "FROM ai_chat_message c " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND c.content LIKE CONCAT('%', #{keyword}, '%')" +
            "  </if>" +
            "  <if test='withUserOnly != null and withUserOnly'>" +
            "    AND c.user_id IS NOT NULL" +
            "  </if>" +
            "</where> " +
            "GROUP BY c.conversation_id " +
            "ORDER BY last_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String, Object>> findConversationSummaries(@Param("keyword") String keyword,
                                                         @Param("withUserOnly") Boolean withUserOnly,
                                                         @Param("offset") int offset,
                                                         @Param("limit") int limit);

    /**
     * 统计会话总数（用于分页 total）
     */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT c.conversation_id) " +
            "FROM ai_chat_message c " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND c.content LIKE CONCAT('%', #{keyword}, '%')" +
            "  </if>" +
            "  <if test='withUserOnly != null and withUserOnly'>" +
            "    AND c.user_id IS NOT NULL" +
            "  </if>" +
            "</where>" +
            "</script>")
    long countConversations(@Param("keyword") String keyword,
                            @Param("withUserOnly") Boolean withUserOnly);

    /**
     * 获取整体统计（COUNT 聚合，不加载任何消息体）
     */
    @Select("SELECT " +
            "  COUNT(*) AS total_messages, " +
            "  SUM(CASE WHEN role='user' THEN 1 ELSE 0 END) AS total_user_messages, " +
            "  COUNT(DISTINCT conversation_id) AS total_sessions, " +
            "  COUNT(DISTINCT user_id) AS active_user_count " +
            "FROM ai_chat_message")
    Map<String, Object> getMessageStats();
}