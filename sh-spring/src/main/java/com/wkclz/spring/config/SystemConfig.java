package com.wkclz.spring.config;

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
    @Value("${alarm.email.host:}")
    private String alarmEmailHost;
    @Value("${alarm.email.from:}")
    private String alarmEmailFrom;
    // 请勿在此处硬编码密码，务必通过环境变量或配置中心注入
    @Value("${alarm.email.password:}")
    private String alarmEmailPassword;
    @Value("${alarm.email.to:}")
    private String alarmEmailTo;

}
