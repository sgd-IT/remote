package com.enjoyiot.module.eiot.service.remote;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.enjoyiot.framework.common.exception.ServiceException;
import com.enjoyiot.framework.common.pojo.PageParam;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.device.dto.DevicePropertyCache;
import com.enjoyiot.module.eiot.api.device.dto.DeviceShortInfo;
import com.enjoyiot.module.eiot.controller.admin.device.vo.DeviceInfoPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FireDutyScheduleRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FirePropertyRespVO;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import com.enjoyiot.module.eiot.service.device.DeviceCtrlService;
import com.enjoyiot.module.eiot.service.device.DeviceInfoService;
import com.enjoyiot.module.eiot.service.firewatch.FireDutyScheduleService;
import com.enjoyiot.module.eiot.service.firewatch.FirePropertyService;
import com.enjoyiot.module.system.api.dept.DeptApi;
import com.enjoyiot.module.system.api.dept.PostApi;
import com.enjoyiot.module.system.api.dept.dto.DeptRespDTO;
import com.enjoyiot.module.system.api.dept.dto.PostRespDTO;
import com.enjoyiot.module.system.api.user.AdminUserApi;
import com.enjoyiot.module.system.api.user.dto.AdminUserRespDTO;
import com.enjoyiot.module.system.dal.dataobject.tenant.TenantDO;
import com.enjoyiot.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RemoteConsoleServiceImpl implements RemoteConsoleService {

    @Resource
    private DeviceInfoService deviceInfoService;

    @Resource
    private DeviceCtrlService deviceCtrlService;

    @Resource
    private RemoteOperationLogService remoteOperationLogService;

    @Resource
    private TenantService tenantService;

    @Resource
    private PostApi postApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private FirePropertyService firePropertyService;

    @Resource
    private FireDutyScheduleService fireDutyScheduleService;

    @Resource
    private ValveApiProperties valveApiProperties;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final Integer DEVICE_STATE_ONLINE = 1;
    private static final String VALVE_COMMAND_KEY_PREFIX = "eiot:valve:command:";
    private static final long VALVE_COMMAND_TTL_SECONDS = 15L;
    private static final DefaultRedisScript<String> CONSUME_VALVE_COMMAND_SCRIPT = new DefaultRedisScript<>(
            "local command = redis.call('GET', KEYS[1]); if command then redis.call('DEL', KEYS[1]); end; return command;",
            String.class);

    @Override
    public List<RemoteConsoleDeviceTreeRespVO> getDeviceTree() {
        DeviceInfoPageReqVO pageReqVO = new DeviceInfoPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<DeviceShortInfo> pageResult = deviceInfoService.getDeviceInfoPage(pageReqVO);
        List<DeviceShortInfo> devices = pageResult.getList();
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyList();
        }
        // 只显示父设备（顶层设备），过滤掉网关子设备等有父级的子设备
        devices = devices.stream()
                .filter(d -> d.getParentId() == null || d.getParentId().isEmpty())
                .collect(Collectors.toList());
        // 过滤不可见设备（visible 为 false 的不返回）
        devices = devices.stream()
                .filter(d -> d.getVisible() == null || d.getVisible())
                .collect(Collectors.toList());
        if (devices.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集租户ID用于批量查询名称
        Set<Long> tenantIds = devices.stream()
                .map(DeviceShortInfo::getTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> tenantNameMap = new HashMap<>();
        TenantUtils.executeIgnore(() -> {
            for (Long tenantId : tenantIds) {
                try {
                    TenantDO tenant = tenantService.getTenant(tenantId);
                    if (tenant != null) {
                        tenantNameMap.put(tenantId, tenant.getName());
                    }
                } catch (Exception ignored) {
                }
            }
        });

        Map<Long, List<DeviceShortInfo>> grouped = devices.stream()
                .collect(Collectors.groupingBy(d -> d.getTenantId() != null ? d.getTenantId() : 0L));

        List<RemoteConsoleDeviceTreeRespVO> result = new ArrayList<>();
        grouped.forEach((tenantId, list) -> {
            RemoteConsoleDeviceTreeRespVO node = new RemoteConsoleDeviceTreeRespVO();
            node.setId(tenantId);
            node.setName(tenantNameMap.getOrDefault(tenantId, "未知企业"));
            node.setDeviceCount(list.size());
            node.setRiskLevel(calculateRiskLevel(list));
            node.setDevices(list.stream().map(this::convertDeviceItem).collect(Collectors.toList()));
            result.add(node);
        });
        return result;
    }

    @Override
    public RemoteConsoleDeviceDetailRespVO getDeviceDetail(Long deviceId) {
        DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo(deviceId);
        if (deviceInfo == null) {
            throw new ServiceException(500, "设备不存在");
        }
        RemoteConsoleDeviceDetailRespVO vo = new RemoteConsoleDeviceDetailRespVO();
        vo.setId(deviceInfo.getId());
        vo.setName(deviceInfo.getName());
        vo.setState(deviceInfo.getState());
        vo.setMode(getDeviceControlMode(deviceId));
        vo.setAlarmStatus(getAlarmStatus(deviceInfo));
        return vo;
    }

    @Override
    public List<RemoteConsoleDeviceParamRespVO> getDeviceParams(Long deviceId) {
        Map<String, DevicePropertyCache> properties = deviceInfoService.getPropertiesFromCache(deviceId);
        List<RemoteConsoleDeviceParamRespVO> result = new ArrayList<>();
        addParam(result, properties, "temperature", "当前温度", "°C", "🌡️");
        addParam(result, properties, "humidity", "当前湿度", "%", "💧");
        addParam(result, properties, "voltage", "电压", "V", "⚡");
        addParam(result, properties, "battery", "电池电量", "%", "🔋");
        addParam(result, properties, "signal", "信号强度", "dBm", "📶");
        addParam(result, properties, "runtime", "运行时长", "h", "⏱️");
        return result;
    }

    @Override
    public RemoteConsoleDeviceStatusStatsRespVO getDeviceStatusStats() {
        DeviceInfoPageReqVO pageReqVO = new DeviceInfoPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<DeviceShortInfo> pageResult = deviceInfoService.getDeviceInfoPage(pageReqVO);
        List<DeviceShortInfo> devices = pageResult.getList();

        RemoteConsoleDeviceStatusStatsRespVO vo = new RemoteConsoleDeviceStatusStatsRespVO();
        vo.setTotal(devices != null ? devices.size() : 0);
        vo.setOnline((int) (devices != null ? devices.stream().filter(d -> DEVICE_STATE_ONLINE.equals(d.getState())).count() : 0));
        vo.setOffline((int) (devices != null ? devices.stream().filter(d -> !DEVICE_STATE_ONLINE.equals(d.getState())).count() : 0));
        // 告警/故障数量需要结合设备属性或告警记录统计，当前先返回 0
        vo.setAlarm(0);
        vo.setFault(0);
        return vo;
    }

    @Override
    public List<ConsolePersonRespVO> getConsolePersons(Long tenantId) {
        List<ConsolePersonRespVO> persons = new ArrayList<>();
        if (tenantId == null) {
            return persons;
        }

        // 1. 展示岗位下的人员，按岗位排序升序
        Set<Long> displayedUserIds = new HashSet<>();
        TenantUtils.execute(tenantId, () -> {
            List<PostRespDTO> posts = postApi.getConsoleDisplayPostList();
            if (posts == null || posts.isEmpty()) {
                return;
            }
            List<Long> postIds = posts.stream().map(PostRespDTO::getId).collect(Collectors.toList());
            Map<Long, List<Long>> postUserMap = postApi.getUserIdsByPostIds(postIds);
            Set<Long> userIds = postUserMap.values().stream()
                    .flatMap(List::stream).collect(Collectors.toSet());
            if (userIds.isEmpty()) {
                return;
            }
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserList(userIds).stream()
                    .collect(Collectors.toMap(AdminUserRespDTO::getId, u -> u));
            for (PostRespDTO post : posts) {
                List<Long> uids = postUserMap.getOrDefault(post.getId(), Collections.emptyList());
                for (Long uid : uids) {
                    AdminUserRespDTO user = userMap.get(uid);
                    if (user == null) {
                        continue;
                    }
                    ConsolePersonRespVO person = new ConsolePersonRespVO();
                    person.setUserId(user.getId());
                    person.setUserName(user.getNickname());
                    person.setUserPhone(user.getMobile());
                    person.setPosition(post.getName());
                    person.setSource("post");
                    persons.add(person);
                    displayedUserIds.add(user.getId());
                }
            }
        });

        // 2. 当前时间值班人员（已在展示岗位中的去重）
        try {
            FirePropertyRespVO property = firePropertyService.getPropertyByTenantId(tenantId);
            if (property != null) {
                List<FireDutyScheduleRespVO> duties = fireDutyScheduleService.getCurrentDuty(property.getId());
                if (duties != null) {
                    for (FireDutyScheduleRespVO duty : duties) {
                        if (duty.getDutyUserId() != null && displayedUserIds.contains(duty.getDutyUserId())) {
                            continue;
                        }
                        ConsolePersonRespVO person = new ConsolePersonRespVO();
                        person.setUserId(duty.getDutyUserId());
                        person.setUserName(duty.getDutyUserName());
                        person.setUserPhone(duty.getDutyUserPhone());
                        // 值班岗位为空时，用用户部门名称兜底
                        String position = duty.getDutyUserPosition();
                        if (StrUtil.isBlank(position) && duty.getDutyUserId() != null) {
                            position = getDeptNameByUserId(tenantId, duty.getDutyUserId());
                        }
                        person.setPosition(StrUtil.isBlank(position) ? "值班员" : position);
                        person.setSource("duty");
                        persons.add(person);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[getConsolePersons] 查询当前值班人员失败, tenantId={}", tenantId, e);
        }

        return persons;
    }

    /**
     * 在指定租户上下文下查询用户的部门名称
     */
    private String getDeptNameByUserId(Long tenantId, Long userId) {
        return TenantUtils.execute(tenantId, () -> {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user == null || user.getDeptId() == null) {
                return "";
            }
            DeptRespDTO dept = deptApi.getDept(user.getDeptId());
            return dept == null || dept.getName() == null ? "" : dept.getName();
        });
    }

    @Override
    public Boolean switchDeviceMode(Long deviceId, SwitchDeviceModeReqVO reqVO) {
        DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo(deviceId);
        if (deviceInfo == null) {
            throw new ServiceException(500, "设备不存在");
        }

        boolean success;
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("control_mode", reqVO.getMode());
            deviceCtrlService.setProperty(deviceId, properties);
            success = true;
        } catch (Exception e) {
            log.error("切换设备模式失败: deviceId={}, mode={}", deviceId, reqVO.getMode(), e);
            success = false;
        }

        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        RemoteOperationLogDO operationLog = new RemoteOperationLogDO();
        operationLog.setTenantId(deviceInfo.getTenantId());
        operationLog.setDeviceId(deviceId);
        operationLog.setDeviceName(deviceInfo.getName());
        operationLog.setOperationType("SWITCH_MODE");
        operationLog.setOperationName("切换" + ("auto".equals(reqVO.getMode()) ? "自动" : "手动") + "模式");
        operationLog.setOperatorId(operatorId != null ? operatorId : 0L);
        operationLog.setOperatorName(operatorName != null ? operatorName : "");
        operationLog.setCommandStatus(success ? 1 : 0);
        operationLog.setResultMsg(success ? "切换成功" : "切换失败");
        operationLog.setRequestParams(JSONUtil.toJsonStr(reqVO));
        operationLog.setOperationTime(LocalDateTime.now());
        remoteOperationLogService.createLog(operationLog);

        return success;
    }

    @Override
    public Boolean valveControl(String action, ValveControlReqVO reqVO) {
        Long deviceId = reqVO.getDeviceId();
        DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo(deviceId);

        boolean isOpen = "open".equals(action);
        String operationName = isOpen ? "开闸" : "合闸";
        ValveCommandRespVO command = new ValveCommandRespVO();
        command.setAction(action);
        command.setIp(valveApiProperties.getIp());
        command.setProductID(valveApiProperties.getProductId());
        command.setDeviceID(valveApiProperties.getDeviceId());
        command.setActionValue(isOpen ? 1 : 0);
        command.setCreatedAt(LocalDateTime.now());
        String commandJson = JSONUtil.toJsonStr(command);
        redisTemplate.opsForValue().set(getValveCommandKey(deviceId), commandJson,
                VALVE_COMMAND_TTL_SECONDS, TimeUnit.SECONDS);

        // 记录操作日志
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        RemoteOperationLogDO operationLog = new RemoteOperationLogDO();
        operationLog.setTenantId(deviceInfo != null ? deviceInfo.getTenantId() : null);
        operationLog.setDeviceId(deviceId);
        operationLog.setDeviceName(deviceInfo != null ? deviceInfo.getName() : "");
        operationLog.setOperationType("VALVE_CONTROL");
        operationLog.setOperationName(operationName);
        operationLog.setButtonName(operationName);
        operationLog.setFunctionType("VALVE");
        operationLog.setOperatorId(operatorId != null ? operatorId : 0L);
        operationLog.setOperatorName(operatorName != null ? operatorName : "");
        operationLog.setCommandStatus(1);
        operationLog.setResultMsg(operationName + "命令已缓存，等待外部执行");
        operationLog.setRequestParams(commandJson);
        operationLog.setOperationTime(LocalDateTime.now());
        remoteOperationLogService.createLog(operationLog);

        return true;
    }

    @Override
    public ValveCommandRespVO consumeValveCommand(Long deviceId) {
        String commandJson = redisTemplate.execute(CONSUME_VALVE_COMMAND_SCRIPT,
                Collections.singletonList(getValveCommandKey(deviceId)));
        if (StrUtil.isBlank(commandJson)) {
            return null;
        }
        try {
            return JSONUtil.toBean(commandJson, ValveCommandRespVO.class);
        } catch (Exception e) {
            log.error("智能空开命令反序列化异常: deviceId={}", deviceId, e);
            return null;
        }
    }

    private String getValveCommandKey(Long deviceId) {
        return VALVE_COMMAND_KEY_PREFIX + deviceId;
    }

    private RemoteConsoleDeviceTreeItemRespVO convertDeviceItem(DeviceShortInfo device) {
        RemoteConsoleDeviceTreeItemRespVO item = new RemoteConsoleDeviceTreeItemRespVO();
        item.setId(device.getId());
        item.setName(device.getName());
        item.setBrandCode(device.getBrandCode());
        item.setModel(device.getModel());
        item.setState(device.getState());
        item.setAlarmStatus(getAlarmStatus(device));
        return item;
    }

    private String calculateRiskLevel(List<DeviceShortInfo> devices) {
        boolean hasAlarm = devices.stream().anyMatch(d -> "alarm".equals(getAlarmStatus(d)));
        if (hasAlarm) {
            return "danger";
        }
        boolean hasFault = devices.stream().anyMatch(d -> "fault".equals(getAlarmStatus(d)));
        if (hasFault) {
            return "warning";
        }
        return "normal";
    }

    private String getAlarmStatus(DeviceShortInfo device) {
        if (!DEVICE_STATE_ONLINE.equals(device.getState())) {
            return "offline";
        }
        // TODO: 结合设备属性或告警记录判断 alarm/fault 状态
        return "online";
    }

    private String getAlarmStatus(DeviceInfo deviceInfo) {
        if (deviceInfo == null || !DEVICE_STATE_ONLINE.equals(deviceInfo.getState())) {
            return "offline";
        }
        return "online";
    }

    private String getDeviceControlMode(Long deviceId) {
        Map<String, DevicePropertyCache> properties = deviceInfoService.getPropertiesFromCache(deviceId);
        DevicePropertyCache modeProperty = properties != null ? properties.get("control_mode") : null;
        if (modeProperty != null && modeProperty.getValue() != null) {
            String mode = modeProperty.getValue().toString();
            if ("auto".equalsIgnoreCase(mode) || "automatic".equalsIgnoreCase(mode)) {
                return "auto";
            }
        }
        return "manual";
    }

    private void addParam(List<RemoteConsoleDeviceParamRespVO> result, Map<String, DevicePropertyCache> properties,
                          String code, String name, String unit, String icon) {
        RemoteConsoleDeviceParamRespVO vo = new RemoteConsoleDeviceParamRespVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setUnit(unit);
        vo.setIcon(icon);
        if (properties != null && properties.containsKey(code)) {
            DevicePropertyCache cache = properties.get(code);
            vo.setValue(cache != null ? cache.getValue() : null);
        }
        result.add(vo);
    }
}
