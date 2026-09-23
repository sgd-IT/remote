package com.enjoyiot.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.ai.dto.AiAlarmDetailDTO;
import com.enjoyiot.module.ai.dto.AiDataPageReqDTO;
import com.enjoyiot.module.ai.dto.AiDeviceDetailDTO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.service.firewatch.FireAlarmOrderService;
import com.enjoyiot.module.eiot.service.firewatch.FireDashboardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiDataServiceImpl implements AiDataService {

    @Resource
    private FireDashboardService fireDashboardService;

    @Resource
    private FireAlarmOrderService fireAlarmOrderService;

    @Resource
    private EiotDeviceInfoMapper eiotDeviceInfoMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 设备相关 ====================

    @Override
    public AiDeviceDetailDTO getDeviceDetail(Long deviceId) {
        EiotDeviceInfoDO device = eiotDeviceInfoMapper.selectById(deviceId);
        if (device == null) {
            return null;
        }
        return convertDevice(device);
    }

    @Override
    public PageResult<AiDeviceDetailDTO> getDevicePage(AiDataPageReqDTO reqDTO) {
        LambdaQueryWrapper<EiotDeviceInfoDO> wrapper = new LambdaQueryWrapper<>();
        if (reqDTO.getState() != null) {
            wrapper.eq(EiotDeviceInfoDO::getState, reqDTO.getState());
        }
        if (reqDTO.getKeyword() != null && !reqDTO.getKeyword().isBlank()) {
            wrapper.and(w -> w
                    .like(EiotDeviceInfoDO::getName, reqDTO.getKeyword())
                    .or()
                    .like(EiotDeviceInfoDO::getAddr, reqDTO.getKeyword()));
        }
        wrapper.orderByDesc(EiotDeviceInfoDO::getId);

        int pageNo = reqDTO.getPageNo() != null ? reqDTO.getPageNo() : 1;
        int pageSize = reqDTO.getPageSize() != null ? reqDTO.getPageSize() : 20;

        Page<EiotDeviceInfoDO> page = new Page<>(pageNo, pageSize);
        Page<EiotDeviceInfoDO> result = eiotDeviceInfoMapper.selectPage(page, wrapper);

        List<AiDeviceDetailDTO> list = result.getRecords().stream()
                .map(this::convertDevice)
                .toList();
        return new PageResult<>(list, result.getTotal());
    }

    @Override
    public Map<String, Object> getDeviceStatistics() {
        DeviceStatusStatisticsRespVO stats = fireDashboardService.getDeviceStatusStatistics();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("设备总数", stats.getTotal());
        map.put("在线数", stats.getOnline());
        map.put("离线数", stats.getOffline());
        map.put("正常数", stats.getNormal());
        map.put("告警数", stats.getAlarm());
        map.put("故障数", stats.getFault());
        map.put("未激活数", stats.getInactive());
        map.put("在线率", stats.getOnlineRate());
        map.put("告警率", stats.getAlarmRate());
        map.put("故障率", stats.getFaultRate());
        return map;
    }

    // ==================== 告警相关 ====================

    @Override
    public AiAlarmDetailDTO getAlarmDetail(Long orderId) {
        AlarmOrderRespVO order = fireAlarmOrderService.getAlarmOrder(orderId);
        if (order == null) {
            return null;
        }
        return convertAlarm(order);
    }

    @Override
    public PageResult<AiAlarmDetailDTO> getAlarmPage(AiDataPageReqDTO reqDTO) {
        AlarmOrderPageReqVO pageReqVO = new AlarmOrderPageReqVO();
        pageReqVO.setPageNo(reqDTO.getPageNo() != null ? reqDTO.getPageNo() : 1);
        pageReqVO.setPageSize(reqDTO.getPageSize() != null ? reqDTO.getPageSize() : 20);
        pageReqVO.setPropertyId(reqDTO.getPropertyId());
        pageReqVO.setAlarmType(reqDTO.getAlarmType());
        pageReqVO.setAlarmLevel(reqDTO.getAlarmLevel());
        pageReqVO.setOrderStatus(reqDTO.getOrderStatus());
        if (reqDTO.getStartTime() != null && !reqDTO.getStartTime().isBlank()) {
            pageReqVO.setAlarmTimeBegin(LocalDateTime.parse(reqDTO.getStartTime(), DTF));
        }
        if (reqDTO.getEndTime() != null && !reqDTO.getEndTime().isBlank()) {
            pageReqVO.setAlarmTimeEnd(LocalDateTime.parse(reqDTO.getEndTime(), DTF));
        }

        PageResult<AlarmOrderRespVO> pageResult = fireAlarmOrderService.pageAlarmOrders(pageReqVO);
        List<AiAlarmDetailDTO> list = pageResult.getList().stream()
                .map(this::convertAlarm)
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public Map<String, Object> getAlarmStatistics() {
        TodayAlarmStatisticsRespVO today = fireDashboardService.getTodayAlarmStatistics();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("今日告警总数", today.getTotalAlarms());
        map.put("已处置数", today.getHandledCount());
        map.put("待处理数", today.getPendingCount());
        map.put("误报数", today.getFalseAlarmCount());
        map.put("处置率", today.getHandleRate());
        map.put("平均响应时间(秒)", today.getAvgResponseTime());
        map.put("火警数", today.getFireAlarmCount());
        map.put("故障告警数", today.getFaultAlarmCount());
        return map;
    }

    @Override
    public List<AiAlarmDetailDTO> getRealtimeAlarms(Integer limit) {
        int actualLimit = (limit != null && limit > 0) ? limit : 10;
        return fireDashboardService.getRealtimeAlarms(actualLimit).stream()
                .map(this::convertAlarm)
                .toList();
    }

    // ==================== 转换方法 ====================

    private AiDeviceDetailDTO convertDevice(EiotDeviceInfoDO device) {
        AiDeviceDetailDTO dto = new AiDeviceDetailDTO();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setDn(device.getDn());
        dto.setSerialNo(device.getSerialNo());
        dto.setState(device.getState());
        dto.setStateName(resolveStateName(device.getState()));
        dto.setModel(device.getModel());
        dto.setAddr(device.getAddr());
        dto.setLon(device.getLon());
        dto.setLat(device.getLat());
        dto.setProductKey(device.getProductKey());
        dto.setFirmVersion(device.getFirmVersion());
        dto.setTenantId(device.getTenantId());
        dto.setCreateTime(device.getCreateTime());
        return dto;
    }

    private AiAlarmDetailDTO convertAlarm(AlarmOrderRespVO order) {
        AiAlarmDetailDTO dto = new AiAlarmDetailDTO();
        BeanUtils.copyProperties(order, dto);
        return dto;
    }

    private String resolveStateName(Integer state) {
        if (state == null) return "未知";
        return switch (state) {
            case 0 -> "离线";
            case 1 -> "在线";
            case 3 -> "未激活";
            case 4 -> "禁用";
            default -> "未知";
        };
    }
}
