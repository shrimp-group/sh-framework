package com.wkclz.dynamicdb.config;

import com.wkclz.dynamicdb.DynamicDataSource;
import com.wkclz.dynamicdb.DynamicDataSourceFactory;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;


@Configuration
@ConditionalOnBean({DynamicDataSourceFactory.class})
public class DynamicDataSourceAutoConfig {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceAutoConfig.class);

    @Resource
    private DataSource dataSource;
    @Resource
    private DynamicDataSourceConfig dynamicDataSourceConfig;

    private ScheduledExecutorService cleanupScheduler;

    @Bean
    public ScheduledExecutorService dynamicDsCleanupScheduler() {
        cleanupScheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "dynamic-ds-cleanup");
            t.setDaemon(true);
            return t;
        });
        return cleanupScheduler;
    }

    @Bean
    @Primary
    public DynamicDataSource dynamicDataSource(ScheduledExecutorService dynamicDsCleanupScheduler) {
        logger.info("dynamicData Source, load default dataSource...");
        DynamicDataSource dynamicDataSource = new DynamicDataSource();

        dynamicDataSource.setDefaultTargetDataSource(dataSource);
        dynamicDataSource.setTargetDataSources(new ConcurrentHashMap<>());
        dynamicDataSource.afterPropertiesSet();

        // 启动定时清理任务
        dynamicDataSource.startCleanupTask(dynamicDsCleanupScheduler, dynamicDataSourceConfig);

        return dynamicDataSource;
    }

    @PreDestroy
    public void stopCleanupTask() {
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
        }
    }
}
