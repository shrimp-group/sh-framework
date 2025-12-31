package com.wkclz.pring.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author shrimp @ 2019-07-21 23:46:08
 */

@Data
@Configuration
public class SystemConfig {

    @Value("${spring.application.name:APP}")
    private String applicationName;
    @Value("${spring.profiles.active:dev}")
    private String profiles;

    // 配置解密

    @Value("${sh.config.decrypt-aes-key:}")
    private String configDecryptAesKey;


    // 告警邮件发送

    @Value("${alarm.email.enabled:false}")
    private boolean alarmEmailEnabled;
    @Value("${alarm.email.host:smtp.exmail.qq.com}")
    private String alarmEmailHost;
    @Value("${alarm.email.from:alarm@wkclz.com}")
    private String alarmEmailFrom;
    @Value("${alarm.email.password:your_password}")
    private String alarmEmailPassword;
    @Value("${alarm.email.to:admin@wkclz.com}")
    private String alarmEmailTo;

}
