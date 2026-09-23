/*  
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
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
package com.enjoyiot.module.system.controller.admin.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - SSO ticket 创建 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoTicketCreateReqVO {

    @Schema(description = "第三方应用标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "fire-park-app")
    @NotEmpty(message = "应用标识不能为空")
    private String appKey;

    @Schema(description = "应用密钥", requiredMode = Schema.RequiredMode.REQUIRED, example = "demo-secret")
    @NotEmpty(message = "应用密钥不能为空")
    private String secret;

}