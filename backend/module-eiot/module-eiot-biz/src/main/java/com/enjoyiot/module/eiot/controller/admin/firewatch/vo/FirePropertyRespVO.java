package com.enjoyiot.module.eiot.controller.admin.firewatch.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 物业单位 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FirePropertyRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "物业名称")
    @ExcelProperty("物业名称")
    private String name;

    @Schema(description = "物业名称(兼容)")
    private String propertyName;

    @Schema(description = "物业编码")
    @ExcelProperty("物业编码")
    private String code;

    @Schema(description = "详细地址")
    @ExcelProperty("详细地址")
    private String address;

    @Schema(description = "省份")
    @ExcelProperty("省份")
    private String province;

    @Schema(description = "城市")
    @ExcelProperty("城市")
    private String city;

    @Schema(description = "区县")
    @ExcelProperty("区县")
    private String district;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "联系人")
    @ExcelProperty("联系人")
    private String contactName;

    @Schema(description = "联系电话")
    @ExcelProperty("联系电话")
    private String contactPhone;

    @Schema(description = "风险等级")
    @ExcelProperty("风险等级")
    private String riskLevel;

    @Schema(description = "风险等级名称")
    private String riskLevelName;

    @Schema(description = "物业类型")
    @ExcelProperty("物业类型")
    private String propertyType;

    @Schema(description = "统一社会信用代码")
    @ExcelProperty("统一社会信用代码")
    private String creditCode;

    @Schema(description = "客户类型")
    @ExcelProperty("客户类型")
    private String customerType;

    @Schema(description = "楼层数")
    @ExcelProperty("楼层数")
    private Integer floorCount;

    @Schema(description = "建筑面积(平方米)")
    @ExcelProperty("建筑面积")
    private BigDecimal buildingArea;

    @Schema(description = "主要负责人")
    @ExcelProperty("主要负责人")
    private String mainPerson;

    @Schema(description = "安全管理人")
    @ExcelProperty("安全管理人")
    private String safetyPerson;

    @Schema(description = "建筑层数")
    @ExcelProperty("建筑层数")
    private String buildingFloors;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "设备数量")
    private Integer deviceCount;

    // ========== 许可证信息 ==========

    @Schema(description = "许可证编号")
    @ExcelProperty("许可证编号")
    private String licenseNo;

    @Schema(description = "发证机关")
    @ExcelProperty("发证机关")
    private String licenseAuthority;

    @Schema(description = "许可证有效期开始")
    @ExcelProperty("许可证有效期开始")
    private LocalDate licenseStartDate;

    @Schema(description = "许可证有效期截止")
    @ExcelProperty("许可证有效期截止")
    private LocalDate licenseEndDate;

    @Schema(description = "许可范围")
    @ExcelProperty("许可范围")
    private String licenseScope;

    // ========== 仓储信息 ==========

    @Schema(description = "仓储类型")
    @ExcelProperty("仓储类型")
    private String storageType;

    @Schema(description = "仓储面积(平方米)")
    @ExcelProperty("仓储面积")
    private String storageArea;

    @Schema(description = "仓储容量")
    @ExcelProperty("仓储容量")
    private String storageCapacity;

    @Schema(description = "存储物品类型")
    @ExcelProperty("存储物品类型")
    private String storageGoodsType;

    // ========== 其他 ==========

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
