package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 值守中心座机号码更新 Request VO")
@Data
public class DutyPhoneUpdateReqVO {

    @Schema(description = "主键ID", required = true)
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "座机电话号码", required = true)
    @NotBlank(message = "座机号码不能为空")
    private String phoneNumber;

    @Schema(description = "座机名称（如：XX值班中心前台）")
    private String name;

    @Schema(description = "备注")
    private String remark;
}
