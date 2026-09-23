
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
package com.enjoyiot.module.system.controller.admin.user.vo.user;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - 用户更新密码 Request VO")
@Data
public class UserUpdatePasswordReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long id;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Abc123!@#")
    @NotEmpty(message = "密码不能为空")
    @Length(min = 8, max = 16, message = "密码长度为 8-16 位")
    private String password;

    @AssertTrue(message = "密码必须包含大小写字母、数字、特殊符号中至少三类")
    @JsonIgnore
    public boolean isPasswordComplexityValid() {
        if (ObjectUtil.isEmpty(password)) {
            return true; // 非空校验由 @NotEmpty 保证
        }
        int categoryCount = 0;
        if (password.matches(".*[a-z].*")) categoryCount++;
        if (password.matches(".*[A-Z].*")) categoryCount++;
        if (password.matches(".*\\d.*")) categoryCount++;
        if (password.matches(".*[^a-zA-Z0-9].*")) categoryCount++;
        return categoryCount >= 3;
    }

}
