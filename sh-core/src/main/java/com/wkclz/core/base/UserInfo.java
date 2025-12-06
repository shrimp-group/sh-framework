package com.wkclz.core.base;

import com.wkclz.core.annotation.Desc;
import lombok.Data;
import java.io.Serializable;

/**
 * 用户基础信息实体，登录后保存用户基础信息
 */
@Data
public class UserInfo implements Serializable {

    @Desc("用户ID")
    private Long userId;
    
    @Desc("用户编码")
    private String userCode;
    
    @Desc("用户姓名")
    private String userName;
    
    @Desc("租户编码")
    private String tenantCode;
    
    @Desc("用户邮箱")
    private String email;
    
    @Desc("用户手机号")
    private String phone;
    
    @Desc("用户角色")
    private String role;
    
    @Desc("用户状态")
    private Integer status;
    
}
