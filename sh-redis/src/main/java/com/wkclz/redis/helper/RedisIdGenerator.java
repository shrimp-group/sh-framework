package com.wkclz.redis.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Redis ID生成器
 * 基于时间戳 + Redis自增序列号实现
 * 考虑时间回拨问题，确保ID尽可能短且永不重复
 * 每秒最多生成1w个ID
 *
 * @author wkclz
 * @date 2024-07-15
 */
@Slf4j
@Component
public class RedisIdGenerator {

    @Autowired
    private Redishelper redishelper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 基础时间（2024-01-01 00:00:00）
    private static final long BASE_TIME = 1704067200000L;
    
    // 序列号位数（14位，最大支持每秒16384个ID）
    private static final long SEQUENCE_BITS = 14L;
    private static final long MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1; // 16383
    
    // 机器标识位数（6位，最大支持64台机器）
    private static final long MACHINE_BITS = 6L;
    private static final long MAX_MACHINE_ID = (1 << MACHINE_BITS) - 1; // 63
    
    // 机器标识
    private Long machineId = null;
    
    /**
     * 初始化机器标识
     */
    private void initMachineId() {
        if (machineId == null) {
            synchronized (this) {
                if (machineId == null) {
                    try {
                        // 根据IP地址生成机器标识
                        InetAddress addr = InetAddress.getLocalHost();
                        byte[] ipBytes = addr.getAddress();
                        machineId = ((ipBytes[2] & 0xFF) << 8 | (ipBytes[3] & 0xFF)) & MAX_MACHINE_ID;
                    } catch (UnknownHostException e) {
                        // 如果无法获取IP地址，使用随机数
                        machineId = (long) (Math.random() * MAX_MACHINE_ID);
                    }
                    log.info("RedisIdGenerator machine id: {}", machineId);
                }
            }
        }
    }
    
    // Redis键前缀
    private static final String ID_GENERATOR_KEY_PREFIX = "id:generator:";
    
    // 上次生成ID的时间戳（用于处理时间回拨）
    private long lastTimestamp = -1L;
    
    // 上次生成的序列号
    private long lastSequence = 0L;
    
    // 62进制字符集
    private static final char[] BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    
    /**
     * 生成ID
     *
     * @param businessType 业务类型
     * @return 生成的ID
     */
    public String generateId(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            businessType = "default";
        }
        
        // 懒加载初始化机器标识
        initMachineId();
        
        long timestamp = System.currentTimeMillis();
        
        // 处理时间回拨问题
        if (timestamp < lastTimestamp) {
            log.warn("Clock moved backwards. Refusing to generate id for {} milliseconds", lastTimestamp - timestamp);
            timestamp = lastTimestamp;
        }
        
        // Redis键
        String key = ID_GENERATOR_KEY_PREFIX + businessType;
        
        try {
            // 获取当前时间戳对应的序列号
            Long sequence = redishelper.increment(key);
            if (sequence == null) {
                sequence = 1L;
            }
            
            // 如果是同一毫秒
            if (timestamp == lastTimestamp) {
                // 如果序列号超过最大值，等待下一毫秒
                if (sequence > MAX_SEQUENCE) {
                    timestamp = waitNextMillis(lastTimestamp);
                    sequence = 1L;
                }
            } else {
                // 新的毫秒，重置序列号
                sequence = 1L;
            }
            
            // 更新上次生成ID的时间戳和序列号
            lastTimestamp = timestamp;
            lastSequence = sequence;
            
            // 设置键的过期时间（当前时间 + 5秒）
            redishelper.set(key, sequence, 5, TimeUnit.SECONDS);
            
            // 计算相对时间戳（当前时间戳 - 基础时间）
            long relativeTimestamp = timestamp - BASE_TIME;
            
            // 组合时间戳、机器标识和序列号
            long id = (relativeTimestamp << (MACHINE_BITS + SEQUENCE_BITS)) 
                    | (machineId << SEQUENCE_BITS) 
                    | sequence;
            
            // 转换为62进制，使其更短
            return base62Encode(id);
            
        } catch (Exception e) {
            log.error("Redis generateId error: ", e);
            // 如果Redis不可用，使用本地生成策略
            return generateLocalId(timestamp);
        }
    }
    
    /**
     * 等待下一毫秒
     *
     * @param lastTimestamp 上次时间戳
     * @return 新的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
    
    /**
     * 本地生成ID（当Redis不可用时）
     *
     * @param timestamp 时间戳
     * @return 生成的ID
     */
    private String generateLocalId(long timestamp) {
        // 使用当前时间戳 + 本地自增序列号
        long sequence;
        if (timestamp == lastTimestamp) {
            sequence = (lastSequence + 1) % MAX_SEQUENCE;
            // 如果序列号超过最大值，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        
        lastTimestamp = timestamp;
        lastSequence = sequence;
        
        long relativeTimestamp = timestamp - BASE_TIME;
        long id = (relativeTimestamp << (MACHINE_BITS + SEQUENCE_BITS)) 
                | (machineId << SEQUENCE_BITS) 
                | sequence;
        
        return base62Encode(id);
    }
    
    /**
     * 62进制编码
     *
     * @param number 数字
     * @return 62进制字符串
     */
    private String base62Encode(long number) {
        if (number == 0) {
            return "0";
        }
        
        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            sb.append(BASE62_CHARS[(int) (number % 62)]);
            number = number / 62;
        }
        
        // 反转字符串，因为我们是从低位开始构建的
        return sb.reverse().toString();
    }
    
    /**
     * 获取当前时间戳（用于测试）
     *
     * @return 当前时间戳
     */
    public long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * 获取基础时间（用于测试）
     *
     * @return 基础时间
     */
    public long getBaseTime() {
        return BASE_TIME;
    }
    
    /**
     * 获取当前时间与基础时间的差值（用于测试）
     *
     * @return 时间差值（毫秒）
     */
    public long getRelativeTimestamp() {
        return System.currentTimeMillis() - BASE_TIME;
    }
}
