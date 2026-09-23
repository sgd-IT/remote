package com.enjoyiot.eiot.ruleengine.action.device;

import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.ruleengine.action.Action;
import com.enjoyiot.eiot.ruleengine.action.device.DeviceActionService.Service;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DeviceAction implements Action<Service> {

    public static final String TYPE = "device";

    private String type;

    private List<Service> services;

    private DeviceActionService deviceActionService;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public List<String> execute(ThingModelMessage msg) {
        List<String> results = new ArrayList<>();
        for (Service service : services) {
            deviceActionService.invoke(service);
            results.add(JsonUtils.toJsonString(service));
        }
        return results;
    }

}
