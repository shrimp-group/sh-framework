package com.wkclz.redis.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Redis 消息队列使用示例
 */
@Component
public class RedisMessageQueueExample {
    
    @Autowired
    private RedisMessageQueueManager messageQueueManager;
    
    /**
     * 初始化示例
     */
    @PostConstruct
    public void init() {
        // 1. 订阅订单队列
        messageQueueManager.subscribe("order_queue", new MessageListener<OrderMessage>() {
            @Override
            public void onMessage(OrderMessage message) {
                System.out.println("Received order message: " + message);
                // 处理订单逻辑
                processOrder(message);
            }
            
            @Override
            public Class<OrderMessage> getMessageType() {
                return OrderMessage.class;
            }
        });
        
        // 2. 订阅日志队列
        messageQueueManager.subscribe("log_queue", new MessageListener<LogMessage>() {
            @Override
            public void onMessage(LogMessage message) {
                System.out.println("Received log message: " + message);
                // 处理日志逻辑
                processLog(message);
            }
            
            @Override
            public Class<LogMessage> getMessageType() {
                return LogMessage.class;
            }
        });
        
        // 3. 发送示例消息
        sendExampleMessages();
    }
    
    /**
     * 发送示例消息
     */
    private void sendExampleMessages() {
        // 发送订单消息
        OrderMessage orderMessage = new OrderMessage("order_123456", "user_789", "PRODUCT_001", 100.0);
        messageQueueManager.sendMessage("order_queue", orderMessage);
        
        // 发送日志消息
        LogMessage logMessage = new LogMessage("INFO", "User login", "user_789", System.currentTimeMillis());
        messageQueueManager.sendMessage("log_queue", logMessage);
        
        // 另一种方式：直接获取队列并发送消息
        RedisMessageQueue<OrderMessage> orderQueue = messageQueueManager.getQueue("order_queue", OrderMessage.class);
        OrderMessage orderMessage2 = new OrderMessage("order_123457", "user_790", "PRODUCT_002", 200.0);
        orderQueue.sendMessage(orderMessage2);
    }
    
    /**
     * 处理订单
     */
    private void processOrder(OrderMessage message) {
        // 实际业务逻辑
        System.out.println("Processing order: " + message.getOrderId());
    }
    
    /**
     * 处理日志
     */
    private void processLog(LogMessage message) {
        // 实际业务逻辑
        System.out.println("Processing log: " + message.getMessage());
    }
    
    // 订单消息类
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderMessage {
        private String orderId;
        private String userId;
        private String productId;
        private double amount;
    }
    
    // 日志消息类
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LogMessage {
        private String level;
        private String message;
        private String userId;
        private long timestamp;
    }
}