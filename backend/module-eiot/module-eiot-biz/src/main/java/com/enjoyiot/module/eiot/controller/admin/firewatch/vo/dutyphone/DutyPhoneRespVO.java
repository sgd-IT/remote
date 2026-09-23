package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 值班中心座机号码 Response VO")
@Data
public class DutyPhoneRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "归属租户ID（值班中心所属租户）")
    private Long tenantId;

    @Schema(description = "座机电话号码")
    private String phoneNumber;

    @Schema(description = "座机名称")
    private String name;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
