package com.wkclz.demo.entity;

import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    private String userCode;
    private String username;
    private String nickname;
    private Integer userStatus;
}