package com.enjoyiot.eiot.task.config;

import com.enjoyiot.eiot.task.TaskServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskConfiguration {

    @Bean
    public TaskServiceImpl getTaskManager() {
        return new TaskServiceImpl();
    }
}
