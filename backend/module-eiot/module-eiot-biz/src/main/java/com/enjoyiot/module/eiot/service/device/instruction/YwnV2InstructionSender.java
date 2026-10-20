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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class YwnV2InstructionSender implements InstructionSender {

    private static final String VENDOR_CODE = "YWN_V2";

    private final VendorCommandMapper vendorCommandMapper;
    private final DeviceCtrlService deviceCtrlService;

    @Override
    public String getInvokeType() {
        return ProtocolVendorDO.INVOKE_TYPE_LOCAL_COMMAND;
    }

    @Override
    public boolean supportsVendor(String vendorCode) {
        return VENDOR_CODE.equals(vendorCode);
    }

    @Override
    public boolean isVendorSpecific() {
        return true;
    }

    @Override
    public InstructionSendRespVO send(InstructionContext context) {
        InstructionSendReqVO reqVO = context.getReqVO();
        ProtocolVendorDO vendor = context.getVendor();
        if (!supportsVendor(vendor.getVendorCode())) {
            return buildResp(false, "非运维牛 V2 厂商不支持此下发策略");
        }

        String commandKey = context.getFunctionDef().getVendorCommandKey();
        if (StrUtil.isBlank(commandKey)) {
            return buildResp(false, "功能未配置厂商指令标识(vendorCommandKey)");
        }
        VendorCommandDO command = vendorCommandMapper.selectByVendorAndKey(vendor.getVendorCode(), commandKey);
        if (command == null) {
            return buildResp(false, "厂商指令字典不存在: " + vendor.getVendorCode() + "/" + commandKey);
        }

        Integer controlType = resolveControlType(command, reqVO.getCommandType());
        if (controlType == null) {
            return buildResp(false, "指令 " + command.getCommandName() + " 不支持该操作类型");
        }

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("control_type", controlType);
        args.put("host", 1);
        args.put("loop", 0);
        args.put("once", 0);
        args.put("devtype", 0);
        args.put("twice", "");
        args.put("zongxian", false);
        try {
            applyParams(args, command.getParamSchema(), context.getDeviceFunction().getParamValues());
        } catch (Exception e) {
            log.warn("运维牛 V2 指令参数解析失败: vendor={}, commandKey={}", vendor.getVendorCode(), commandKey, e);
            return buildResp(false, "运维牛 V2 指令参数解析失败");
        }

        String serviceName = StrUtil.isNotBlank(vendor.getServiceName()) ? vendor.getServiceName() : "remote_control";
        log.info("运维牛 V2 指令下发: deviceId={}, commandKey={}, args={}", reqVO.getDeviceId(), commandKey, args);
        deviceCtrlService.invokeService(reqVO.getDeviceId(), serviceName, args);
        return buildResp(true, "指令已下发");
    }

    private Integer resolveControlType(VendorCommandDO command, String commandType) {
        if (command.getCodeStop() == null) {
            return command.getCodeStart();
        }
        return isStop(commandType) ? command.getCodeStop() : command.getCodeStart();
    }

    private void applyParams(Map<String, Object> args, String paramSchema, String paramValuesJson) {
        if (StrUtil.isBlank(paramSchema)) {
            return;
        }
        Map<String, Object> paramValues = parseParamValues(paramValuesJson);
        JSONArray params = JSONUtil.parseObj(paramSchema).getJSONArray("params");
        if (params == null) {
            return;
        }
        for (Object item : params) {
            JSONObject definition = (JSONObject) item;
            String code = definition.getStr("code");
            String target = definition.getStr("target");
            if (StrUtil.isBlank(target)) {
                continue;
            }
            Object value = paramValues.containsKey(code) ? paramValues.get(code) : definition.get("default");
            applyParam(args, target, value);
        }
    }

    private void applyParam(Map<String, Object> args, String target, Object value) {
        switch (target) {
            case "host", "loop", "once", "devtype" -> args.put(target, toInt(value, (Integer) args.get(target)));
            case "twice" -> args.put(target, value == null ? "" : value.toString());
            case "zongxian" -> args.put(target, toBoolean(value, (Boolean) args.get(target)));
            default -> log.warn("忽略不支持的运维牛 V2 参数目标: {}", target);
        }
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null || StrUtil.isBlank(value.toString())) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null || StrUtil.isBlank(value.toString())) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = value.toString().trim();
        if ("1".equals(text)) {
            return true;
        }
        if ("0".equals(text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
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
