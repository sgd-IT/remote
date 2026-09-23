package com.enjoyiot.module.eiot.service.firewatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enjoyiot.framework.common.exception.ServiceException;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.excel.core.util.ExcelImportUtils;
import com.enjoyiot.framework.excel.core.util.ExcelUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyScheduleDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDutyScheduleMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.system.api.user.AdminUserApi;
import com.enjoyiot.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 值班管理管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireDutyScheduleServiceImpl implements FireDutyScheduleService {

    @Resource
    private FireDutyScheduleMapper fireDutyScheduleMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(FireDutyScheduleCreateReqVO createReqVO) {
        // 记录请求参数和租户上下文，方便排查问题
        Long tenantId = TenantContextHolder.getTenantId();
        log.info("[createSchedule] 开始创建排班，tenantId={}, propertyId={}, scheduleDate={}, shiftType={}, dutyUserId={}",
                tenantId, createReqVO.getPropertyId(), createReqVO.getScheduleDate(),
                createReqVO.getShiftType(), createReqVO.getDutyUserId());

        // 获取物业名称
        String propertyName = getPropertyName(createReqVO.getPropertyId());
        log.info("[createSchedule] 获取物业名称结果: propertyId={}, propertyName={}", createReqVO.getPropertyId(), propertyName);

        // 同一人员检查是否已存在相同日期和班次的排班
        LambdaQueryWrapper<FireDutyScheduleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FireDutyScheduleDO::getPropertyId, createReqVO.getPropertyId())
                .eq(FireDutyScheduleDO::getScheduleDate, createReqVO.getScheduleDate())
                .eq(FireDutyScheduleDO::getShiftType, createReqVO.getShiftType())
                .eq(FireDutyScheduleDO::getDutyUserId, createReqVO.getDutyUserId());
        FireDutyScheduleDO existSchedule = fireDutyScheduleMapper.selectOne(wrapper);
        if (existSchedule != null) {
            log.warn("[createSchedule] 排班已存在，propertyId={}, date={}, shiftType={}",
                    createReqVO.getPropertyId(), createReqVO.getScheduleDate(), createReqVO.getShiftType());
            throw new ServiceException(400, "该日期和班次已存在排班记录");
        }

        FireDutyScheduleDO schedule = FireDutyScheduleDO.builder()
                .propertyId(createReqVO.getPropertyId())
                .propertyName(propertyName)
                .scheduleDate(createReqVO.getScheduleDate())
                .shiftType(createReqVO.getShiftType())
                .shiftName(createReqVO.getShiftName() != null ? createReqVO.getShiftName() : getShiftName(createReqVO.getShiftType()))
                .startTime(createReqVO.getStartTime())
                .endTime(createReqVO.getEndTime())
                .dutyUserId(createReqVO.getDutyUserId())
                .dutyUserName(createReqVO.getDutyUserName())
                .dutyUserPhone(createReqVO.getDutyUserPhone())
                .dutyUserPosition(createReqVO.getDutyUserPosition())
                .status("scheduled")
                .remark(createReqVO.getRemark())
                .build();

        log.warn("[createSchedule] 准备插入排班记录: {}", schedule);

        try {
            fireDutyScheduleMapper.insert(schedule);
        } catch (DataIntegrityViolationException e) {
            // 数据库唯一约束冲突（uk_tenant_property_date_shift），说明数据库中存在完全重复的排班记录
            String rootMsg = cn.hutool.core.exceptions.ExceptionUtil.getRootCauseMessage(e);
            log.error("[createSchedule] 创建排班失败，数据库唯一约束冲突！原因：数据库中已存在一条相同物业、相同日期、相同班次的排班记录。" +
                    "重复键信息: {}", rootMsg, e);
            throw new ServiceException(400, "该日期和班次已存在排班记录");
        } catch (Exception e) {
            log.error("[createSchedule] 数据库插入失败，schedule={}", schedule, e);
            throw new ServiceException(500, "排班保存失败，请查看服务端日志确认具体原因");
        }

        log.info("[createSchedule] 创建排班成功，ID={}，日期：{}，班次：{}，值班人：{}",
                schedule.getId(), createReqVO.getScheduleDate(),
                createReqVO.getShiftType(), createReqVO.getDutyUserName());

        return schedule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchCreateSchedule(List<FireDutyScheduleCreateReqVO> createReqVOList) {
        int count = 0;
        for (FireDutyScheduleCreateReqVO createReqVO : createReqVOList) {
            try {
                createSchedule(createReqVO);
                count++;
            } catch (Exception e) {
                log.warn("[batchCreateSchedule] 创建排班失败：{}", e.getMessage());
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSchedule(FireDutyScheduleUpdateReqVO updateReqVO) {
        log.warn("[updateSchedule] ===== 开始更新排班 ===== 请求参数: id={}, propertyId={}, scheduleDate={}, shiftType={}, " +
                        "startTime={}, endTime={}, dutyUserId={}, dutyUserName={}, dutyUserPhone={}, dutyUserPosition={}, remark={}",
                updateReqVO.getId(), updateReqVO.getPropertyId(), updateReqVO.getScheduleDate(),
                updateReqVO.getShiftType(), updateReqVO.getStartTime(), updateReqVO.getEndTime(),
                updateReqVO.getDutyUserId(), updateReqVO.getDutyUserName(), updateReqVO.getDutyUserPhone(),
                updateReqVO.getDutyUserPosition(), updateReqVO.getRemark());
        try {
            FireDutyScheduleDO existSchedule = fireDutyScheduleMapper.selectById(updateReqVO.getId());
            if (existSchedule == null) {
                throw new ServiceException(400, "排班记录不存在");
            }
            log.warn("[updateSchedule] 查询到现有排班: id={}, tenantId={}, propertyId={}, propertyName={}, scheduleDate={}, shiftType={}, status={}",
                    existSchedule.getId(), existSchedule.getTenantId(), existSchedule.getPropertyId(),
                    existSchedule.getPropertyName(), existSchedule.getScheduleDate(),
                    existSchedule.getShiftType(), existSchedule.getStatus());

            // 如果 propertyId 变了，同步更新 propertyName
            String propertyName = existSchedule.getPropertyName();
            if (updateReqVO.getPropertyId() != null
                    && !updateReqVO.getPropertyId().equals(existSchedule.getPropertyId())) {
                log.warn("[updateSchedule] propertyId 已变更: {} -> {}, 重新查询 propertyName",
                        existSchedule.getPropertyId(), updateReqVO.getPropertyId());
                propertyName = getPropertyName(updateReqVO.getPropertyId());
            }

            FireDutyScheduleDO updateObj = FireDutyScheduleDO.builder()
                    .id(updateReqVO.getId())
                    .propertyId(updateReqVO.getPropertyId())
                    .propertyName(propertyName)
                    .scheduleDate(updateReqVO.getScheduleDate())
                    .shiftType(updateReqVO.getShiftType())
                    .shiftName(updateReqVO.getShiftName())
                    .startTime(updateReqVO.getStartTime())
                    .endTime(updateReqVO.getEndTime())
                    .dutyUserId(updateReqVO.getDutyUserId())
                    .dutyUserName(updateReqVO.getDutyUserName())
                    .dutyUserPhone(updateReqVO.getDutyUserPhone())
                    .dutyUserPosition(updateReqVO.getDutyUserPosition())
                    .remark(updateReqVO.getRemark())
                    .build();
            log.warn("[updateSchedule] 构造更新对象完成，准备执行 updateById");

            fireDutyScheduleMapper.updateById(updateObj);
            log.warn("[updateSchedule] 更新排班成功，排班ID：{}", updateReqVO.getId());
        } catch (ServiceException e) {
            // ServiceException 直接抛出，由全局异常处理器处理
            log.warn("[updateSchedule] 业务异常: code={}, message={}", e.getCode(), e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            // 数据库唯一约束冲突（uk_tenant_property_date_shift），说明数据库中存在完全重复的排班记录
            // 导致 MySQL InnoDB 在更新时先删除旧索引再插入新索引时发现冲突
            // 冲突键格式: tenant_id-property_id-date-shift_type
            String rootMsg = cn.hutool.core.exceptions.ExceptionUtil.getRootCauseMessage(e);
            log.error("[updateSchedule] 更新排班失败，唯一约束冲突！原因：数据库中该物业({})在该日期({})、班次({})下存在多条重复的排班记录。" +
                            "重复键信息: {}。请检查并清理 fire_duty_schedule 表中的重复记录。",
                    updateReqVO.getPropertyId(), updateReqVO.getScheduleDate(),
                    updateReqVO.getShiftType(), rootMsg, e);
            throw new ServiceException(400, "该日期和班次存在重复排班记录，请联系管理员清理重复数据");
        } catch (Exception e) {
            log.error("[updateSchedule] ===== 更新排班异常 ===== 排班ID: {}, 请求数据: propertyId={}, scheduleDate={}, shiftType={}",
                    updateReqVO.getId(), updateReqVO.getPropertyId(), updateReqVO.getScheduleDate(),
                    updateReqVO.getShiftType(), e);
            log.error("[updateSchedule] 租户上下文: tenantId={}, isIgnore={}",
                    com.enjoyiot.framework.tenant.core.context.TenantContextHolder.getTenantId(),
                    com.enjoyiot.framework.tenant.core.context.TenantContextHolder.isIgnore());
            // 打印完整调用栈
            log.error("[updateSchedule] 异常详情: ", e);
            throw new RuntimeException("更新排班异常: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        FireDutyScheduleDO existSchedule = fireDutyScheduleMapper.selectById(id);
        if (existSchedule == null) {
            throw new ServiceException(400, "排班记录不存在");
        }

        fireDutyScheduleMapper.deleteById(id);

        log.info("[deleteSchedule] 删除排班成功，排班ID：{}", id);
    }

    @Override
    public FireDutyScheduleRespVO getSchedule(Long id) {
        return tenantAware(() -> {
            FireDutyScheduleDO schedule = fireDutyScheduleMapper.selectById(id);
            if (schedule == null) {
                return null;
            }
            // 普通租户只能访问自己物业的排班数据（不依赖 schedule.tenant_id，
            // 因为排班可能是管理员创建的，tenant_id=1）
            if (!isSystemTenant()) {
                List<Long> tenantPropertyIds = getTenantPropertyIds();
                if (schedule.getPropertyId() != null && !tenantPropertyIds.contains(schedule.getPropertyId())) {
                    log.warn("[getSchedule] 租户越权访问排班数据，tenantId={}, scheduleId={}, schedulePropertyId={}",
                            TenantContextHolder.getTenantId(), id, schedule.getPropertyId());
                    return null;
                }
            }
            return convertToRespVO(schedule);
        });
    }

    @Override
    public PageResult<FireDutyScheduleRespVO> pageSchedules(FireDutySchedulePageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireDutyScheduleDO> wrapper = new LambdaQueryWrapper<>();

            // 非系统租户：只能看到自己企业物业的排班（通过物业归属判断，不依赖排班表的 tenant_id）
            if (!isSystemTenant()) {
                Long currentTenantId = TenantContextHolder.getTenantId();
                if (currentTenantId != null) {
                    // 临时使用正确的租户上下文查询当前租户的物业列表
                    List<FirePropertyDO> tenantProperties = TenantUtils.execute(currentTenantId, () ->
                            firePropertyMapper.selectList(null));
                    List<Long> propertyIds = new ArrayList<>();
                    for (FirePropertyDO p : tenantProperties) {
                        propertyIds.add(p.getId());
                    }
                    if (!propertyIds.isEmpty()) {
                        wrapper.in(FireDutyScheduleDO::getPropertyId, propertyIds);
                    } else {
                        wrapper.eq(FireDutyScheduleDO::getId, -1L); // 无物业 → 无数据
                    }
                }
            }

            wrapper.eq(pageReqVO.getPropertyId() != null, FireDutyScheduleDO::getPropertyId, pageReqVO.getPropertyId());
            wrapper.ge(pageReqVO.getScheduleDateStart() != null, FireDutyScheduleDO::getScheduleDate, pageReqVO.getScheduleDateStart());
            wrapper.le(pageReqVO.getScheduleDateEnd() != null, FireDutyScheduleDO::getScheduleDate, pageReqVO.getScheduleDateEnd());
            wrapper.eq(pageReqVO.getShiftType() != null, FireDutyScheduleDO::getShiftType, pageReqVO.getShiftType());
            wrapper.like(pageReqVO.getDutyUserName() != null, FireDutyScheduleDO::getDutyUserName, pageReqVO.getDutyUserName());
            wrapper.like(pageReqVO.getDutyUserPhone() != null, FireDutyScheduleDO::getDutyUserPhone, pageReqVO.getDutyUserPhone());
            
            // 状态筛选项统一走 Mapper 的 SHIFT_END 口径（夜班跨天），与统计/前端 cardStatus 一致；
            // absent / early_leave 既包含已落库纠正的历史数据，也包含尚未转态的实时推导
            String status = pageReqVO.getStatus();
            String ended = FireDutyScheduleMapper.SHIFT_ENDED;
            if ("absent".equals(status)) {
                wrapper.apply("(status = 'absent' OR (status = 'scheduled' AND check_in_time IS NULL AND " + ended + "))");
            } else if ("early_leave".equals(status)) {
                wrapper.apply("(status = 'early_leave' OR (status = 'on_duty' AND check_out_time IS NULL AND " + ended + "))");
            } else if ("scheduled".equals(status) || "on_duty".equals(status)) {
                // 班次一旦结束就归入缺勤/早退，不再计入已排班/值班中
                wrapper.eq(FireDutyScheduleDO::getStatus, status);
                wrapper.apply("NOT (" + ended + ")");
            } else {
                wrapper.eq(status != null, FireDutyScheduleDO::getStatus, status);
            }

            wrapper.orderByDesc(FireDutyScheduleDO::getScheduleDate);

            PageResult<FireDutyScheduleDO> pageResult = fireDutyScheduleMapper.selectPage(pageReqVO, wrapper);

            List<FireDutyScheduleRespVO> list = new ArrayList<>();
            for (FireDutyScheduleDO schedule : pageResult.getList()) {
                list.add(convertToRespVO(schedule));
            }

            return new PageResult<>(list, pageResult.getTotal());
        });
    }

    @Override
    public List<FireDutyScheduleRespVO> listSchedulesByDateRange(Long propertyId, LocalDate startDate, LocalDate endDate) {
        return tenantAware(() -> {
            // 普通租户不按 tenant_id 过滤（排班可能是管理员创建，tenant_id 不匹配），
            // 改为物业归属隔离数据
            List<FireDutyScheduleDO> schedules = fireDutyScheduleMapper.selectByDateRange(null, startDate, endDate);

            // 普通租户不过滤 tenant_id 时，需要限制只看到自己物业的排班
            Set<Long> allowedPropertyIds = null;
            if (!isSystemTenant()) {
                List<Long> tenantPropertyIds = getTenantPropertyIds();
                allowedPropertyIds = new HashSet<>(tenantPropertyIds);
            }

            List<FireDutyScheduleRespVO> list = new ArrayList<>();
            for (FireDutyScheduleDO schedule : schedules) {
                if (propertyId != null && !propertyId.equals(schedule.getPropertyId())) {
                    continue;
                }
                if (allowedPropertyIds != null && !allowedPropertyIds.contains(schedule.getPropertyId())) {
                    continue;
                }
                list.add(convertToRespVO(schedule));
            }

            return list;
        });
    }

    @Override
    public List<FireDutyScheduleRespVO> listSchedulesByDate(Long propertyId, LocalDate date) {
        return tenantAware(() -> {
            // 普通租户不按 tenant_id 过滤（排班可能是管理员创建，tenant_id 不匹配），
            // 改为物业归属隔离数据
            List<FireDutyScheduleDO> schedules = fireDutyScheduleMapper.selectByDate(null, date);

            // 普通租户不过滤 tenant_id 时，需要限制只看到自己物业的排班
            Set<Long> allowedPropertyIds = null;
            if (!isSystemTenant()) {
                List<Long> tenantPropertyIds = getTenantPropertyIds();
                allowedPropertyIds = new HashSet<>(tenantPropertyIds);
            }

            List<FireDutyScheduleRespVO> list = new ArrayList<>();
            for (FireDutyScheduleDO schedule : schedules) {
                if (propertyId != null && !propertyId.equals(schedule.getPropertyId())) {
                    continue;
                }
                if (allowedPropertyIds != null && !allowedPropertyIds.contains(schedule.getPropertyId())) {
                    continue;
                }
                list.add(convertToRespVO(schedule));
            }

            return list;
        });
    }

    @Override
    public List<FireDutyScheduleRespVO> getCurrentDuty(Long propertyId) {
        return tenantAware(() -> {
            // 普通租户不按 tenant_id 过滤（排班可能是管理员创建，tenant_id 不匹配），
            // 改为物业归属隔离数据
            List<FireDutyScheduleDO> schedules = fireDutyScheduleMapper.selectCurrentDuty(null);

            // 普通租户不过滤 tenant_id 时，需要限制只看到自己物业的排班
            Set<Long> allowedPropertyIds = null;
            if (!isSystemTenant()) {
                List<Long> tenantPropertyIds = getTenantPropertyIds();
                allowedPropertyIds = new HashSet<>(tenantPropertyIds);
            }

            List<FireDutyScheduleRespVO> list = new ArrayList<>();
            for (FireDutyScheduleDO schedule : schedules) {
                if (propertyId != null && !propertyId.equals(schedule.getPropertyId())) {
                    continue;
                }
                if (allowedPropertyIds != null && !allowedPropertyIds.contains(schedule.getPropertyId())) {
                    continue;
                }
                list.add(convertToRespVO(schedule));
            }

            return list;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long id, String checkInType) {
        FireDutyScheduleDO schedule = fireDutyScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new ServiceException(400, "排班记录不存在");
        }

        if (isShiftEnded(schedule)) {
            throw new ServiceException(400, "班次已结束，不可签到");
        }

        fireDutyScheduleMapper.update(null, new LambdaUpdateWrapper<FireDutyScheduleDO>()
                .eq(FireDutyScheduleDO::getId, id)
                .set(FireDutyScheduleDO::getStatus, "on_duty")
                .set(FireDutyScheduleDO::getCheckInTime, LocalDateTime.now())
                .set(FireDutyScheduleDO::getCheckInType, checkInType));

        log.info("[checkIn] 签到成功，排班ID：{}，签到方式：{}", id, checkInType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkOut(Long id) {
        FireDutyScheduleDO schedule = fireDutyScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new ServiceException(400, "排班记录不存在");
        }

        if (isShiftEnded(schedule)) {
            throw new ServiceException(400, "班次已结束，不可签退");
        }

        fireDutyScheduleMapper.update(null, new LambdaUpdateWrapper<FireDutyScheduleDO>()
                .eq(FireDutyScheduleDO::getId, id)
                .set(FireDutyScheduleDO::getStatus, "completed")
                .set(FireDutyScheduleDO::getCheckOutTime, LocalDateTime.now()));

        log.info("[checkOut] 签退成功，排班ID：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeDuty(Long id, Long newUserId, String reason) {
        FireDutyScheduleDO schedule = fireDutyScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new ServiceException(400, "排班记录不存在");
        }

        // 查询新值班人信息
        AdminUserRespDTO newUser = adminUserApi.getUser(newUserId);
        String newUserName = newUser != null ? newUser.getNickname() : "未知";
        String newUserPhone = newUser != null ? (newUser.getMobile() != null ? newUser.getMobile() : "") : "";

        String remark = schedule.getRemark();
        if (remark == null) {
            remark = "";
        }
        remark += " | 换班原因：" + (reason != null ? reason : "");

        fireDutyScheduleMapper.update(null, new LambdaUpdateWrapper<FireDutyScheduleDO>()
                .eq(FireDutyScheduleDO::getId, id)
                .set(FireDutyScheduleDO::getDutyUserId, newUserId)
                .set(FireDutyScheduleDO::getDutyUserName, newUserName)
                .set(FireDutyScheduleDO::getDutyUserPhone, newUserPhone)
                .set(FireDutyScheduleDO::getRemark, remark));

        log.info("[changeDuty] 换班成功，排班ID：{}，新值班人：{}（ID：{}）", id, newUserName, newUserId);
    }

    @Override
    public Map<String, Object> getScheduleStatistics(Long propertyId, LocalDate startDate, LocalDate endDate) {
        return tenantAware(() -> {
            Map<String, Object> result = new HashMap<>();
            Map<String, Integer> statusCount = new HashMap<>();
            statusCount.put("scheduled", 0);
            statusCount.put("on_duty", 0);
            statusCount.put("completed", 0);
            statusCount.put("absent", 0);
            statusCount.put("early_leave", 0);

            if (isSystemTenant()) {
                // 系统管理员：不按 tenant_id 过滤，通过可选的 propertyId 过滤
                List<Map<String, Object>> statusStats = fireDutyScheduleMapper.countByStatus(null, propertyId, startDate, endDate);
                mergeStatusCounts(statusCount, statusStats);
            } else {
                // 普通租户：不依赖 schedule.tenant_id（排班可能是管理员创建的，tenant_id=1）
                // 改为通过物业归属过滤
                List<Long> targetPropertyIds = new ArrayList<>();
                if (propertyId != null) {
                    // 前端已选特定物业（下拉框已按租户过滤，propertyId 天然隔离）
                    targetPropertyIds.add(propertyId);
                } else {
                    // 未选物业时，获取当前租户的所有物业
                    List<Long> tenantPropertyIds = getTenantPropertyIds();
                    targetPropertyIds.addAll(tenantPropertyIds);
                }

                // 按每个物业独立统计并汇总
                for (Long pid : targetPropertyIds) {
                    List<Map<String, Object>> propStats = fireDutyScheduleMapper.countByStatus(null, pid, startDate, endDate);
                    mergeStatusCounts(statusCount, propStats);
                }
            }

            result.put("statusStatistics", statusCount);
            return result;
        });
    }

    @Override
    public Integer getUserDutyDays(Long userId, Integer year, Integer month) {
        return tenantAware(() -> {
            Long tenantId = isSystemTenant() ? null : TenantContextHolder.getRequiredTenantId();
            return fireDutyScheduleMapper.countDutyDays(tenantId, userId, year, month);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer autoSchedule(Long propertyId, Integer year, Integer month, List<Long> dutyUsers) {
        if (dutyUsers == null || dutyUsers.isEmpty()) {
            throw new ServiceException(400, "值班人员列表不能为空");
        }

        // 批量查询用户信息
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(dutyUsers);

        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();

        String propertyName = getPropertyName(propertyId);
        int count = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            int userIndex = (day - 1) % dutyUsers.size();
            Long userId = dutyUsers.get(userIndex);
            AdminUserRespDTO user = userMap.get(userId);
            String userName = user != null ? user.getNickname() : ("用户" + userId);
            String userPhone = user != null && user.getMobile() != null ? user.getMobile() : "";

            // 检查该日白班是否已存在
            LambdaQueryWrapper<FireDutyScheduleDO> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(FireDutyScheduleDO::getPropertyId, propertyId)
                    .eq(FireDutyScheduleDO::getScheduleDate, date)
                    .eq(FireDutyScheduleDO::getShiftType, "day");
            if (fireDutyScheduleMapper.selectCount(checkWrapper) == 0) {
                FireDutyScheduleDO dayShift = FireDutyScheduleDO.builder()
                        .propertyId(propertyId)
                        .propertyName(propertyName)
                        .scheduleDate(date)
                        .shiftType("day")
                        .shiftName("白班")
                        .startTime("08:00")
                        .endTime("18:00")
                        .dutyUserId(userId)
                        .dutyUserName(userName)
                        .dutyUserPhone(userPhone)
                        .status("scheduled")
                        .build();
                fireDutyScheduleMapper.insert(dayShift);
                count++;
            }

            // 夜班
            int nightUserIndex = (day) % dutyUsers.size();
            Long nightUserId = dutyUsers.get(nightUserIndex);
            AdminUserRespDTO nightUser = userMap.get(nightUserId);
            String nightUserName = nightUser != null ? nightUser.getNickname() : ("用户" + nightUserId);
            String nightUserPhone = nightUser != null && nightUser.getMobile() != null ? nightUser.getMobile() : "";

            // 检查该日夜班是否已存在
            LambdaQueryWrapper<FireDutyScheduleDO> nightCheckWrapper = new LambdaQueryWrapper<>();
            nightCheckWrapper.eq(FireDutyScheduleDO::getPropertyId, propertyId)
                    .eq(FireDutyScheduleDO::getScheduleDate, date)
                    .eq(FireDutyScheduleDO::getShiftType, "night");
            if (fireDutyScheduleMapper.selectCount(nightCheckWrapper) == 0) {
                FireDutyScheduleDO nightShift = FireDutyScheduleDO.builder()
                        .propertyId(propertyId)
                        .propertyName(propertyName)
                        .scheduleDate(date)
                        .shiftType("night")
                        .shiftName("夜班")
                        .startTime("18:00")
                        .endTime("08:00")
                        .dutyUserId(nightUserId)
                        .dutyUserName(nightUserName)
                        .dutyUserPhone(nightUserPhone)
                        .status("scheduled")
                        .build();
                fireDutyScheduleMapper.insert(nightShift);
                count++;
            }
        }

        log.info("[autoSchedule] 自动排班完成，物业ID：{}，年月：{}-{}，生成排班数：{}",
                propertyId, year, month, count);

        return count;
    }

    /**
     * 导入排班数据
     *
     * @param file       Excel文件
     * @param propertyId 所属物业ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcelSchedule(MultipartFile file, Long propertyId) {
        // 参数校验
        if (propertyId == null) {
            throw new ServiceException(400, "物业ID不能为空");
        }
        String propertyName = getPropertyName(propertyId);
        if (propertyName == null) {
            throw new ServiceException(400, "物业不存在，propertyId=" + propertyId);
        }

        // 使用通用导入工具类处理
        return ExcelImportUtils.processImport(file, ImportScheduleDataModel.class, (data, rowNum, result) -> {
            // 校验必填字段
            if (data.getScheduleDate() == null) {
                result.addError(rowNum, "排班日期不能为空");
                return;
            }

            String shiftType = convertShiftType(data.getShiftType(), data.getShiftName());
            if (shiftType == null) {
                result.addError(rowNum, "班次类型不能为空，请填写 day/night/wholeday 或 白班/夜班/全天班");
                return;
            }
            if (data.getDutyUserName() == null || data.getDutyUserName().isEmpty()) {
                result.addError(rowNum, "值班人姓名不能为空");
                return;
            }

            // 构建创建 VO
            FireDutyScheduleCreateReqVO createReqVO = new FireDutyScheduleCreateReqVO();
            createReqVO.setPropertyId(propertyId);
            createReqVO.setScheduleDate(data.getScheduleDate());
            createReqVO.setShiftType(shiftType);
            createReqVO.setShiftName(data.getShiftName() != null ? data.getShiftName() : getShiftName(shiftType));
            // 根据班次类型推导默认时间
            String[] times = getDefaultTimes(shiftType);
            createReqVO.setStartTime(times[0]);
            createReqVO.setEndTime(times[1]);
            createReqVO.setDutyUserName(data.getDutyUserName());
            createReqVO.setDutyUserPhone(data.getDutyUserPhone());

            createSchedule(createReqVO);
        }, "importExcelSchedule");
    }

    /**
     * 构建错误信息
     */
    private Map<String, Object> buildError(int rowNum, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("row", rowNum);
        error.put("message", message);
        return error;
    }

    /**
     * 班次类型转换：支持中文名称 → 编码
     */
    private String convertShiftType(String shiftType, String shiftName) {
        if (shiftType == null || shiftType.isEmpty()) {
            // 尝试从班次名称反推
            return convertShiftNameToType(shiftName);
        }
        // 如果已经是编码，直接返回
        if ("day".equals(shiftType) || "night".equals(shiftType) || "wholeday".equals(shiftType)) {
            return shiftType;
        }
        // 尝试作为中文名称转换
        return convertShiftNameToType(shiftType);
    }

    /**
     * 班次名称 → 编码
     */
    private String convertShiftNameToType(String name) {
        if (name == null) return null;
        switch (name.trim()) {
            case "白班": return "day";
            case "夜班": return "night";
            case "全天班": return "wholeday";
            default: return null;
        }
    }

    /**
     * 根据班次类型获取默认时间
     */
    private String[] getDefaultTimes(String shiftType) {
        switch (shiftType) {
            case "day": return new String[]{"08:00", "18:00"};
            case "night": return new String[]{"18:00", "08:00"};
            case "wholeday": return new String[]{"08:00", "08:00"};
            default: return new String[]{"08:00", "18:00"};
        }
    }

    /**
     * 获取物业名称（忽略租户过滤，因为 property/list 接口也是 @TenantIgnore 跨租户的）
     * 支持跨租户查询：使用 TenantUtils.executeIgnoreWithResult 临时忽略租户过滤
     */
    private String getPropertyName(Long propertyId) {
        if (propertyId == null) return null;
        return TenantUtils.executeIgnoreWithResult(() -> {
            FirePropertyDO property = firePropertyMapper.selectById(propertyId);
            if (property == null) {
                log.warn("[getPropertyName] 物业不存在于 fire_property 表中, propertyId={}", propertyId);
                return null;
            }
            String name = property.getPropertyName();
            if (name == null || name.isEmpty()) {
                log.warn("[getPropertyName] 物业存在但名称为空, propertyId={}", propertyId);
                return null;
            }
            log.info("[getPropertyName] 查询成功: propertyId={}, propertyName={}", propertyId, name);
            return name;
        });
    }

    /**
     * 获取班次名称
     */
    private String getShiftName(String shiftType) {
        Map<String, String> shiftMap = new HashMap<>();
        shiftMap.put("day", "白班");
        shiftMap.put("night", "夜班");
        shiftMap.put("wholeday", "全天班");
        return shiftMap.getOrDefault(shiftType, shiftType);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("scheduled", "已排班");
        statusMap.put("on_duty", "值班中");
        statusMap.put("completed", "已完成");
        statusMap.put("absent", "缺勤");
        statusMap.put("early_leave", "早退");
        return statusMap.getOrDefault(status, status);
    }

    /**
     * 转换为响应VO
     */
    private FireDutyScheduleRespVO convertToRespVO(FireDutyScheduleDO schedule) {
        FireDutyScheduleRespVO vo = new FireDutyScheduleRespVO();
        vo.setId(schedule.getId());
        vo.setPropertyId(schedule.getPropertyId());
        // 如果 property_name 为空，动态从物业表查询（兼容存量数据）
        if (schedule.getPropertyName() == null) {
            vo.setPropertyName(getPropertyName(schedule.getPropertyId()));
        } else {
            vo.setPropertyName(schedule.getPropertyName());
        }
        vo.setScheduleDate(schedule.getScheduleDate());
        vo.setShiftType(schedule.getShiftType());
        vo.setShiftTypeName(getShiftName(schedule.getShiftType()));
        vo.setShiftName(schedule.getShiftName());
        vo.setStartTime(schedule.getStartTime());
        vo.setEndTime(schedule.getEndTime());
        vo.setDutyUserId(schedule.getDutyUserId());
        vo.setDutyUserName(schedule.getDutyUserName());
        vo.setDutyUserPhone(schedule.getDutyUserPhone());
        vo.setDutyUserPosition(schedule.getDutyUserPosition());
        String displayStatus = deriveStatus(schedule);
        vo.setStatus(displayStatus);
        vo.setStatusName(getStatusName(displayStatus));
        vo.setCheckInTime(schedule.getCheckInTime());
        vo.setCheckOutTime(schedule.getCheckOutTime());
        vo.setCheckInType(schedule.getCheckInType());
        vo.setRemark(schedule.getRemark());
        vo.setCreateTime(schedule.getCreateTime());
        return vo;
    }

    /**
     * 落库 status 不随时间自动流转（无定时任务翻转），统一在出参处推导：
     * 班次结束后 scheduled 未签到 = absent，on_duty 未签退 = early_leave，
     * 使 PC 表格、移动端卡片、统计与筛选四处口径一致
     */
    private String deriveStatus(FireDutyScheduleDO schedule) {
        if (!isShiftEnded(schedule)) {
            return schedule.getStatus();
        }
        if ("scheduled".equals(schedule.getStatus()) && schedule.getCheckInTime() == null) {
            return "absent";
        }
        if ("on_duty".equals(schedule.getStatus()) && schedule.getCheckOutTime() == null) {
            return "early_leave";
        }
        return schedule.getStatus();
    }

    /**
     * 班次是否已结束，与 Mapper.SHIFT_ENDED 同口径（夜班 start_time >= end_time 跨天到次日）
     */
    private boolean isShiftEnded(FireDutyScheduleDO schedule) {
        try {
            LocalTime start = LocalTime.parse(schedule.getStartTime());
            LocalTime end = LocalTime.parse(schedule.getEndTime());
            LocalDate day = start.compareTo(end) >= 0 ? schedule.getScheduleDate().plusDays(1) : schedule.getScheduleDate();
            return LocalDateTime.of(day, end).isBefore(LocalDateTime.now());
        } catch (RuntimeException e) {
            // 时间字段缺失或格式异常时不推导，保持库内状态
            return false;
        }
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

    /**
     * 合并统计数据到 statusCount 中
     */
    private void mergeStatusCounts(Map<String, Integer> statusCount, List<Map<String, Object>> stats) {
        for (Map<String, Object> stat : stats) {
            String status = (String) stat.get("status");
            Long count = (Long) stat.get("count");
            statusCount.merge(status, count.intValue(), Integer::sum);
        }
    }

    /**
     * 获取当前租户的物业ID列表（用于物业归属过滤，不依赖 schedule.tenant_id）
     */
    private List<Long> getTenantPropertyIds() {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || isSystemTenant()) {
            return Collections.emptyList();
        }
        return TenantUtils.execute(currentTenantId, () -> {
            List<FirePropertyDO> properties = firePropertyMapper.selectList(null);
            return properties.stream().map(FirePropertyDO::getId).collect(Collectors.toList());
        });
    }

}
