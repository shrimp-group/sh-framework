package com.wkclz.web.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RestInfo implements Serializable {

    private Class<?> clazz;
    private String appCode;
    private String code;
    private String module;
    private String method;
    private String uri;
    private String name;
    private String desc;
    private Integer writeFlag;

    /**
     * 接口参数列表
     */
    private List<RestParam> parameters;

    /**
     * 返回类型（完整类名）
     */
    private String returnType;

    /**
     * 返回类型泛型信息（JSON 格式）
     */
    private String returnGenericInfo;

}
