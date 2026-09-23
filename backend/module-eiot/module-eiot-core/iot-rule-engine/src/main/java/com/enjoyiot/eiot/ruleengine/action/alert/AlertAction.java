package com.enjoyiot.eiot.ruleengine.action.alert;


import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.ruleengine.action.Action;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sjg
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AlertAction implements Action<AlertService> {

    public static final String TYPE = "alert";

    private String type;

    private List<AlertService> services;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public List<String> execute(ThingModelMessage msg) {
        List<String> results = new ArrayList<>();
        for (AlertService service : services) {
            results.add(service.execute(msg));
        }
        return results;
    }

    /**
     * 告警解除动作，使用恢复脚本（如未配置则复用触发脚本）
     */
    public List<String> recover(ThingModelMessage msg) {
        List<String> results = new ArrayList<>();
        for (AlertService service : services) {
            results.add(service.executeRecover(msg));
        }
        return results;
    }

}
