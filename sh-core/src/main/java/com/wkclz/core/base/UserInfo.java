package com.wkclz.core.base;

import com.wkclz.core.annotation.FieldDesc;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户基础信息实体，登录后保存用户基础信息
 */
@Data
public class UserInfo implements Serializable {

    @FieldDesc("用户编码")
    private String userCode;

    @FieldDesc("用户名")
    private String username;

    @FieldDesc("用户姓名")
    private String nickname;

    @FieldDesc("手机号(脱敏)")
    private String mobile;

    @FieldDesc("租户编码")
    private String tenantCode;

    @FieldDesc("头像")
    private String avatar;

    @FieldDesc("三方平台标识符")
    private String authIdentifier;

}
