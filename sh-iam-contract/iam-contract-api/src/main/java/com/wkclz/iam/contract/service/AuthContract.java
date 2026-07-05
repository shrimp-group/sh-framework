package com.wkclz.iam.contract.service;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证契约
 * 实现方负责从 HTTP 请求或 token 中认证用户，返回 Principal + Session
 *
 * @author shrimp
 */
public interface AuthContract {

    /**
     * 从 HTTP 请求中认证用户（过滤器主入口）
     *
     * 实现职责：
     * 1. 从请求头提取 token（Authorization / token，去 Bearer 前缀）
     * 2. 校验 JWT 签名与有效期
     * 3. 校验 Session 存在性（如 Redis）
     * 4. 返回 Principal + Session
     *
     * @param request HTTP 请求
     * @return 认证结果；token 不存在时返回 null（由过滤器处理 public 路径放行）
     * @throws AuthException token 无效、签名错误、会话过期等
     */
    AuthResult authenticate(HttpServletRequest request);

    /**
     * 校验 token（非 HTTP 请求场景：WebSocket、定时任务等）
     *
     * @param token          JWT token
     * @param authIdentifier 认证标识符（用户名 / 三方平台标识）
     * @return 会话信息
     * @throws AuthException token 无效或会话过期
     */
    Session checkToken(String token, String authIdentifier);
}
