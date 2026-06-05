package com.wkclz.redis.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.List;

@Data
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;
    @Value("${spring.data.redis.port:6379}")
    private int port;
    @Value("${spring.data.redis.password:}")
    private String password;
    @Value("${spring.data.redis.database:0}")
    private int database;

    /**
     * AutoType 白名单扩展，允许用户自定义额外的包路径前缀。
     * 默认白名单（com.wkclz. / java.util. / java.lang. / java.time.）已在序列化器中硬编码，
     * 此配置项用于扩展白名单以支持业务自定义类。
     */
    @Value("${sh.redis.auto-type-whitelist:}")
    private List<String> autoTypeWhitelist;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }


}