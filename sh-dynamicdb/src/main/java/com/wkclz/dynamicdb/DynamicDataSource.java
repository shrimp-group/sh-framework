package com.wkclz.dynamicdb;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.wkclz.core.exception.SystemException;
import com.wkclz.dynamicdb.bean.DefaultDataSourceConfig;
import com.wkclz.dynamicdb.config.DynamicDataSourceConfig;
import com.wkclz.mybatis.bean.DataSourceInfo;
import com.wkclz.spring.config.SpringContextHolder;
import com.wkclz.tool.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;

/**
 * 重写 determineCurrentLookupKey() 方法来实现数据源切换功能
 * 若数据源不存在，需要到 DataSourceFactory 获取
 */
@Slf4j
public class DynamicDataSource extends AbstractShrimpRoutingDataSource implements DisposableBean {

    // 已经初始化的，不再初始化了
    private final Map<String, Long> hasCreateDataSource = new ConcurrentHashMap<>();

    // 正在创建中的数据源 Future，用于同 key 复用
    private final ConcurrentHashMap<String, CompletableFuture<String>> creatingDataSources = new ConcurrentHashMap<>();

    // 专用线程池，避免使用 ForkJoinPool.commonPool()
    private final ThreadPoolExecutor dataSourceExecutor = new ThreadPoolExecutor(
        2, 4, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(64),
        r -> {
            Thread t = new Thread(r, "dynamic-ds-creator-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // 定时清理任务的 Future，用于停止
    private volatile ScheduledFuture<?> cleanupFuture;

    @Override
    protected Object determineCurrentLookupKey() {
        String key = DynamicDataSourceHolder.get();
        if (key == null) {
            return null;
        }
        log.debug("determineCurrentLookupKey: {}", key);

        // 存在，并在有效期内
        Long latest = hasCreateDataSource.get(key);
        long now = System.currentTimeMillis();
        DynamicDataSourceConfig dsConfig = SpringContextHolder.getBean(DynamicDataSourceConfig.class);
        long cacheTimeMs = dsConfig.getDynamicdbCacheSecond() * 1_000L;
        if (latest != null && ((now - latest) < cacheTimeMs)) {
            return key;
        }

        // 使用 computeIfAbsent 实现 key 级别锁，同 key 共享同一个 Future
        CompletableFuture<String> future = creatingDataSources.computeIfAbsent(key, k -> {
            log.info("dataSource creation started for key: {}", k);

            // 缓存过期，关闭旧数据源
            Long oldLatest = hasCreateDataSource.get(k);
            if (oldLatest != null) {
                DataSource oldDataSource = getDataSource(k);
                if (oldDataSource instanceof DruidDataSource dds) {
                    try {
                        dds.close();
                    } catch (Exception e) {
                        log.error("Failed to close old dataSource for key: {}", k, e);
                    }
                }
            }

            CompletableFuture<String> f = CompletableFuture.supplyAsync(() -> {
                // 若想用多数据源，必须注入此工厂
                DynamicDataSourceFactory dynamicDataSourceFactory = SpringContextHolder.getBean(DynamicDataSourceFactory.class);
                // 只返回基础数据
                DataSourceInfo ds = dynamicDataSourceFactory.createDataSource(k);
                if (ds == null) {
                    throw SystemException.of("can not find dataSource by key: {}", k);
                }

                // 使用当前数据库连接池参数，仅是换了地址，用户名，密码的方案
                DefaultDataSourceConfig config = new DefaultDataSourceConfig();
                config.setUrl(ds.getUrl());
                config.setUsername(ds.getUsername());
                config.setPassword(ds.getPassword());
                Map<String, Object> map = MapUtil.obj2Map(config);
                DataSource dataSource;
                try {
                    dataSource = DruidDataSourceFactory.createDataSource(map);
                } catch (Exception e) {
                    throw SystemException.of("Failed to create dataSource for key: {}", k, e);
                }
                /*
                使用默认参数的方案
                DruidDataSource druidDataSource = DataSourceInfo.getDruidDataSource(ds);
                */
                addDataSource(k, dataSource);
                hasCreateDataSource.put(k, System.currentTimeMillis());
                log.info("dataSource creation success for key: {}", k);
                return k;
            }, dataSourceExecutor);

            // 创建失败时移除 Future，允许重试
            f.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("dataSource creation failed for key: {}", k, ex);
                }
                creatingDataSources.remove(k, f);
            });

            return f;
        });

        // 同 key 的其他线程复用已有 Future
        if (creatingDataSources.containsKey(key)) {
            log.debug("reusing in-progress creation future for key: {}", key);
        }

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SystemException.of(e.getMessage());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw SystemException.of(cause != null ? cause.getMessage() : e.getMessage());
        }
    }

    /**
     * 启动定时清理任务，扫描并关闭缓存过期的数据源
     */
    public void startCleanupTask(ScheduledExecutorService scheduler, DynamicDataSourceConfig dsConfig) {
        if (cleanupFuture != null && !cleanupFuture.isDone()) {
            log.warn("cleanup task already running, skip");
            return;
        }
        long intervalSeconds = dsConfig.getCleanupIntervalSecond();
        long cacheTimeMs = dsConfig.getDynamicdbCacheSecond() * 1_000L;
        cleanupFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredDataSources(cacheTimeMs);
            } catch (Exception e) {
                log.error("cleanup task error", e);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        log.info("dynamic datasource cleanup task started, interval: {}s, cache: {}s", intervalSeconds, dsConfig.getDynamicdbCacheSecond());
    }

    /**
     * 停止定时清理任务
     */
    public void stopCleanupTask() {
        if (cleanupFuture != null && !cleanupFuture.isDone()) {
            cleanupFuture.cancel(false);
            log.info("dynamic datasource cleanup task stopped");
        }
    }

    /**
     * 扫描并关闭缓存过期的数据源
     */
    private void cleanupExpiredDataSources(long cacheTimeMs) {
        long now = System.currentTimeMillis();
        int cleaned = 0;
        List<String> expiredKeys = new ArrayList<>();

        for (Map.Entry<String, Long> entry : hasCreateDataSource.entrySet()) {
            String key = entry.getKey();
            Long createTime = entry.getValue();

            // 跳过正在创建中的数据源
            if (creatingDataSources.containsKey(key)) {
                continue;
            }

            // 缓存未过期，跳过
            if ((now - createTime) < cacheTimeMs) {
                continue;
            }

            expiredKeys.add(key);
        }

        for (String key : expiredKeys) {
            DataSource ds = removeDataSource(key);
            if (ds instanceof DruidDataSource dds) {
                try {
                    dds.close();
                } catch (Exception e) {
                    log.error("failed to close expired dataSource for key: {}", key, e);
                }
            }
            hasCreateDataSource.remove(key);
            cleaned++;
            log.info("cleaned up expired dataSource for key: {}", key);
        }

        log.info("dynamic datasource cleanup completed, active: {}, cleaned: {}", hasCreateDataSource.size(), cleaned);
    }

    /**
     * 在数据源变更时，需要销毁旧数据源的连接池
     */
    public void destroyDataSource(String key) {
        // 如果正在创建中，移除 Future
        CompletableFuture<String> future = creatingDataSources.remove(key);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            log.info("cancelled in-progress creation for key: {}", key);
        }

        DataSource dataSource = removeDataSource(key);
        if (dataSource instanceof DruidDataSource dds) {
            try {
                dds.close();
            } catch (Exception e) {
                log.error("Failed to destroy dataSource for key: {}", key, e);
            }
        }
        hasCreateDataSource.remove(key);
        log.info("destroyed dataSource for key: {}", key);
    }

    @Override
    public void destroy() {
        // 停止清理任务
        stopCleanupTask();

        // 优雅关闭线程池
        dataSourceExecutor.shutdown();
        try {
            if (!dataSourceExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                dataSourceExecutor.shutdownNow();
                if (!dataSourceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("dataSourceExecutor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            dataSourceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 关闭所有动态数据源
        Map<Object, DataSource> resolved = getResolvedDataSources();
        for (Map.Entry<Object, DataSource> entry : resolved.entrySet()) {
            DataSource ds = entry.getValue();
            if (ds instanceof DruidDataSource dds) {
                try {
                    dds.close();
                } catch (Exception e) {
                    log.error("Failed to close dataSource for key: {}", entry.getKey(), e);
                }
            }
        }

        hasCreateDataSource.clear();
        creatingDataSources.clear();
        log.info("DynamicDataSource destroyed, all dynamic data sources closed");
    }

}
