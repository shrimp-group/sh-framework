package com.wkclz.core.identity;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户身份 — 最精简的标识信息集，仅回答"当前请求是谁"。
 *
 * <p>attributes 用于扩展场景（如小程序 openid）。</p>
 */
@Data
public class UserIdentity implements Serializable {

    /** 用户唯一编码 */
    private String userCode;

    /** 用户名（登录名） */
    private String username;

    /** 用户昵称（显示名） */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 扩展属性 */
    private Map<String, Object> attributes = Collections.emptyMap();


    /**
     * 便捷添加单个扩展属性
     */
    public void addAttribute(String key, Object value) {
        if (this.attributes.isEmpty()) {
            this.attributes = new LinkedHashMap<>();
        }
        this.attributes.put(key, value);
    }

}
