package com.enjoyiot.eiot.ruleengine.action;

import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.enjoyiot.eiot.engine.IScriptEngine;
import com.enjoyiot.eiot.engine.ScriptEngineFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
public class ScriptService {

    private IScriptEngine scriptEngine = ScriptEngineFactory.getScriptEngine("js");

    private String script;


    private DeviceApi deviceApi;

    public void setScript(String script) {
        this.script=script;
        scriptEngine.setScript(script);
    }

    public <T> T execScript(TypeReference<T> type, ThingModelMessage msg) {
        try {
            //取设备信息
            DeviceInfo deviceInfo = deviceApi.getDeviceInfoFromCache(msg.getDeviceId());

            //执行转换脚本
            return scriptEngine.invokeMethod(type, "translate", msg, deviceInfo);
        } catch (Throwable e) {
            log.error("run script error", e);
            return null;
        }
    }
}
