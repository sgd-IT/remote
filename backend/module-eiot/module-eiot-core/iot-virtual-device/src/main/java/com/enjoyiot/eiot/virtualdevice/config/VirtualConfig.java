package com.enjoyiot.eiot.virtualdevice.config;


import com.enjoyiot.eiot.virtualdevice.VirtualManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirtualConfig {

    @Bean
    public VirtualManager getVirtualManager() {
        return new VirtualManager();
    }

}
