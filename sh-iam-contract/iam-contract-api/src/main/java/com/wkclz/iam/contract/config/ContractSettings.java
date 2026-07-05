package com.wkclz.iam.contract.config;

import lombok.Getter;

/**
 * 契约层静态配置持有器
 * 由 IamContractAutoConfig 在启动时通过 @PostConstruct 初始化
 * 供契约接口的 default 方法（如 AkSignContract.sign()）访问配置
 * default 方法无法访问 Spring 上下文，因此通过静态持有器桥接
 *
 * @author shrimp
 */
@Getter
public final class ContractSettings {

    private static String appId;
    private static String appSecret;
    private static String publicKey;
    private static String serverUrl;
    private static String jwtSecretKey;

    private ContractSettings() {
    }

    public static void setAppId(String appId) {
        ContractSettings.appId = appId;
    }

    public static void setAppSecret(String appSecret) {
        ContractSettings.appSecret = appSecret;
    }

    public static void setPublicKey(String publicKey) {
        ContractSettings.publicKey = publicKey;
    }

    public static void setServerUrl(String serverUrl) {
        ContractSettings.serverUrl = serverUrl;
    }

    public static void setJwtSecretKey(String jwtSecretKey) {
        ContractSettings.jwtSecretKey = jwtSecretKey;
    }
}
