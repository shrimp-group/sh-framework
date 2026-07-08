package com.wkclz.core.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户基础信息实体，登录后保存用户基础信息
 */
@Data
public class UserInfo implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户姓名")
    private String nickname;

    @Schema(description = "手机号(脱敏)")
    private String mobile;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "三方平台标识符")
    private String authIdentifier;

}
