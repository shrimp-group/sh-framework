package com.wkclz.mqtt.demo;

import com.wkclz.mqtt.annotation.MqttController;
import com.wkclz.mqtt.annotation.MqttTopicMapping;
import com.wkclz.mqtt.bean.MqttHexMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@MqttController("keepalive")
public class MqttConsumerDemo {

    private static final Logger logger = LoggerFactory.getLogger(MqttConsumerDemo.class);

    @MqttTopicMapping("breath")
    public void breath(MqttHexMsg msg) {
        String data = new String(msg.getPayload(), StandardCharsets.UTF_8);
        logger.info("breath message: {}", data);
    }

}