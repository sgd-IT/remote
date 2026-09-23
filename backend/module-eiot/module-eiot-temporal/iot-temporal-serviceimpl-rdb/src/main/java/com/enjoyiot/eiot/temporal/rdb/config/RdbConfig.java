package com.enjoyiot.eiot.temporal.rdb.config;

import com.enjoyiot.eiot.temporal.rdb.dao.RdbTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class RdbConfig {

    @Autowired
    private DataSource dataSource;

    @Bean("rdbTemplate")
    @Primary
    public RdbTemplate rdbTemplate() {
        return new RdbTemplate(dataSource);
    }
}
