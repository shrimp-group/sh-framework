package com.wkclz.redis.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁实现（支持 Watchdog 自动续期）
 */
@Slf4j
@Component
public class RedisLock implements DisposableBean {

    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Lua脚本：释放锁
    private static final String RELEASE_LOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    // Lua脚本：续期锁
    private static final String RENEW_LOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end";

    private volatile DefaultRedisScript<Long> releaseLockScript = null;
    private volatile DefaultRedisScript<Long> renewLockScript = null;

    // Watchdog 调度器（懒加载，守护线程）
    private volatile ScheduledExecutorService watchdogExecutor = null;

    // 活跃的 Watchdog 任务映射
    private final ConcurrentHashMap<String, ScheduledFuture<?>> watchdogTasks = new ConcurrentHashMap<>();

    /**
     * 初始化释放锁脚本
     */
    private void initReleaseLockScript() {
        if (releaseLockScript == null) {
            synchronized (this) {
                if (releaseLockScript == null) {
                    releaseLockScript = new DefaultRedisScript<>();
                    releaseLockScript.setScriptText(RELEASE_LOCK_SCRIPT);
                    releaseLockScript.setResultType(Long.class);
                }
            }
        }
    }

    /**
     * 初始化续期锁脚本
     */
    private void initRenewLockScript() {
        if (renewLockScript == null) {
            synchronized (this) {
                if (renewLockScript == null) {
                    renewLockScript = new DefaultRedisScript<>();
                    renewLockScript.setScriptText(RENEW_LOCK_SCRIPT);
                    renewLockScript.setResultType(Long.class);
                }
            }
        }
    }

    /**
     * 懒加载获取 Watchdog 调度器（单守护线程）
     */
    private ScheduledExecutorService getWatchdogExecutor() {
        if (watchdogExecutor == null) {
            synchronized (this) {
                if (watchdogExecutor == null) {
                    watchdogExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "redis-lock-watchdog");
                        t.setDaemon(true);
                        return t;
                    });
                }
            }
        }
        return watchdogExecutor;
    }

    /**
     * 启动 Watchdog 续期任务
     *
     * @param lockKey   锁的键
     * @param requestId 锁的唯一标识
     * @param lockTime  锁的时间
     * @param timeUnit  时间单位
     */
    private void startWatchdog(String lockKey, String requestId, long lockTime, TimeUnit timeUnit) {
        // 续期间隔为锁时间的 1/3，转换为毫秒
        long renewalIntervalMs = timeUnit.toMillis(lockTime) / 3;

        ScheduledFuture<?> future = getWatchdogExecutor().scheduleAtFixedRate(() -> {
            try {
                renewLock(lockKey, requestId, lockTime, timeUnit);
            } catch (Exception e) {
                log.error("Redis watchdog renewal error: ", e);
                stopWatchdog(lockKey);
            }
        }, renewalIntervalMs, renewalIntervalMs, TimeUnit.MILLISECONDS);

        ScheduledFuture<?> old = watchdogTasks.put(lockKey, future);
        if (old != null) {
            old.cancel(false);
        }
    }

    /**
     * 停止 Watchdog 续期任务
     *
     * @param lockKey 锁的键
     */
    private void stopWatchdog(String lockKey) {
        ScheduledFuture<?> future = watchdogTasks.remove(lockKey);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 续期锁（Lua 脚本保证原子性）
     *
     * @param lockKey   锁的键
     * @param requestId 锁的唯一标识
     * @param lockTime  锁的时间
     * @param timeUnit  时间单位
     */
    private void renewLock(String lockKey, String requestId, long lockTime, TimeUnit timeUnit) {
        try {
            initRenewLockScript();
            Long result = redisTemplate.execute(
                renewLockScript,
                Collections.singletonList(lockKey),
                requestId,
                String.valueOf(timeUnit.toSeconds(lockTime))
            );
            if (result == null || result == 0) {
                log.warn("Redis lock renewal failed, lock may have been released or taken over: {}", lockKey);
                stopWatchdog(lockKey);
            }
        } catch (Exception e) {
            log.error("Redis renewLock error: ", e);
            stopWatchdog(lockKey);
        }
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey  锁的键
     * @param lockTime 锁的时间
     * @param timeUnit 时间单位
     * @return 锁的唯一标识，如果获取失败返回null
     */
    public String tryLock(String lockKey, long lockTime, TimeUnit timeUnit) {
        if (lockKey == null || lockTime <= 0) {
            return null;
        }

        // 生成唯一标识
        String requestId = UUID.randomUUID().toString();

        try {
            // 尝试获取锁，使用SETNX命令的逻辑（原子操作）
            boolean locked = redisHelper.setIfAbsent(lockKey, requestId, lockTime, timeUnit);
            if (locked) {
                return requestId;
            }
        } catch (Exception e) {
            log.error("Redis tryLock error: ", e);
        }

        return null;
    }

    /**
     * 尝试获取锁（带 Watchdog 自动续期）
     *
     * @param lockKey  锁的键
     * @param lockTime 锁的时间
     * @param timeUnit 时间单位
     * @return 锁持有信息，如果获取失败返回null
     */
    public LockHolder tryLockWithWatchdog(String lockKey, long lockTime, TimeUnit timeUnit) {
        if (lockKey == null || lockTime <= 0) {
            return null;
        }

        // 生成唯一标识
        String requestId = UUID.randomUUID().toString();

        try {
            // 尝试获取锁，使用SETNX命令的逻辑（原子操作）
            boolean locked = redisHelper.setIfAbsent(lockKey, requestId, lockTime, timeUnit);
            if (locked) {
                // 启动 Watchdog 续期任务
                startWatchdog(lockKey, requestId, lockTime, timeUnit);
                return new LockHolder(lockKey, requestId);
            }
        } catch (Exception e) {
            log.error("Redis tryLockWithWatchdog error: ", e);
        }

        return null;
    }

    /**
     * 释放锁
     *
     * @param lockKey   锁的键
     * @param requestId 锁的唯一标识
     * @return 是否成功释放锁
     */
    public boolean releaseLock(String lockKey, String requestId) {
        if (lockKey == null || requestId == null) {
            return false;
        }

        // 停止 Watchdog 续期任务
        stopWatchdog(lockKey);

        try {
            // 懒加载初始化释放锁脚本
            initReleaseLockScript();

            // 使用Lua脚本确保原子性
            Long result = redisTemplate.execute(
                releaseLockScript,
                Collections.singletonList(lockKey),
                requestId
            );
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Redis releaseLock error: ", e);
        }

        return false;
    }

    /**
     * 释放锁（通过 LockHolder）
     *
     * @param holder 锁持有信息
     * @return 是否成功释放锁
     */
    public boolean releaseLock(LockHolder holder) {
        if (holder == null) {
            return false;
        }
        return releaseLock(holder.getLockKey(), holder.getRequestId());
    }

    /**
     * 尝试获取锁，如果获取失败则等待重试
     *
     * @param lockKey       锁的键
     * @param lockTime      锁的时间
     * @param timeUnit      时间单位
     * @param retryCount    重试次数
     * @param retryDelay    重试间隔时间
     * @param retryTimeUnit 重试间隔时间单位
     * @return 锁的唯一标识，如果获取失败返回null
     */
    public String tryLockWithRetry(String lockKey, long lockTime, TimeUnit timeUnit, int retryCount, long retryDelay, TimeUnit retryTimeUnit) {
        if (lockKey == null || lockTime <= 0) {
            return null;
        }

        // 第一次尝试获取锁
        String requestId = tryLock(lockKey, lockTime, timeUnit);
        if (requestId != null) {
            return requestId;
        }

        // 如果获取失败，进行重试
        for (int i = 0; i < retryCount; i++) {
            try {
                // 等待重试间隔时间
                retryTimeUnit.sleep(retryDelay);

                // 再次尝试获取锁
                requestId = tryLock(lockKey, lockTime, timeUnit);
                if (requestId != null) {
                    return requestId;
                }
            } catch (InterruptedException e) {
                log.error("Redis tryLockWithRetry interrupted: ", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Redis tryLockWithRetry error: ", e);
            }
        }

        return null;
    }

    @Override
    public void destroy() {
        // 取消所有 Watchdog 续期任务
        for (String lockKey : watchdogTasks.keySet()) {
            stopWatchdog(lockKey);
        }

        // 关闭调度器
        if (watchdogExecutor != null) {
            synchronized (this) {
                if (watchdogExecutor != null) {
                    watchdogExecutor.shutdown();
                    watchdogExecutor = null;
                }
            }
        }
    }

}
