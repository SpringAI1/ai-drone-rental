package com.drone.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("kb_chunk")
@Schema(description = "知识库切块（持久化到 SQLite，含 embedding）")
public class KbChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "切块ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联 kb_document.id")
    private Long documentId;

    @Schema(description = "块顺序（从 0 开始）")
    private Integer chunkIndex;

    @Schema(description = "切块原文")
    private String chunkText;

    @Schema(description = "embedding 向量（Base64 编码的 float[1024]）")
    private String embedding;

    @Schema(description = "切块字符数")
    private Integer charCount;

    @Schema(description = "逻辑删除 0=未删 1=已删")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
