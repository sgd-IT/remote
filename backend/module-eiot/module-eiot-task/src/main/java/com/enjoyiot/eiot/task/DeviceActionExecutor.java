package com.enjoyiot.eiot.task;


import com.enjoyiot.eiot.common.thing.ThingService;
import com.enjoyiot.eiot.common.utils.UniqueIdUtil;
import com.enjoyiot.eiot.task.model.DeviceActionMsg;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 设备输出器
 */
@Slf4j
@Component
public class DeviceActionExecutor implements ActionExecutor<Object> {

    @Resource
    @org.springframework.context.annotation.Lazy
    private DeviceApi deviceApi;

    private Map<Integer, DeviceActionMsg> actionMap = Maps.newConcurrentMap();

    @Override
    public String getName() {
        return "device";
    }

    @Override
    public void execute(String config) {
        if (StringUtils.isBlank(config)) {
            log.error("device executor's config is blank");
            return;
        }
        //将执行的数据转换为动作配置
        Integer code = config.hashCode();
        DeviceActionMsg action = actionMap.computeIfAbsent(code, k -> JsonUtils.parseObject(config, DeviceActionMsg.class));

        log.info("start device service invoke,{}", JsonUtils.toJsonString(action));
        for (DeviceActionMsg.Service service : action.getServices()) {
            invoke(service);
        }
    }


    public String invoke(DeviceActionMsg.Service service) {
        String[] pkDn = service.getDevice().split("/");
        ThingService<Map<String, Object>> thingService = new ThingService<>();
        thingService.setMid(UniqueIdUtil.newRequestId());
        thingService.setProductKey(pkDn[0]);
        thingService.setDn(pkDn[1]);
        thingService.setType(service.getType());
        thingService.setIdentifier(service.getIdentifier());
        thingService.setParams(service.parseInputData());
        deviceApi.invoke(thingService);
        return thingService.getMid();
    }
}
