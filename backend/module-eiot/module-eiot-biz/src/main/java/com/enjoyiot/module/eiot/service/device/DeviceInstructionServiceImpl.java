package com.enjoyiot.module.eiot.service.device;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.enjoyiot.framework.security.core.util.SecurityFrameworkUtils;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendReqVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandDO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelDO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceRemoteConfigDO;
import com.enjoyiot.module.eiot.dal.dataobject.protocol.ProtocolVendorDO;
import com.enjoyiot.module.eiot.dal.dataobject.remote.RemoteOperationLogDO;
import com.enjoyiot.module.eiot.dal.mysql.remote.RemoteOperationLogMapper;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandMapper;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandModelFunctionMapper;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandModelMapper;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceFunctionMapper;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceRemoteConfigMapper;
import com.enjoyiot.module.eiot.dal.mysql.protocol.ProtocolVendorMapper;
import com.enjoyiot.module.eiot.service.device.instruction.InstructionContext;
import com.enjoyiot.module.eiot.service.device.instruction.InstructionSender;
import com.enjoyiot.module.eiot.service.remote.RemoteOperationLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class DeviceInstructionServiceImpl implements DeviceInstructionService {

    @Resource
    private DeviceFunctionMapper deviceFunctionMapper;
    @Resource
    private BrandModelFunctionMapper brandModelFunctionMapper;
    @Resource
    private DeviceRemoteConfigMapper deviceRemoteConfigMapper;
    @Resource
    private BrandModelMapper brandModelMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private ProtocolVendorMapper protocolVendorMapper;
    @Resource
    private List<InstructionSender> instructionSenders;

    @Resource
    private DeviceInfoService deviceInfoService;

    @Resource
    private RemoteOperationLogService remoteOperationLogService;

    @Resource
    private RemoteOperationLogMapper remoteOperationLogMapper;

    @Override
    public InstructionSendRespVO sendInstruction(InstructionSendReqVO reqVO) {
        DeviceFunctionDO deviceFunction = deviceFunctionMapper.selectById(reqVO.getDeviceFunctionId());
        if (deviceFunction == null) {
            return buildResp(false, "功能不存在");
        }

        BrandModelFunctionDO functionDef = brandModelFunctionMapper.selectById(deviceFunction.getBrandModelFunctionId());
        if (functionDef == null) {
            return buildResp(false, "功能定义不存在");
        }

        DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo(reqVO.getDeviceId());
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();

        // 创建操作日志
        RemoteOperationLogDO operationLog = new RemoteOperationLogDO();
        operationLog.setTenantId(deviceInfo != null ? deviceInfo.getTenantId() : null);
        operationLog.setDeviceId(reqVO.getDeviceId());
        operationLog.setDeviceName(deviceInfo != null ? deviceInfo.getName() : "");
        operationLog.setOperationType(mapOperationType(functionDef.getName(), reqVO.getCommandType()));
        operationLog.setOperationName(buildOperationName(functionDef.getName(), reqVO.getCommandType()));
        operationLog.setButtonName(deviceFunction.getButtonName());
        operationLog.setFunctionType(deviceFunction.getFunctionType());
        operationLog.setOperatorId(operatorId != null ? operatorId : 0L);
        operationLog.setOperatorName(operatorName != null ? operatorName : "");
        operationLog.setCommandStatus(2);
        operationLog.setRequestParams(JSONUtil.toJsonStr(reqVO));
        operationLog.setOperationTime(LocalDateTime.now());
        remoteOperationLogService.createLog(operationLog);

        InstructionSendRespVO resp = doSendInstruction(reqVO, deviceFunction, functionDef);

        operationLog.setCommandStatus(resp.getSuccess() ? 1 : 0);
        operationLog.setResultMsg(resp.getMessage());
        remoteOperationLogMapper.updateById(operationLog);

        return resp;
    }

    private InstructionSendRespVO doSendInstruction(InstructionSendReqVO reqVO, DeviceFunctionDO deviceFunction,
                                                    BrandModelFunctionDO functionDef) {
        // 二次确认密码校验（消音操作不需要）
        if (!"消音".equals(functionDef.getName())) {
            DeviceRemoteConfigDO config = deviceRemoteConfigMapper.selectByDeviceId(reqVO.getDeviceId());
            if (config != null && StrUtil.isNotBlank(config.getConfirmPassword())) {
                if (!config.getConfirmPassword().equals(reqVO.getPassword())) {
                    return buildResp(false, "二次确认密码错误");
                }
            }
        }

        // 获取品牌型号版本信息
        DeviceRemoteConfigDO config = deviceRemoteConfigMapper.selectByDeviceId(reqVO.getDeviceId());
        if (config == null || config.getBrandModelId() == null) {
            return buildResp(false, "设备未配置远程控制");
        }

        BrandModelDO brandModel = brandModelMapper.selectById(config.getBrandModelId());
        if (brandModel == null) {
            return buildResp(false, "品牌型号配置不存在");
        }

        BrandDO brand = brandMapper.selectById(brandModel.getBrandId());
        if (brand == null) {
            return buildResp(false, "品牌信息不存在");
        }

        // 解析协议厂商(向后兼容: 未配置时默认四信)
        ProtocolVendorDO vendor = resolveVendor(brand);

        InstructionContext context = InstructionContext.builder()
                .reqVO(reqVO)
                .deviceFunction(deviceFunction)
                .functionDef(functionDef)
                .brand(brand)
                .brandModel(brandModel)
                .vendor(vendor)
                .build();

        InstructionSender sender = instructionSenders.stream()
                .filter(item -> item.supportsVendor(vendor.getVendorCode()))
                .findFirst()
                .orElseGet(() -> instructionSenders.stream()
                        .filter(item -> !item.isVendorSpecific())
                        .filter(item -> vendor.getInvokeType().equals(item.getInvokeType()))
                        .findFirst()
                        .orElse(null));
        if (sender == null) {
            return buildResp(false, "不支持的厂商调用类型: " + vendor.getInvokeType());
        }
        return sender.send(context);
    }

    /**
     * 解析品牌关联的协议厂商; 品牌未配置 vendorCode 或厂商表无记录时,
     * 默认按四信(REMOTE_API)处理, 保证存量数据向后兼容
     */
    private ProtocolVendorDO resolveVendor(BrandDO brand) {
        String vendorCode = StrUtil.isNotBlank(brand.getVendorCode()) ? brand.getVendorCode() : "FOURFAITH";
        ProtocolVendorDO vendor = protocolVendorMapper.selectByCode(vendorCode);
        if (vendor == null) {
            vendor = new ProtocolVendorDO();
            vendor.setVendorCode(vendorCode);
            vendor.setInvokeType(ProtocolVendorDO.INVOKE_TYPE_REMOTE_API);
            vendor.setServiceName("remote_control");
        }
        return vendor;
    }

    private String mapOperationType(String functionName, String commandType) {
        if (functionName == null) return "OTHER";
        if (functionName.contains("消音")) return "MUTE";
        if (functionName.contains("复位")) return "RESET";
        if (functionName.contains("模式")) return "SWITCH_MODE";
        if ("start".equalsIgnoreCase(commandType) || "启动".equals(commandType)) return "START";
        if ("stop".equalsIgnoreCase(commandType) || "停止".equals(commandType)) return "STOP";
        return "OTHER";
    }

    private String buildOperationName(String functionName, String commandType) {
        if (functionName == null) return "未知操作";
        if (functionName.contains("消音")) return "消音";
        if (functionName.contains("复位")) return "复位";
        if (functionName.contains("模式")) return "切换模式";
        if ("start".equalsIgnoreCase(commandType) || "启动".equals(commandType)) return functionName + "-启动";
        if ("stop".equalsIgnoreCase(commandType) || "停止".equals(commandType)) return functionName + "-停止";
        return functionName;
    }

    private InstructionSendRespVO buildResp(boolean success, String message) {
        InstructionSendRespVO resp = new InstructionSendRespVO();
        resp.setSuccess(success);
        resp.setMessage(message);
        return resp;
    }
}
