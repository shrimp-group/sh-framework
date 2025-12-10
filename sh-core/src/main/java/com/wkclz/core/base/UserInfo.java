package com.wkclz.core.base;

import com.wkclz.core.annotation.Desc;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户基础信息实体，登录后保存用户基础信息
 */
@Data
public class UserInfo implements Serializable {

    @Desc("用户编码")
    private String userCode;

    @Desc("用户名")
    private String username;

    @Desc("用户姓名")
    private String nickname;
    
    @Desc("租户编码")
    private String tenantCode;

}
