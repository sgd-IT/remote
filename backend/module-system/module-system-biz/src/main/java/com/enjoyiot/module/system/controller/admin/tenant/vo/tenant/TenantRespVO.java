
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

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.enjoyiot.framework.excel.core.annotations.DictFormat;
import com.enjoyiot.framework.excel.core.convert.DictConvert;
import com.enjoyiot.module.system.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 租户 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TenantRespVO {

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("租户编号")
    private Long id;

    @Schema(description = "租户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "test")
    @ExcelProperty("租户名")
    private String name;

    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED, example = "ENJOY")
    @ExcelProperty("联系人")
    private String contactName;

    @Schema(description = "联系手机", example = "15601691300")
    @ExcelProperty("联系手机")
    private String contactMobile;

    @Schema(description = "租户状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "绑定域名", example = "http://www.enjoy-iot.cn")
    private String website;

    @Schema(description = "租户套餐编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long packageId;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expireTime;

    @Schema(description = "账号数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
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

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
