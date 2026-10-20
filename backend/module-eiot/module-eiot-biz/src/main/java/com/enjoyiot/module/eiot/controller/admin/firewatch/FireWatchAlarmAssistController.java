package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist.AlarmAssistRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FireWatchAlarmAssistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 告警辅助判断 Controller（报警弹窗同区域设备/视频资源查询）
 *
 * @author sgd
 */
@Tag(name = "管理后台 - 告警辅助判断")
@RestController
@RequestMapping("/eiot/firewatch/alarm-assist")
@Validated
public class FireWatchAlarmAssistController {

    @Resource
    private FireWatchAlarmAssistService fireWatchAlarmAssistService;

    @GetMapping("/get")
    @Operation(summary = "按告警ID查询辅助判断", description = "返回报警设备、安装空间、同区域设备与关联摄像头；告警不存在返回 null")
    @Parameter(name = "alarmId", description = "告警记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    public CommonResult<AlarmAssistRespVO> getAlarmAssist(@RequestParam("alarmId") Long alarmId) {
        return success(fireWatchAlarmAssistService.getAlarmAssist(alarmId));
    }
}
