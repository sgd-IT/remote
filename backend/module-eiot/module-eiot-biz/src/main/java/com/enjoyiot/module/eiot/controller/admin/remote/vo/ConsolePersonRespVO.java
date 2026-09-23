package com.enjoyiot.module.eiot.controller.admin.remote.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 远程控制台联系人 Response VO")
@Data
public class ConsolePersonRespVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "用户电话")
    private String userPhone;

    @Schema(description = "展示岗位（展示岗位名或值班岗位名）")
    private String position;

    @Schema(description = "来源：post-展示岗位人员；duty-当前值班人员")
    private String source;

}
