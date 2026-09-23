package com.enjoyiot.eiot.temporal.rdb.dao;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class RdbTemplate extends JdbcTemplate {

    public RdbTemplate() {
    }

    public RdbTemplate(DataSource dataSource) {
        super(dataSource);
    }

    public RdbTemplate(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }
}
