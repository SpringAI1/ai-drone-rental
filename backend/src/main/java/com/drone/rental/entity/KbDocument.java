package com.drone.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("kb_document")
@Schema(description = "AI 知识库文档元数据")
public class KbDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "上传者用户ID；scope=public 时为 0（系统公共）")
    private Long userId;

    @Schema(description = "作用域：private / public")
    private String scope;

    @Schema(description = "原始文件名")
    private String filename;

    @Schema(description = "文件类型（pdf/docx/xlsx/txt...）")
    private String fileType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "磁盘保存路径")
    private String filePath;

    @Schema(description = "切块数量")
    private Integer chunkCount;

    @Schema(description = "总字符数")
    private Integer charCount;

    @Schema(description = "状态：1=ready / 0=processing / -1=failed")
    private Integer status;

    @Schema(description = "错误信息（解析失败时）")
    private String errorMessage;

    @Schema(description = "逻辑删除 0=未删 1=已删")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
