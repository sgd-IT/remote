package com.enjoyiot.module.eiot.service.device.instruction;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendReqVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.protocol.ProtocolVendorDO;
import com.enjoyiot.module.eiot.dal.dataobject.protocol.VendorCommandDO;
import com.enjoyiot.module.eiot.dal.mysql.protocol.VendorCommandMapper;
import com.enjoyiot.module.eiot.service.device.DeviceCtrlService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LOCAL_COMMAND 策略: 本地命令码映射(YWN类厂商)
 *
 * 无需外部API换码, 根据厂商指令字典(eiot_vendor_command)解析命令码,
 * 按 param_schema 把设备功能参数装配到 machineNo/param1/param2 槽位,
 * 统一调用组件 remote_control 服务。未来固定命令码厂商纯配置零代码接入。
 */
@Component
@Slf4j
public class LocalCommandInstructionSender implements InstructionSender {

    @Resource
    private VendorCommandMapper vendorCommandMapper;
    @Resource
    private DeviceCtrlService deviceCtrlService;

    @Override
    public String getInvokeType() {
        return ProtocolVendorDO.INVOKE_TYPE_LOCAL_COMMAND;
    }

    @Override
    public InstructionSendRespVO send(InstructionContext context) {
        InstructionSendReqVO reqVO = context.getReqVO();
        ProtocolVendorDO vendor = context.getVendor();

        // 1. 定位厂商指令字典
        String commandKey = context.getFunctionDef().getVendorCommandKey();
        if (StrUtil.isBlank(commandKey)) {
            return buildResp(false, "功能未配置厂商指令标识(vendorCommandKey)");
        }
        VendorCommandDO command = vendorCommandMapper.selectByVendorAndKey(vendor.getVendorCode(), commandKey);
        if (command == null) {
            return buildResp(false, "厂商指令字典不存在: " + vendor.getVendorCode() + "/" + commandKey);
        }

        // 2. 解析命令码(启/停语义)
        Integer commandCode = resolveCommandCode(command, reqVO.getCommandType());
        if (commandCode == null) {
            return buildResp(false, "指令 " + command.getCommandName() + " 不支持该操作类型");
        }

        // 3. 按 param_schema 装配槽位参数
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("command_code", commandCode);
        args.put("machineNo", 1);
        args.put("param1", 0);
        args.put("param2", 0);

        if (StrUtil.isNotBlank(command.getParamSchema())) {
            Map<String, Object> paramValues = parseParamValues(context.getDeviceFunction().getParamValues());
            JSONObject schema = JSONUtil.parseObj(command.getParamSchema());
            JSONArray params = schema.getJSONArray("params");
            if (params != null) {
                for (Object item : params) {
                    JSONObject def = (JSONObject) item;
                    String code = def.getStr("code");
                    String slot = def.getStr("slot");
                    Integer defaultVal = def.getInt("default", 0);
                    if (StrUtil.isBlank(slot)) {
                        continue;
                    }
                    Object value = paramValues.get(code);
                    // 未填或非法数字时用默认值(前端未填会提交空字符串)
                    int intValue = defaultVal;
                    if (value != null && StrUtil.isNotBlank(value.toString())) {
                        try {
                            intValue = Integer.parseInt(value.toString().trim());
                        } catch (NumberFormatException e) {
                            log.warn("参数 {} 值非法: {}, 使用默认值 {}", code, value, defaultVal);
                        }
                    }
                    args.put(slot, intValue);
                }
            }
        }

        // 4. 统一走组件下发通道
        String serviceName = StrUtil.isNotBlank(vendor.getServiceName()) ? vendor.getServiceName() : "remote_control";
        log.info("本地命令下发: deviceId={}, vendor={}, commandKey={}, args={}",
                reqVO.getDeviceId(), vendor.getVendorCode(), commandKey, args);
        deviceCtrlService.invokeService(reqVO.getDeviceId(), serviceName, args);

        return buildResp(true, "指令已下发");
    }

    /**
     * 解析命令码: 有启停语义时按 commandType 选 codeStart/codeStop, 否则用 codeStart
     * commandType: '1'/start/启动 -> 启动码; '0'/stop/停止 -> 停止码
     */
    private Integer resolveCommandCode(VendorCommandDO command, String commandType) {
        if (command.getCodeStop() == null) {
            // 无启停语义的指令(复位/消音/自检等)
            return command.getCodeStart();
        }
        if (isStop(commandType)) {
            return command.getCodeStop();
        }
        return command.getCodeStart();
    }

    private boolean isStop(String commandType) {
        return "0".equals(commandType) || "stop".equalsIgnoreCase(commandType) || "停止".equals(commandType);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseParamValues(String paramValuesJson) {
        if (StrUtil.isBlank(paramValuesJson)) {
            return new LinkedHashMap<>();
        }
        return JSONUtil.toBean(paramValuesJson, Map.class);
    }
}
