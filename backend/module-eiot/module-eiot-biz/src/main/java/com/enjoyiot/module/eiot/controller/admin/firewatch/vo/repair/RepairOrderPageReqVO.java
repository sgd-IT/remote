package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 维修工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RepairOrderPageReqVO extends PageParam {

    @Schema(description = "工单编号")
    private String orderNo;

    @Schema(description = "所属租户ID")
    private Long tenantId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "故障类型")
    private String faultType;

    @Schema(description = "工单状态")
    private String orderStatus;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "创建时间-开始")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间-结束")
    private LocalDateTime createTimeEnd;

    @Schema(description = "关键字搜索")
    private String keyword;

    @Schema(description = "报修来源")
    private String reportSource;

    @Schema(description = "维保人")
    private String repairPerson;

    @Schema(description = "所属企业")
    private String propertyName;
}
