package com.wkclz.mqtt.client;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wkclz.mqtt.enums.Qos;
import com.wkclz.mqtt.exception.MqttBeansException;
import com.wkclz.mqtt.exception.MqttSendException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author wangkaicun
 * @date 2022-04-08
 */
@Slf4j
@Component
public class MqttProducer {

    @Autowired(required = false)
    private MqttAsyncClient mqttAsyncClient;

    // 共享的调度线程池，避免每次调用 sendDelay 都创建新线程池导致资源泄漏
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = new ScheduledThreadPoolExecutor(
        2,
        ThreadUtil.newNamedThreadFactory("mqtt-delay-", null, false),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );


    public void sendDelay(String topic, String msg, Integer delay) {
        sendDelay(topic, Arrays.asList(msg), delay, Qos.QOS_1);
    }
    public void sendDelay(String topic, String msg, Qos qos) {
        sendDelay(topic, Arrays.asList(msg), 500, qos);
    }
    public void sendDelay(String topic, String msg, Integer delay, Qos qos) {
        sendDelay(topic, Arrays.asList(msg), delay, qos);
    }

    public void sendDelay(String topic, List<String> msgs, Integer delay) {
        sendDelay(topic, msgs, delay, Qos.QOS_1);
    }
    public void sendDelay(String topic, List<String> msgs, Qos qos) {
        sendDelay(topic, msgs, 500, qos);
    }
    public void sendDelay(String topic, List<String> msgs, Integer delay, Qos qos) {
        if (topic == null || CollectionUtils.isEmpty(msgs)) {
            return;
        }
        if (delay == null) {
            delay = 500;
        }
        if (qos == null) {
            qos = Qos.QOS_1;
        }
        Qos finalQos = qos;
        long finalDelay = delay;
        try {
            long totalDelay = 0;
            for (String msg : msgs) {
                totalDelay += finalDelay;
                String finalMsg = msg;
                long scheduleDelay = totalDelay;
                SCHEDULED_EXECUTOR.schedule(() -> {
                    try {
                        log.info("mqtt sent msg, topic:{}", topic);
                        byte[] bytes = finalMsg.getBytes(StandardCharsets.UTF_8);
                        sendMsg(topic, bytes, finalQos);
                    } catch (Exception e) {
                        log.error("mqtt send delay error, topic:{}, error: {}", topic, e.getMessage(), e);
                    }
                }, scheduleDelay, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error("mqtt sendDelay schedule error: {}", e.getMessage(), e);
        }
    }

    public void send(String topic, Object msg) {
        if (msg == null) {
            return;
        }
        send(topic, msg, Qos.QOS_1);
    }

    public void send(String topic, Object msg, Qos qos) {
        if (msg == null) {
            return;
        }
        String json = JSONObject.toJSONString(msg);
        log.info("mqtt sent msg, topic:{}", topic);
        log.debug("mqtt sent msg detail, topic:{}, message: {}", topic, json);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        sendMsg(topic, bytes, qos);
    }

    public void send(String topic, byte[] msg) {
        if (msg == null) {
            return;
        }
        send(topic, msg, Qos.QOS_1);
    }

    public void send(String topic, byte[] msg, Qos qos) {
        if (msg == null) {
            return;
        }
        sendMsg(topic, msg, qos);
    }

    private void sendMsg(String topic, byte[] msg, Qos qos) {
        if (mqttAsyncClient == null) {
            throw new MqttBeansException("mqtt is disabled!");
        }
        MqttMessage message = new MqttMessage(msg);
        message.setQos(qos.getValue());
        try {
            mqttAsyncClient.publish(topic, message);
        } catch (MqttException e) {
            log.error(e.getMessage(), e);
        }
    }

}
