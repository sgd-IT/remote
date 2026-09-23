package com.enjoyiot.module.eiot.controller.app.cameraalert.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @Description:
 * @Auther: yanglu
 * @Date: 2025/06/09 16:44
 */
@Data
public class CameraAlarmInfoVo {

    private Long cameraId;

    private String cameraNum;

    private Integer alarm;

    private Integer peopleCount;

    private Integer eventType;
    
    private String frame;
            
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date frameCaptureTime;
}

