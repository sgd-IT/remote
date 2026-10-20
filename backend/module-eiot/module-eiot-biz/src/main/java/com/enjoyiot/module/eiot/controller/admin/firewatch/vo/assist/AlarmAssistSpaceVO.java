package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 告警辅助判断 - 安装空间 VO
 *
 * @author sgd
 */
@Schema(description = "管理后台 - 告警辅助判断空间 VO")
@Data
public class AlarmAssistSpaceVO {

    @Schema(description = "空间ID", example = "4")
    private Long id;

    @Schema(description = "楼栋", example = "1号楼")
    private String building;

    @Schema(description = "楼层", example = "3层")
    private String floor;

    @Schema(description = "区域", example = "东区")
    private String area;

    @Schema(description = "详细位置", example = "东侧走廊北端")
    private String position;

    @Schema(description = "空间完整路径", example = "1号楼 / 3层 / 东区 / 东侧走廊北端")
    private String path;
}
