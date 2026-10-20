package com.enjoyiot.module.eiot.service.device.instruction;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendRespVO;

/**
 * 指令下发策略接口
 *
 * 按厂商 invoke_type 路由:
 * - REMOTE_API: 调远程API换取指令码(四信类)
 * - LOCAL_COMMAND: 本地命令码映射(YWN类, 纯配置零代码接入)
 */
public interface InstructionSender {

    /** 对应 eiot_protocol_vendor.invoke_type */
    String getInvokeType();

    /** 执行指令下发 */
    InstructionSendRespVO send(InstructionContext context);

    default boolean supportsVendor(String vendorCode) {
        return false;
    }

    default boolean isVendorSpecific() {
        return false;
    }

    default InstructionSendRespVO buildResp(boolean success, String message) {
        InstructionSendRespVO resp = new InstructionSendRespVO();
        resp.setSuccess(success);
        resp.setMessage(message);
        return resp;
    }
}
