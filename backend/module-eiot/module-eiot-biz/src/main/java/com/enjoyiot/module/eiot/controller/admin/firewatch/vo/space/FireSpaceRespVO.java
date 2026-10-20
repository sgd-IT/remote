package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 空间管理 Response VO
 * <p>
 * 带租户名称（同名空间跨租户靠租户名区分）。
 *
 * @author sgd
 */
@Schema(description = "管理后台 - 空间管理 Response VO")
@Data
public class FireSpaceRespVO {

    @Schema(description = "空间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "租户编号", example = "166")
    private Long tenantId;

    @Schema(description = "租户名称（园区名称）", example = "广东安为安全科技有限公司")
    private String tenantName;

    @Schema(description = "楼栋（几号楼，如：1号楼/A栋）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1号楼")
    private String building;

    @Schema(description = "楼层（几层，如：3层/-1层/3F）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3层")
    private String floor;

    @Schema(description = "区域（如：东区/厨房/配电房）", example = "东区")
    private String area;

    @Schema(description = "详细位置（区域内具体点位，如：东侧走廊北端/301室）", example = "东侧走廊北端")
    private String position;

    @Schema(description = "备注", example = "备注")
    private String remark;
}
