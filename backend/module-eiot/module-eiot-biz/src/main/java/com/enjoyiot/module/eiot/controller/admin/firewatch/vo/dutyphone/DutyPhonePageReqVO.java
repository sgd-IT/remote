package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 值守中心座机号码分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DutyPhonePageReqVO extends PageParam {

    @Schema(description = "座机号码（模糊）")
    private String phoneNumber;

    @Schema(description = "座机名称（模糊）")
    private String name;
}
