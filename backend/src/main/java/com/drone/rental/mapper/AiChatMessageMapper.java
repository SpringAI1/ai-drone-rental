package com.drone.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.rental.entity.AiChatMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    @Select("SELECT * FROM ai_chat_message WHERE conversation_id = #{conversationId} ORDER BY created_time DESC LIMIT #{limit}")
    List<AiChatMessage> findRecentMessages(@Param("conversationId") String conversationId, @Param("limit") int limit);
}