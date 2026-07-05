package com.wkclz.iam.contract.facade;

import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.context.PrincipalContext;

/**
 * SSO RPC 门面契约
 * 客户端应用通过此契约调用 SSO 服务端
 *
 * @author shrimp
 */
public interface SsoFacadeContract {

    /**
     * 远程登录（创建会话并记录登录日志）
     *
     * @param req 会话创建请求
     * @return 登录响应（含 JWT Token）
     */
    LoginResp login(SessionCreateReq req);

    /**
     * 远程保存请求日志
     * 客户端应用将请求日志上报到 SSO 服务端集中存储
     *
     * @param log 请求日志
     */
    void saveLog(RequestLog log);

    /**
     * 远程登出（指定 token）
     *
     * @param token JWT token
     */
    void logout(String token);

    /**
     * 远程登出（从 PrincipalContext 获取 token）
     */
    default void logout() {
        logout(PrincipalContext.getToken());
    }
}
