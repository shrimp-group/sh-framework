package com.wkclz.redis.queue;

import com.wkclz.redis.helper.Redishelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 消息队列实现类
 *
 * @param <T> 消息类型
 */
@Slf4j
@Component
public class RedisMessageQueueImpl<T> implements RedisMessageQueue<T> {
    
    @Autowired
    private Redishelper redishelper;
    
    private final String queueName;
    private final Class<T> messageType;
    
    /**
     * 构造方法
     *
     * @param queueName 队列名称
     * @param messageType 消息类型
     */
    public RedisMessageQueueImpl(String queueName, Class<T> messageType) {
        if (queueName == null || queueName.isEmpty()) {
            throw new IllegalArgumentException("Queue name cannot be null or empty");
        }
        if (messageType == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }
        this.queueName = queueName;
        this.messageType = messageType;
    }
    
    /**
     * 获取队列的 Redis 键
     *
     * @return Redis 键
     */
    private String getQueueKey() {
        return "queue:" + queueName;
    }
    
    @Override
    public boolean sendMessage(T message) {
        if (message == null) {
            return false;
        }
        
        try {
            // 直接使用 Redishelper 保存对象，利用 RedisTemplate 的序列化机制
            redishelper.lPush(getQueueKey(), message);
            return true;
        } catch (Exception e) {
            log.error("Redis sendMessage error: ", e);
            return false;
        }
    }
    
    @Override
    public T receiveMessage() throws InterruptedException {
        return receiveMessage(0, TimeUnit.SECONDS);
    }
    
    @Override
    public T receiveMessageNonBlocking() {
        try {
            // 使用 Redis 的 LPOP 命令从队列头部获取消息（非阻塞）
            Object messageObj = redishelper.lPop(getQueueKey());
            if (messageObj != null) {
                // 直接转换类型，利用 RedisTemplate 的反序列化机制
                return messageType.cast(messageObj);
            }
        } catch (Exception e) {
            log.error("Redis receiveMessageNonBlocking error: ", e);
        }
        return null;
    }
    
    @Override
    public T receiveMessage(long timeout, TimeUnit timeUnit) throws InterruptedException {
        try {
            // 使用 Redis 的 BLPOP 命令从队列头部获取消息（阻塞式）
            Object messageObj = redishelper.bLPop(getQueueKey(), timeout, timeUnit);
            if (messageObj != null) {
                // 直接转换类型，利用 RedisTemplate 的反序列化机制
                return messageType.cast(messageObj);
            }
        } catch (Exception e) {
            log.error("Redis receiveMessage error: ", e);
        }
        return null;
    }
    
    @Override
    public long getMessageCount() {
        try {
            // 使用 Redis 的 LLEN 命令获取队列长度
            return redishelper.lLen(getQueueKey());
        } catch (Exception e) {
            log.error("Redis getMessageCount error: ", e);
            return 0;
        }
    }
    
    @Override
    public void clear() {
        try {
            // 使用 Redis 的 DEL 命令清空队列
            redishelper.delete(getQueueKey());
        } catch (Exception e) {
            log.error("Redis clearQueue error: ", e);
        }
    }
}