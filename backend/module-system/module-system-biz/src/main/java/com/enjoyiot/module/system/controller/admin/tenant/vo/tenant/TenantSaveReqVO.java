
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
package com.enjoyiot.module.system.controller.admin.tenant.vo.tenant;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 租户创建/修改 Request VO")
@Data
public class TenantSaveReqVO {

    @Schema(description = "租户编号", example = "1024")
    private Long id;

    @Schema(description = "租户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "test")
    @NotNull(message = "租户名不能为空")
    private String name;

    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED, example = "ENJOY")
    @NotNull(message = "联系人不能为空")
    private String contactName;

    @Schema(description = "联系手机", example = "15601691300")
    private String contactMobile;

    @Schema(description = "租户状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "租户状态")
    private Integer status;

    @Schema(description = "绑定域名", example = "http://www.enjoy-iot.cn")
    private String website;

    @Schema(description = "租户套餐编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "租户套餐编号不能为空")
    private Long packageId;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "过期时间不能为空")
    private LocalDateTime expireTime;

    @Schema(description = "账号数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "账号数量不能为空")
    private Integer accountCount;

    @Schema(description = "客户类型", example = "factory")
    private String customerType;

    @Schema(description = "详细地址", example = "广东省深圳市南山区科技园路1号")
    private String address;

    @Schema(description = "省份", example = "广东省")
    private String province;

    @Schema(description = "省份编码", example = "440000")
    private Integer provinceCode;

    @Schema(description = "城市", example = "深圳市")
    private String city;

    @Schema(description = "城市编码", example = "440300")
    private Integer cityCode;

    @Schema(description = "区县", example = "南山区")
    private String district;

    @Schema(description = "区县编码", example = "440305")
    private Integer districtCode;

    @Schema(description = "经度", example = "113.9348134")
    private java.math.BigDecimal longitude;

    @Schema(description = "纬度", example = "22.5304253")
    private java.math.BigDecimal latitude;

    @Schema(description = "风险等级", example = "normal")
    private String riskLevel;

    @Schema(description = "统一社会信用代码", example = "91440300MA5EXAMPLE")
    private String creditCode;

    // ========== 仅【创建】时，需要传递的字段 ==========

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "enjoy")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "用户账号由 数字、字母 组成")
    @Size(min = 4, max = 30, message = "用户账号长度为 4-30 个字符")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Abc123!@#")
    @Length(min = 8, max = 16, message = "密码长度为 8-16 位")
    private String password;

    @AssertTrue(message = "用户账号、密码不能为空")
    @JsonIgnore
    public boolean isUsernameValid() {
        return id != null // 修改时，不需要传递
                || (ObjectUtil.isAllNotEmpty(username, password)); // 新增时，必须都传递 username、password
    }

    @AssertTrue(message = "密码必须包含大小写字母、数字、特殊符号中至少三类")
    @JsonIgnore
    public boolean isPasswordComplexityValid() {
        if (ObjectUtil.isEmpty(password)) {
            return true; // 修改时不传密码，跳过复杂度校验；新增时的非空校验由 isUsernameValid 保证
        }
        int categoryCount = 0;
        if (password.matches(".*[a-z].*")) categoryCount++; // 小写字母
        if (password.matches(".*[A-Z].*")) categoryCount++; // 大写字母
        if (password.matches(".*\\d.*")) categoryCount++; // 数字
        if (password.matches(".*[^a-zA-Z0-9].*")) categoryCount++; // 特殊符号
        return categoryCount >= 3;
    }

}
