
/*
 *
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
 *  * +----------------------------------------------------------------------
 *
 *  Copyright [2025] [Enjoy-iot] | Tel: 19918996474
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package com.enjoyiot.module.eiot.controller.admin.alertconfig.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;

@Schema(description = "管理后台 - 报警配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class AlertConfigRespVO {

    @Schema(description = "告警配置id", requiredMode = Schema.RequiredMode.REQUIRED, example = "7010")
    @ExcelProperty("告警配置id")
    private Long id;

    @Schema(description = "告警名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @ExcelProperty("告警名称")
    private String name;

    @Schema(description = "关联消息转发模板ID", example = "9137")
    @ExcelProperty("关联消息转发模板ID")
    private Long messageTemplateId;

    @Schema(description = "规则引擎id（未选择算法报警类型时必填）", example = "14376")
    @ExcelProperty("规则引擎id")
    private Long ruleInfoId;

    @Schema(description = "算法报警类型：1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警", example = "3")
    private Integer algorithmAlertType;

    @Schema(description = "告警等级")
    @ExcelProperty("告警等级")
    private String level;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "状态(0启动 1禁用)", example = "1")
    @ExcelProperty("状态(0启动 1禁用)")
    private Integer status;

    @Schema(description = "消息推送岗位ID数组（最多3个）", example = "[1,2,3]")
    private List<Long> pushPostIds;

}
