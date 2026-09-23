package com.enjoyiot.eiot.ruleengine.action.tcp;


import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.ruleengine.action.Action;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangwenl
 * @date 2022-12-14
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TcpAction implements Action<TcpService> {
    public static final String TYPE = "tcp";


    private List<TcpService> services;

    @Override
    public String getType() {
        return TYPE;
    }


    @Override
    public List<String> execute(ThingModelMessage msg) {
        List<String> results = new ArrayList<>();
        for (TcpService service : services) {
            results.add(service.execute(msg));
        }
        return results;
    }
}
