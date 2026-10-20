package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 空间管理创建 Request DTO
 *
 * @author sgd
 */
@Schema(description = "管理后台 - 空间管理创建 Request DTO")
@Data
public class FireSpaceDTO {

    @Schema(description = "楼栋（几号楼，如：1号楼/A栋）", required = true)
    @NotBlank(message = "楼栋不能为空")
    private String building;

    @Schema(description = "楼层（几层，如：3层/-1层/3F）", required = true)
    @NotBlank(message = "楼层不能为空")
    private String floor;

    @Schema(description = "区域（如：东区/厨房/配电房），可不填")
    private String area;

    @Schema(description = "详细位置（区域内具体点位，如：东侧走廊北端/301室），可不填")
    private String position;

    @Schema(description = "备注（夹层、别名、期数等特例说明）")
    private String remark;
}
