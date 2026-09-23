package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enjoyiot.framework.common.exception.ServiceException;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.excel.core.util.ExcelImportUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePatrolPointDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePatrolTaskDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePatrolPointMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePatrolTaskMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * 巡检任务管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FirePatrolTaskServiceImpl implements FirePatrolTaskService {

    @Resource
    private FirePatrolTaskMapper firePatrolTaskMapper;

    @Resource
    private FirePatrolPointMapper firePatrolPointMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Resource
    private EiotDeviceInfoMapper eiotDeviceInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(FirePatrolTaskCreateReqVO createReqVO) {
        log.info("[createTask] ====== 开始创建巡检任务 ======");
        log.info("[createTask] 请求参数: taskName={}, propertyId={}, taskType={}, planDate={}, patrolUserId={}, deviceIds={}",
                createReqVO.getTaskName(), createReqVO.getPropertyId(), createReqVO.getTaskType(),
                createReqVO.getPlanDate(), createReqVO.getPatrolUserId(), createReqVO.getDeviceIds());

        String propertyName = getPropertyName(createReqVO.getPropertyId());

        List<Long> deviceIds = createReqVO.getDeviceIds();
        int totalPoints = (deviceIds != null) ? deviceIds.size() : 0;

        log.info("[createTask] deviceIds 原始数据: {}, size={}, totalPoints={}", deviceIds,
                deviceIds != null ? deviceIds.size() : 0, totalPoints);

        // 检查 deviceIds 是否为空的关键日志
        if (deviceIds == null) {
            log.warn("[createTask] >>>>> deviceIds 为 null，不会创建任何巡检点位！");
        } else if (deviceIds.isEmpty()) {
            log.warn("[createTask] >>>>> deviceIds 为空列表，不会创建任何巡检点位！");
        } else {
            log.info("[createTask] >>>>> deviceIds 非空，将创建 {} 个巡检点位", deviceIds.size());
        }

        FirePatrolTaskDO task = FirePatrolTaskDO.builder()
                .taskNo(generateTaskNo())
                .propertyId(createReqVO.getPropertyId())
                .propertyName(propertyName)
                .taskName(createReqVO.getTaskName())
                .taskType(createReqVO.getTaskType())
                .planDate(createReqVO.getPlanDate())
                .planStartTime(createReqVO.getPlanStartTime())
                .planEndTime(createReqVO.getPlanEndTime())
                .status("pending")
                .patrolUserId(createReqVO.getPatrolUserId())
                .patrolUserName(createReqVO.getPatrolUserName())
                .patrolUserPhone(createReqVO.getPatrolUserPhone())
                .totalPoints(totalPoints)
                .completedPoints(0)
                .normalPoints(0)
                .abnormalPoints(0)
                .progress(0)
                .remark(createReqVO.getRemark())
                .build();
        // 设置租户ID，确保任务归属于当前操作的租户
        task.setTenantId(TenantContextHolder.getTenantId());

        firePatrolTaskMapper.insert(task);
        log.info("[createTask] 巡检任务插入成功，taskId={}, taskNo={}", task.getId(), task.getTaskNo());

        // 在同一个事务中创建设备巡检点位
        if (deviceIds != null && !deviceIds.isEmpty()) {
            log.info("[createTask] 开始创建设备巡检点位，共 {} 个设备", deviceIds.size());
            int inserted = 0;
            for (int i = 0; i < deviceIds.size(); i++) {
                Long deviceId = deviceIds.get(i);
                log.info("[createTask] 处理第 {}/{} 个设备，deviceId={}", i + 1, deviceIds.size(), deviceId);

                // 使用 TenantUtils.executeIgnoreWithResult 忽略租户过滤，
                // 因为设备下拉框查询（DeviceInfoServiceImpl.getDeviceInfoPage）
                // 对 admin 账号也跳过租户过滤，若此处不跳过会导致 admin 选的非本租户设备查不到
                EiotDeviceInfoDO device = TenantUtils.executeIgnoreWithResult(() -> {
                    try {
                        return eiotDeviceInfoMapper.selectById(deviceId);
                    } catch (Exception e) {
                        log.error("[createTask] 查询设备信息异常，deviceId={}", deviceId, e);
                        throw new RuntimeException("查询设备信息失败，deviceId=" + deviceId, e);
                    }
                });

                if (device == null) {
                    log.warn("[createTask] 设备不存在，跳过: deviceId={}", deviceId);
                    continue;
                }

                log.info("[createTask] 设备查询成功: deviceId={}, deviceName={}, dn={}, productKey={}, addr={}",
                        deviceId, device.getName(), device.getDn(), device.getProductKey(), device.getAddr());

                FirePatrolPointDO point = FirePatrolPointDO.builder()
                        .taskId(task.getId())
                        .deviceId(deviceId)
                        .deviceName(device.getName())
                        .deviceCode(device.getDn())
                        .deviceType(device.getProductKey())
                        .location(device.getAddr())
                        .status("pending")
                        .build();
                // 设置租户ID，确保点位数据归属于当前操作的租户
                point.setTenantId(TenantContextHolder.getTenantId());

                try {
                    firePatrolPointMapper.insert(point);
                    log.info("[createTask] 巡检点位插入成功: taskId={}, deviceId={}, pointId={}",
                            task.getId(), deviceId, point.getId());
                    inserted++;
                } catch (Exception e) {
                    log.error("[createTask] 巡检点位插入异常: taskId={}, deviceId={}", task.getId(), deviceId, e);
                    throw new RuntimeException("巡检点位插入失败，deviceId=" + deviceId, e);
                }
            }

            log.info("[createTask] 点位创建完成: 预期={}, 实际插入={}, 跳过={}",
                    totalPoints, inserted, totalPoints - inserted);

            // 如果实际插入数与预期不符，更新 totalPoints
            if (inserted != totalPoints) {
                firePatrolTaskMapper.update(null, new LambdaUpdateWrapper<FirePatrolTaskDO>()
                        .eq(FirePatrolTaskDO::getId, task.getId())
                        .set(FirePatrolTaskDO::getTotalPoints, inserted));
                log.warn("[createTask] 部分设备未找到，预期点位：{}，实际插入：{}，已更新任务 totalPoints={}", totalPoints, inserted, inserted);
            }

            log.info("[createTask] 创建巡检点位成功，任务ID：{}，点位数：{}", task.getId(), inserted);
        } else {
            log.warn("[createTask] >>>>> 没有传入设备ID列表，任务创建时不会生成任何巡检点位！");
            log.warn("[createTask] >>>>> 请检查前端是否已选择巡检设备并正确传递 deviceIds 参数");
        }

        log.info("[createTask] ====== 创建巡检任务完成 ====== 任务编号：{}，任务名称：{}，物业名称：{}，点位总数：{}",
                task.getTaskNo(), task.getTaskName(), propertyName, task.getTotalPoints());

        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(FirePatrolTaskUpdateReqVO updateReqVO) {
        FirePatrolTaskDO existTask = firePatrolTaskMapper.selectById(updateReqVO.getId());
        if (existTask == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        FirePatrolTaskDO updateObj = FirePatrolTaskDO.builder()
                .id(updateReqVO.getId())
                .taskName(updateReqVO.getTaskName())
                .planDate(updateReqVO.getPlanDate())
                .planStartTime(updateReqVO.getPlanStartTime())
                .planEndTime(updateReqVO.getPlanEndTime())
                .patrolUserId(updateReqVO.getPatrolUserId())
                .patrolUserName(updateReqVO.getPatrolUserName())
                .patrolUserPhone(updateReqVO.getPatrolUserPhone())
                .remark(updateReqVO.getRemark())
                .build();

        firePatrolTaskMapper.updateById(updateObj);

        log.info("[updateTask] 更新巡检任务成功，任务ID：{}", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        FirePatrolTaskDO existTask = firePatrolTaskMapper.selectById(id);
        if (existTask == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        // 删除任务下的所有点位
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<FirePatrolPointDO> points = firePatrolPointMapper.selectByTaskId(tenantId, id);
        for (FirePatrolPointDO point : points) {
            firePatrolPointMapper.deleteById(point.getId());
        }

        firePatrolTaskMapper.deleteById(id);

        log.info("[deleteTask] 删除巡检任务成功，任务ID：{}", id);
    }

    @Override
    public FirePatrolTaskRespVO getTask(Long id) {
        return tenantAware(() -> {
            FirePatrolTaskDO task = firePatrolTaskMapper.selectById(id);
            if (task == null) {
                return null;
            }
            return convertToRespVO(task);
        });
    }

    @Override
    public PageResult<FirePatrolTaskRespVO> pageTasks(FirePatrolTaskPageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FirePatrolTaskDO> wrapper = new LambdaQueryWrapper<>();

            wrapper.eq(pageReqVO.getPropertyId() != null, FirePatrolTaskDO::getPropertyId, pageReqVO.getPropertyId());
            wrapper.ge(pageReqVO.getPlanDateStart() != null, FirePatrolTaskDO::getPlanDate, pageReqVO.getPlanDateStart());
            wrapper.le(pageReqVO.getPlanDateEnd() != null, FirePatrolTaskDO::getPlanDate, pageReqVO.getPlanDateEnd());
            wrapper.eq(pageReqVO.getTaskType() != null, FirePatrolTaskDO::getTaskType, pageReqVO.getTaskType());
            wrapper.eq(pageReqVO.getStatus() != null, FirePatrolTaskDO::getStatus, pageReqVO.getStatus());
            wrapper.like(pageReqVO.getPatrolUserName() != null, FirePatrolTaskDO::getPatrolUserName, pageReqVO.getPatrolUserName());
            wrapper.eq(pageReqVO.getResult() != null, FirePatrolTaskDO::getResult, pageReqVO.getResult());

            wrapper.orderByDesc(FirePatrolTaskDO::getPlanDate);

            PageResult<FirePatrolTaskDO> pageResult = firePatrolTaskMapper.selectPage(pageReqVO, wrapper);

            List<FirePatrolTaskRespVO> list = new ArrayList<>();
            for (FirePatrolTaskDO task : pageResult.getList()) {
                list.add(convertToRespVO(task));
            }

            return new PageResult<>(list, pageResult.getTotal());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long id) {
        FirePatrolTaskDO task = firePatrolTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        firePatrolTaskMapper.update(null, new LambdaUpdateWrapper<FirePatrolTaskDO>()
                .eq(FirePatrolTaskDO::getId, id)
                .set(FirePatrolTaskDO::getStatus, "in_progress")
                .set(FirePatrolTaskDO::getActualStartTime, LocalDateTime.now()));

        log.info("[startTask] 开始巡检任务，任务ID：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long id, String summary) {
        FirePatrolTaskDO task = firePatrolTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 统计点位情况
        Integer normalPoints = firePatrolPointMapper.countNormalPoints(tenantId, id);
        Integer abnormalPoints = firePatrolPointMapper.countAbnormalPoints(tenantId, id);
        String result = (abnormalPoints != null && abnormalPoints > 0) ? "abnormal" : "normal";

        firePatrolTaskMapper.update(null, new LambdaUpdateWrapper<FirePatrolTaskDO>()
                .eq(FirePatrolTaskDO::getId, id)
                .set(FirePatrolTaskDO::getStatus, "completed")
                .set(FirePatrolTaskDO::getActualEndTime, LocalDateTime.now())
                .set(FirePatrolTaskDO::getResult, result)
                .set(FirePatrolTaskDO::getSummary, summary)
                .set(FirePatrolTaskDO::getNormalPoints, normalPoints)
                .set(FirePatrolTaskDO::getAbnormalPoints, abnormalPoints)
                .set(FirePatrolTaskDO::getProgress, 100));

        log.info("[completeTask] 完成巡检任务，任务ID：{}，结果：{}", id, result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long id, String reason) {
        FirePatrolTaskDO task = firePatrolTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        firePatrolTaskMapper.update(null, new LambdaUpdateWrapper<FirePatrolTaskDO>()
                .eq(FirePatrolTaskDO::getId, id)
                .set(FirePatrolTaskDO::getStatus, "cancelled")
                .set(FirePatrolTaskDO::getRemark, task.getRemark() + " | 取消原因：" + reason));

        log.info("[cancelTask] 取消巡检任务，任务ID：{}，原因：{}", id, reason);
    }

    @Override
    public List<FirePatrolPointRespVO> listTaskPoints(Long taskId) {
        return tenantAware(() -> {
            Long tenantId = isSystemTenant() ? null : TenantContextHolder.getRequiredTenantId();
            List<FirePatrolPointDO> points = firePatrolPointMapper.selectByTaskId(tenantId, taskId);

            List<FirePatrolPointRespVO> list = new ArrayList<>();
            for (FirePatrolPointDO point : points) {
                list.add(convertPointToRespVO(point));
            }

            return list;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkPoint(FirePatrolPointCheckReqVO checkReqVO) {
        FirePatrolPointDO point = firePatrolPointMapper.selectById(checkReqVO.getId());
        if (point == null) {
            throw new IllegalArgumentException("点位不存在");
        }

        // 更新点位状态
        firePatrolPointMapper.update(null, new LambdaUpdateWrapper<FirePatrolPointDO>()
                .eq(FirePatrolPointDO::getId, checkReqVO.getId())
                .set(FirePatrolPointDO::getStatus, "completed")
                .set(FirePatrolPointDO::getResult, checkReqVO.getResult())
                .set(FirePatrolPointDO::getCheckTime, LocalDateTime.now())
                .set(FirePatrolPointDO::getProblemDesc, checkReqVO.getProblemDesc())
                .set(FirePatrolPointDO::getSuggestion, checkReqVO.getSuggestion())
                .set(FirePatrolPointDO::getPhotos, checkReqVO.getPhotos())
                .set(FirePatrolPointDO::getRemark, checkReqVO.getRemark()));

        // 更新任务进度
        updateTaskProgress(point.getTaskId());

        log.info("[checkPoint] 检查点位成功，点位ID：{}，结果：{}", checkReqVO.getId(), checkReqVO.getResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer addTaskPoints(Long taskId, List<Long> deviceIds) {
        FirePatrolTaskDO task = firePatrolTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        log.info("[addTaskPoints] 开始添加巡检点位，任务ID：{}，设备数：{}", taskId, deviceIds.size());

        int count = 0;
        for (Long deviceId : deviceIds) {
            // 与 createTask 保持一致，忽略租户过滤查询设备
            EiotDeviceInfoDO device = TenantUtils.executeIgnoreWithResult(() ->
                    eiotDeviceInfoMapper.selectById(deviceId));

            if (device == null) {
                log.warn("[addTaskPoints] 设备不存在，跳过: deviceId={}", deviceId);
                continue;
            }

            FirePatrolPointDO point = FirePatrolPointDO.builder()
                    .taskId(taskId)
                    .deviceId(deviceId)
                    .deviceName(device.getName())
                    .deviceCode(device.getDn())
                    .deviceType(device.getProductKey())
                    .location(device.getAddr())
                    .floor(null)
                    .area(null)
                    .status("pending")
                    .build();
            point.setTenantId(TenantContextHolder.getTenantId());

            firePatrolPointMapper.insert(point);
            count++;
        }

        // 更新任务点位总数
        firePatrolTaskMapper.update(null, new LambdaUpdateWrapper<FirePatrolTaskDO>()
                .eq(FirePatrolTaskDO::getId, taskId)
                .set(FirePatrolTaskDO::getTotalPoints, task.getTotalPoints() + count));

        log.info("[addTaskPoints] 添加巡检点位成功，任务ID：{}，新增点位数：{}（跳过 {} 个）",
                taskId, count, deviceIds.size() - count);

        return count;
    }

    @Override
    public Map<String, Object> getTaskStatistics(Long propertyId, LocalDate startDate, LocalDate endDate) {
        return tenantAware(() -> {
            Long tenantId = isSystemTenant() ? null : TenantContextHolder.getRequiredTenantId();

            Map<String, Object> result = new HashMap<>();

            // 按状态统计
            List<Map<String, Object>> statusStats = firePatrolTaskMapper.countByStatus(tenantId, propertyId, startDate, endDate);
            Map<String, Integer> statusCount = new HashMap<>();
            statusCount.put("pending", 0);
            statusCount.put("in_progress", 0);
            statusCount.put("completed", 0);
            statusCount.put("overdue", 0);
            statusCount.put("cancelled", 0);
            for (Map<String, Object> stat : statusStats) {
                String status = (String) stat.get("status");
                Long count = (Long) stat.get("count");
                statusCount.put(status, count.intValue());
            }
            result.put("statusStatistics", statusCount);

            // 按类型统计
            List<Map<String, Object>> typeStats = firePatrolTaskMapper.countByType(tenantId, propertyId, startDate, endDate);
            result.put("typeStatistics", typeStats);

            return result;
        });
    }

    @Override
    public List<FirePatrolTaskRespVO> getUserPendingTasks(Long userId) {
        return tenantAware(() -> {
            Long tenantId = isSystemTenant() ? null : TenantContextHolder.getRequiredTenantId();
            List<FirePatrolTaskDO> tasks = firePatrolTaskMapper.selectUserPendingTasks(tenantId, userId);

            List<FirePatrolTaskRespVO> list = new ArrayList<>();
            for (FirePatrolTaskDO task : tasks) {
                list.add(convertToRespVO(task));
            }

            return list;
        });
    }

    @Override
    public Integer countTodayPendingTasks() {
        return tenantAware(() -> {
            Long tenantId = isSystemTenant() ? null : TenantContextHolder.getRequiredTenantId();
            return firePatrolTaskMapper.countTodayPendingTasks(tenantId);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer checkOverdueTasks() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 查询所有待执行且计划日期已过的任务
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<FirePatrolTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirePatrolTaskDO::getStatus, "pending")
               .lt(FirePatrolTaskDO::getPlanDate, today)
               .eq(FirePatrolTaskDO::getTenantId, tenantId);
        List<FirePatrolTaskDO> overdueTasks = firePatrolTaskMapper.selectList(wrapper);

        int count = 0;
        for (FirePatrolTaskDO task : overdueTasks) {
            firePatrolTaskMapper.update(null, new LambdaUpdateWrapper<FirePatrolTaskDO>()
                    .eq(FirePatrolTaskDO::getId, task.getId())
                    .set(FirePatrolTaskDO::getStatus, "overdue"));
            count++;
        }

        if (count > 0) {
            log.info("[checkOverdueTasks] 超期任务检测完成，共 {} 个任务标记为超期", count);
        }

        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importTasks(List<FirePatrolTaskImportVO> list) {
        log.info("[importTasks] 开始导入巡检任务，共 {} 条", list.size());

        Long tenantId = TenantContextHolder.getTenantId();

        final String propertyName = (tenantId != null) ? getPropertyName(tenantId) : null;

        // 使用通用导入工具类处理
        return ExcelImportUtils.processImport(list, (importVO, rowNum, result) -> {
            if (importVO.getTaskName() == null || importVO.getTaskName().isEmpty()) {
                result.addError(rowNum, importVO.getTaskName(), "任务名称为空");
                return;
            }

            FirePatrolTaskDO task = FirePatrolTaskDO.builder()
                    .taskNo(generateTaskNo())
                    .propertyId(tenantId)
                    .propertyName(propertyName)
                    .taskName(importVO.getTaskName())
                    .taskType(importVO.getTaskType() != null ? importVO.getTaskType() : "routine")
                    .planDate(importVO.getPlanDate())
                    .status("pending")
                    .patrolUserName(importVO.getPatrolUserName())
                    .patrolUserPhone(importVO.getPatrolUserPhone())
                    .totalPoints(0)
                    .completedPoints(0)
                    .normalPoints(0)
                    .abnormalPoints(0)
                    .progress(0)
                    .remark(importVO.getRemark())
                    .build();
            task.setTenantId(TenantContextHolder.getTenantId());
            firePatrolTaskMapper.insert(task);

            log.info("[importTasks] 导入巡检任务成功: taskName={}", task.getTaskName());
        }, "importTasks");
    }

    /**
     * 更新任务进度
     */
    private void updateTaskProgress(Long taskId) {
        tenantAware(() -> {
            Long tenantId = isSystemTenant() ? null : TenantContextHolder.getRequiredTenantId();

            Integer totalPoints = firePatrolPointMapper.selectByTaskId(tenantId, taskId).size();
            Integer completedPoints = firePatrolPointMapper.countCompletedPoints(tenantId, taskId);
            Integer normalPoints = firePatrolPointMapper.countNormalPoints(tenantId, taskId);
            Integer abnormalPoints = firePatrolPointMapper.countAbnormalPoints(tenantId, taskId);

            int progress = totalPoints > 0 ? (completedPoints * 100 / totalPoints) : 0;

            firePatrolTaskMapper.update(null, new LambdaUpdateWrapper<FirePatrolTaskDO>()
                    .eq(FirePatrolTaskDO::getId, taskId)
                    .set(FirePatrolTaskDO::getCompletedPoints, completedPoints)
                    .set(FirePatrolTaskDO::getNormalPoints, normalPoints)
                    .set(FirePatrolTaskDO::getAbnormalPoints, abnormalPoints)
                    .set(FirePatrolTaskDO::getProgress, progress));
            return null;
        });
    }

    /**
     * 生成任务编号
     */
    private String generateTaskNo() {
        return "PT" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }

    /**
     * 获取物业名称（忽略租户过滤，因为 property/list 接口也是 @TenantIgnore 跨租户的）
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
     * 获取任务类型名称
     */
    private String getTaskTypeName(String taskType) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("routine", "日常巡检");
        typeMap.put("special", "专项检查");
        typeMap.put("maintenance", "维保检查");
        return typeMap.getOrDefault(taskType, taskType);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("pending", "待执行");
        statusMap.put("in_progress", "进行中");
        statusMap.put("completed", "已完成");
        statusMap.put("overdue", "已超期");
        statusMap.put("cancelled", "已取消");
        return statusMap.getOrDefault(status, status);
    }

    /**
     * 获取结果名称
     */
    private String getResultName(String result) {
        if (result == null) return null;
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("normal", "正常");
        resultMap.put("abnormal", "异常");
        return resultMap.getOrDefault(result, result);
    }

    /**
     * 转换为响应VO
     */
    private FirePatrolTaskRespVO convertToRespVO(FirePatrolTaskDO task) {
        FirePatrolTaskRespVO vo = new FirePatrolTaskRespVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setPropertyId(task.getPropertyId());
        // Dynamically fill property_name if null in database (handles cross-tenant data)
        if (task.getPropertyName() == null) {
            vo.setPropertyName(getPropertyName(task.getPropertyId()));
        } else {
            vo.setPropertyName(task.getPropertyName());
        }
        vo.setTaskName(task.getTaskName());
        vo.setTaskType(task.getTaskType());
        vo.setTaskTypeName(getTaskTypeName(task.getTaskType()));
        vo.setPlanDate(task.getPlanDate());
        vo.setPlanStartTime(task.getPlanStartTime());
        vo.setPlanEndTime(task.getPlanEndTime());
        vo.setActualStartTime(task.getActualStartTime());
        vo.setActualEndTime(task.getActualEndTime());
        vo.setStatus(task.getStatus());
        vo.setStatusName(getStatusName(task.getStatus()));
        vo.setPatrolUserId(task.getPatrolUserId());
        vo.setPatrolUserName(task.getPatrolUserName());
        vo.setPatrolUserPhone(task.getPatrolUserPhone());
        vo.setTotalPoints(task.getTotalPoints());
        vo.setCompletedPoints(task.getCompletedPoints());
        vo.setNormalPoints(task.getNormalPoints());
        vo.setAbnormalPoints(task.getAbnormalPoints());
        vo.setProgress(task.getProgress());
        vo.setResult(task.getResult());
        vo.setResultName(getResultName(task.getResult()));
        vo.setSummary(task.getSummary());
        vo.setRemark(task.getRemark());
        vo.setCreateTime(task.getCreateTime());
        return vo;
    }

    /**
     * 转换点位为响应VO
     */
    private FirePatrolPointRespVO convertPointToRespVO(FirePatrolPointDO point) {
        FirePatrolPointRespVO vo = new FirePatrolPointRespVO();
        vo.setId(point.getId());
        vo.setTaskId(point.getTaskId());
        vo.setDeviceId(point.getDeviceId());
        vo.setDeviceName(point.getDeviceName());
        vo.setDeviceCode(point.getDeviceCode());
        vo.setDeviceType(point.getDeviceType());
        vo.setLocation(point.getLocation());
        vo.setFloor(point.getFloor());
        vo.setArea(point.getArea());
        vo.setCheckItems(point.getCheckItems());
        vo.setResult(point.getResult());
        vo.setResultName(getResultName(point.getResult()));
        vo.setStatus(point.getStatus());
        vo.setStatusName("completed".equals(point.getStatus()) ? "已检" : "待检");
        vo.setCheckTime(point.getCheckTime());
        vo.setCheckerId(point.getCheckerId());
        vo.setCheckerName(point.getCheckerName());
        vo.setProblemDesc(point.getProblemDesc());
        vo.setSuggestion(point.getSuggestion());
        vo.setPhotos(point.getPhotos());
        vo.setRemark(point.getRemark());
        vo.setCreateTime(point.getCreateTime());
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
