package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Schema(description = "管理后台 - 隐患分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FireHazardPageReqVO extends PageParam {

    @Schema(description = "所属物业ID")
    private Long propertyId;

    @Schema(description = "隐患等级")
    private String hazardLevel;

    @Schema(description = "隐患状态")
    private String status;

    @Schema(description = "发现日期-开始")
    private LocalDate foundDateStart;

    @Schema(description = "发现日期-结束")
    private LocalDate foundDateEnd;

    @Schema(description = "关键字(隐患描述/设备名称)")
    private String keyword;

}
