package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "远程控制操作日志分页请求 VO")
public class RemoteOperationLogPageReqVO extends PageParam {

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "企业/物业ID")
    private Long propertyId;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "命令状态")
    private Integer commandStatus;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
