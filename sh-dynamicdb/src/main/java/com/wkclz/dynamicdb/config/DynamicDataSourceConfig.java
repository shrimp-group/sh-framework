package com.wkclz.dynamicdb.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class DynamicDataSourceConfig {

    @Value("${sh.dynamicdb.cache-second:60}")
    private Integer dynamicdbCacheSecond;


}
