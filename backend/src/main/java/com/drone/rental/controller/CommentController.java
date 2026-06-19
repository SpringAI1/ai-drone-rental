package com.drone.rental.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drone.rental.common.Result;
import com.drone.rental.dto.CommentDTO;
import com.drone.rental.entity.Comment;
import com.drone.rental.entity.Drone;
import com.drone.rental.service.CommentService;
import com.drone.rental.service.DroneService;
import com.drone.rental.service.NotificationService;
import com.drone.rental.vo.CommentVO;
import com.drone.rental.websocket.OrderNotificationHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 评论控制器（用户端）
 */
@Tag(name = "评论管理-用户端")
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private DroneService droneService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderNotificationHandler notificationHandler;

    @Operation(summary = "发表评论")
    @PostMapping("/add")
    public Result<Void> addComment(@Validated @RequestBody CommentDTO dto) {
        Comment comment = commentService.addComment(dto);

        // 广播新评论通知给管理员
        try {
            String droneName = "";
            if (dto.getDroneId() != null) {
                Drone drone = droneService.getById(dto.getDroneId());
                if (drone != null) {
                    droneName = drone.getModel() != null ? drone.getModel() : "";
                }
            }
            String content = "收到新评论：" + (comment.getContent().length() > 30 ? comment.getContent().substring(0, 30) + "..." : comment.getContent());
            notificationService.sendNotification(-1L, 2, droneName + " 有新评论待审核", content, comment.getId());

            Map<String, Object> commentInfo = new HashMap<>();
            commentInfo.put("id", comment.getId());
            commentInfo.put("userId", comment.getUserId());
            commentInfo.put("content", comment.getContent());
            commentInfo.put("droneName", droneName);
            commentInfo.put("title", "新的待审核评论");
            notificationHandler.notifyNewComment(commentInfo);
        } catch (Exception e) {
            // 通知失败不影响主流程
        }

        return Result.success();
    }

    @Operation(summary = "获取当前用户的评论列表")
    @GetMapping("/my")
    public Result<IPage<CommentVO>> getCurrentUserComments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<CommentVO> page = commentService.getCurrentUserComments(pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }
}
