package com.wkclz.iam.contract.exception;

import lombok.Getter;

/**
 * 契约层统一异常
 * 用于认证、鉴权、AK 签名等场景的错误标识
 *
 * @author shrimp
 */
@Getter
public class AuthException extends RuntimeException {

    private final AuthErrorType errorType;

    public AuthException(AuthErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public AuthException(AuthErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    /**
     * 认证错误类型
     */
    public enum AuthErrorType {
        /** token 不存在 */
        TOKEN_MISSING,
        /** JWT 签名无效 */
        TOKEN_INVALID,
        /** JWT 已过期 */
        TOKEN_EXPIRED,
        /** 会话已过期（如 Redis 无记录） */
        SESSION_EXPIRED,
        /** AK 签名无效 */
        AK_SIGN_INVALID,
        /** AK 签名已过期 */
        AK_SIGN_EXPIRED,
        /** nonce 重放检测命中 */
        AK_NONCE_REPLAY,
        /** 接口鉴权拒绝 */
        ACCESS_DENIED
    }
}
