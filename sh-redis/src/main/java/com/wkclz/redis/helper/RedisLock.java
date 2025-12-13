package com.wkclz.redis.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁实现
 */
@Slf4j
@Component
public class RedisLock {

    @Autowired
    private Redishelper redishelper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Lua脚本：释放锁
    private static final String RELEASE_LOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    private DefaultRedisScript<Long> releaseLockScript = null;
    
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
     * 尝试获取锁
     *
     * @param lockKey   锁的键
     * @param lockTime  锁的时间
     * @param timeUnit  时间单位
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
            boolean locked = redishelper.setIfAbsent(lockKey, requestId, lockTime, timeUnit);
            if (locked) {
                return requestId;
            }
        } catch (Exception e) {
            log.error("Redis tryLock error: ", e);
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
     * 尝试获取锁，如果获取失败则等待重试
     *
     * @param lockKey     锁的键
     * @param lockTime    锁的时间
     * @param timeUnit    时间单位
     * @param retryCount  重试次数
     * @param retryDelay  重试间隔时间
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

}