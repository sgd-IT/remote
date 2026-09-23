package com.enjoyiot.eiot.ruleengine.action.device;


import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.common.thing.ThingService;
import com.enjoyiot.eiot.common.utils.UniqueIdUtil;

import com.enjoyiot.module.eiot.api.device.DeviceApi;
import lombok.Data;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeviceActionService {

    @Resource
    private DeviceApi deviceApi;

    public String invoke(Service service) {
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

    @Data
    public static class Service {

        private String device;

        private String identifier;

        private String type;

        public String getType() {
            //identifier为set固定为属性设置，其它为服务调用
            if (ThingModelMessage.ID_PROPERTY_SET.equals(identifier) ||
                    ThingModelMessage.ID_PROPERTY_GET.equals(identifier)) {
                return ThingModelMessage.TYPE_PROPERTY;
            }
            return ThingModelMessage.TYPE_SERVICE;
        }

        private List<Parameter> inputData;

        public Map<String, Object> parseInputData() {
            Map<String, Object> data = new HashMap<>();
            for (Parameter p : inputData) {
                data.put(p.getIdentifier(), p.getValue());
            }
            return data;
        }

        @Data
        public static class Parameter {
            private String identifier;
            private Object value;
        }
    }

}
