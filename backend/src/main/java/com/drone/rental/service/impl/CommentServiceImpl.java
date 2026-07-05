package com.drone.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.Constants;
import com.drone.rental.common.ResultCode;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dto.CommentDTO;
import com.drone.rental.entity.Comment;
import com.drone.rental.entity.Drone;
import com.drone.rental.entity.User;
import com.drone.rental.mapper.CommentMapper;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.CommentService;
import com.drone.rental.service.DroneService;
import com.drone.rental.service.UserService;
import com.drone.rental.vo.CommentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private UserService userService;

    @Autowired
    private DroneService droneService;

    @Override
    public Comment addComment(CommentDTO dto) {
        Long userId = UserContext.getCurrentUserId();

        // 检查用户状态
        userService.checkUserOperable(userId);

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setDroneId(dto.getDroneId());
        comment.setOrderId(dto.getOrderId());
        comment.setContent(dto.getContent());
        comment.setRating(dto.getRating() != null ? dto.getRating() : 5);
        comment.setImages(toJsonString(dto.getImages()));
        comment.setParentId(dto.getParentId());
        comment.setStatus(Constants.COMMENT_STATUS_NORMAL);

        this.save(comment);
        return comment;
    }

    private static String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public IPage<CommentVO> getCurrentUserComments(Integer pageNum, Integer pageSize) {
        Long userId = UserContext.getCurrentUserId();
        Page<Comment> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getUserId, userId);
        wrapper.orderByDesc(Comment::getCreatedTime);

        Page<Comment> commentPage = this.page(page, wrapper);
        Page<CommentVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(commentPage.getTotal());
        voPage.setRecords(commentPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public IPage<CommentVO> getDroneComments(Long droneId, Integer pageNum, Integer pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);

        // 只获取一级评论（parentId为null）
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getDroneId, droneId);
        wrapper.eq(Comment::getStatus, Constants.COMMENT_STATUS_NORMAL);
        wrapper.isNull(Comment::getParentId);
        wrapper.orderByDesc(Comment::getCreatedTime);

        Page<Comment> commentPage = this.page(page, wrapper);
        Page<CommentVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(commentPage.getTotal());

        List<Comment> records = commentPage.getRecords();

        // 批量加载用户信息，避免 N+1
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        for (Comment c : records) { if (c.getUserId() != null) userIds.add(c.getUserId()); }
        java.util.Map<Long, User> userMap = userIds.isEmpty() ? java.util.Collections.emptyMap() :
                userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<CommentVO> voList = records.stream()
                .map(c -> convertToVO(c, userMap, null))
                .collect(Collectors.toList());

        // 为每个一级评论获取子评论
        for (CommentVO vo : voList) {
            List<Comment> childComments = this.list(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getParentId, vo.getId())
                    .eq(Comment::getStatus, Constants.COMMENT_STATUS_NORMAL)
                    .orderByAsc(Comment::getCreatedTime));

            // 子评论也批量加载用户信息
            java.util.Set<Long> childUserIds = new java.util.HashSet<>();
            for (Comment c : childComments) { if (c.getUserId() != null) childUserIds.add(c.getUserId()); }
            java.util.Map<Long, User> childUserMap = childUserIds.isEmpty() ? userMap :
                    userService.listByIds(childUserIds).stream().collect(Collectors.toMap(User::getId, u -> u));

            List<CommentVO> childVOs = childComments.stream()
                    .map(c -> convertToVO(c, childUserMap, null))
                    .collect(Collectors.toList());
            vo.setChildren(childVOs);
        }

        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void deleteComment(Long id) {
        Comment comment = this.getById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_EXIST);
        }

        // 只能删除自己的评论
        Long currentUserId = UserContext.getCurrentUserId();
        if (!comment.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 级联删除子评论
        this.remove(new LambdaQueryWrapper<Comment>().eq(Comment::getParentId, id));
        this.removeById(id);
    }

    @Override
    public IPage<CommentVO> pageCommentsVO(Integer pageNum, Integer pageSize, Long droneId, Integer status,
                                            String userName, String droneModel, Integer rating, Boolean hasReply) {
        IPage<Comment> commentPage = pageComments(pageNum, pageSize, droneId, status, userName, droneModel, rating, hasReply);

        List<Comment> records = commentPage.getRecords();

        // 批量加载用户信息，避免 N+1
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        for (Comment c : records) { if (c.getUserId() != null) userIds.add(c.getUserId()); }
        java.util.Map<Long, User> userMap = userIds.isEmpty() ? java.util.Collections.emptyMap() :
                userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        // 批量加载无人机信息
        java.util.Set<Long> droneIds = new java.util.HashSet<>();
        for (Comment c : records) { if (c.getDroneId() != null) droneIds.add(c.getDroneId()); }
        java.util.Map<Long, Drone> droneMap = droneIds.isEmpty() ? java.util.Collections.emptyMap() :
                droneService.listByIds(droneIds).stream().collect(Collectors.toMap(Drone::getId, d -> d));

        Page<CommentVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(commentPage.getTotal());
        voPage.setRecords(records.stream()
                .map(c -> convertToVO(c, userMap, droneMap))
                .collect(Collectors.toList()));

        return voPage;
    }

    /**
     * 将Comment转换为CommentVO（批量加载版本）
     */
    private CommentVO convertToVO(Comment comment) {
        return convertToVO(comment, null, null);
    }

    /**
     * 将Comment转换为CommentVO（预加载用户/设备信息，避免 N+1）
     */
    private CommentVO convertToVO(Comment comment, java.util.Map<Long, User> userMap,
                                   java.util.Map<Long, Drone> droneMap) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setDroneId(comment.getDroneId());
        vo.setOrderId(comment.getOrderId());
        vo.setContent(comment.getContent());
        vo.setRating(comment.getRating());
        vo.setParentId(comment.getParentId());
        vo.setImages(comment.getImages());
        vo.setStatus(comment.getStatus());
        vo.setReply(comment.getReplyContent());
        vo.setReplyTime(comment.getReplyTime());
        vo.setCreateTime(comment.getCreatedTime());

        // 获取用户信息 - 优先从预加载 Map 取
        if (comment.getUserId() != null) {
            User user = userMap != null ? userMap.get(comment.getUserId()) : userService.getById(comment.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setUserAvatar(user.getAvatar());
            }
        }

        // 获取无人机信息 - 优先从预加载 Map 取
        if (comment.getDroneId() != null) {
            Drone drone = droneMap != null ? droneMap.get(comment.getDroneId()) : droneService.getById(comment.getDroneId());
            if (drone != null) {
                vo.setDroneModel(drone.getModel());
                vo.setDroneImage(drone.getImage());
                vo.setDroneBrand(drone.getBrand());
            }
        }

        return vo;
    }

    @Override
    public IPage<Comment> pageComments(Integer pageNum, Integer pageSize, Long droneId, Integer status,
                                        String userName, String droneModel, Integer rating, Boolean hasReply) {
        Page<Comment> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (droneId != null) {
            wrapper.eq(Comment::getDroneId, droneId);
        }
        if (status != null) {
            wrapper.eq(Comment::getStatus, status);
        }
        // 根据用户昵称查询 - 先查出符合条件的userId列表
        if (org.springframework.util.StringUtils.hasText(userName)) {
            List<Long> userIds = userService.list(
                new LambdaQueryWrapper<User>().like(User::getNickname, userName)
            ).stream().map(User::getId).collect(java.util.stream.Collectors.toList());
            if (userIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(Comment::getUserId, userIds);
        }
        // 根据设备型号查询 - 先查出符合条件的droneId列表
        if (org.springframework.util.StringUtils.hasText(droneModel)) {
            List<Long> droneIds = droneService.list(
                new LambdaQueryWrapper<Drone>().like(Drone::getModel, droneModel)
            ).stream().map(Drone::getId).collect(java.util.stream.Collectors.toList());
            if (droneIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(Comment::getDroneId, droneIds);
        }
        // 根据评分查询
        if (rating != null) {
            if (rating == 2) {
                // 2星及以下
                wrapper.le(Comment::getRating, 2);
            } else {
                wrapper.eq(Comment::getRating, rating);
            }
        }
        // 根据是否已回复查询
        if (hasReply != null) {
            if (hasReply) {
                wrapper.isNotNull(Comment::getReplyContent);
            } else {
                wrapper.isNull(Comment::getReplyContent);
            }
        }
        wrapper.orderByDesc(Comment::getCreatedTime);

        return this.page(page, wrapper);
    }

    @Override
    public void updateCommentStatus(Long id, Integer status) {
        Comment comment = this.getById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_EXIST);
        }

        comment.setStatus(status);
        this.updateById(comment);
    }

    @Override
    public void replyComment(Long id, String replyContent) {
        Comment comment = this.getById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_EXIST);
        }

        comment.setReplyContent(replyContent);
        comment.setReplyTime(LocalDateTime.now());
        this.updateById(comment);
    }
}
