package com.enjoyiot.eiot.temporal.kw.dao;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class KwJdbcTemplate extends JdbcTemplate {

    public KwJdbcTemplate() {
    }

    public KwJdbcTemplate(DataSource dataSource) {
        super(dataSource);
    }

    public KwJdbcTemplate(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }
}
