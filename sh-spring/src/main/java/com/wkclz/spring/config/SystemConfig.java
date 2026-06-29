package com.wkclz.spring.config;

import com.wkclz.core.exception.SystemException;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 系统配置类，支持敏感配置的加密存储与自动解密。
 *
 * <h3>加密模式</h3>
 * <ol>
 *   <li><b>RSA 模式（推荐）</b>：私钥存于 PKCS12 密钥库文件，密钥库密码通过环境变量注入。
 *       攻击者需同时获取密钥库文件 + 密钥库密码 + 配置文件才能解密，三重防护。</li>
 *   <li><b>AES 模式</b>：对称密钥通过环境变量注入，适合简单场景。</li>
 *   <li><b>明文模式</b>：不加密，直接使用明文值（仅限开发环境）。</li>
 * </ol>
 *
 * <h3>RSA 模式配置示例</h3>
 * <pre>
 * # application.yml
 * sh:
 *   config:
 *     keystore:
 *       path: /path/to/config-decrypt.p12
 *       alias: config-decrypt
 *
 * # 环境变量注入密钥库密码（不在配置文件中）
 * export SH_CONFIG_KEYSTORE_PASSWORD=your-keystore-password
 *
 * # 敏感值使用 ENC(...) 格式
 * alarm:
 *   email:
 *     password: ENC(xxx.yyy)
 * </pre>
 *
 * @author shrimp @ 2019-07-21 23:46:08
 */
@Data
@Configuration
public class SystemConfig {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfig.class);

    // ==================== RSA 密钥库配置（推荐） ====================

    /**
     * PKCS12 密钥库文件路径
     */
    @Value("${sh.config.keystore.path:}")
    private String configKeystorePath;

    /**
     * 密钥库中私钥的别名
     */
    @Value("${sh.config.keystore.alias:config-decrypt}")
    private String configKeystoreAlias;

    /**
     * 密钥库密码，优先从环境变量 SH_CONFIG_KEYSTORE_PASSWORD 注入
     */
    @Value("${SH_CONFIG_KEYSTORE_PASSWORD:${sh.config.keystore.password:}}")
    private String configKeystorePassword;

    // ==================== AES 对称密钥配置（简单场景） ====================

    /**
     * AES 解密密钥，优先从环境变量 SH_CONFIG_DECRYPT_AES_KEY 注入
     */
    @Value("${SH_CONFIG_DECRYPT_AES_KEY:${sh.config.decrypt-aes-key:}}")
    private String configDecryptAesKey;

    // ==================== 通用配置 ====================

    @Value("${spring.application.name:APP}")
    private String applicationName;
    @Value("${spring.profiles.active:dev}")
    private String profiles;

    // ==================== 告警邮件配置 ====================

    @Value("${alarm.email.enabled:false}")
    private boolean alarmEmailEnabled;
    @Value("${alarm.email.host:smtp.exmail.qq.com}")
    private String alarmEmailHost;
    @Value("${alarm.email.from:alarm@wkclz.com}")
    private String alarmEmailFrom;
    // 请勿在此处硬编码密码，务必使用 ENC(...) 格式加密存储
    @Value("${alarm.email.password:}")
    private String alarmEmailPassword;
    @Value("${alarm.email.to:admin@wkclz.com}")
    private String alarmEmailTo;

    // ==================== 初始化 ====================

    @PostConstruct
    public void initSensitiveConfig() {
        boolean rsaMode = configKeystorePath != null && !configKeystorePath.isEmpty();

        if (rsaMode) {
            // RSA 模式：从密钥库加载私钥
            logger.info("使用 RSA 密钥库模式解密敏感配置，密钥库: {}", configKeystorePath);
            String rsaPrivateKey = SensitiveConfigEncryptor.loadPrivateKeyBase64(
                configKeystorePath, configKeystoreAlias, configKeystorePassword);
            alarmEmailPassword = SensitiveConfigDecryptor.decryptRsa(alarmEmailPassword, rsaPrivateKey);
        } else if (configDecryptAesKey != null && !configDecryptAesKey.isEmpty()) {
            // AES 模式：使用对称密钥
            checkAesKeySourceSecurity();
            alarmEmailPassword = SensitiveConfigDecryptor.decrypt(alarmEmailPassword, configDecryptAesKey);
        } else {
            // 明文模式：无解密
            if (SensitiveConfigDecryptor.isEncrypted(alarmEmailPassword)) {
                throw SystemException.of("敏感配置已加密但未配置解密密钥，" +
                    "请配置 RSA 密钥库(sh.config.keystore.path)或 AES 密钥(sh.config.decrypt-aes-key)");
            }
            logger.info("未配置解密密钥，敏感配置将以明文模式运行（仅建议开发环境使用）");
        }
    }

    /**
     * 检测 AES 密钥来源安全性
     */
    private void checkAesKeySourceSecurity() {
        String envValue = System.getenv("SH_CONFIG_DECRYPT_AES_KEY");
        if (envValue != null && !envValue.isEmpty()) {
            return;
        }
        String jvmValue = System.getProperty("sh.config.decrypt-aes-key");
        if (jvmValue != null && !jvmValue.isEmpty()) {
            return;
        }
        logger.warn("安全警告: AES 解密密钥疑似来自配置文件，密钥与密文同处一文件不安全！" +
            "建议使用 RSA 密钥库模式，或通过环境变量 SH_CONFIG_DECRYPT_AES_KEY 注入密钥。");
    }

}
