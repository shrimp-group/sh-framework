package com.wkclz.core.identity;

/**
 * 身份上下文 — 请求级 ThreadLocal 实现，零外部依赖。
 *
 * <p>全部 static 方法，线程隔离。使用方式：
 * <pre>{@code
 * // 设置当前请求的身份（通常在认证过滤器中调用）
 * IdentityContext.set(userIdentity, token);
 *
 * // 业务代码中获取用户信息
 * String username = IdentityContext.getUsername();
 *
 * // 请求结束后清理（在 Filter finally 块中调用）
 * IdentityContext.clear();
 * }</pre>
 *
 * <p>不负责自动清理（由上层 Filter/Interceptor 负责）。</p>
 */
public final class IdentityContext {

    private static final ThreadLocal<IdentityContext> HOLDER = ThreadLocal.withInitial(IdentityContext::new);

    private UserIdentity identity;
    private String token;
    private String appCode;
    private String tenantCode;


    // ========== 写入 ==========

    /**
     * 设置当前线程的身份和 Token。
     *
     * @param identity 用户身份，不可为 null
     * @param token    认证 Token，不可为 null
     */
    public static void set(UserIdentity identity, String token) {
        if (identity == null) {
            throw new IllegalArgumentException("identity must not be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        IdentityContext ctx = HOLDER.get();
        ctx.identity = identity;
        ctx.token = token;
    }

    // ========== 读取 ==========

    /**
     * 获取当前线程的用户身份。
     */
    public static UserIdentity get() {
        return HOLDER.get().identity;
    }

    /**
     * 获取当前线程的认证 Token。
     */
    public static String getToken() {
        return HOLDER.get().token;
    }

    /**
     * 获取当前线程的用户唯一编码。
     */
    public static String getUserCode() {
        UserIdentity id = HOLDER.get().identity;
        return id != null ? id.getUserCode() : null;
    }

    /**
     * 获取当前线程的用户名。
     */
    public static String getUsername() {
        UserIdentity id = HOLDER.get().identity;
        return id != null ? id.getUsername() : null;
    }

    /**
     * 获取当前线程的用户昵称。
     */
    public static String getNickname() {
        UserIdentity id = HOLDER.get().identity;
        return id != null ? id.getNickname() : null;
    }

    /**
     * 获取当前线程的用户头像地址。
     */
    public static String getAvatar() {
        UserIdentity id = HOLDER.get().identity;
        return id != null ? id.getAvatar() : null;
    }

    // ========== 应用/租户上下文 ==========

    /**
     * 设置当前线程的应用编码。
     */
    public static void setAppCode(String appCode) {
        HOLDER.get().appCode = appCode;
    }

    /**
     * 获取当前线程的应用编码。
     */
    public static String getAppCode() {
        return HOLDER.get().appCode;
    }

    /**
     * 设置当前线程的租户编码。
     */
    public static void setTenantCode(String tenantCode) {
        HOLDER.get().tenantCode = tenantCode;
    }

    /**
     * 获取当前线程的租户编码。
     */
    public static String getTenantCode() {
        return HOLDER.get().tenantCode;
    }

    // ========== 清理 ==========

    /**
     * 清理当前线程的身份信息。调用后 get()/getToken() 等均返回 null。
     */
    public static void clear() {
        HOLDER.remove();
    }

}
