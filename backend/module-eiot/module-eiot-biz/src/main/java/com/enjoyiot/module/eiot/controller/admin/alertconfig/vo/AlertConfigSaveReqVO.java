
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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "管理后台 - 报警配置新增/修改 Request VO")
@Data
public class AlertConfigSaveReqVO {

    @Schema(description = "告警配置id", requiredMode = Schema.RequiredMode.REQUIRED, example = "7010")
    private Long id;

    @Schema(description = "告警名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @NotEmpty(message = "告警名称不能为空")
    private String name;

    @Schema(description = "关联消息转发模板ID", example = "9137")
    @NotNull(message = "关联消息转发模板ID不能为空")
    private Long messageTemplateId;

    @Schema(description = "规则引擎id（未选择算法报警类型时必填）", example = "14376")
    private Long ruleInfoId;

    @Schema(description = "算法报警类型：1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警（可选，未选择时为通用规则配置）", example = "3")
    private Integer algorithmAlertType;

    @Schema(description = "告警等级")
    private String level;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "状态(0启动 1禁用)", example = "1")
    private Integer status;

    @Schema(description = "消息推送岗位ID数组（最多3个，未选择时按值班人员推送）", example = "[1,2,3]")
    @Size(max = 3, message = "消息推送岗位最多支持选择3个")
    private List<Long> pushPostIds;

}
