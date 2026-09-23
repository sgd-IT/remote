package com.enjoyiot.module.eiot.controller.admin.firewatch;

import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.service.firewatch.FireAlarmOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.io.IOException;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 火警处置中心 Controller
 * 
 * 功能概述：提供火警警情的接收、确认、处置全流程支持。
 * 
 * 主要功能：
 * - 警情接收与展示：实时接收火警信息，展示告警详情
 * - AI辅助判断：烟雾/温度/复合报警多维度分析，辅助人工决策
 * - 视频联动窗口：火警发生时自动调取相关区域视频
 * - 快捷控制面板：复位/消音/电话等快捷操作
 * - 警情处置流程：确认、误报、真实火警等状态流转
 * - 处置记录：完整记录处置过程和时间节点
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 火警处置中心")
@RestController
@RequestMapping("/eiot/firewatch/alarm-order")
@Validated
public class FireAlarmOrderController {

    @Resource
    private FireAlarmOrderService fireAlarmOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建火警处置工单", description = "接收告警并创建处置工单")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:create')")
    public CommonResult<Long> createAlarmOrder(@Valid @RequestBody AlarmOrderCreateReqVO createReqVO) {
        return success(fireAlarmOrderService.createAlarmOrder(createReqVO));
    }

    @PutMapping("/handle")
    @Operation(summary = "处置火警工单", description = "处理火警工单，支持多种处置类型")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> handleAlarmOrder(@Valid @RequestBody AlarmOrderHandleReqVO handleReqVO) {
        fireAlarmOrderService.handleAlarmOrder(handleReqVO);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认火警", description = "确认火警告警")
    @Parameter(name = "id", description = "工单ID", required = true)
    @Parameter(name = "handler", description = "处理人")
    @Parameter(name = "remark", description = "备注")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> confirmAlarm(
            @RequestParam("id") Long id,
            @RequestParam(value = "handler", required = false) String handler,
            @RequestParam(value = "remark", required = false) String remark) {
        if (StrUtil.isBlank(handler)) {
            handler = SecurityFrameworkUtils.getLoginUserNickname();
        }
        fireAlarmOrderService.confirmAlarm(id, handler, remark);
        return success(true);
    }

    @PutMapping("/false-alarm")
    @Operation(summary = "标记为误报", description = "将火警标记为误报")
    @Parameter(name = "id", description = "工单ID", required = true)
    @Parameter(name = "handler", description = "处理人")
    @Parameter(name = "remark", description = "备注")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> markFalseAlarm(
            @RequestParam("id") Long id,
            @RequestParam(value = "handler", required = false) String handler,
            @RequestParam(value = "remark", required = false) String remark) {
        fireAlarmOrderService.markFalseAlarm(id, handler, remark);
        return success(true);
    }

    @PutMapping("/real-fire")
    @Operation(summary = "确认真实火警", description = "确认为真实火警")
    @Parameter(name = "id", description = "工单ID", required = true)
    @Parameter(name = "handler", description = "处理人")
    @Parameter(name = "remark", description = "备注")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> confirmRealFire(
            @RequestParam("id") Long id,
            @RequestParam(value = "handler", required = false) String handler,
            @RequestParam(value = "remark", required = false) String remark) {
        fireAlarmOrderService.confirmRealFire(id, handler, remark);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭工单", description = "关闭火警处置工单")
    @Parameter(name = "id", description = "工单ID", required = true)
    @Parameter(name = "handler", description = "处理人")
    @Parameter(name = "remark", description = "备注")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> closeOrder(
            @RequestParam("id") Long id,
            @RequestParam(value = "handler", required = false) String handler,
            @RequestParam(value = "remark", required = false) String remark) {
        fireAlarmOrderService.closeOrder(id, handler, remark);
        return success(true);
    }

    @PutMapping("/reset-device")
    @Operation(summary = "复位设备", description = "复位告警设备")
    @Parameter(name = "id", description = "工单ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> resetDevice(@RequestParam("id") Long id) {
        fireAlarmOrderService.resetDevice(id);
        return success(true);
    }

    @PutMapping("/mute")
    @Operation(summary = "消音", description = "对告警进行消音处理")
    @Parameter(name = "id", description = "工单ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> muteAlarm(@RequestParam("id") Long id) {
        fireAlarmOrderService.muteAlarm(id);
        return success(true);
    }

    @PostMapping("/call")
    @Operation(summary = "拨打电话", description = "拨打电话联系相关人员")
    @Parameter(name = "id", description = "工单ID", required = true)
    @Parameter(name = "phone", description = "电话号码", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> makePhoneCall(
            @RequestParam("id") Long id,
            @RequestParam("phone") String phone) {
        fireAlarmOrderService.makePhoneCall(id, phone);
        return success(true);
    }

    @PostMapping("/notify")
    @Operation(summary = "发送通知", description = "发送告警通知")
    @Parameter(name = "id", description = "工单ID", required = true)
    @Parameter(name = "channels", description = "通知渠道(sms,wechat,dingtalk等)")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:handle')")
    public CommonResult<Boolean> sendNotification(
            @RequestParam("id") Long id,
            @RequestParam(value = "channels", required = false) String[] channels) {
        fireAlarmOrderService.sendNotification(id, channels);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取火警工单详情", description = "获取火警处置工单详情")
    @Parameter(name = "id", description = "工单ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    public CommonResult<AlarmOrderRespVO> getAlarmOrder(@RequestParam("id") Long id) {
        return success(fireAlarmOrderService.getAlarmOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询火警工单", description = "分页查询火警处置工单列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    public CommonResult<PageResult<AlarmOrderRespVO>> pageAlarmOrders(@Valid AlarmOrderPageReqVO pageReqVO) {
        return success(fireAlarmOrderService.pageAlarmOrders(pageReqVO));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取报警记录统计", description = "获取报警记录的全量聚合统计数据")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    public CommonResult<AlarmRecordStatisticsRespVO> getAlarmRecordStatistics() {
        return success(fireAlarmOrderService.getAlarmRecordStatistics());
    }

    @GetMapping("/export-pdf")
    @Operation(summary = "导出报警记录PDF", description = "根据筛选条件导出报警记录为PDF文件")
    @Parameter(name = "deviceType", description = "设备类型")
    @Parameter(name = "alarmType", description = "报警类型(fire/fault/risk/offline/false_alarm)")
    @Parameter(name = "status", description = "处置状态(pending/handling/handled/closed)")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "startTime", description = "开始时间(yyyy-MM-dd HH:mm:ss)")
    @Parameter(name = "endTime", description = "结束时间(yyyy-MM-dd HH:mm:ss)")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    public void exportAlarmOrderPdf(
            @RequestParam(value = "deviceType", required = false) String deviceType,
            @RequestParam(value = "alarmType", required = false) String alarmType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            HttpServletResponse response) throws IOException {
        fireAlarmOrderService.exportAlarmOrderPdf(deviceType, alarmType, status,
                propertyId, startTime, endTime, response);
    }

}
