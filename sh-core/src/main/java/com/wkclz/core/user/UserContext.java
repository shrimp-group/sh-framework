package com.wkclz.core.user;

import com.wkclz.core.base.UserInfo;

/**
 * 用户上下文工具类，用于在登录后存储和获取用户信息
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> USER_CONTEXT = new ThreadLocal<>();

    /**
     * 设置用户信息到上下文
     * @param userInfo 用户信息
     */
    public static void setUserInfo(UserInfo userInfo) {
        USER_CONTEXT.set(userInfo);
    }

    /**
     * 从上下文获取用户信息
     * @return 用户信息
     */
    public static UserInfo getUserInfo() {
        return USER_CONTEXT.get();
    }

    /**
     * 从上下文获取用户编码
     * @return 用户编码
     */
    public static String getUserCode() {
        UserInfo userInfo = USER_CONTEXT.get();
        return userInfo != null ? userInfo.getUserCode() : null;
    }

    /**
     * 从上下文获取租户编码
     * @return 租户编码
     */
    public static String getTenantCode() {
        UserInfo userInfo = USER_CONTEXT.get();
        return userInfo != null ? userInfo.getTenantCode() : null;
    }

    /**
     * 清除上下文用户信息
     */
    public static void clear() {
        USER_CONTEXT.remove();
    }

}
