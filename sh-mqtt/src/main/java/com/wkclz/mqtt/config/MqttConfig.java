package com.wkclz.mqtt.config;

import com.wkclz.mqtt.exception.MqttRemoteException;
import com.wkclz.tool.utils.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Slf4j
@Component
public class MqttConfig {

    @Value("${shrimp.cloud.mqtt.enabled:true}")
    private String enabled;

    // 自定义
    @Value("${shrimp.cloud.mqtt.username:}")
    private String username;
    @Value("${shrimp.cloud.mqtt.password:}")
    private String password;
    @Value("${shrimp.cloud.mqtt.ca-path:}")
    private String caPath;

    // 公共
    @Value("${shrimp.cloud.mqtt.end-point:}")
    private String endPoint;
    @Value("${shrimp.cloud.mqtt.client-id-prefix:}")
    private String clientIdPrefix;
    @Value("${shrimp.cloud.mqtt.keep-alive-interval:60}")
    private Integer keepAliveInterval;
    @Value("${shrimp.cloud.mqtt.keep-alive-task:0}")
    private Integer keepAliveTask;

    // 阿里云
    @Value("${shrimp.cloud.mqtt.instance-id:}")
    private String instanceId;
    @Value("${shrimp.cloud.mqtt.access-key:}")
    private String accessKey;
    @Value("${shrimp.cloud.mqtt.secret-key:}")
    private String secretKey;

    private MqttAsyncClient mqttClient;

    @Bean
    public MqttAsyncClient mqttClient() {
        if (!"true".equals(enabled)) {
            log.warn("mqtt is disabled!");
            return null;
        }

        if (StringUtils.isBlank(endPoint)) {
            log.warn("mqtt: endpoint is empty!");
            return null;
        }

        String prefix = getClientIdPrefix();
        if (StringUtils.isBlank(prefix)) {
            prefix = "server";
        }
        if (keepAliveInterval == null || keepAliveInterval < 0) {
            keepAliveInterval = 60;
        }

        String clientId = prefix + "@" + NetworkUtil.getServerIp();
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            mqttClient = new MqttAsyncClient(getEndPoint(), clientId, persistence);
            // MQTT 连接选项
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(getUsername());
            connOpts.setPassword(getPassword().toCharArray());
            // 保留会话: 不需要保留
            connOpts.setCleanSession(true);
            // 建立连接
            connOpts.setConnectionTimeout(0);
            connOpts.setAutomaticReconnect(true);
            connOpts.setKeepAliveInterval(keepAliveInterval);

            // CA 证书
            if (endPoint.startsWith("ssl") && StringUtils.isNotBlank(caPath)) {
                ClassPathResource resource = new ClassPathResource(caPath);
                InputStream is = resource.getInputStream();
                SSLSocketFactory factory = getSingleSocketFactory(is);
                connOpts.setSocketFactory(factory);
            }

            mqttClient.setCallback(new MqttReconnectCallback());

            log.info("Connecting to broker: " + getEndPoint());
            mqttClient.connect(connOpts);

            log.info("Connected");
        } catch (MqttException me) {
            String msg = String.format("reason: %s, msg: %s, loc: %s, cause: %s",
                me.getReasonCode(), me.getMessage(), me.getLocalizedMessage(), me.getCause());
            throw MqttRemoteException.error(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return mqttClient;
    }

    class MqttReconnectCallback implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverUri) {
            if (reconnect) {
                log.warn("MqttReconnectCallback: reconnect connectComplete");
                MqttSubscribe.subscribeTopics(mqttClient);
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            log.error("MqttReconnectCallback: connectionLost");
            log.error(cause.getMessage(), cause);
        }
        @Override
        public void messageArrived(String topic, MqttMessage message) {
        }
        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaPath() {
        return caPath;
    }

    public void setCaPath(String caPath) {
        this.caPath = caPath;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getClientIdPrefix() {
        return clientIdPrefix;
    }

    public void setClientIdPrefix(String clientIdPrefix) {
        this.clientIdPrefix = clientIdPrefix;
    }

    public Integer getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(Integer keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public Integer getKeepAliveTask() {
        return keepAliveTask;
    }

    public void setKeepAliveTask(Integer keepAliveTask) {
        this.keepAliveTask = keepAliveTask;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    // 单向认证
    private static SSLSocketFactory getSingleSocketFactory(InputStream is) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            X509Certificate caCert = null;
            BufferedInputStream bis = new BufferedInputStream(is);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            while (bis.available() > 0) {
                caCert = (X509Certificate) cf.generateCertificate(bis);
            }
            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("cert-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, tmf.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}