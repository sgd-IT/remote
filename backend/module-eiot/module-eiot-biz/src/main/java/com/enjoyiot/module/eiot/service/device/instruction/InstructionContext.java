package com.enjoyiot.module.eiot.service.device.instruction;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandDO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelDO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import com.enjoyiot.module.eiot.dal.dataobject.protocol.ProtocolVendorDO;
import lombok.Builder;
import lombok.Data;

/**
 * 指令下发上下文: 汇集下发所需的全部信息, 供各策略实现使用
 */
@Data
@Builder
public class InstructionContext {

    /** 前端请求 */
    private InstructionSendReqVO reqVO;

    /** 设备功能实例(含 paramValues) */
    private DeviceFunctionDO deviceFunction;

    /** 品牌型号功能定义 */
    private BrandModelFunctionDO functionDef;

    /** 品牌 */
    private BrandDO brand;

    /** 品牌型号 */
    private BrandModelDO brandModel;

    /** 协议厂商配置(决定下发策略) */
    private ProtocolVendorDO vendor;
}
