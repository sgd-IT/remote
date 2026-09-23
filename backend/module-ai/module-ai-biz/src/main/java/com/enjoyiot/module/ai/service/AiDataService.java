package com.enjoyiot.module.ai.service;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.ai.dto.AiAlarmDetailDTO;
import com.enjoyiot.module.ai.dto.AiDataPageReqDTO;
import com.enjoyiot.module.ai.dto.AiDeviceDetailDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 数据服务接口 - 供外部 AI 服务通过 HTTP 调用
 * <p>
 * 提供设备、告警的详细数据和列表数据，
 * 返回结构化 JSON，供 Python AI 服务消费。
 */
@Service("aiDataService")
public interface AiDataService {

    // ==================== 设备相关 ====================

    /**
     * 获取设备详情
     *
     * @param deviceId 设备ID
     * @return 设备详情
     */
    AiDeviceDetailDTO getDeviceDetail(Long deviceId);

    /**
     * 获取设备列表（分页）
     *
     * @param reqDTO 分页 + 筛选条件
     * @return 设备分页列表
     */
    PageResult<AiDeviceDetailDTO> getDevicePage(AiDataPageReqDTO reqDTO);

    /**
     * 获取设备统计概览
     *
     * @return 总数、在线数、离线数、告警数、故障数、在线率等
     */
    Map<String, Object> getDeviceStatistics();

    // ==================== 告警相关 ====================

    /**
     * 获取告警工单详情
     *
     * @param orderId 工单ID
     * @return 告警工单详情
     */
    AiAlarmDetailDTO getAlarmDetail(Long orderId);

    /**
     * 获取告警工单列表（分页）
     *
     * @param reqDTO 分页 + 筛选条件
     * @return 告警工单分页列表
     */
    PageResult<AiAlarmDetailDTO> getAlarmPage(AiDataPageReqDTO reqDTO);

    /**
     * 获取告警统计概览
     *
     * @return 今日告警数、已处置数、待处理数、处置率等
     */
    Map<String, Object> getAlarmStatistics();

    /**
     * 获取实时告警列表
     *
     * @param limit 数量限制
     * @return 最近的告警列表
     */
    List<AiAlarmDetailDTO> getRealtimeAlarms(Integer limit);
}
