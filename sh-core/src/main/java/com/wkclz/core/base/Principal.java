package com.wkclz.core.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class Principal implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户姓名")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "认证标识（用户名/手机号/openId，按 authType 区分含义，可选）")
    private String authIdentifier;

}
