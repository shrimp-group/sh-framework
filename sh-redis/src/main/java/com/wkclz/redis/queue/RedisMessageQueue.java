package com.wkclz.redis.queue;

import java.util.concurrent.TimeUnit;

/**
 * Redis 消息队列接口
 *
 * @param <T> 消息类型
 */
public interface RedisMessageQueue<T> {
    
    /**
     * 发送消息到队列
     *
     * @param message 消息内容
     * @return 是否发送成功
     */
    boolean sendMessage(T message);
    
    /**
     * 从队列中接收消息（阻塞式）
     *
     * @return 消息内容，如果队列中没有消息则阻塞等待
     * @throws InterruptedException 如果线程被中断
     */
    T receiveMessage() throws InterruptedException;
    
    /**
     * 从队列中接收消息（非阻塞式）
     *
     * @return 消息内容，如果队列中没有消息则返回 null
     */
    T receiveMessageNonBlocking();
    
    /**
     * 从队列中接收消息（阻塞式，带超时时间）
     *
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @return 消息内容，如果在超时时间内没有收到消息则返回 null
     * @throws InterruptedException 如果线程被中断
     */
    T receiveMessage(long timeout, TimeUnit timeUnit) throws InterruptedException;
    
    /**
     * 获取队列中的消息数量
     *
     * @return 消息数量
     */
    long getMessageCount();
    
    /**
     * 清空队列
     */
    void clear();
}