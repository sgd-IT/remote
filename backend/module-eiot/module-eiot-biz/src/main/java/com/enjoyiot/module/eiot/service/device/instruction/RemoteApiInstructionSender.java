package com.enjoyiot.module.eiot.service.device.instruction;

import cn.hutool.json.JSONUtil;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendReqVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionParamDO;
import com.enjoyiot.module.eiot.dal.dataobject.protocol.ProtocolVendorDO;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandModelFunctionParamMapper;
import com.enjoyiot.module.eiot.service.device.DeviceCtrlService;
import com.enjoyiot.module.eiot.service.remote.FourfaithRemoteControlClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REMOTE_API 策略: 调用四信远程API换取指令码后下发
 */
@Component
@Slf4j
public class RemoteApiInstructionSender implements InstructionSender {

    @Resource
    private FourfaithRemoteControlClient fourfaithClient;
    @Resource
    private BrandModelFunctionParamMapper brandModelFunctionParamMapper;
    @Resource
    private DeviceCtrlService deviceCtrlService;

    @Override
    public String getInvokeType() {
        return ProtocolVendorDO.INVOKE_TYPE_REMOTE_API;
    }

    @Override
    public InstructionSendRespVO send(InstructionContext context) {
        InstructionSendReqVO reqVO = context.getReqVO();

        // 解析参数值：根据功能定义的参数动态取值
        Map<String, Object> paramMap = JSONUtil.toBean(context.getDeviceFunction().getParamValues(), Map.class);
        List<BrandModelFunctionParamDO> paramDefs =
                brandModelFunctionParamMapper.selectByFunctionId(context.getFunctionDef().getId());
        Map<String, String> functionValueMap = new LinkedHashMap<>();
        for (BrandModelFunctionParamDO paramDef : paramDefs) {
            Object value = paramMap.get(paramDef.getParamCode());
            if (value == null) {
                // 校验参数， 如果参数没有值，直接返回错误
                return buildResp(false, "参数 " + paramDef.getParamName() + " 值不能为空");
            }
            functionValueMap.put(paramDef.getParamCode(), value.toString());
        }

        // 根据功能名称映射功能类型
        String functionType = mapFunctionType(context.getFunctionDef().getName());
        String commandType = reqVO.getCommandType();

        // 调用四信API生成指令码
        FourfaithRemoteControlClient.CommandResult result = fourfaithClient.createCommand(
                context.getFunctionDef().getFunctionId(), functionType, commandType, functionValueMap,
                context.getBrand().getBrandCode(), context.getBrandModel().getModel(),
                context.getBrandModel().getVersion()
        );

        if (!result.isSuccess()) {
            log.error("指令生成失败: {}", result.getErrorMsg());
            return buildResp(false, "指令生成失败: " + result.getErrorMsg());
        }

        log.info("指令生成成功: deviceId={}, functionId={}, commandCode={}",
                reqVO.getDeviceId(), context.getFunctionDef().getFunctionId(), result.getCommandCode());

        // 通过组件下发通道发送指令
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("command_code", result.getCommandCode());
        String serviceName = resolveServiceName(context.getVendor());
        deviceCtrlService.invokeService(reqVO.getDeviceId(), serviceName, args);

        return buildResp(true, "指令已下发");
    }

    private String resolveServiceName(ProtocolVendorDO vendor) {
        if (vendor != null && vendor.getServiceName() != null && !vendor.getServiceName().isEmpty()) {
            return vendor.getServiceName();
        }
        return "remote_control";
    }

    /**
     * 根据功能名称映射四信功能类型
     * 10:复位, 20:消音, 30:开关, 40:总线开关, 41:多线开关
     */
    private String mapFunctionType(String functionName) {
        if (functionName == null) return null;
        if (functionName.contains("复位")) return "10";
        if (functionName.contains("消音")) return "20";
        if (functionName.contains("总线开关")) return "40";
        if (functionName.contains("多线开关") || functionName.contains("多线")) return "41";
        if (functionName.contains("开关")) return "30";
        return null;
    }
}
