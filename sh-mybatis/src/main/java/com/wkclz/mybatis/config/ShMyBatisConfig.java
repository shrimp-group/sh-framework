package com.wkclz.mybatis.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ShMyBatisConfig {

    @Value("${sh.mybatis.data-length-check:1}")
    private Integer dataLengthCheck;
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public String getTableSchema() {
        if (StringUtils.isBlank(datasourceUrl)) {
            return null;
        }
        String schema = datasourceUrl;
        // 解析 JDBC URL 格式: jdbc:mysql://host:port/schema?params
        int hostStart = schema.indexOf("//");
        if (hostStart > -1) {
            schema = schema.substring(hostStart + 2);
        }
        int pathStart = schema.indexOf("/");
        if (pathStart > -1) {
            schema = schema.substring(pathStart + 1);
        } else {
            return null;
        }
        int queryStart = schema.indexOf("?");
        if (queryStart > -1) {
            schema = schema.substring(0, queryStart);
        }
        return schema.isEmpty() ? null : schema;
    }


}
