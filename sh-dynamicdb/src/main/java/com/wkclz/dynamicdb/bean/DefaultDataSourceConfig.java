package com.wkclz.dynamicdb.bean;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class DefaultDataSourceConfig {
    @Value("${spring.datasource.name:default}")
    private String name;
    @Value("${spring.datasource.username:}")
    private String username;
    @Value("${spring.datasource.password:}")
    private String password;
    @Value("${spring.datasource.url:}")
    private String url;
    @Value("${spring.datasource.driverClassName:}")
    private String driverClassName;
    @Value("${spring.datasource.druid.initialSize:0}")
    private String initialSize;
    @Value("${spring.datasource.druid.maxActive:8}")
    private String maxActive;
    @Value("${spring.datasource.druid.minIdle:0}")
    private String minIdle;
    @Value("${spring.datasource.druid.maxWait:-1}")
    private String maxWait;
    // Druid 过滤器配置，log4j 已过时，建议使用 slf4j 或 log4j2
    @Value("${spring.datasource.druid.filters:stat,wall,slf4j}")
    private String filters;

}
