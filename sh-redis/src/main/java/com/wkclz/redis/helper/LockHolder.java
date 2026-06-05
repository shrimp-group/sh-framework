package com.wkclz.redis.helper;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Redis 分布式锁持有信息
 */
@Data
@AllArgsConstructor
public class LockHolder {

    /**
     * 锁的键
     */
    private String lockKey;

    /**
     * 锁的唯一标识
     */
    private String requestId;

}
