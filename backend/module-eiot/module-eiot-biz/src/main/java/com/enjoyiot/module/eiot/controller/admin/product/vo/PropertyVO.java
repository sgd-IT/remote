package com.enjoyiot.module.eiot.controller.admin.product.vo;


import com.enjoyiot.module.eiot.api.thingmodel.dto.ThingModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "OpenPropertyVo")
@Data
public class PropertyVO {
    private String identifier;
    private ThingModel.DataType dataType;
    private String name;
    private String accessMode = "rw";

    // 描述
    private String description;

    // 单位
    private String unit;

    private String time;

    private String value;

    public PropertyVO() {
    }

    public PropertyVO(String identifier, ThingModel.DataType dataType, String name, String accessMode, String description, String unit) {
        this.identifier = identifier;
        this.dataType = dataType;
        this.name = name;
        this.accessMode = accessMode;
        this.description = description;
        this.unit = unit;
    }
}
