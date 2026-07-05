package com.wkclz.iam.contract.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应
 * 由 SsoFacadeContract.login() 返回
 *
 * @author shrimp
 */
@Data
@Schema(description = "登录响应")
public class LoginResp implements Serializable {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;
}
