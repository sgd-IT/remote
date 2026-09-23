package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 物业单位创建 Request VO")
@Data
public class FirePropertyCreateReqVO {

    @Schema(description = "物业名称", required = true)
    @NotBlank(message = "物业名称不能为空")
    private String name;

    @Schema(description = "物业编码")
    private String code;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "风险等级: critical-重大, major-较大, normal-一般")
    private String riskLevel;

    @Schema(description = "物业类型")
    private String propertyType;

    @Schema(description = "统一社会信用代码")
    private String creditCode;

    @Schema(description = "客户类型")
    private String customerType;

    @Schema(description = "楼层数")
    private Integer floorCount;

    @Schema(description = "建筑面积(平方米)")
    private BigDecimal buildingArea;

    @Schema(description = "主要负责人")
    private String mainPerson;

    @Schema(description = "安全管理人")
    private String safetyPerson;

    @Schema(description = "建筑层数")
    private String buildingFloors;

    @Schema(description = "状态")
    private Integer status;

    // ========== 许可证信息 ==========

    @Schema(description = "许可证编号")
    private String licenseNo;

    @Schema(description = "发证机关")
    private String licenseAuthority;

    @Schema(description = "许可证有效期开始")
    private LocalDate licenseStartDate;

    @Schema(description = "许可证有效期截止")
    private LocalDate licenseEndDate;

    @Schema(description = "许可范围")
    private String licenseScope;

    // ========== 仓储信息 ==========

    @Schema(description = "仓储类型")
    private String storageType;

    @Schema(description = "仓储面积(平方米)")
    private String storageArea;

    @Schema(description = "仓储容量")
    private String storageCapacity;

    @Schema(description = "存储物品类型")
    private String storageGoodsType;

    // ========== 其他 ==========

    @Schema(description = "备注")
    private String remark;

}
