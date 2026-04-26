package com.wkclz.redis.queue;

import com.wkclz.redis.helper.RedisHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Redis 消息队列管理器
 * 用于统一管理多个消息队列，并支持消息的订阅和消费
 */
@Slf4j
@Component
public class RedisMessageQueueManager {
    
    @Autowired
    private RedisHelper redisHelper;
    
    // 线程池，用于消息消费（有界线程池，核心线程数4，最大线程数16，空闲60秒回收）
    private final ExecutorService executorService = new ThreadPoolExecutor(
        4, 16, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
    
    // 消息队列映射（队列名称 -> 消息队列实例）
    private final Map<String, RedisMessageQueue<?>> messageQueueMap = new ConcurrentHashMap<>();
    
    // 消息监听器映射（队列名称 -> 消息监听器实例）
    private final Map<String, MessageListener<?>> messageListenerMap = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建消息队列
     *
     * @param queueName 队列名称
     * @param messageType 消息类型
     * @param <T> 消息类型
     * @return 消息队列实例
     */
    @SuppressWarnings("unchecked")
    public <T> RedisMessageQueue<T> getQueue(String queueName, Class<T> messageType) {
        if (queueName == null || queueName.isEmpty() || messageType == null) {
            throw new IllegalArgumentException("Queue name and message type cannot be null or empty");
        }
        
        // 检查是否已经存在该队列
        RedisMessageQueue<?> queue = messageQueueMap.get(queueName);
        if (queue == null) {
            // 创建新的消息队列实例
            RedisMessageQueueImpl<T> newQueue = new RedisMessageQueueImpl<>(queueName, messageType);
            RedisMessageQueue<?> existing = messageQueueMap.putIfAbsent(queueName, newQueue);
            queue = (existing != null) ? existing : newQueue;
        }
        return (RedisMessageQueue<T>) queue;
    }
    
    /**
     * 订阅消息队列
     *
     * @param queueName 队列名称
     * @param listener 消息监听器
     * @param <T> 消息类型
     * @return 是否订阅成功
     */
    public <T> boolean subscribe(String queueName, MessageListener<T> listener) {
        if (queueName == null || queueName.isEmpty() || listener == null) {
            return false;
        }
        
        // 检查是否已经订阅该队列（putIfAbsent 原子操作）
        if (messageListenerMap.putIfAbsent(queueName, listener) != null) {
            log.warn("Queue {} is already subscribed", queueName);
            return false;
        }
        
        // 获取或创建消息队列
        RedisMessageQueue<T> queue = getQueue(queueName, listener.getMessageType());
        
        // 启动消费线程
        startConsumeThread(queueName, queue, listener);
        
        return true;
    }
    
    /**
     * 取消订阅消息队列
     *
     * @param queueName 队列名称
     * @return 是否取消订阅成功
     */
    public boolean unsubscribe(String queueName) {
        if (queueName == null || queueName.isEmpty()) {
            return false;
        }
        // ConcurrentHashMap 的 remove 是线程安全的
        return messageListenerMap.remove(queueName) != null;
    }
    
    /**
     * 发送消息到指定队列
     *
     * @param queueName 队列名称
     * @param message 消息内容
     * @param <T> 消息类型
     * @return 是否发送成功
     */
    @SuppressWarnings("unchecked")
    public <T> boolean sendMessage(String queueName, T message) {
        if (queueName == null || queueName.isEmpty() || message == null) {
            return false;
        }
        
        // 获取或创建消息队列
        RedisMessageQueue<T> queue = getQueue(queueName, (Class<T>) message.getClass());
        
        // 发送消息
        return queue.sendMessage(message);
    }
    
    /**
     * 启动消费线程
     *
     * @param queueName 队列名称
     * @param queue 消息队列
     * @param listener 消息监听器
     * @param <T> 消息类型
     */
    private <T> void startConsumeThread(String queueName, RedisMessageQueue<T> queue, MessageListener<T> listener) {
        executorService.submit(() -> {
            log.info("Start consuming messages from queue: {}", queueName);
            
            try {
                while (messageListenerMap.containsKey(queueName)) {
                    // 从队列中接收消息（阻塞式）
                    T message = queue.receiveMessage();
                    if (message != null) {
                        try {
                            // 调用业务方的消息处理方法
                            listener.onMessage(message);
                        } catch (Exception e) {
                            log.error("Message listener error for queue {}: ", queueName, e);
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.info("Consume thread interrupted for queue: {}", queueName);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Consume thread error for queue {}: ", queueName, e);
            }
            
            log.info("Stop consuming messages from queue: {}", queueName);
        });
    }
}