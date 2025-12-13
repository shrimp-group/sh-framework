package com.wkclz.redis.queue;

/**
 * 消息监听器接口
 * 业务方需要实现此接口来处理消息
 *
 * @param <T> 消息类型
 */
public interface MessageListener<T> {
    
    /**
     * 处理消息的方法
     *
     * @param message 消息内容
     */
    void onMessage(T message);
    
    /**
     * 获取消息类型
     *
     * @return 消息类型的 Class 对象
     */
    Class<T> getMessageType();
}