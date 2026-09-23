package com.enjoyiot.eiot.ruleengine.config;


import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.message.core.MqConsumer;
import com.enjoyiot.eiot.ruleengine.handler.RuleDeviceConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleConfiguration {

    @Bean
    public RuleDeviceConsumer getConsumer(MqConsumer<ThingModelMessage> consumer) {
        return new RuleDeviceConsumer(consumer);
    }

}
