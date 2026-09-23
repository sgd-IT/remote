package com.enjoyiot.eiot.temporal.timescaledb.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.enjoyiot.eiot.temporal.timescaledb.dao.PgTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class PgDatasourceConfig {

    @Value("${spring.postgres.url}")
    private String url;

    @Value("${spring.postgres.driverClassName}")
    private String driverClassName;

    @Value("${spring.postgres.username}")
    private String username;

    @Value("${spring.postgres.password}")
    private String password;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DefaultDataSourceCreator dataSourceCreator;

    @Bean("pgJdbcTemplate")
    public PgTemplate pgJdbcTemplate() {
        // dynamic-datasource 动态添加数据源
        DataSourceProperty dataSourceProperty = new DataSourceProperty();
        dataSourceProperty.setUrl(url);
        dataSourceProperty.setUsername(username);
        dataSourceProperty.setPassword(password);
        dataSourceProperty.setDriverClassName(driverClassName);
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        DataSource dataSource = dataSourceCreator.createDataSource(dataSourceProperty);
        ds.addDataSource("timescaledb", dataSource);
        // 创建JdbcTemplate
        return new PgTemplate(dataSource);
    }

}
