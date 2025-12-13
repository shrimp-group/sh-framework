package com.wkclz.redis.queue;

import com.wkclz.redis.helper.Redishelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Redis 消息队列管理器
 * 用于统一管理多个消息队列，并支持消息的订阅和消费
 */
@Slf4j
@Component
public class RedisMessageQueueManager {
    
    @Autowired
    private Redishelper redishelper;
    
    // 线程池，用于消息消费
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 消息队列映射（队列名称 -> 消息队列实例）
    private final Map<String, RedisMessageQueue<?>> messageQueueMap = new HashMap<>();
    
    // 消息监听器映射（队列名称 -> 消息监听器实例）
    private final Map<String, MessageListener<?>> messageListenerMap = new HashMap<>();
    
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
        
        synchronized (messageQueueMap) {
            // 检查是否已经存在该队列
            RedisMessageQueue<?> queue = messageQueueMap.get(queueName);
            if (queue == null) {
                // 创建新的消息队列实例
                queue = new RedisMessageQueueImpl<>(queueName, messageType);
                messageQueueMap.put(queueName, queue);
            }
            return (RedisMessageQueue<T>) queue;
        }
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
        
        synchronized (messageListenerMap) {
            // 检查是否已经订阅该队列
            if (messageListenerMap.containsKey(queueName)) {
                log.warn("Queue {} is already subscribed", queueName);
                return false;
            }
            
            // 获取或创建消息队列
            RedisMessageQueue<T> queue = getQueue(queueName, listener.getMessageType());
            
            // 保存消息监听器
            messageListenerMap.put(queueName, listener);
            
            // 启动消费线程
            startConsumeThread(queueName, queue, listener);
            
            return true;
        }
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
        
        synchronized (messageListenerMap) {
            // 移除消息监听器
            return messageListenerMap.remove(queueName) != null;
        }
    }
    
    /**
     * 发送消息到指定队列
     *
     * @param queueName 队列名称
     * @param message 消息内容
     * @param <T> 消息类型
     * @return 是否发送成功
     */
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