package com.enjoyiot.module.eiot.controller.admin.firewatch.vo.callrecord;

import com.enjoyiot.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 话单通话记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CallRecordPageReqVO extends PageParam {

    @Schema(description = "归属租户ID（系统租户可按租户筛选）")
    private Long tenantId;

    @Schema(description = "匹配到的座机号码")
    private String matchPhone;

    @Schema(description = "主叫号码（模糊）")
    private String callerNbr;

    @Schema(description = "被叫号码（模糊）")
    private String calledNbr;

    @Schema(description = "通话方向：0=未匹配 1=呼出 2=呼入")
    private Integer callDirection;

    @Schema(description = "通话响应状态(NO ANSWER/FAILED/BUSY/ANSWERED/CONGESTION)")
    private String disposition;

    @Schema(description = "开始时间起（YYYYMMDDHH24MISS）")
    private String startTimeBegin;

    @Schema(description = "开始时间止（YYYYMMDDHH24MISS）")
    private String startTimeEnd;
}
