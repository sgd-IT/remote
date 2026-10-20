package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 空间管理更新 Request DTO
 *
 * @author sgd
 */
@Schema(description = "管理后台 - 空间管理更新 Request DTO")
@Data
public class FireSpaceUpdateDTO {

    @Schema(description = "空间ID", required = true, example = "1")
    @NotNull(message = "空间ID不能为空")
    private Long id;

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
