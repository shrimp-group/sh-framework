package com.wkclz.core.user;

import com.wkclz.core.base.Principal;

/**
 * 会话信息契约
 * <p>底层模块通过此类获取用户信息，鉴权模块通过此类存放用户会话信息。
 * <p>基于 ThreadLocal 实现，仅在当前线程内有效。
 */
public class PrincipalContext {

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    /**
     * 设置会话信息
     * @param principal 会话信息；为 null 时不做任何操作
     */
    public static void cache(Principal principal) {
        if (principal == null) {
            return;
        }
        HOLDER.set(principal);
    }

    /**
     * 获取会话信息
     * @return 会话信息；未设置时返回 null
     */
    public static Principal getPrincipal() {
        return HOLDER.get();
    }

    /**
     * 获取用户编码
     */
    public static String getUserCode() {
        Principal p = HOLDER.get();
        return p == null ? null : p.getUserCode();
    }

    /**
     * 获取用户名
     */
    public static String getUsername() {
        Principal p = HOLDER.get();
        return p == null ? null : p.getUsername();
    }

    /**
     * 获取用户姓名
     */
    public static String getNickname() {
        Principal p = HOLDER.get();
        return p == null ? null : p.getNickname();
    }

    /**
     * 获取用户头像
     */
    public static String getAvatar() {
        Principal p = HOLDER.get();
        return p == null ? null : p.getAvatar();
    }

    /**
     * 获取应用编码
     */
    public static String getAppCode() {
        Principal p = HOLDER.get();
        return p == null ? null : p.getAppCode();
    }

    /**
     * 获取认证标识
     */
    public static String getAuthIdentifier() {
        Principal p = HOLDER.get();
        return p == null ? null : p.getAuthIdentifier();
    }

    /**
     * 清理会话信息
     */
    public static void clear() {
        HOLDER.remove();
    }

}
