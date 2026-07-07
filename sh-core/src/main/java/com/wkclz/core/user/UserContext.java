package com.wkclz.core.user;

import com.wkclz.core.base.UserInfo;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.context.PrincipalContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文工具类（已废弃）
 *
 * @deprecated 请使用 {@link PrincipalContext} 获取用户信息。
 *             此类仅作为兼容层保留，所有方法委托 PrincipalContext。
 *             framework 已知代码已迁移至直接使用 PrincipalContext。
 */
@Deprecated
@Slf4j
public class UserContext {

    private UserContext() {
    }

    /**
     * 设置用户信息到上下文（已废弃）
     * <p>注意：仅迁移 userCode/username/nickname/avatar 到 Principal。
     * tenantCode/mobile 会被丢弃（Principal 不含这些字段）。
     * @param userInfo 用户信息
     * @deprecated 请使用 {@link PrincipalContext#cache}
     */
    @Deprecated
    public static void setUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        Principal principal = new Principal();
        principal.setUserCode(userInfo.getUserCode());
        principal.setUsername(userInfo.getUsername());
        principal.setNickname(userInfo.getNickname());
        principal.setAvatar(userInfo.getAvatar());
        principal.setAuthIdentifier(userInfo.getAuthIdentifier());
        log.warn("UserContext.setUserInfo is deprecated, please use PrincipalContext.cache() instead");
        PrincipalContext.cache(null, principal, null);
    }

    /**
     * 从上下文获取用户信息（已废弃）
     * @return 用户信息；无 Principal 时返回 null
     * @deprecated 请使用 {@link PrincipalContext#getPrincipal()}
     */
    @Deprecated
    public static UserInfo getUserInfo() {
        Principal principal = PrincipalContext.getPrincipal();
        if (principal == null) {
            return null;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUserCode(principal.getUserCode());
        userInfo.setUsername(principal.getUsername());
        userInfo.setNickname(principal.getNickname());
        userInfo.setAvatar(principal.getAvatar());
        userInfo.setTenantCode(PrincipalContext.getTenantCode());
        userInfo.setAuthIdentifier(principal.getAuthIdentifier());
        // mobile 在 Principal 中不存在，无法填充
        return userInfo;
    }

    /**
     * 从上下文获取用户编码（已废弃）
     * @return 用户编码；无 Principal 时返回 null
     * @deprecated 请使用 {@link PrincipalContext#getUserCode()}
     */
    @Deprecated
    public static String getUserCode() {
        return PrincipalContext.getUserCode();
    }

    /**
     * 从上下文获取租户编码（已废弃）
     * @return 租户编码；无请求上下文时返回 null
     * @deprecated 请使用 {@link PrincipalContext#getTenantCode()}
     */
    @Deprecated
    public static String getTenantCode() {
        return PrincipalContext.getTenantCode();
    }

    /**
     * 清除上下文用户信息（已废弃）
     * @deprecated 请使用 {@link PrincipalContext#clear()}
     */
    @Deprecated
    public static void clear() {
        PrincipalContext.clear();
    }

}
