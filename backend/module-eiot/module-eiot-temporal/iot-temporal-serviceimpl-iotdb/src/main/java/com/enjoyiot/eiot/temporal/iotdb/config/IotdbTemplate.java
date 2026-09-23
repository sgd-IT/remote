package com.enjoyiot.eiot.temporal.iotdb.config;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class IotdbTemplate extends JdbcTemplate {

    public IotdbTemplate() {
    }

    public IotdbTemplate(DataSource dataSource) {
        super(dataSource);
    }

    public IotdbTemplate(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }
}
