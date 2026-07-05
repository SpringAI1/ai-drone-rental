package com.drone.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.Constants;
import com.drone.rental.common.ResultCode;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dto.AirspaceRecordDTO;
import com.drone.rental.dto.AuditDTO;
import com.drone.rental.entity.AirspaceRecord;
import com.drone.rental.mapper.AirspaceRecordMapper;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.AirspaceRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空域备案服务实现类
 */
@Service
public class AirspaceRecordServiceImpl extends ServiceImpl<AirspaceRecordMapper, AirspaceRecord>
        implements AirspaceRecordService {

    @Override
    public AirspaceRecord submitAirspaceRecord(AirspaceRecordDTO dto) {
        Long userId = UserContext.getCurrentUserId();

        // 基础校验
        if (dto.getRegionName() == null || dto.getRegionName().trim().isEmpty()) {
            throw new BusinessException("飞行区域名称不能为空");
        }
        if (dto.getMaxAltitude() == null || dto.getMaxAltitude() <= 0) {
            throw new BusinessException("请输入有效的最大飞行高度");
        }
        if (dto.getPlannedStartTime() == null) {
            throw new BusinessException("请输入有效的开始时间");
        }
        if (dto.getPlannedEndTime() == null) {
            throw new BusinessException("请输入有效的结束时间");
        }
        if (dto.getPlannedEndTime().isBefore(dto.getPlannedStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }

        AirspaceRecord record = new AirspaceRecord();
        BeanUtils.copyProperties(dto, record);
        record.setUserId(userId);
        record.setAuditStatus(Constants.AUDIT_STATUS_PENDING);

        this.save(record);
        return record;
    }

    @Override
    public List<AirspaceRecord> getCurrentUserAirspaceRecords() {
        Long userId = UserContext.getCurrentUserId();
        return this.list(new LambdaQueryWrapper<AirspaceRecord>()
                .eq(AirspaceRecord::getUserId, userId)
                .orderByDesc(AirspaceRecord::getCreatedTime));
    }

    @Override
    public List<AirspaceRecord> getCurrentUserApprovedRecords() {
        Long userId = UserContext.getCurrentUserId();
        return this.list(new LambdaQueryWrapper<AirspaceRecord>()
                .eq(AirspaceRecord::getUserId, userId)
                .eq(AirspaceRecord::getAuditStatus, Constants.AUDIT_STATUS_APPROVED)
                .orderByDesc(AirspaceRecord::getCreatedTime));
    }

    @Override
    public IPage<AirspaceRecord> pageAirspaceRecords(Integer pageNum, Integer pageSize,
                                                      Integer auditStatus, String regionName) {
        Page<AirspaceRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AirspaceRecord> wrapper = new LambdaQueryWrapper<>();

        if (auditStatus != null) {
            wrapper.eq(AirspaceRecord::getAuditStatus, auditStatus);
        }
        if (StringUtils.hasText(regionName)) {
            wrapper.like(AirspaceRecord::getRegionName, regionName);
        }
        wrapper.orderByDesc(AirspaceRecord::getCreatedTime);

        return this.page(page, wrapper);
    }

    @Override
    public void auditAirspaceRecord(Long id, AuditDTO dto) {
        AirspaceRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.AIRSPACE_NOT_EXIST);
        }

        if (record.getAuditStatus() != Constants.AUDIT_STATUS_PENDING) {
            throw new BusinessException("该备案已审核，不能重复审核");
        }

        record.setAuditStatus(dto.getAuditStatus());
        record.setAuditRemark(dto.getAuditRemark());
        record.setAuditTime(LocalDateTime.now());
        record.setAuditorId(UserContext.getCurrentUserId());

        this.updateById(record);
    }

    @Override
    public void checkAirspaceRecordValid(Long airspaceRecordId, Long userId) {
        AirspaceRecord record = this.getById(airspaceRecordId);
        if (record == null) {
            throw new BusinessException(ResultCode.AIRSPACE_NOT_EXIST);
        }

        // 检查是否是该用户的备案
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("该空域备案不属于当前用户");
        }

        // 检查是否已审核通过
        if (record.getAuditStatus() != Constants.AUDIT_STATUS_APPROVED) {
            throw new BusinessException(ResultCode.AIRSPACE_NOT_APPROVED);
        }
    }
}
