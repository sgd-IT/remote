package com.enjoyiot.eiot.temporal.kw.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.enjoyiot.eiot.temporal.kw.dao.KwJdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class KwDatasourceConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DefaultDataSourceCreator dataSourceCreator;

    @Bean(name = "kwDataSource")
    @ConfigurationProperties(prefix = "spring.kw-datasource")
    public DataSourceProperty kwDataSource() {
        return new DataSourceProperty();
    }

    @Bean(name = "kwJdbcTemplate")
    public KwJdbcTemplate tsJdbcTemplate(@Qualifier("kwDataSource") DataSourceProperty dataSourceProperty) {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        DataSource dataSource = dataSourceCreator.createDataSource(dataSourceProperty);
        ds.addDataSource("kwDataSource", dataSource);
        return new KwJdbcTemplate(dataSource);
    }

}
