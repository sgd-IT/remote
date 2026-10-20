package com.enjoyiot.module.eiot.controller.app.cameraalert;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.module.eiot.controller.app.cameraalert.vo.CameraAlarmInfoVo;
import com.enjoyiot.module.eiot.controller.app.cameraalert.vo.CameraAlertReportReqVO;
import com.enjoyiot.module.eiot.service.devicealert.DeviceAlertConfigService;
import com.enjoyiot.module.infra.api.file.FileApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 摄像头报警")
@RestController
@RequestMapping("/open/camera-alert")
@Validated
@Slf4j
public class CameraAlertOpenController {

    @Resource
    private DeviceAlertConfigService deviceAlertConfigService;

    @Resource
    private FileApi fileApi;

    @PostMapping("/report")
    @Operation(summary = "上报摄像头报警事件", description = "接收摄像头设备推送的报警事件，包含抓拍图片和事件数据")
    @PermitAll
    public CommonResult<Boolean> reportCameraAlert(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) String data) throws Exception {

        log.info("摄像头报警事件：{}", data);

        // 1. 校验 data 参数
        if (ObjectUtil.isEmpty(data)) {
            return success(false);
        }

        // 2. 上传抓拍到文件存储，获取图片 URL
        // String alertImageUrl = null;
        // if (file != null && !file.isEmpty()) {
        //     alertImageUrl = fileApi.createFile(file.getOriginalFilename(), null, file.getBytes());
        //     log.info("摄像头报警图片上传成功：{}", alertImageUrl);
        // }

        // 3. 解析事件数据
        CameraAlertReportReqVO reqVO = JsonUtils.parseObject(data, CameraAlertReportReqVO.class);
        if (reqVO == null) {
            log.error("摄像头报警数据解析失败：{}", data);
            return success(false);
        }

        // 4. 写入报警记录
        reqVO.setEventDate(reqVO.getEventDate() != null ? reqVO.getEventDate() : System.currentTimeMillis());
        reqVO.setFile(file);
        
        deviceAlertConfigService.reportCameraAlert(reqVO);

        return success(true);
    }

    /**
     * 接收摄像头报警信息
     *
     * @return
     */
    @PostMapping(value = "/receiveCameraAlarm")
    @PermitAll
    public CommonResult<Boolean> receiveCameraAlarm(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @ModelAttribute CameraAlarmInfoVo cameraAlarmInfoVo) {

        CameraAlertReportReqVO cameraAlertReportReqVO = new CameraAlertReportReqVO();

        cameraAlertReportReqVO.setDevSno(cameraAlarmInfoVo.getCameraNum());

        if (cameraAlarmInfoVo.getFrameCaptureTime() != null) {
            cameraAlertReportReqVO.setEventDate(cameraAlarmInfoVo.getFrameCaptureTime().getTime());
        }

        cameraAlertReportReqVO.setEventType(ObjectUtils.isEmpty(cameraAlarmInfoVo.getEventType()) ? 4 : cameraAlarmInfoVo.getEventType());

        cameraAlertReportReqVO.setFile(file);

        deviceAlertConfigService.reportCameraAlert(cameraAlertReportReqVO);

        return success(true);
    }

    private String getImgUrl(String frame) {
        String imgUrl;
        if (!ObjectUtils.isEmpty(frame) && frame.contains("https://")) {
            imgUrl = frame;
        } else {
            byte[] decode = Base64.decode(frame);
            imgUrl = fileApi.createFile(String.valueOf(new Date().getTime()), null, frame.getBytes());
            ;
        }
        return imgUrl;
    }
}
