package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist.AlarmAssistCameraVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist.AlarmAssistDeviceVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist.AlarmAssistRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist.AlarmAssistSpaceVO;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireSpaceDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.DeviceCameraRelationDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireSpaceMapper;
import com.enjoyiot.module.eiot.dal.mysql.video.DeviceCameraRelationMapper;
import com.enjoyiot.module.eiot.dal.mysql.video.VideoCameraMapper;
import com.enjoyiot.module.eiot.service.UserTenantResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 告警辅助判断 Service 实现（数据以告警记录租户为锚）
 *
 * @author sgd
 */
@Service
public class FireWatchAlarmAssistServiceImpl implements FireWatchAlarmAssistService {

    /** 「报警中」时间窗口：近 30 分钟 */
    private static final long ALARMING_WINDOW_MS = 30 * 60 * 1000L;
    /** 父链上溯最大层数（对齐 AlertServiceImpl.findTransferDeviceId） */
    private static final int TRANSFER_MAX_DEPTH = 5;
    /** 用传设备类型 */
    private static final String DEVICE_TYPE_TRANSFER = "transfer";

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private EiotDeviceInfoMapper deviceInfoMapper;

    @Resource
    private FireSpaceMapper fireSpaceMapper;

    @Resource
    private DeviceCameraRelationMapper relationMapper;

    @Resource
    private VideoCameraMapper videoCameraMapper;

    @Resource
    private UserTenantResolver userTenantResolver;

    @Override
    public AlarmAssistRespVO getAlarmAssist(Long alarmId) {
        if (alarmId == null) {
            return null;
        }
        // 读告警记录：普通租户由租户过滤限定本租户，系统租户可跨租户读取
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        AlertRecordDO record = userTenantId != null
                ? TenantUtils.execute(userTenantId, () -> alertRecordMapper.selectById(alarmId))
                : TenantUtils.executeIgnoreWithResult(() -> alertRecordMapper.selectById(alarmId));
        if (record == null) {
            return null;
        }
        // 锚定告警记录租户；历史记录租户为空/0 时回退登录租户
        Long anchorTenantId = record.getTenantId() != null && record.getTenantId() != 0
                ? record.getTenantId() : userTenantId;
        if (anchorTenantId == null) {
            return buildResp(record);
        }
        Long anchor = anchorTenantId;
        // 锚租户上下文内取数：空间/设备/告警统计自动按锚租户过滤
        return TenantUtils.execute(anchor, () -> buildResp(record));
    }

    private AlarmAssistRespVO buildResp(AlertRecordDO record) {
        AlarmAssistRespVO resp = new AlarmAssistRespVO();
        resp.setAlarmId(record.getId());
        resp.setAlertName(record.getName());
        resp.setAlertLevel(record.getLevel());
        resp.setRelatedDevices(Collections.emptyList());
        resp.setOwnCameras(Collections.emptyList());

        // 报警设备（device_id 为空/0 视为未关联设备）
        EiotDeviceInfoDO alarmDevice = null;
        if (record.getDeviceId() != null && record.getDeviceId() > 0) {
            alarmDevice = deviceInfoMapper.selectById(record.getDeviceId());
        }

        // 同区域设备：同楼栋+楼层+区域（详细位置可不同），排除自身
        AlarmAssistSpaceVO spaceVO = null;
        List<EiotDeviceInfoDO> relatedDevices = Collections.emptyList();
        if (alarmDevice != null && alarmDevice.getSpaceId() != null) {
            FireSpaceDO space = fireSpaceMapper.selectById(alarmDevice.getSpaceId());
            if (space != null) {
                spaceVO = toSpaceVO(space);
                List<Long> areaSpaceIds = fireSpaceMapper.selectList(new LambdaQueryWrapperX<FireSpaceDO>()
                                .eq(FireSpaceDO::getBuilding, space.getBuilding())
                                .eq(FireSpaceDO::getFloor, space.getFloor())
                                .eq(FireSpaceDO::getArea, space.getArea()))
                        .stream().map(FireSpaceDO::getId).collect(Collectors.toList());
                if (!areaSpaceIds.isEmpty()) {
                    relatedDevices = deviceInfoMapper.selectList(new LambdaQueryWrapperX<EiotDeviceInfoDO>()
                            .in(EiotDeviceInfoDO::getSpaceId, areaSpaceIds)
                            .ne(EiotDeviceInfoDO::getId, alarmDevice.getId())
                            .orderByAsc(EiotDeviceInfoDO::getId));
                }
            }
        }
        resp.setSpace(spaceVO);

        // 参与计算的设备集合（报警设备 + 同区域设备）
        List<EiotDeviceInfoDO> allDevices = new ArrayList<>();
        if (alarmDevice != null) {
            allDevices.add(alarmDevice);
        }
        allDevices.addAll(relatedDevices);
        List<Long> allDeviceIds = allDevices.stream().map(EiotDeviceInfoDO::getId).collect(Collectors.toList());

        Map<Long, FireSpaceDO> spaceMap = loadSpaces(allDevices);
        Map<Long, List<AlarmAssistCameraVO>> cameraMap = loadCameras(allDeviceIds);
        Map<Long, Long> transferMap = resolveTransferDeviceIds(allDevices);
        Set<Long> alarmingIds = loadAlarmingDeviceIds(allDeviceIds);

        if (alarmDevice != null) {
            resp.setAlarmDevice(toDeviceVO(alarmDevice, spaceMap, null, transferMap, alarmingIds));
            resp.setOwnCameras(cameraMap.getOrDefault(alarmDevice.getId(), Collections.emptyList()));
        }
        resp.setRelatedDevices(relatedDevices.stream()
                .map(d -> toDeviceVO(d, spaceMap, cameraMap.getOrDefault(d.getId(), Collections.emptyList()),
                        transferMap, alarmingIds))
                .collect(Collectors.toList()));
        return resp;
    }

    /** 批量取设备所属空间（space_id 去重后一次 IN 查询） */
    private Map<Long, FireSpaceDO> loadSpaces(List<EiotDeviceInfoDO> devices) {
        List<Long> spaceIds = devices.stream().map(EiotDeviceInfoDO::getSpaceId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (spaceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return fireSpaceMapper.selectBatchIds(spaceIds).stream()
                .collect(Collectors.toMap(FireSpaceDO::getId, s -> s));
    }

    /** 批量取设备显式绑定摄像头（关系/摄像头沿用既有直查语义，信任边界=设备已归属锚租户） */
    private Map<Long, List<AlarmAssistCameraVO>> loadCameras(List<Long> deviceIds) {
        if (deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DeviceCameraRelationDO> relations = TenantUtils.executeIgnoreWithResult(
                () -> relationMapper.selectListByDeviceIds(deviceIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> cameraIds = relations.stream().map(DeviceCameraRelationDO::getCameraId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, VideoCameraDO> cameraMap = TenantUtils.executeIgnoreWithResult(
                        () -> videoCameraMapper.selectBatchIds(cameraIds)).stream()
                .collect(Collectors.toMap(VideoCameraDO::getId, c -> c));
        Map<Long, List<AlarmAssistCameraVO>> result = new LinkedHashMap<>();
        for (DeviceCameraRelationDO relation : relations) {
            VideoCameraDO camera = cameraMap.get(relation.getCameraId());
            if (camera == null) {
                continue;
            }
            AlarmAssistCameraVO vo = new AlarmAssistCameraVO();
            vo.setCameraId(camera.getId());
            vo.setName(camera.getChannelName());
            vo.setStatus(camera.getStatus());
            result.computeIfAbsent(relation.getDeviceId(), k -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /** 批量解析可反控的用传设备ID（自身是用传设备或父链≤5层上溯命中） */
    private Map<Long, Long> resolveTransferDeviceIds(List<EiotDeviceInfoDO> devices) {
        if (devices.isEmpty()) {
            return Collections.emptyMap();
        }
        // 逐层批量上溯，构建 id→设备 池（最多5层）
        Map<Long, EiotDeviceInfoDO> pool = new HashMap<>();
        devices.forEach(d -> pool.put(d.getId(), d));
        List<Long> levelIds = devices.stream().map(EiotDeviceInfoDO::getParentId)
                .filter(Objects::nonNull).distinct()
                .filter(id -> !pool.containsKey(id)).collect(Collectors.toList());
        int depth = 0;
        while (!levelIds.isEmpty() && depth < TRANSFER_MAX_DEPTH) {
            // lambda 捕获要求 effectively final，先拷一份查询参数（levelIds 会在本轮末尾被重赋值）
            List<Long> queryIds = levelIds;
            List<EiotDeviceInfoDO> parents = TenantUtils.executeIgnoreWithResult(
                    () -> deviceInfoMapper.selectBatchIds(queryIds));
            parents.forEach(p -> pool.put(p.getId(), p));
            levelIds = parents.stream().map(EiotDeviceInfoDO::getParentId)
                    .filter(Objects::nonNull).distinct()
                    .filter(id -> !pool.containsKey(id)).collect(Collectors.toList());
            depth++;
        }
        // 每台设备沿池子上溯
        Map<Long, Long> result = new HashMap<>();
        for (EiotDeviceInfoDO device : devices) {
            result.put(device.getId(), climbTransferDevice(device, pool));
        }
        return result;
    }

    private Long climbTransferDevice(EiotDeviceInfoDO device, Map<Long, EiotDeviceInfoDO> pool) {
        if (DEVICE_TYPE_TRANSFER.equals(device.getDeviceType())) {
            return device.getId();
        }
        Long parentId = device.getParentId();
        int depth = 0;
        while (parentId != null && depth < TRANSFER_MAX_DEPTH) {
            EiotDeviceInfoDO parent = pool.get(parentId);
            if (parent == null) {
                break;
            }
            if (DEVICE_TYPE_TRANSFER.equals(parent.getDeviceType())) {
                return parent.getId();
            }
            parentId = parent.getParentId();
            depth++;
        }
        return null;
    }

    /** 近窗口内存在未闭环告警的设备集合 */
    private Set<Long> loadAlarmingDeviceIds(List<Long> deviceIds) {
        if (deviceIds.isEmpty()) {
            return Collections.emptySet();
        }
        long threshold = System.currentTimeMillis() - ALARMING_WINDOW_MS;
        List<Map<String, Object>> rows = alertRecordMapper.countUnclosedAlertsByDeviceIds(deviceIds, threshold);
        Set<Long> result = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object deviceId = row.get("deviceId");
            if (deviceId instanceof Number) {
                result.add(((Number) deviceId).longValue());
            }
        }
        return result;
    }

    private AlarmAssistDeviceVO toDeviceVO(EiotDeviceInfoDO device, Map<Long, FireSpaceDO> spaceMap,
                                           List<AlarmAssistCameraVO> cameras, Map<Long, Long> transferMap,
                                           Set<Long> alarmingIds) {
        AlarmAssistDeviceVO vo = new AlarmAssistDeviceVO();
        vo.setDeviceId(device.getId());
        vo.setName(device.getName());
        vo.setDn(device.getDn());
        vo.setType(device.getDeviceType());
        vo.setLocation(buildLocation(device, spaceMap));
        vo.setOnline(Integer.valueOf(1).equals(device.getState()));
        vo.setAlarming(alarmingIds.contains(device.getId()));
        vo.setDataTime(resolveDataTime(device));
        vo.setTransferDeviceId(transferMap.get(device.getId()));
        vo.setCameras(cameras);
        return vo;
    }

    /** 位置：优先空间完整路径，无空间回退设备 addr 快照 */
    private String buildLocation(EiotDeviceInfoDO device, Map<Long, FireSpaceDO> spaceMap) {
        FireSpaceDO space = device.getSpaceId() != null ? spaceMap.get(device.getSpaceId()) : null;
        if (space != null) {
            return buildSpacePath(space);
        }
        return device.getAddr() != null ? device.getAddr() : "";
    }

    /** 数据时间：在线时间，缺失回退更新时间 */
    private Long resolveDataTime(EiotDeviceInfoDO device) {
        if (device.getOnlineTime() != null) {
            return device.getOnlineTime();
        }
        return device.getUpdateTime() != null
                ? device.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;
    }

    private AlarmAssistSpaceVO toSpaceVO(FireSpaceDO space) {
        AlarmAssistSpaceVO vo = new AlarmAssistSpaceVO();
        vo.setId(space.getId());
        vo.setBuilding(space.getBuilding());
        vo.setFloor(space.getFloor());
        vo.setArea(space.getArea());
        vo.setPosition(space.getPosition());
        vo.setPath(buildSpacePath(space));
        return vo;
    }

    /** 空间完整路径：楼栋/楼层/区域/详细位置 以 " / " 连接（空段跳过） */
    private String buildSpacePath(FireSpaceDO space) {
        return Stream.of(space.getBuilding(), space.getFloor(), space.getArea(), space.getPosition())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" / "));
    }
}
