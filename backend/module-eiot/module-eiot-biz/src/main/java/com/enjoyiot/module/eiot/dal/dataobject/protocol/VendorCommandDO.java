package com.enjoyiot.module.eiot.dal.dataobject.protocol;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 厂商指令字典 DO (LOCAL_COMMAND 类厂商)
 *
 * 定义厂商支持的指令及其命令码/参数模板,
 * eiot_brand_model_function.vendor_command_key 关联本表 command_key
 */
@TableName("eiot_vendor_command")
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorCommandDO extends BaseDO {

    @TableId
    private Long id;

    /** 厂商编码 */
    private String vendorCode;

    /** 指令标识: reset/bus_switch/multi_line_switch... */
    private String commandKey;

    /** 指令名称: 复位/总线开关/多线开关 */
    private String commandName;

    /** 按钮类型: GENERAL/BUS/MULTI */
    private String functionType;

    /** 启动/执行命令码 */
    private Integer codeStart;

    /** 停止命令码(无启停语义则为NULL) */
    private Integer codeStop;

    /**
     * 参数模板JSON:
     * {"params":[{"code":"loopNo","name":"回路号","slot":"param1","default":0}]}
     * slot 取值: machineNo/param1/param2
     */
    private String paramSchema;
}
