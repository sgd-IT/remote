package com.enjoyiot.module.eiot.service.firewatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyScheduleDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDutyRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDutyScheduleMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * 值班记录管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireDutyRecordServiceImpl implements FireDutyRecordService {

    @Resource
    private FireDutyRecordMapper fireDutyRecordMapper;

    @Resource
    private FireDutyScheduleMapper fireDutyScheduleMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(FireDutyRecordCreateReqVO createReqVO) {
        // 获取物业名称
        String propertyName = getPropertyName(createReqVO.getPropertyId());
        
        FireDutyRecordDO record = FireDutyRecordDO.builder()
                .scheduleId(createReqVO.getScheduleId())
                .propertyId(createReqVO.getPropertyId())
                .propertyName(propertyName)
                .recordTime(createReqVO.getRecordTime() != null ? createReqVO.getRecordTime() : LocalDateTime.now())
                .recordType(createReqVO.getRecordType())
                .title(createReqVO.getTitle())
                .content(createReqVO.getContent())
                .deviceId(createReqVO.getDeviceId())
                .deviceName(createReqVO.getDeviceName())
                .orderId(createReqVO.getOrderId())
                .recorderId(createReqVO.getRecorderId())
                .recorderName(createReqVO.getRecorderName())
                .attachments(createReqVO.getAttachments())
                .status(createReqVO.getStatus() != null ? createReqVO.getStatus() : "normal")
                .remark(createReqVO.getRemark())
                .build();
        
        fireDutyRecordMapper.insert(record);
        
        log.info("[createRecord] 创建值班记录成功，类型：{}，标题：{}", createReqVO.getRecordType(), createReqVO.getTitle());
        
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(FireDutyRecordUpdateReqVO updateReqVO) {
        FireDutyRecordDO existRecord = fireDutyRecordMapper.selectById(updateReqVO.getId());
        if (existRecord == null) {
            throw new IllegalArgumentException("值班记录不存在");
        }
        
        FireDutyRecordDO updateObj = FireDutyRecordDO.builder()
                .id(updateReqVO.getId())
                .title(updateReqVO.getTitle())
                .content(updateReqVO.getContent())
                .attachments(updateReqVO.getAttachments())
                .status(updateReqVO.getStatus())
                .remark(updateReqVO.getRemark())
                .build();
        
        fireDutyRecordMapper.updateById(updateObj);
        
        log.info("[updateRecord] 更新值班记录成功，记录ID：{}", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        FireDutyRecordDO existRecord = fireDutyRecordMapper.selectById(id);
        if (existRecord == null) {
            throw new IllegalArgumentException("值班记录不存在");
        }
        
        fireDutyRecordMapper.deleteById(id);
        
        log.info("[deleteRecord] 删除值班记录成功，记录ID：{}", id);
    }

    @Override
    public FireDutyRecordRespVO getRecord(Long id) {
        return tenantAware(() -> {
            FireDutyRecordDO record = fireDutyRecordMapper.selectById(id);
            if (record == null) {
                return null;
            }
            return convertToRespVO(record);
        });
    }

    @Override
    public PageResult<FireDutyRecordRespVO> pageRecords(FireDutyRecordPageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireDutyRecordDO> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(pageReqVO.getScheduleId() != null, FireDutyRecordDO::getScheduleId, pageReqVO.getScheduleId());
        wrapper.eq(pageReqVO.getPropertyId() != null, FireDutyRecordDO::getPropertyId, pageReqVO.getPropertyId());
        wrapper.ge(pageReqVO.getRecordTimeStart() != null, FireDutyRecordDO::getRecordTime, pageReqVO.getRecordTimeStart());
        wrapper.le(pageReqVO.getRecordTimeEnd() != null, FireDutyRecordDO::getRecordTime, pageReqVO.getRecordTimeEnd());
        wrapper.eq(pageReqVO.getRecordType() != null, FireDutyRecordDO::getRecordType, pageReqVO.getRecordType());
        wrapper.like(pageReqVO.getRecorderName() != null, FireDutyRecordDO::getRecorderName, pageReqVO.getRecorderName());
        wrapper.eq(pageReqVO.getStatus() != null, FireDutyRecordDO::getStatus, pageReqVO.getStatus());
        
        wrapper.orderByDesc(FireDutyRecordDO::getRecordTime);
        
        PageResult<FireDutyRecordDO> pageResult = fireDutyRecordMapper.selectPage(pageReqVO, wrapper);
        
        List<FireDutyRecordRespVO> list = new ArrayList<>();
        for (FireDutyRecordDO record : pageResult.getList()) {
            list.add(convertToRespVO(record));
        }
        
        return new PageResult<>(list, pageResult.getTotal());
        });
    }

    @Override
    public List<FireDutyRecordRespVO> listRecordsBySchedule(Long scheduleId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<FireDutyRecordDO> records = fireDutyRecordMapper.selectByScheduleId(tenantId, scheduleId);
        
        List<FireDutyRecordRespVO> list = new ArrayList<>();
        for (FireDutyRecordDO record : records) {
            list.add(convertToRespVO(record));
        }
        
        return list;
    }

    @Override
    public List<FireDutyRecordRespVO> listRecordsByTimeRange(Long propertyId, LocalDateTime startTime, LocalDateTime endTime) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<FireDutyRecordDO> records = fireDutyRecordMapper.selectByTimeRange(tenantId, startTime, endTime);
        
        List<FireDutyRecordRespVO> list = new ArrayList<>();
        for (FireDutyRecordDO record : records) {
            if (propertyId == null || propertyId.equals(record.getPropertyId())) {
                list.add(convertToRespVO(record));
            }
        }
        
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHandoverRecord(Long scheduleId, String handoverContent, Long nextDutyUserId) {
        FireDutyScheduleDO schedule = fireDutyScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("排班记录不存在");
        }
        
        FireDutyRecordCreateReqVO createReqVO = new FireDutyRecordCreateReqVO();
        createReqVO.setScheduleId(scheduleId);
        createReqVO.setPropertyId(schedule.getPropertyId());
        createReqVO.setRecordType("handover");
        createReqVO.setTitle("交接班记录");
        createReqVO.setContent(handoverContent);
        createReqVO.setRecorderId(schedule.getDutyUserId());
        createReqVO.setRecorderName(schedule.getDutyUserName());
        createReqVO.setStatus("normal");
        
        return createRecord(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPatrolRecord(Long scheduleId, String content, String status, String attachments) {
        FireDutyScheduleDO schedule = fireDutyScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("排班记录不存在");
        }
        
        FireDutyRecordCreateReqVO createReqVO = new FireDutyRecordCreateReqVO();
        createReqVO.setScheduleId(scheduleId);
        createReqVO.setPropertyId(schedule.getPropertyId());
        createReqVO.setRecordType("patrol");
        createReqVO.setTitle("巡查记录");
        createReqVO.setContent(content);
        createReqVO.setRecorderId(schedule.getDutyUserId());
        createReqVO.setRecorderName(schedule.getDutyUserName());
        createReqVO.setStatus(status);
        createReqVO.setAttachments(attachments);
        
        return createRecord(createReqVO);
    }

    @Override
    public Map<String, Object> getRecordStatistics(Long propertyId, LocalDateTime startTime, LocalDateTime endTime) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        
        Map<String, Object> result = new HashMap<>();
        
        // 按类型统计
        List<Map<String, Object>> typeStats = fireDutyRecordMapper.countByType(tenantId, startTime, endTime);
        Map<String, Integer> typeCount = new HashMap<>();
        typeCount.put("patrol", 0);
        typeCount.put("alarm", 0);
        typeCount.put("handover", 0);
        typeCount.put("other", 0);
        for (Map<String, Object> stat : typeStats) {
            String type = (String) stat.get("record_type");
            Long count = (Long) stat.get("count");
            typeCount.put(type, count.intValue());
        }
        result.put("typeStatistics", typeCount);
        
        // 按状态统计
        List<Map<String, Object>> statusStats = fireDutyRecordMapper.countByStatus(tenantId, startTime, endTime);
        Map<String, Integer> statusCount = new HashMap<>();
        statusCount.put("normal", 0);
        statusCount.put("abnormal", 0);
        for (Map<String, Object> stat : statusStats) {
            String status = (String) stat.get("status");
            Long count = (Long) stat.get("count");
            statusCount.put(status, count.intValue());
        }
        result.put("statusStatistics", statusCount);
        
        return result;
    }

    /**
     * 获取物业名称（忽略租户过滤，因为 property/list 接口也是 @TenantIgnore 跨租户的）
     */
    private String getPropertyName(Long propertyId) {
        if (propertyId == null) return null;
        return TenantUtils.executeIgnoreWithResult(() -> {
            FirePropertyDO property = firePropertyMapper.selectById(propertyId);
            return property != null ? property.getPropertyName() : null;
        });
    }

    /**
     * 获取记录类型名称
     */
    private String getRecordTypeName(String recordType) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("patrol", "巡查记录");
        typeMap.put("alarm", "告警处理");
        typeMap.put("handover", "交接班");
        typeMap.put("other", "其他");
        return typeMap.getOrDefault(recordType, recordType);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("normal", "正常");
        statusMap.put("abnormal", "异常");
        return statusMap.getOrDefault(status, status);
    }

    /**
     * 转换为响应VO
     */
    private FireDutyRecordRespVO convertToRespVO(FireDutyRecordDO record) {
        FireDutyRecordRespVO vo = new FireDutyRecordRespVO();
        vo.setId(record.getId());
        vo.setScheduleId(record.getScheduleId());
        vo.setPropertyId(record.getPropertyId());
        vo.setPropertyName(record.getPropertyName());
        vo.setRecordTime(record.getRecordTime());
        vo.setRecordType(record.getRecordType());
        vo.setRecordTypeName(getRecordTypeName(record.getRecordType()));
        vo.setTitle(record.getTitle());
        vo.setContent(record.getContent());
        vo.setDeviceId(record.getDeviceId());
        vo.setDeviceName(record.getDeviceName());
        vo.setOrderId(record.getOrderId());
        vo.setRecorderId(record.getRecorderId());
        vo.setRecorderName(record.getRecorderName());
        vo.setAttachments(record.getAttachments());
        vo.setStatus(record.getStatus());
        vo.setStatusName(getStatusName(record.getStatus()));
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private boolean isSystemTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId == 1L;
    }

    private <T> T tenantAware(Supplier<T> supplier) {
        if (isSystemTenant()) {
            return TenantUtils.executeIgnoreWithResult(supplier::get);
        }
        return supplier.get();
    }

}
