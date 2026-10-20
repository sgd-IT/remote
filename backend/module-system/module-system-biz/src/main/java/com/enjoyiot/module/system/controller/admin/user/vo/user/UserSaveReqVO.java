
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
import com.enjoyiot.framework.common.validation.Mobile;
import com.enjoyiot.module.system.framework.operatelog.core.DeptParseFunction;
import com.enjoyiot.module.system.framework.operatelog.core.PostParseFunction;
import com.enjoyiot.module.system.framework.operatelog.core.SexParseFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.*;
import java.util.Set;

@Schema(description = "管理后台 - 用户创建/修改 Request VO")
@Data
public class UserSaveReqVO {

    @Schema(description = "用户编号", example = "1024")
    private Long id;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "enjoy")
    @NotBlank(message = "用户账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "用户账号由 数字、字母 组成")
    @Size(min = 4, max = 30, message = "用户账号长度为 4-30 个字符")
    @DiffLogField(name = "用户账号")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "ENJOY")
    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    @DiffLogField(name = "用户昵称")
    private String nickname;

    @Schema(description = "登录平台：1=PC 端 2=移动端 3=双端（位掩码）", example = "1")
    @DiffLogField(name = "登录平台")
    private Integer userType;

    @Schema(description = "备注", example = "我是一个用户")
    @DiffLogField(name = "备注")
    private String remark;

    @Schema(description = "部门编号", example = "我是一个用户")
    @DiffLogField(name = "部门", function = DeptParseFunction.NAME)
    private Long deptId;

    @Schema(description = "岗位编号数组", example = "1")
    @DiffLogField(name = "岗位", function = PostParseFunction.NAME)
    private Set<Long> postIds;

    @Schema(description = "用户邮箱", example = "admin@enjoy-iot.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    @DiffLogField(name = "用户邮箱")
    private String email;

    @Schema(description = "手机号码", example = "15601691300")
    @Mobile
    @DiffLogField(name = "手机号码")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    @DiffLogField(name = "用户性别", function = SexParseFunction.NAME)
    private Integer sex;

    @Schema(description = "用户头像", example = "http://www.enjoy-iot.cn/xxx.png")
    @DiffLogField(name = "用户头像")
    private String avatar;

    @Schema(description = "区域编码（数字编码路径，用/分隔），仅监管角色用户选填", example = "330000/330100/330106")
    @Size(max = 50, message = "区域编码长度不能超过 50 个字符")
    @DiffLogField(name = "区域编码")
    private String areaCode;

    @Schema(description = "是否允许消息推送（默认关）", example = "false")
    private Boolean pushEnabled;

    @Schema(description = "归属租户编号。平台租户管理员创建账号时指定；为空则归属当前租户", example = "166")
    private Long tenantId;

    // ========== 仅【创建】时，需要传递的字段 ==========

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Abc123!@#")
    @Length(min = 8, max = 16, message = "密码长度为 8-16 位")
    private String password;

    @AssertTrue(message = "密码不能为空")
    @JsonIgnore
    public boolean isPasswordValid() {
        return id != null // 修改时，不需要传递
                || (ObjectUtil.isAllNotEmpty(password)); // 新增时，必须都传递 password
    }

    @AssertTrue(message = "密码必须包含大小写字母、数字、特殊符号中至少三类")
    @JsonIgnore
    public boolean isPasswordComplexityValid() {
        if (ObjectUtil.isEmpty(password)) {
            return true; // 修改时不传密码，跳过复杂度校验；新增时的非空校验由 isPasswordValid 保证
        }
        int categoryCount = 0;
        if (password.matches(".*[a-z].*")) categoryCount++; // 小写字母
        if (password.matches(".*[A-Z].*")) categoryCount++; // 大写字母
        if (password.matches(".*\\d.*")) categoryCount++; // 数字
        if (password.matches(".*[^a-zA-Z0-9].*")) categoryCount++; // 特殊符号
        return categoryCount >= 3;
    }

}
