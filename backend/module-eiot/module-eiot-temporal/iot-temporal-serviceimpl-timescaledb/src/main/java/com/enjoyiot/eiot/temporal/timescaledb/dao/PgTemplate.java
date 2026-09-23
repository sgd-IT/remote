package com.enjoyiot.eiot.temporal.timescaledb.dao;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class PgTemplate extends JdbcTemplate {

    public PgTemplate() {
    }

    public PgTemplate(DataSource dataSource) {
        super(dataSource);
    }

    public PgTemplate(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }
}
