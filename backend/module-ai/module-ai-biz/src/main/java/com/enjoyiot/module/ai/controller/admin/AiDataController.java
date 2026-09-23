package com.enjoyiot.module.ai.controller.admin;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.ai.dto.AiAlarmDetailDTO;
import com.enjoyiot.module.ai.dto.AiDataPageReqDTO;
import com.enjoyiot.module.ai.dto.AiDeviceDetailDTO;
import com.enjoyiot.module.ai.service.AiDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "AI 数据接口 - 供外部AI服务调用")
@RestController
@RequestMapping("/ai/data")
@Validated
public class AiDataController {

    @Resource
    private AiDataService aiDataService;

    // ==================== 设备接口 ====================

    @GetMapping("/device/detail")
    @Operation(summary = "获取设备详情")
    @Parameter(name = "deviceId", description = "设备ID", required = true)
    public CommonResult<AiDeviceDetailDTO> getDeviceDetail(@RequestParam Long deviceId) {
        return success(aiDataService.getDeviceDetail(deviceId));
    }

    @GetMapping("/device/page")
    @Operation(summary = "获取设备列表(分页)")
    public CommonResult<PageResult<AiDeviceDetailDTO>> getDevicePage(@Valid AiDataPageReqDTO reqDTO) {
        return success(aiDataService.getDevicePage(reqDTO));
    }

    @GetMapping("/device/statistics")
    @Operation(summary = "获取设备统计概览")
    public CommonResult<Map<String, Object>> getDeviceStatistics() {
        return success(aiDataService.getDeviceStatistics());
    }

    // ==================== 告警接口 ====================

    @GetMapping("/alarm/detail")
    @Operation(summary = "获取告警工单详情")
    @Parameter(name = "orderId", description = "工单ID", required = true)
    public CommonResult<AiAlarmDetailDTO> getAlarmDetail(@RequestParam Long orderId) {
        return success(aiDataService.getAlarmDetail(orderId));
    }

    @GetMapping("/alarm/page")
    @Operation(summary = "获取告警工单列表(分页)")
    public CommonResult<PageResult<AiAlarmDetailDTO>> getAlarmPage(@Valid AiDataPageReqDTO reqDTO) {
        return success(aiDataService.getAlarmPage(reqDTO));
    }

    @GetMapping("/alarm/statistics")
    @Operation(summary = "获取告警统计概览")
    public CommonResult<Map<String, Object>> getAlarmStatistics() {
        return success(aiDataService.getAlarmStatistics());
    }

    @GetMapping("/alarm/realtime")
    @Operation(summary = "获取实时告警列表")
    @Parameter(name = "limit", description = "数量限制，默认10")
    public CommonResult<List<AiAlarmDetailDTO>> getRealtimeAlarms(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return success(aiDataService.getRealtimeAlarms(limit));
    }
}
