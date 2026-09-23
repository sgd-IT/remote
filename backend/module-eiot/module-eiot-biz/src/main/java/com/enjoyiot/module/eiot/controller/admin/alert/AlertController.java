package com.enjoyiot.module.eiot.controller.admin.alert;


import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.module.eiot.api.IdReqVo;
import com.enjoyiot.module.eiot.api.alert.dto.AlertConfig;
import com.enjoyiot.module.eiot.api.alert.dto.AlertConfigPageReqVO;
import com.enjoyiot.module.eiot.api.alert.dto.AlertRecord;
import com.enjoyiot.module.eiot.controller.admin.alertconfig.vo.AlertConfigRespVO;
import com.enjoyiot.module.eiot.controller.admin.alertconfig.vo.AlertConfigSaveReqVO;
import com.enjoyiot.module.eiot.controller.admin.alertconfig.vo.AlertRecordPageReq;
import com.enjoyiot.module.eiot.service.alert.AlertConfigService;
import com.enjoyiot.module.eiot.service.alert.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;


@Tag(name = "告警中心")
@Slf4j
@RestController
@RequestMapping("/alert")
public class AlertController {

    @Resource
    private AlertService alertService;

    @Resource
    private AlertConfigService alertConfigService;

    @Operation(summary ="新增告警中心配置")
    @PreAuthorize("@ss.hasPermission('iot:alertConfig:add')")
    @PostMapping("/createAlertConfig")
    public CommonResult<Long> createAlertConfig(@Valid @RequestBody AlertConfigSaveReqVO createReqVO) {
        return success(alertConfigService.createAlertConfig(createReqVO));
    }

    @Operation(summary ="编辑告警中心配置")
    @PreAuthorize("@ss.hasPermission('iot:alertConfig:edit')")
    @PostMapping("/updateAlertConfig")
    public CommonResult<Boolean> updateAlertConfig(@Valid @RequestBody AlertConfigSaveReqVO updateReqVO) {
        alertConfigService.updateAlertConfig(updateReqVO);
        return success(true);
    }
    @Operation(summary ="删除告警中心配置")
    @PreAuthorize("@ss.hasPermission('iot:alertConfig:remove')")
    @PostMapping("/deleteAlertConfigById")
    public CommonResult<Boolean> deleteAlertConfig(@RequestBody IdReqVo reqVo) {
        alertConfigService.deleteAlertConfig(reqVo.getId());
        return success(true);
    }

    @Operation(summary ="查询告警中心配置分页")
    @PreAuthorize("@ss.hasPermission('iot:alertConfig:query')")
    @PostMapping("/selectAlertConfigPage")
    public CommonResult<PageResult<AlertConfigRespVO>> getAlertConfigPage(@Valid @RequestBody AlertConfigPageReqVO pageReqVO) {
        PageResult<AlertConfig> pageResult = alertConfigService.getAlertConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AlertConfigRespVO.class));
    }


    @Operation(summary ="查询告警消息分页")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    @PostMapping("/selectAlertRecordPage")
    public CommonResult<PageResult<AlertRecord>> selectAlertRecordPage(@RequestBody @Validated AlertRecordPageReq request) {
        return success(alertService.selectAlertRecordPage(request));
    }

    @Operation(summary ="确认处理告警记录")
    @PreAuthorize("@ss.hasPermission('iot:alert:edit')")
    @PostMapping("/confirmAlertRecord")
    public CommonResult<Boolean> confirmAlertRecord(@RequestBody IdReqVo reqVo) {
        alertService.confirmAlertRecord(reqVo.getId());
        return success(true);
    }

    @Operation(summary ="查询近 N 分钟未处置告警")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    @GetMapping("/recentUnprocessedAlerts")
    public CommonResult<List<AlertRecord>> getRecentUnprocessedAlerts(
            @RequestParam(value = "minutes", defaultValue = "5") Integer minutes) {
        return success(alertService.getRecentUnprocessedAlerts(minutes));
    }

    @Operation(summary ="稍后处置告警")
    @PreAuthorize("@ss.hasPermission('iot:alert:edit')")
    @PostMapping("/deferAlertRecord")
    public CommonResult<Boolean> deferAlertRecord(@RequestBody IdReqVo reqVo) {
        alertService.deferAlertRecord(reqVo.getId());
        return success(true);
    }

    @Operation(summary ="去处置告警（状态设为处置中）")
    @PreAuthorize("@ss.hasPermission('iot:alert:edit')")
    @PostMapping("/handleAlertRecord")
    public CommonResult<Boolean> handleAlertRecord(@RequestBody IdReqVo reqVo) {
        alertService.handleAlertRecord(reqVo.getId());
        return success(true);
    }

    @Operation(summary ="查询指定父设备（含子设备）近 N 分钟未处置告警")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    @GetMapping("/unprocessedAlertsByDevices")
    public CommonResult<List<AlertRecord>> getUnprocessedAlertsByDevices(
            @RequestParam("parentDeviceId") Long parentDeviceId,
            @RequestParam(value = "minutes", defaultValue = "5") Integer minutes) {
        return success(alertService.getUnprocessedAlertsByParentDeviceId(parentDeviceId, minutes));
    }

    @Operation(summary ="根据报警设备ID查找用传设备ID")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:alarm:query')")
    @GetMapping("/findTransferDeviceId")
    public CommonResult<Long> findTransferDeviceId(
            @RequestParam("alertDeviceId") Long alertDeviceId) {
        return success(alertService.findTransferDeviceId(alertDeviceId));
    }


}
