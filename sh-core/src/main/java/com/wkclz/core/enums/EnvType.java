package com.wkclz.core.enums;


import com.wkclz.core.annotation.Desc;

/**
 * 系统环境类型
 */
@Desc("系统环境")
public enum EnvType {

    /** 环境 */
    DEV( "开发环境"),
    SIT( "集成测试环境"),
    UAT("验收测试环境"),
    PROD("生产环境");

    private String desc;

    EnvType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
