package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enjoyiot.framework.common.exception.ServiceException;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireHazardDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireHazardMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.system.api.user.AdminUserApi;
import com.enjoyiot.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * 隐患管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireHazardServiceImpl implements FireHazardService {

    @Resource
    private FireHazardMapper fireHazardMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Resource
    private EiotDeviceInfoMapper eiotDeviceInfoMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHazard(FireHazardCreateReqVO createReqVO) {
        log.info("[createHazard] ====== 开始创建隐患 =====");
        log.info("[createHazard] 请求参数: propertyId={}, hazardLevel={}, hazardDesc={}, foundUserName={}, foundDate={}, deadline={}, deviceId={}",
                createReqVO.getPropertyId(), createReqVO.getHazardLevel(), createReqVO.getHazardDesc(),
                createReqVO.getFoundUserName(), createReqVO.getFoundDate(), createReqVO.getDeadline(),
                createReqVO.getDeviceId());

        try {
            String propertyName = getPropertyName(createReqVO.getPropertyId());
            String deviceName = getDeviceName(createReqVO.getDeviceId());


            FireHazardDO hazard = new FireHazardDO();
            hazard.setHazardNo(generateHazardNo());
            hazard.setPropertyId(createReqVO.getPropertyId());
            hazard.setPropertyName(propertyName);
            hazard.setDeviceId(createReqVO.getDeviceId());
            hazard.setDeviceName(deviceName);
            hazard.setHazardLevel(createReqVO.getHazardLevel());
            hazard.setHazardDesc(createReqVO.getHazardDesc());
            hazard.setLocation(createReqVO.getLocation());
            hazard.setFoundUserName(createReqVO.getFoundUserName());
            hazard.setFoundDate(createReqVO.getFoundDate());
            hazard.setDeadline(createReqVO.getDeadline());
            hazard.setSuggestion(createReqVO.getSuggestion());
            hazard.setPhotos(createReqVO.getPhotos());
            hazard.setStatus("pending");
            hazard.setRectificationProgress(0);
            hazard.setRemark(createReqVO.getRemark());
            hazard.setTenantId(TenantContextHolder.getTenantId());

            log.info("[createHazard] 准备插入数据, tenantId={}", hazard.getTenantId());
            fireHazardMapper.insert(hazard);
            log.info("[createHazard] 隐患创建成功, id={}, hazardNo={}", hazard.getId(), hazard.getHazardNo());

            log.info("[createHazard] ====== 创建隐患完成 ====== 隐患编号：{}", hazard.getHazardNo());
            return hazard.getId();
        } catch (Exception e) {
            log.error("[createHazard] 创建隐患异常", e);
            throw new ServiceException(500, "创建隐患失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHazard(FireHazardUpdateReqVO updateReqVO) {
        FireHazardDO existHazard = fireHazardMapper.selectById(updateReqVO.getId());
        if (existHazard == null) {
            throw new IllegalArgumentException("隐患不存在");
        }

        FireHazardDO updateObj = FireHazardDO.builder()
                .id(updateReqVO.getId())
                .hazardLevel(updateReqVO.getHazardLevel())
                .hazardDesc(updateReqVO.getHazardDesc())
                .location(updateReqVO.getLocation())
                .deviceId(updateReqVO.getDeviceId())
                .deviceName(getDeviceName(updateReqVO.getDeviceId()))
                .foundUserName(updateReqVO.getFoundUserName())
                .foundDate(updateReqVO.getFoundDate())
                .deadline(updateReqVO.getDeadline())
                .suggestion(updateReqVO.getSuggestion())
                .photos(updateReqVO.getPhotos())
                .remark(updateReqVO.getRemark())
                .build();

        fireHazardMapper.updateById(updateObj);
        log.info("[updateHazard] 更新隐患成功, id={}", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHazard(Long id) {
        FireHazardDO existHazard = fireHazardMapper.selectById(id);
        if (existHazard == null) {
            throw new IllegalArgumentException("隐患不存在");
        }
        fireHazardMapper.deleteById(id);
        log.info("[deleteHazard] 删除隐患成功, id={}", id);
    }

    @Override
    public FireHazardRespVO getHazard(Long id) {
        return tenantAware(() -> {
            FireHazardDO hazard = fireHazardMapper.selectById(id);
            if (hazard == null) {
                return null;
            }
            return convertToRespVO(hazard);
        });
    }

    @Override
    public PageResult<FireHazardRespVO> pageHazards(FireHazardPageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireHazardDO> wrapper = new LambdaQueryWrapper<>();

            wrapper.eq(pageReqVO.getPropertyId() != null, FireHazardDO::getPropertyId, pageReqVO.getPropertyId());
            wrapper.eq(pageReqVO.getHazardLevel() != null, FireHazardDO::getHazardLevel, pageReqVO.getHazardLevel());
            wrapper.eq(pageReqVO.getStatus() != null, FireHazardDO::getStatus, pageReqVO.getStatus());
            wrapper.ge(pageReqVO.getFoundDateStart() != null, FireHazardDO::getFoundDate, pageReqVO.getFoundDateStart());
            wrapper.le(pageReqVO.getFoundDateEnd() != null, FireHazardDO::getFoundDate, pageReqVO.getFoundDateEnd());

            // 关键字搜索：隐患描述或设备名称
            if (pageReqVO.getKeyword() != null) {
                wrapper.and(w -> w
                        .like(FireHazardDO::getHazardDesc, pageReqVO.getKeyword())
                        .or()
                        .like(FireHazardDO::getDeviceName, pageReqVO.getKeyword())
                );
            }

            wrapper.orderByDesc(FireHazardDO::getFoundDate);

            PageResult<FireHazardDO> pageResult = fireHazardMapper.selectPage(pageReqVO, wrapper);

            List<FireHazardRespVO> list = new ArrayList<>();
            for (FireHazardDO hazard : pageResult.getList()) {
                list.add(convertToRespVO(hazard));
            }

            return new PageResult<>(list, pageResult.getTotal());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRectification(FireHazardRectificationReqVO reqVO) {
        FireHazardDO hazard = fireHazardMapper.selectById(reqVO.getId());
        if (hazard == null) {
            throw new IllegalArgumentException("隐患不存在");
        }

        // 构建整改记录
        String rectificationUserName = getRectificationUserName(reqVO.getRectificationUserId());
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("type", "progress");
        record.put("title", "提交整改进度(" + reqVO.getProgress() + "%)");
        record.put("content", reqVO.getMeasure());
        record.put("rectificationUserId", reqVO.getRectificationUserId());
        record.put("rectificationUserName", rectificationUserName);
        record.put("progress", reqVO.getProgress());
        record.put("remark", reqVO.getRemark());
        record.put("photos", reqVO.getPhotos());
        record.put("createTime", LocalDateTime.now().toString());

        // 合并历史记录
        String existingRecords = hazard.getRectificationRecords();
        List<Map<String, Object>> recordsList;
        if (existingRecords != null && !existingRecords.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parsedList = (List<Map<String, Object>>) (List<?>)
                        cn.hutool.json.JSONUtil.toList(
                                cn.hutool.json.JSONUtil.parseArray(existingRecords),
                                Map.class
                        );
                recordsList = new ArrayList<>(parsedList);
            } catch (Exception e) {
                log.warn("[submitRectification] 解析历史整改记录失败, 将覆盖: {}", e.getMessage());
                recordsList = new ArrayList<>();
            }
        } else {
            recordsList = new ArrayList<>();
        }
        recordsList.add(record);

        String newStatus;
        if (Boolean.TRUE.equals(reqVO.getApplyReview()) && reqVO.getProgress() >= 100) {
            newStatus = "reviewing";
        } else {
            newStatus = "rectifying";
        }

        fireHazardMapper.update(null, new LambdaUpdateWrapper<FireHazardDO>()
                .eq(FireHazardDO::getId, reqVO.getId())
                .set(FireHazardDO::getRectificationUserId, reqVO.getRectificationUserId())
                .set(FireHazardDO::getRectificationUserName, rectificationUserName)
                .set(FireHazardDO::getMeasure, reqVO.getMeasure())
                .set(FireHazardDO::getRectificationProgress, reqVO.getProgress())
                .set(FireHazardDO::getRectificationRecords, cn.hutool.json.JSONUtil.toJsonStr(recordsList))
                .set(FireHazardDO::getStatus, newStatus));

        log.info("[submitRectification] 提交整改进度成功, id={}, progress={}%, status={}",
                reqVO.getId(), reqVO.getProgress(), newStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRectification(Long id, String result, String remark) {
        FireHazardDO hazard = fireHazardMapper.selectById(id);
        if (hazard == null) {
            throw new IllegalArgumentException("隐患不存在");
        }

        if (!"reviewing".equals(hazard.getStatus())) {
            throw new IllegalArgumentException("当前状态不允许验收");
        }

        // 构建验收记录
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("type", "review");
        record.put("title", "review".equals(result) ? "验收不通过" : "验收通过");
        record.put("content", remark);
        record.put("reviewResult", result);
        record.put("createTime", LocalDateTime.now().toString());

        String existingRecords = hazard.getRectificationRecords();
        List<Map<String, Object>> recordsList;
        if (existingRecords != null && !existingRecords.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parsedList = (List<Map<String, Object>>) (List<?>)
                        cn.hutool.json.JSONUtil.toList(
                                cn.hutool.json.JSONUtil.parseArray(existingRecords),
                                Map.class
                        );
                recordsList = new ArrayList<>(parsedList);
            } catch (Exception e) {
                log.warn("[reviewRectification] 解析历史整改记录失败: {}", e.getMessage());
                recordsList = new ArrayList<>();
            }
        } else {
            recordsList = new ArrayList<>();
        }
        recordsList.add(record);

        String newStatus;
        if ("approved".equals(result)) {
            newStatus = "closed";
        } else {
            newStatus = "rectifying";
        }

        fireHazardMapper.update(null, new LambdaUpdateWrapper<FireHazardDO>()
                .eq(FireHazardDO::getId, id)
                .set(FireHazardDO::getReviewResult, result)
                .set(FireHazardDO::getReviewRemark, remark)
                .set(FireHazardDO::getReviewTime, LocalDateTime.now())
                .set(FireHazardDO::getRectificationRecords, cn.hutool.json.JSONUtil.toJsonStr(recordsList))
                .set(FireHazardDO::getStatus, newStatus)
                .set(newStatus.equals("closed"), FireHazardDO::getRectificationProgress, 100));

        log.info("[reviewRectification] 验收复查完成, id={}, result={}, newStatus={}", id, result, newStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeHazard(Long id, String remark) {
        FireHazardDO hazard = fireHazardMapper.selectById(id);
        if (hazard == null) {
            throw new IllegalArgumentException("隐患不存在");
        }

        fireHazardMapper.update(null, new LambdaUpdateWrapper<FireHazardDO>()
                .eq(FireHazardDO::getId, id)
                .set(FireHazardDO::getStatus, "cancelled")
                .set(FireHazardDO::getCloseRemark, remark));

        log.info("[closeHazard] 关闭隐患成功, id={}", id);
    }

    @Override
    public Map<String, Object> getHazardStatistics(Long propertyId, LocalDate startDate, LocalDate endDate) {
        return tenantAware(() -> {
            Long tenantId = isSystemTenant() ? null : TenantContextHolder.getRequiredTenantId();

            Map<String, Object> result = new HashMap<>();

            // 按等级统计
            List<Map<String, Object>> levelStats = fireHazardMapper.countByLevel(tenantId, propertyId, startDate, endDate);
            Map<String, Integer> levelCount = new HashMap<>();
            levelCount.put("general", 0);
            levelCount.put("major", 0);
            levelCount.put("critical", 0);
            for (Map<String, Object> stat : levelStats) {
                String level = (String) stat.get("hazard_level");
                Long count = (Long) stat.get("count");
                levelCount.put(level, count.intValue());
            }
            result.put("levelStatistics", levelCount);

            // 按状态统计
            List<Map<String, Object>> statusStats = fireHazardMapper.countByStatus(tenantId, propertyId, startDate, endDate);
            Map<String, Integer> statusCount = new HashMap<>();
            statusCount.put("pending", 0);
            statusCount.put("rectifying", 0);
            statusCount.put("reviewing", 0);
            statusCount.put("closed", 0);
            statusCount.put("cancelled", 0);
            for (Map<String, Object> stat : statusStats) {
                String status = (String) stat.get("status");
                Long count = (Long) stat.get("count");
                statusCount.put(status, count.intValue());
            }
            result.put("statusStatistics", statusCount);

            // 按趋势统计
            List<Map<String, Object>> trendStats = fireHazardMapper.countTrend(tenantId, propertyId, startDate, endDate);
            result.put("trendStatistics", trendStats);

            return result;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importHazards(List<FireHazardImportVO> list) {
        log.info("[importHazards] 开始导入隐患，共 {} 条", list.size());

        int successCount = 0;
        int failCount = 0;
        List<Map<String, String>> failDetails = new ArrayList<>();

        Long tenantId = TenantContextHolder.getTenantId();
        String propertyName = null;
        if (tenantId != null) {
            propertyName = getPropertyName(tenantId);
        }

        for (int i = 0; i < list.size(); i++) {
            FireHazardImportVO importVO = list.get(i);
            try {
                if (importVO.getHazardDesc() == null || importVO.getHazardDesc().isEmpty()) {
                    throw new IllegalArgumentException("第" + (i + 1) + "行：隐患描述为空");
                }

                FireHazardDO hazard = new FireHazardDO();
                hazard.setHazardNo(generateHazardNo());
                hazard.setPropertyId(tenantId);
                hazard.setPropertyName(propertyName);
                hazard.setHazardLevel(importVO.getHazardLevel() != null ? importVO.getHazardLevel() : "general");
                hazard.setHazardDesc(importVO.getHazardDesc());
                hazard.setLocation(importVO.getLocation());
                hazard.setFoundUserName(importVO.getFoundUserName());
                hazard.setFoundDate(importVO.getFoundDate());
                hazard.setDeadline(importVO.getDeadline());
                hazard.setSuggestion(importVO.getSuggestion());
                hazard.setStatus("pending");
                hazard.setRectificationProgress(0);
                hazard.setRemark(importVO.getRemark());
                hazard.setTenantId(TenantContextHolder.getTenantId());

                fireHazardMapper.insert(hazard);

                log.info("[importHazards] 导入隐患成功: hazardDesc={}", hazard.getHazardDesc());
                successCount++;
            } catch (Exception e) {
                log.error("[importHazards] 导入隐患失败: 第{}行, {}", i + 1, e.getMessage());
                failCount++;
                Map<String, String> failItem = new HashMap<>();
                failItem.put("row", String.valueOf(i + 1));
                failItem.put("name", importVO.getHazardDesc() != null ? importVO.getHazardDesc() : "");
                failItem.put("reason", e.getMessage());
                failDetails.add(failItem);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failDetails", failDetails);
        log.info("[importHazards] 导入完成: 成功={}, 失败={}", successCount, failCount);
        return result;
    }

    /**
     * 生成隐患编号
     */
    private String generateHazardNo() {
        return "HZ" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }

    /**
     * 获取物业名称（忽略租户过滤）
     */
    private String getPropertyName(Long propertyId) {
        if (propertyId == null) return null;
        return TenantUtils.executeIgnoreWithResult(() -> {
            FirePropertyDO property = firePropertyMapper.selectById(propertyId);
            if (property == null) {
                log.warn("[getPropertyName] 物业不存在, propertyId={}", propertyId);
                return null;
            }
            return property.getPropertyName();
        });
    }

    /**
     * 获取设备名称（忽略租户过滤）
     */
    private String getDeviceName(Long deviceId) {
        if (deviceId == null) return null;
        return TenantUtils.executeIgnoreWithResult(() -> {
            EiotDeviceInfoDO device = eiotDeviceInfoMapper.selectById(deviceId);
            if (device == null) {
                log.warn("[getDeviceName] 设备不存在, deviceId={}", deviceId);
                return null;
            }
            return device.getName();
        });
    }

    /**
     * 获取整改人姓名
     */
    private String getRectificationUserName(Long userId) {
        if (userId == null) return null;
        return TenantUtils.executeIgnoreWithResult(() -> {
            try {
                AdminUserRespDTO user = adminUserApi.getUser(userId);
                if (user != null) {
                    return user.getNickname();
                }
            } catch (Exception e) {
                log.warn("[getRectificationUserName] 查询用户信息失败, userId={}", userId, e);
            }
            return null;
        });
    }

    /**
     * 转换为响应VO
     */
    private FireHazardRespVO convertToRespVO(FireHazardDO hazard) {
        FireHazardRespVO vo = new FireHazardRespVO();
        vo.setId(hazard.getId());
        vo.setHazardNo(hazard.getHazardNo());
        vo.setPropertyId(hazard.getPropertyId());
        if (hazard.getPropertyName() == null) {
            vo.setPropertyName(getPropertyName(hazard.getPropertyId()));
        } else {
            vo.setPropertyName(hazard.getPropertyName());
        }
        vo.setDeviceId(hazard.getDeviceId());
        vo.setDeviceName(hazard.getDeviceName());
        vo.setHazardLevel(hazard.getHazardLevel());
        vo.setHazardLevelName(getHazardLevelName(hazard.getHazardLevel()));
        vo.setHazardDesc(hazard.getHazardDesc());
        vo.setLocation(hazard.getLocation());
        vo.setFoundUserName(hazard.getFoundUserName());
        vo.setFoundDate(hazard.getFoundDate());
        vo.setDeadline(hazard.getDeadline());
        vo.setSuggestion(hazard.getSuggestion());
        vo.setPhotos(hazard.getPhotos());
        vo.setStatus(hazard.getStatus());
        vo.setStatusName(getStatusName(hazard.getStatus()));
        vo.setRectificationUserId(hazard.getRectificationUserId());
        vo.setRectificationUserName(hazard.getRectificationUserName());
        vo.setMeasure(hazard.getMeasure());
        vo.setRectificationProgress(hazard.getRectificationProgress());
        vo.setRectificationRecords(hazard.getRectificationRecords());
        vo.setReviewResult(hazard.getReviewResult());
        vo.setReviewRemark(hazard.getReviewRemark());
        vo.setReviewTime(hazard.getReviewTime());
        vo.setCloseRemark(hazard.getCloseRemark());
        vo.setRemark(hazard.getRemark());
        vo.setCreateTime(hazard.getCreateTime());
        return vo;
    }

    /**
     * 获取隐患等级名称
     */
    private String getHazardLevelName(String level) {
        Map<String, String> levelMap = new HashMap<>();
        levelMap.put("general", "一般隐患");
        levelMap.put("major", "较大隐患");
        levelMap.put("critical", "重大隐患");
        return levelMap.getOrDefault(level, level);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("pending", "待整改");
        statusMap.put("rectifying", "整改中");
        statusMap.put("reviewing", "待验收");
        statusMap.put("closed", "已闭环");
        statusMap.put("cancelled", "已关闭");
        return statusMap.getOrDefault(status, status);
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
