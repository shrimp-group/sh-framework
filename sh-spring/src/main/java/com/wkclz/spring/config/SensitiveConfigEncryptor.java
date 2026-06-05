package com.wkclz.spring.config;

import com.wkclz.core.exception.SystemException;
import com.wkclz.tool.tools.AesTool;
import com.wkclz.tool.tools.Base64Tool;
import com.wkclz.tool.tools.RsaTool;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;

/**
 * 配置值加密工具类，支持 AES 和 RSA 两种模式，并提供密钥库管理命令。
 *
 * <h3>使用方式</h3>
 * <pre>
 * # AES 模式（简单场景）
 * java SensitiveConfigEncryptor aes &lt;plaintext&gt; &lt;aesKey&gt;
 *
 * # RSA 模式（推荐，安全性更高）
 * java SensitiveConfigEncryptor rsa &lt;plaintext&gt; &lt;rsaPublicKeyBase64&gt;
 *
 * # 生成 RSA 密钥对并导入密钥库
 * java SensitiveConfigEncryptor keygen &lt;keystorePath&gt; &lt;alias&gt; &lt;password&gt; [keySize]
 *
 * # 从密钥库导出公钥（用于加密配置值）
 * java SensitiveConfigEncryptor export-pub &lt;keystorePath&gt; &lt;alias&gt; &lt;password&gt;
 * </pre>
 */
public final class SensitiveConfigEncryptor {

    private static final String HYBRID_SEPARATOR = ".";
    private static final String KEYSTORE_TYPE = "PKCS12";

    private SensitiveConfigEncryptor() {
    }

    // ==================== AES 模式 ====================

    /**
     * AES 对称加密，返回 ENC(密文) 格式
     */
    public static String encrypt(String plaintext, String aesKey) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (aesKey == null || aesKey.isEmpty()) {
            throw SystemException.of("加密密钥不能为空");
        }
        String ciphertext = AesTool.encrypt(plaintext, aesKey);
        return SensitiveConfigDecryptor.ENC_PREFIX + ciphertext + SensitiveConfigDecryptor.ENC_SUFFIX;
    }

    // ==================== RSA 混合加密模式 ====================

    /**
     * RSA 混合加密（信封加密），返回 ENC(rsa加密的aes密钥.aes加密的数据) 格式
     * <p>
     * 流程：生成随机 AES 密钥 → AES 加密数据 → RSA 加密 AES 密钥 → 拼接
     * </p>
     *
     * @param plaintext           明文
     * @param rsaPublicKeyBase64  Base64 编码的 RSA 公钥
     */
    public static String encryptRsa(String plaintext, String rsaPublicKeyBase64) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (rsaPublicKeyBase64 == null || rsaPublicKeyBase64.isEmpty()) {
            throw SystemException.of("RSA 公钥不能为空");
        }
        // 1. 生成随机 AES 密钥（UUID 作为种子）
        String aesKey = UUID.randomUUID().toString().replace("-", "");

        // 2. AES 加密数据
        String encryptedData = AesTool.encrypt(plaintext, aesKey);

        // 3. RSA 加密 AES 密钥
        String encryptedAesKey = RsaTool.encryptByPublicKey(aesKey, rsaPublicKeyBase64);

        // 4. 拼接为 ENC(encryptedAesKey.encryptedData)
        return SensitiveConfigDecryptor.ENC_PREFIX + encryptedAesKey + HYBRID_SEPARATOR + encryptedData + SensitiveConfigDecryptor.ENC_SUFFIX;
    }

    // ==================== 密钥库管理 ====================

    /**
     * 生成 RSA 密钥对并存储到 PKCS12 密钥库
     *
     * @param keystorePath 密钥库文件路径
     * @param alias        密钥别名
     * @param password     密钥库密码
     * @param keySize      RSA 密钥长度（1024/2048/4096）
     */
    public static void generateKeyPairToKeystore(String keystorePath, String alias, String password, int keySize) {
        try {
            // 生成 RSA 密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();

            // 生成自签名证书
            X509Certificate certificate = generateSelfSignedCertificate(keyPair);

            // 创建或加载密钥库
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            File keystoreFile = new File(keystorePath);
            if (keystoreFile.exists()) {
                try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                    keyStore.load(fis, password.toCharArray());
                }
            } else {
                keyStore.load(null, null);
            }

            // 存储密钥对
            keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), new Certificate[]{certificate});

            // 保存密钥库
            try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
                keyStore.store(fos, password.toCharArray());
            }

            // 输出公钥
            String publicKeyBase64 = Base64Tool.base64Encode(keyPair.getPublic().getEncoded());
            System.out.println("RSA 密钥对已生成并保存到密钥库: " + keystorePath);
            System.out.println("别名: " + alias);
            System.out.println("密钥长度: " + keySize);
            System.out.println();
            System.out.println("公钥（用于加密配置值）:");
            System.out.println(publicKeyBase64);
        } catch (SystemException e) {
            throw e;
        } catch (Exception e) {
            throw SystemException.of("生成密钥对失败: {}", e.getMessage());
        }
    }

    /**
     * 从 PKCS12 密钥库导出公钥
     *
     * @param keystorePath 密钥库文件路径
     * @param alias        密钥别名
     * @param password     密钥库密码
     * @return Base64 编码的公钥
     */
    public static String exportPublicKey(String keystorePath, String alias, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keyStore.load(fis, password.toCharArray());
            }
            Certificate cert = keyStore.getCertificate(alias);
            if (cert == null) {
                throw SystemException.of("密钥库中未找到别名 '{}' 对应的证书", alias);
            }
            return Base64Tool.base64Encode(cert.getPublicKey().getEncoded());
        } catch (SystemException e) {
            throw e;
        } catch (Exception e) {
            throw SystemException.of("导出公钥失败: {}", e.getMessage());
        }
    }

    /**
     * 从 PKCS12 密钥库加载私钥（Base64 编码）
     *
     * @param keystorePath 密钥库文件路径
     * @param alias        密钥别名
     * @param password     密钥库密码
     * @return Base64 编码的私钥
     */
    public static String loadPrivateKeyBase64(String keystorePath, String alias, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keyStore.load(fis, password.toCharArray());
            }
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            if (privateKey == null) {
                throw SystemException.of("密钥库中未找到别名 '{}' 对应的私钥", alias);
            }
            return Base64Tool.base64Encode(privateKey.getEncoded());
        } catch (SystemException e) {
            throw e;
        } catch (Exception e) {
            throw SystemException.of("加载私钥失败: {}", e.getMessage());
        }
    }

    /**
     * 使用 BouncyCastle 生成自签名 X.509 证书（仅用于配置加密场景，不用于 TLS）
     */
    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now);
        Date notAfter = new Date(now + 365L * 24 * 60 * 60 * 1000 * 25); // 25 年

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            new javax.security.auth.x500.X500Principal("CN=sh-framework-config,OU=config,O=sh-framework,L=default,ST=default,C=CN"),
            BigInteger.valueOf(now),
            notBefore,
            notAfter,
            new javax.security.auth.x500.X500Principal("CN=sh-framework-config,OU=config,O=sh-framework,L=default,ST=default,C=CN"),
            keyPair.getPublic()
        );

        X509CertificateHolder certHolder = certBuilder.build(
            new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate())
        );

        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    // ==================== 命令行入口 ====================

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();
        try {
            switch (command) {
                case "aes" -> {
                    if (args.length < 3) {
                        System.out.println("Usage: ConfigEncryptor aes <plaintext> <aesKey>");
                        return;
                    }
                    System.out.println(encrypt(args[1], args[2]));
                }
                case "rsa" -> {
                    if (args.length < 3) {
                        System.out.println("Usage: ConfigEncryptor rsa <plaintext> <rsaPublicKeyBase64>");
                        return;
                    }
                    System.out.println(encryptRsa(args[1], args[2]));
                }
                case "keygen" -> {
                    if (args.length < 4) {
                        System.out.println("Usage: ConfigEncryptor keygen <keystorePath> <alias> <password> [keySize]");
                        return;
                    }
                    int keySize = args.length >= 5 ? Integer.parseInt(args[4]) : 2048;
                    generateKeyPairToKeystore(args[1], args[2], args[3], keySize);
                }
                case "export-pub" -> {
                    if (args.length < 4) {
                        System.out.println("Usage: ConfigEncryptor export-pub <keystorePath> <alias> <password>");
                        return;
                    }
                    System.out.println(exportPublicKey(args[1], args[2], args[3]));
                }
                default -> printUsage();
            }
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
        }
    }

    private static void printUsage() {
        System.out.println("ConfigEncryptor - 敏感配置加密工具");
        System.out.println();
        System.out.println("命令:");
        System.out.println("  aes <plaintext> <aesKey>                          AES 对称加密");
        System.out.println("  rsa <plaintext> <rsaPublicKeyBase64>              RSA 混合加密（推荐）");
        System.out.println("  keygen <keystorePath> <alias> <password> [keySize] 生成 RSA 密钥对到密钥库");
        System.out.println("  export-pub <keystorePath> <alias> <password>      从密钥库导出公钥");
    }
}
