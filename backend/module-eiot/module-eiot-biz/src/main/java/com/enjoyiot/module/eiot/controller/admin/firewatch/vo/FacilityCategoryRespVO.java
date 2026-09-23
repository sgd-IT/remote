package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 消防设施分类 Response VO
 *
 * @author EnjoyIot
 */
@Schema(description = "管理后台 - 消防设施分类 Response VO")
@Data
public class FacilityCategoryRespVO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类编码")
    private String code;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "父级编码")
    private String parentCode;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "描述")
    private String description;

}
