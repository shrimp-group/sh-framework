package com.wkclz.redis.helper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 *
 */
@Slf4j
@Component
public class RedisHelper {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // ============================ String ============================

    /**
     * 保存字符串
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis set error: ", e);
            return false;
        }
    }

    /**
     * 保存字符串并设置过期时间
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 是否成功
     */
    public boolean set(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Redis set error: ", e);
            return false;
        }
    }

    /**
     * 保存字符串并设置过期时间（如果键不存在）
     * 原子操作，相当于SETNX + EXPIRE
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 是否成功
     */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
            return result != null && result;
        } catch (Exception e) {
            log.error("Redis setIfAbsent error: ", e);
            return false;
        }
    }

    /**
     * 自增
     *
     * @param key 键
     * @return 自增后的值
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis increment error: ", e);
            return null;
        }
    }

    /**
     * 获取字符串
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除键
     *
     * @param key 键
     * @return 是否成功
     */
    public boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis delete error: ", e);
            return false;
        }
    }

    /**
     * 批量删除键
     *
     * @param keys 键集合
     * @return 删除的数量
     */
    public long delete(Set<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("Redis delete error: ", e);
            return 0;
        }
    }

    // ============================ Hash ============================

    /**
     * 保存哈希
     *
     * @param key     键
     * @param hashKey 哈希键
     * @param value   值
     * @return 是否成功
     */
    public boolean hSet(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            return true;
        } catch (Exception e) {
            log.error("Redis hSet error: ", e);
            return false;
        }
    }

    /**
     * 获取哈希值
     *
     * @param key     键
     * @param hashKey 哈希键
     * @return 值
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取所有哈希值
     *
     * @param key 键
     * @return 哈希映射
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    // ============================ List ============================

    /**
     * 从列表左侧添加元素
     *
     * @param key   键
     * @param value 值
     * @return 添加的数量
     */
    public long lPush(String key, Object value) {
        try {
            return redisTemplate.opsForList().leftPush(key, value);
        } catch (Exception e) {
            log.error("Redis lPush error: ", e);
            return 0;
        }
    }

    /**
     * 从列表右侧弹出元素
     *
     * @param key 键
     * @return 弹出的元素
     */
    public Object rPop(String key) {
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            log.error("Redis rPop error: ", e);
            return null;
        }
    }

    /**
     * 从列表左侧弹出元素
     *
     * @param key 键
     * @return 弹出的元素
     */
    public Object lPop(String key) {
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.error("Redis lPop error: ", e);
            return null;
        }
    }

    /**
     * 从列表左侧弹出元素（阻塞式）
     *
     * @param key     键
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @return 弹出的元素，如果超时则返回 null
     */
    public Object bLPop(String key, long timeout, TimeUnit timeUnit) {
        try {
            return redisTemplate.opsForList().leftPop(key, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Redis bLPop error: ", e);
            return null;
        }
    }

    /**
     * 获取列表的长度
     *
     * @param key 键
     * @return 列表长度
     */
    public long lLen(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("Redis lLen error: ", e);
            return 0;
        }
    }

    /**
     * 获取列表范围内的元素
     *
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素列表
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("Redis lRange error: ", e);
            return null;
        }
    }

    // ============================ Set ============================

    /**
     * 添加集合元素
     *
     * @param key    键
     * @param values 值数组
     * @return 添加的数量
     */
    public long sAdd(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("Redis sAdd error: ", e);
            return 0;
        }
    }

    /**
     * 获取集合所有元素
     *
     * @param key 键
     * @return 元素集合
     */
    public Set<Object> sMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Redis sMembers error: ", e);
            return null;
        }
    }

    // ============================ ZSet ============================

    /**
     * 添加有序集合元素
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return 是否成功
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            return redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            log.error("Redis zAdd error: ", e);
            return false;
        }
    }

    /**
     * 获取有序集合范围内的元素
     *
     * @param key    键
     * @param start  开始索引
     * @param end    结束索引
     * @param isDesc 是否降序
     * @return 元素列表
     */
    public Set<Object> zRange(String key, long start, long end, boolean isDesc) {
        try {
            if (isDesc) {
                return redisTemplate.opsForZSet().reverseRange(key, start, end);
            } else {
                return redisTemplate.opsForZSet().range(key, start, end);
            }
        } catch (Exception e) {
            log.error("Redis zRange error: ", e);
            return null;
        }
    }

    // ============================ Common ============================

    /**
     * 设置键的过期时间
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis expire error: ", e);
            return false;
        }
    }

    /**
     * 获取键的剩余过期时间
     *
     * @param key  键
     * @param unit 时间单位
     * @return 剩余过期时间
     */
    public long getExpire(String key, TimeUnit unit) {
        try {
            return redisTemplate.getExpire(key, unit);
        } catch (Exception e) {
            log.error("Redis getExpire error: ", e);
            return 0;
        }
    }

    /**
     * 检查键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis hasKey error: ", e);
            return false;
        }
    }

}