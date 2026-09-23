package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderAssignReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderUpdateReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenancePlanDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireRepairOrderDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireRepairOrderMapper;
import com.enjoyiot.module.system.api.sms.SmsSendApi;
import com.enjoyiot.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.enjoyiot.module.system.dal.dataobject.tenant.TenantDO;
import com.enjoyiot.module.system.dal.mysql.tenant.TenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 维修工单管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireRepairOrderServiceImpl implements FireRepairOrderService {

    @Resource
    private FireRepairOrderMapper fireRepairOrderMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private EiotDeviceInfoMapper eiotDeviceInfoMapper;

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private FireMaintenancePlanService fireMaintenancePlanService;

    @Resource
    private SmsSendApi smsSendApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRepairOrder(RepairOrderCreateReqVO reqVO) {
        // 通过租户ID获取租户名称
        Long propertyId = null;
        String propertyName = null;
        if (reqVO.getTenantId() != null) {
            propertyId = reqVO.getTenantId(); // 直接存储租户ID
            TenantDO tenant = tenantMapper.selectById(reqVO.getTenantId());
            if (tenant != null) {
                propertyName = tenant.getName();
            }
        }

        // 截止日期：如果未传入则根据紧急程度计算默认值
        LocalDateTime deadline = reqVO.getDeadline() != null
                ? reqVO.getDeadline()
                : calculateDefaultDeadline(reqVO.getUrgencyLevel() != null ? reqVO.getUrgencyLevel() : "normal");

        FireRepairOrderDO order = FireRepairOrderDO.builder()
                .orderNo(generateOrderNo())
                .source("manual")
                .deviceId(reqVO.getDeviceId())
                .deviceName(reqVO.getDeviceName())
                .deviceCode(reqVO.getDeviceCode())
                .propertyId(propertyId)
                .propertyName(propertyName)
                .faultType(reqVO.getFaultType())
                .faultDescription(reqVO.getFaultDescription())
                .faultImages(reqVO.getFaultImages())
                .urgencyLevel(reqVO.getUrgencyLevel() != null ? reqVO.getUrgencyLevel() : "normal")
                .orderStatus("pending")
                .reportTime(LocalDateTime.now())
                .deadline(deadline)
                .reportSource(reqVO.getReportSource() != null ? reqVO.getReportSource() : "manual")
                .repairPerson(reqVO.getRepairPerson())
                .repairCompany(reqVO.getRepairCompany())
                .repairPersonPhone(reqVO.getRepairPersonPhone())
                .build();

        // 设置租户ID：使用传入的租户ID（propertyId），确保 tenant_id 与 property_id 一致
        order.setTenantId(propertyId != null ? propertyId : TenantContextHolder.getTenantId());

        fireRepairOrderMapper.insert(order);

        log.info("[createRepairOrder] 手动创建维修工单成功，工单编号：{}", order.getOrderNo());

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromAlert(Long alertRecordId) {
        // 查询告警记录
        // 管理员：忽略租户过滤，可查看所有告警；企业用户：自动按租户过滤，只能看自己的告警
        AlertRecordDO alertRecord;
        if (isSystemTenant()) {
            alertRecord = TenantUtils.executeIgnoreWithResult(() ->
                    alertRecordMapper.selectById(alertRecordId));
        } else {
            alertRecord = alertRecordMapper.selectById(alertRecordId);
        }
        if (alertRecord == null) {
            throw new IllegalArgumentException("告警记录不存在");
        }

        // 检查该告警是否已生成工单，防止重复生成
        FireRepairOrderDO existOrder = fireRepairOrderMapper.selectOne(
                new LambdaQueryWrapper<FireRepairOrderDO>()
                        .eq(FireRepairOrderDO::getAlertRecordId, alertRecordId));
        if (existOrder != null) {
            throw new IllegalArgumentException("该告警记录已生成维修工单，工单编号：" + existOrder.getOrderNo());
        }

        // 查询设备信息（同样，管理员忽略租户过滤，企业用户按租户过滤）
        EiotDeviceInfoDO device = null;
        Long propertyId = null;
        String propertyName = null;
        if (alertRecord.getDeviceId() != null) {
            if (isSystemTenant()) {
                device = TenantUtils.executeIgnoreWithResult(() ->
                        eiotDeviceInfoMapper.selectById(alertRecord.getDeviceId()));
            } else {
                device = eiotDeviceInfoMapper.selectById(alertRecord.getDeviceId());
            }
        }

        // 通过设备所属租户获取租户信息
        if (device != null && device.getTenantId() != null) {
            propertyId = device.getTenantId(); // 直接存储租户ID
            TenantDO tenant = tenantMapper.selectById(device.getTenantId());
            if (tenant != null) {
                propertyName = tenant.getName();
            }
        }

        // 计算默认排查截止时间
        LocalDateTime deadline = calculateDefaultDeadline("normal");

        // 构建故障描述：设备信息 + 告警内容
        String faultDescription = buildFaultDescription(alertRecord, device);

        // 生成工单
        FireRepairOrderDO order = FireRepairOrderDO.builder()
                .orderNo(generateOrderNo())
                .alertRecordId(alertRecordId)
                .source("auto-alert")
                .deviceId(alertRecord.getDeviceId())
                .deviceName(device != null ? device.getName() : null)
                .deviceCode(device != null ? device.getDn() : null)
                .propertyId(propertyId)
                .propertyName(propertyName)
                .faultType("device_fault")
                .faultDescription(faultDescription)
                .urgencyLevel("normal")
                .orderStatus("pending")
                .reportTime(LocalDateTime.now())
                .deadline(deadline)
                .build();

        // 设置租户ID：使用设备所属租户（propertyId），确保 tenant_id 与 property_id 一致
        order.setTenantId(propertyId != null ? propertyId : TenantContextHolder.getTenantId());

        fireRepairOrderMapper.insert(order);

        log.info("[createFromAlert] 从故障告警生成维修工单成功，告警记录ID：{}，工单编号：{}",
                alertRecordId, order.getOrderNo());

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRepairOrder(RepairOrderUpdateReqVO reqVO) {
        tenantAware(() -> {
            FireRepairOrderDO existOrder = fireRepairOrderMapper.selectById(reqVO.getId());
            if (existOrder == null) {
                throw new IllegalArgumentException("工单不存在");
            }

            FireRepairOrderDO updateObj = FireRepairOrderDO.builder()
                    .id(reqVO.getId())
                    .faultType(reqVO.getFaultType())
                    .faultDescription(reqVO.getFaultDescription())
                    .faultImages(reqVO.getFaultImages())
                    .urgencyLevel(reqVO.getUrgencyLevel())
                    .deadline(reqVO.getDeadline())
                    .build();

            fireRepairOrderMapper.updateById(updateObj);

            log.info("[updateRepairOrder] 更新维修工单成功，工单ID：{}", reqVO.getId());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignOrder(RepairOrderAssignReqVO reqVO) {
        tenantAware(() -> {
            FireRepairOrderDO existOrder = fireRepairOrderMapper.selectById(reqVO.getId());
            if (existOrder == null) {
                throw new IllegalArgumentException("工单不存在");
            }
            if (!"pending".equals(existOrder.getOrderStatus())) {
                throw new IllegalArgumentException("仅待处理状态的工单可派单");
            }

            // 从租户信息中获取联系人作为指派人
            String assignee = null;
            String assigneePhone = null;
            if (existOrder.getPropertyId() != null) {
                // propertyId 现在直接存储的是租户ID
                TenantDO tenant = tenantMapper.selectById(existOrder.getPropertyId());
                if (tenant != null) {
                    assignee = tenant.getContactName();
                    assigneePhone = tenant.getContactMobile();
                }
            }

            // 计算截止时间：如果未传入则使用默认
            LocalDateTime deadline = reqVO.getDeadline();
            if (deadline == null) {
                deadline = calculateDefaultDeadline(existOrder.getUrgencyLevel());
            }

            fireRepairOrderMapper.update(null, new LambdaUpdateWrapper<FireRepairOrderDO>()
                    .eq(FireRepairOrderDO::getId, reqVO.getId())
                    .set(FireRepairOrderDO::getOrderStatus, "assigned")
                    .set(FireRepairOrderDO::getAssignee, assignee)
                    .set(FireRepairOrderDO::getAssigneePhone, assigneePhone)
                    .set(FireRepairOrderDO::getAssignTime, LocalDateTime.now())
                    .set(FireRepairOrderDO::getDeadline, deadline));

            log.info("[assignOrder] 派单成功，工单ID：{}，指派人：{}，排查截止：{}",
                    reqVO.getId(), assignee, deadline);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long id, String completeRemark, String completeImages) {
        tenantAware(() -> {
            FireRepairOrderDO existOrder = fireRepairOrderMapper.selectById(id);
            if (existOrder == null) {
                throw new IllegalArgumentException("工单不存在");
            }
            if (!"assigned".equals(existOrder.getOrderStatus()) && !"in_progress".equals(existOrder.getOrderStatus())) {
                throw new IllegalArgumentException("仅待排查或维护中的工单可完成");
            }

            LambdaUpdateWrapper<FireRepairOrderDO> updateWrapper = new LambdaUpdateWrapper<FireRepairOrderDO>()
                    .eq(FireRepairOrderDO::getId, id)
                    .set(FireRepairOrderDO::getOrderStatus, "completed")
                    .set(FireRepairOrderDO::getCompleteTime, LocalDateTime.now())
                    .set(FireRepairOrderDO::getCompleteRemark, completeRemark)
                    .set(FireRepairOrderDO::getCompleteImages, completeImages);

            // 如果工单已有维修人，自动设置为完成时的维修人
            if (existOrder.getRepairPerson() != null) {
                updateWrapper.set(FireRepairOrderDO::getRepairPerson, existOrder.getRepairPerson());
            }

            fireRepairOrderMapper.update(null, updateWrapper);

            log.info("[completeOrder] 完成维修工单，工单ID：{}", id);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(Long id) {
        tenantAware(() -> {
            FireRepairOrderDO existOrder = fireRepairOrderMapper.selectById(id);
            if (existOrder == null) {
                throw new IllegalArgumentException("工单不存在");
            }
            if ("closed".equals(existOrder.getOrderStatus())) {
                throw new IllegalArgumentException("工单已关闭");
            }

            fireRepairOrderMapper.update(null, new LambdaUpdateWrapper<FireRepairOrderDO>()
                    .eq(FireRepairOrderDO::getId, id)
                    .set(FireRepairOrderDO::getOrderStatus, "closed"));

            log.info("[closeOrder] 关闭维修工单，工单ID：{}", id);
        });
    }

    @Override
    public RepairOrderRespVO getRepairOrder(Long id) {
        return tenantAware(() -> {
            FireRepairOrderDO order = fireRepairOrderMapper.selectById(id);
            if (order == null) {
                return null;
            }
            return convertToRespVO(order);
        });
    }

    @Override
    public PageResult<RepairOrderRespVO> pageRepairOrders(RepairOrderPageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireRepairOrderDO> wrapper = new LambdaQueryWrapper<>();

            wrapper.like(pageReqVO.getOrderNo() != null, FireRepairOrderDO::getOrderNo, pageReqVO.getOrderNo());
            // 按租户ID过滤（propertyId 现在直接存储的是租户ID）
            wrapper.eq(pageReqVO.getTenantId() != null, FireRepairOrderDO::getPropertyId, pageReqVO.getTenantId());
            wrapper.like(pageReqVO.getDeviceName() != null, FireRepairOrderDO::getDeviceName, pageReqVO.getDeviceName());
            wrapper.eq(pageReqVO.getFaultType() != null, FireRepairOrderDO::getFaultType, pageReqVO.getFaultType());
            wrapper.eq(pageReqVO.getOrderStatus() != null, FireRepairOrderDO::getOrderStatus, pageReqVO.getOrderStatus());
            wrapper.eq(pageReqVO.getSource() != null, FireRepairOrderDO::getSource, pageReqVO.getSource());
            wrapper.eq(pageReqVO.getReportSource() != null, FireRepairOrderDO::getReportSource, pageReqVO.getReportSource());
            wrapper.eq(pageReqVO.getRepairPerson() != null, FireRepairOrderDO::getRepairPerson, pageReqVO.getRepairPerson());
            wrapper.like(pageReqVO.getPropertyName() != null, FireRepairOrderDO::getPropertyName, pageReqVO.getPropertyName());
            wrapper.ge(pageReqVO.getCreateTimeStart() != null, FireRepairOrderDO::getCreateTime, pageReqVO.getCreateTimeStart());
            wrapper.le(pageReqVO.getCreateTimeEnd() != null, FireRepairOrderDO::getCreateTime, pageReqVO.getCreateTimeEnd());

            // 关键字搜索：匹配工单编号、设备名称、故障描述
            if (pageReqVO.getKeyword() != null) {
                wrapper.and(w -> w
                        .like(FireRepairOrderDO::getOrderNo, pageReqVO.getKeyword())
                        .or().like(FireRepairOrderDO::getDeviceName, pageReqVO.getKeyword())
                        .or().like(FireRepairOrderDO::getFaultDescription, pageReqVO.getKeyword())
                        .or().like(FireRepairOrderDO::getPropertyName, pageReqVO.getKeyword()));
            }

            wrapper.orderByDesc(FireRepairOrderDO::getCreateTime);

            PageResult<FireRepairOrderDO> pageResult = fireRepairOrderMapper.selectPage(pageReqVO, wrapper);

            List<RepairOrderRespVO> list = new ArrayList<>();
            for (FireRepairOrderDO order : pageResult.getList()) {
                list.add(convertToRespVO(order));
            }

            return new PageResult<>(list, pageResult.getTotal());
        });
    }

    @Override
    public Map<String, Integer> getOrderStatusStatistics() {
        return tenantAware(() -> {
            List<Map<String, Object>> dbStats;
            if (isSystemTenant()) {
                // 管理员：统计所有租户的工单
                dbStats = fireRepairOrderMapper.countAllByStatus();
            } else {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                dbStats = fireRepairOrderMapper.countByStatus(tenantId);
            }

            Map<String, Integer> result = new HashMap<>();
            result.put("pending", 0);
            result.put("assigned", 0);
            result.put("in_progress", 0);
            result.put("completed", 0);
            result.put("closed", 0);

            for (Map<String, Object> stat : dbStats) {
                String status = (String) stat.get("order_status");
                Long count = (Long) stat.get("count");
                result.put(status, count.intValue());
            }

            return result;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signOrder(Long id) {
        tenantAware(() -> {
            FireRepairOrderDO existOrder = fireRepairOrderMapper.selectById(id);
            if (existOrder == null) {
                throw new IllegalArgumentException("工单不存在");
            }
            if (!"pending".equals(existOrder.getOrderStatus())) {
                throw new IllegalArgumentException("仅待处理状态的工单可签单");
            }
            fireRepairOrderMapper.update(null, new LambdaUpdateWrapper<FireRepairOrderDO>()
                    .eq(FireRepairOrderDO::getId, id)
                    .set(FireRepairOrderDO::getOrderStatus, "assigned")
                    .set(FireRepairOrderDO::getSignTime, LocalDateTime.now()));
            log.info("[signOrder] 签单成功，工单ID：{}", id);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startRepair(Long id) {
        tenantAware(() -> {
            FireRepairOrderDO existOrder = fireRepairOrderMapper.selectById(id);
            if (existOrder == null) {
                throw new IllegalArgumentException("工单不存在");
            }
            if (!"assigned".equals(existOrder.getOrderStatus())) {
                throw new IllegalArgumentException("仅已派单状态的工单可开始维修");
            }
            fireRepairOrderMapper.update(null, new LambdaUpdateWrapper<FireRepairOrderDO>()
                    .eq(FireRepairOrderDO::getId, id)
                    .set(FireRepairOrderDO::getOrderStatus, "in_progress"));
            log.info("[startRepair] 开始维修，工单ID：{}", id);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rateOrder(Long id, Integer satisfaction, String suggestion) {
        tenantAware(() -> {
            FireRepairOrderDO existOrder = fireRepairOrderMapper.selectById(id);
            if (existOrder == null) {
                throw new IllegalArgumentException("工单不存在");
            }
            if (!"completed".equals(existOrder.getOrderStatus())) {
                throw new IllegalArgumentException("仅已完成状态的工单可评价");
            }
            fireRepairOrderMapper.update(null, new LambdaUpdateWrapper<FireRepairOrderDO>()
                    .eq(FireRepairOrderDO::getId, id)
                    .set(FireRepairOrderDO::getSatisfaction, satisfaction)
                    .set(FireRepairOrderDO::getSuggestion, suggestion));
            log.info("[rateOrder] 评价工单成功，工单ID：{}，满意度：{}", id, satisfaction);
        });
    }

    @Override
    public Integer getOverdueOrderCount() {
        return tenantAware(() -> {
            if (isSystemTenant()) {
                // 管理员：统计所有租户的超期工单
                return fireRepairOrderMapper.countAllOverdueOrders();
            } else {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                return fireRepairOrderMapper.countOverdueOrders(tenantId);
            }
        });
    }

    @Override
    public List<FireRepairOrderDO> getOverdueOrderList() {
        return tenantAware(() -> {
            if (isSystemTenant()) {
                return fireRepairOrderMapper.selectAllOverdueOrders();
            } else {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                return fireRepairOrderMapper.selectOverdueOrders(tenantId);
            }
        });
    }

    @Override
    public List<FireRepairOrderDO> getUnurgedOverdueOrderList() {
        List<FireRepairOrderDO> allOverdue = getOverdueOrderList();
        return allOverdue.stream()
                .filter(o -> o.getUrgeSent() == null || !o.getUrgeSent())
                .collect(Collectors.toList());
    }

    @Override
    @TenantIgnore
    public void markUrgeSent(List<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) return;
        fireRepairOrderMapper.batchMarkUrgeSent(orderIds);
    }

    @Override
    public Integer countUnurgedOverdueOrders() {
        return getUnurgedOverdueOrderList().size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendUrgeNotifications(List<FireRepairOrderDO> orders, List<FireMaintenancePlanDO> plans) {
        // 按 propertyId 分组统计
        Map<Long, Map<String, Object>> propertyStats = new LinkedHashMap<>();
        List<Long> urgedOrderIds = new ArrayList<>();
        List<Long> urgedPlanIds = new ArrayList<>();

        for (FireRepairOrderDO order : orders) {
            Long pid = order.getPropertyId();
            if (pid == null) continue;
            propertyStats.computeIfAbsent(pid, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("orderCount", 0);
                m.put("planCount", 0);
                m.put("propertyName", "");
                return m;
            });
            Map<String, Object> stats = propertyStats.get(pid);
            stats.put("orderCount", (int) stats.get("orderCount") + 1);
            urgedOrderIds.add(order.getId());
        }

        for (FireMaintenancePlanDO plan : plans) {
            Long pid = plan.getPropertyId();
            if (pid == null) continue;
            propertyStats.computeIfAbsent(pid, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("orderCount", 0);
                m.put("planCount", 0);
                m.put("propertyName", "");
                return m;
            });
            Map<String, Object> stats = propertyStats.get(pid);
            stats.put("planCount", (int) stats.get("planCount") + 1);
            urgedPlanIds.add(plan.getId());
        }

        // 发送短信并标记状态
        int successCount = 0;
        int failCount = 0;
        String templateCode = "maintenance_urge";

        for (Map.Entry<Long, Map<String, Object>> entry : propertyStats.entrySet()) {
            Long tenantId = entry.getKey();
            Map<String, Object> stats = entry.getValue();

            TenantDO tenant = tenantMapper.selectById(tenantId);
            if (tenant == null || StrUtil.isBlank(tenant.getContactMobile())) {
                log.warn("企业联系信息不完整，tenantId: {}", tenantId);
                failCount++;
                continue;
            }

            stats.put("propertyName", tenant.getName());

            Map<String, Object> params = new HashMap<>();
            params.put("propertyName", tenant.getName());
            params.put("orderCount", stats.get("orderCount"));
            params.put("planCount", stats.get("planCount"));

            SmsSendSingleToUserReqDTO reqDTO = new SmsSendSingleToUserReqDTO();
            reqDTO.setMobile(tenant.getContactMobile());
            reqDTO.setTemplateCode(templateCode);
            reqDTO.setTemplateParams(params);

            try {
                smsSendApi.sendSingleSmsToAdmin(reqDTO);
                successCount++;
                log.info("催促短信已发送，tenantId: {}, phone: {}", tenantId,
                        StrUtil.hide(tenant.getContactMobile(), 3, 7));
            } catch (Exception e) {
                log.error("发送催促短信失败，tenantId: {}", tenantId, e);
                failCount++;
            }
        }

        // 批量标记催办状态
        // 注意：此处直接调用Mapper，而非通过 self.markUrgeSent() 自调用，
        // 因为 Spring AOP @TenantIgnore 无法拦截自调用（sendUrgeNotifications 在同一对象内）
        if (CollUtil.isNotEmpty(urgedOrderIds)) {
            fireRepairOrderMapper.batchMarkUrgeSent(urgedOrderIds);
        }
        fireMaintenancePlanService.markUrgeSent(urgedPlanIds);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("totalProperties", propertyStats.size());
        result.put("totalOrders", urgedOrderIds.size());
        result.put("totalPlans", urgedPlanIds.size());
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成工单编号：RO + yyyyMMdd + 4位随机
     */
    private String generateOrderNo() {
        String datePart = java.time.LocalDate.now().toString().replace("-", "");
        String randomPart = IdUtil.randomUUID().substring(0, 4).toUpperCase();
        return "RO-" + datePart + "-" + randomPart;
    }

    /**
     * 根据紧急程度计算默认排查截止时间
     */
    private LocalDateTime calculateDefaultDeadline(String urgencyLevel) {
        LocalDateTime now = LocalDateTime.now();
        if ("urgent".equals(urgencyLevel)) {
            return now.plusHours(4);
        } else if ("major".equals(urgencyLevel)) {
            return now.plusHours(8);
        } else {
            return now.plusHours(24);
        }
    }

    /**
     * 构建故障描述：取告警详情原始内容（去掉末尾确认火警等追加记录）
     */
    private String buildFaultDescription(AlertRecordDO alertRecord, EiotDeviceInfoDO device) {
        if (alertRecord.getName() != null || alertRecord.getDetails() != null) {
            StringBuilder sb = new StringBuilder();
            if (alertRecord.getName() != null) {
                sb.append(alertRecord.getName());
            }
            // 取原始告警详情，去掉末尾确认火警等追加的记录行
            String details = alertRecord.getDetails();
            if (details != null) {
                details = details.trim();
                while (true) {
                    int lastNewline = details.lastIndexOf('\n');
                    if (lastNewline < 0) break;
                    String lastLine = details.substring(lastNewline + 1).trim();
                    if (lastLine.startsWith("[")) {
                        details = details.substring(0, lastNewline).trim();
                    } else {
                        break;
                    }
                }
                if (!details.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(details);
                }
            }
            return sb.toString();
        }
        return "设备故障";
    }

    /**
     * 获取租户名称（propertyId 现在直接存储的是租户ID）
     */
    private String getPropertyName(Long propertyId) {
        if (propertyId == null) return null;
        TenantDO tenant = tenantMapper.selectById(propertyId);
        return tenant != null ? tenant.getName() : null;
    }

    /**
     * 获取紧急程度名称
     */
    private String getUrgencyLevelName(String urgencyLevel) {
        Map<String, String> map = new HashMap<>();
        map.put("urgent", "紧急");
        map.put("major", "较大");
        map.put("normal", "一般");
        return map.getOrDefault(urgencyLevel, urgencyLevel);
    }

    /**
     * 获取状态名称
     */
    private String getOrderStatusName(String orderStatus) {
        Map<String, String> map = new HashMap<>();
        map.put("pending", "待处理");
        map.put("assigned", "待排查");
        map.put("in_progress", "维护中");
        map.put("completed", "已完成");
        map.put("closed", "已关闭");
        return map.getOrDefault(orderStatus, orderStatus);
    }

    /**
     * 获取来源名称
     */
    private String getSourceName(String source) {
        Map<String, String> map = new HashMap<>();
        map.put("auto-alert", "告警生成");
        map.put("manual", "手动创建");
        return map.getOrDefault(source, source);
    }

    /**
     * 获取报修来源名称
     */
    private String getReportSourceName(String reportSource) {
        Map<String, String> map = new HashMap<>();
        map.put("platform", "平台推送");
        map.put("phone", "电话通知");
        map.put("manual", "手动登记");
        return map.getOrDefault(reportSource, reportSource);
    }

    /**
     * 转换为响应VO
     */
    private RepairOrderRespVO convertToRespVO(FireRepairOrderDO order) {
        RepairOrderRespVO vo = new RepairOrderRespVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setAlertRecordId(order.getAlertRecordId());
        vo.setSource(order.getSource());
        vo.setSourceName(getSourceName(order.getSource()));
        vo.setDeviceId(order.getDeviceId());
        vo.setDeviceCode(order.getDeviceCode());
        vo.setDeviceName(order.getDeviceName());
        vo.setPropertyId(order.getPropertyId());
        vo.setPropertyName(order.getPropertyName());
        vo.setFaultType(order.getFaultType());
        vo.setFaultDescription(order.getFaultDescription());
        vo.setFaultImages(order.getFaultImages());
        vo.setUrgencyLevel(order.getUrgencyLevel());
        vo.setUrgencyLevelName(getUrgencyLevelName(order.getUrgencyLevel()));
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusName(getOrderStatusName(order.getOrderStatus()));
        vo.setAssignee(order.getAssignee());
        vo.setAssigneePhone(order.getAssigneePhone());
        vo.setAssignTime(order.getAssignTime());
        vo.setDeadline(order.getDeadline());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setCompleteRemark(order.getCompleteRemark());
        vo.setCompleteImages(order.getCompleteImages());
        vo.setSignTime(order.getSignTime());
        vo.setRepairPerson(order.getRepairPerson());
        vo.setRepairPersonPhone(order.getRepairPersonPhone());
        vo.setRepairCompany(order.getRepairCompany());
        vo.setReportSource(order.getReportSource());
        vo.setReportSourceName(getReportSourceName(order.getReportSource()));
        vo.setSatisfaction(order.getSatisfaction());
        vo.setSuggestion(order.getSuggestion());
        vo.setFaultUnit(order.getPropertyName());
        vo.setReportTime(order.getReportTime());
        vo.setCreateTime(order.getCreateTime());
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

    /** 无返回值的 tenantAware，用于写操作 */
    private void tenantAware(Runnable runnable) {
        if (isSystemTenant()) {
            TenantUtils.executeIgnore(runnable);
        } else {
            runnable.run();
        }
    }

}
