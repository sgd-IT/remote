package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "管理后台 - 隐患创建 Request VO")
@Data
public class FireHazardCreateReqVO {

    @Schema(description = "所属物业ID", required = true)
    @NotNull(message = "所属物业ID不能为空")
    private Long propertyId;

    @Schema(description = "隐患等级: general-一般隐患, major-较大隐患, critical-重大隐患", required = true)
    @NotBlank(message = "隐患等级不能为空")
    private String hazardLevel;

    @Schema(description = "隐患描述", required = true)
    @NotBlank(message = "隐患描述不能为空")
    private String hazardDesc;

    @Schema(description = "隐患位置")
    private String location;

    @Schema(description = "关联设备ID")
    private Long deviceId;

    @Schema(description = "发现人", required = true)
    @NotBlank(message = "发现人不能为空")
    private String foundUserName;

    @Schema(description = "发现日期", required = true)
    @NotNull(message = "发现日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate foundDate;

    @Schema(description = "整改截止日期", required = true)
    @NotNull(message = "整改截止日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    @Schema(description = "整改建议")
    private String suggestion;

    @Schema(description = "现场照片(JSON数组)")
    private String photos;

    @Schema(description = "备注")
    private String remark;

}
