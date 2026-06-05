package com.wkclz.spring.config;

import com.wkclz.core.exception.SystemException;
import com.wkclz.tool.tools.AesTool;
import com.wkclz.tool.tools.RsaTool;

/**
 * 敏感配置解密工具类，支持两种模式：
 * <ul>
 *   <li>AES 对称解密：密钥通过环境变量注入，适合简单场景</li>
 *   <li>RSA 混合解密：私钥存于 PKCS12 密钥库，密钥库密码通过环境变量注入，安全性更高</li>
 * </ul>
 * <p>
 * RSA 混合加密格式：ENC(base64(rsa加密的aes密钥).base64(aes加密的数据))
 * </p>
 */
public final class SensitiveConfigDecryptor {

    public static final String ENC_PREFIX = "ENC(";
    public static final String ENC_SUFFIX = ")";
    private static final String HYBRID_SEPARATOR = ".";

    private SensitiveConfigDecryptor() {
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX) && value.endsWith(ENC_SUFFIX);
    }

    /**
     * AES 对称解密（向后兼容）
     */
    public static String decrypt(String value, String aesKey) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!isEncrypted(value)) {
            return value;
        }
        if (aesKey == null || aesKey.isEmpty()) {
            throw SystemException.of("解密密钥未配置，无法解密 ENC 格式的配置值");
        }
        String ciphertext = value.substring(ENC_PREFIX.length(), value.length() - ENC_SUFFIX.length());
        try {
            return AesTool.decrypt(ciphertext, aesKey);
        } catch (Exception e) {
            throw SystemException.of("配置值解密失败: {}", e.getMessage());
        }
    }

    /**
     * RSA 混合解密（信封加密）
     * <p>
     * 流程：拆分密文 → RSA 解密 AES 密钥 → AES 解密数据
     * </p>
     *
     * @param value              ENC(...) 格式的加密值
     * @param rsaPrivateKeyBase64 Base64 编码的 RSA 私钥（从密钥库加载）
     */
    public static String decryptRsa(String value, String rsaPrivateKeyBase64) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!isEncrypted(value)) {
            return value;
        }
        if (rsaPrivateKeyBase64 == null || rsaPrivateKeyBase64.isEmpty()) {
            throw SystemException.of("RSA 私钥未配置，无法解密 ENC 格式的配置值");
        }
        String content = value.substring(ENC_PREFIX.length(), value.length() - ENC_SUFFIX.length());
        try {
            int separatorIndex = content.indexOf(HYBRID_SEPARATOR);
            if (separatorIndex < 0) {
                throw SystemException.of("RSA 混合加密格式错误，缺少分隔符 '.'");
            }
            String encryptedAesKey = content.substring(0, separatorIndex);
            String encryptedData = content.substring(separatorIndex + 1);

            // RSA 解密 AES 密钥
            String aesKey = RsaTool.decryptByPrivateKey(encryptedAesKey, rsaPrivateKeyBase64);
            // AES 解密数据
            return AesTool.decrypt(encryptedData, aesKey);
        } catch (SystemException e) {
            throw e;
        } catch (Exception e) {
            throw SystemException.of("RSA 配置值解密失败: {}", e.getMessage());
        }
    }
}
