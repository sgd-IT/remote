package com.enjoyiot.module.eiot.dal.dataobject.protocol;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 私有协议厂商配置 DO
 *
 * 远程控制多协议兼容: 品牌通过 vendor_code 关联厂商,
 * 厂商的 invoke_type 决定指令下发逻辑(策略路由)
 */
@TableName("eiot_protocol_vendor")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProtocolVendorDO extends BaseDO {

    /** 调用逻辑: 远程API编码(四信类) */
    public static final String INVOKE_TYPE_REMOTE_API = "REMOTE_API";
    /** 调用逻辑: 本地命令码映射(YWN类) */
    public static final String INVOKE_TYPE_LOCAL_COMMAND = "LOCAL_COMMAND";

    @TableId
    private Long id;

    /** 厂商编码: FOURFAITH/YWN/... */
    private String vendorCode;

    /** 厂商名称 */
    private String vendorName;

    /** 调用逻辑: REMOTE_API/LOCAL_COMMAND */
    private String invokeType;

    /** 统一下发服务名(默认 remote_control) */
    private String serviceName;

    /** 厂商级配置JSON(如外部API地址等) */
    private String config;

    private String remark;
}
