package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 物业单位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FirePropertyPageReqVO extends PageParam {

    @Schema(description = "物业名称")
    private String name;

    /**
     * 兼容前端参数名 propertyName → 映射到 name
     */
    public void setPropertyName(String propertyName) {
        this.name = propertyName;
    }

    @Schema(description = "物业编码")
    private String code;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "风险等级")
    private String riskLevel;

    @Schema(description = "物业类型")
    private String propertyType;

    @Schema(description = "状态")
    private Integer status;

}
