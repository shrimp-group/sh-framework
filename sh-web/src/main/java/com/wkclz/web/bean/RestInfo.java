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

    /**
     * 类级别 @Tag 描述
     */
    private String tag;

    /**
     * 方法级别 @Operation(summary)
     */
    private String operationSummary;

    /**
     * 方法级别 @Operation(description)
     */
    private String operationDescription;

    /**
     * 接口是否废弃 @Operation(deprecated)
     */
    private Boolean deprecated;

    /**
     * 返回值完整结构（JSON 格式，包含字段注释、示例值）
     */
    private String returnSchema;

    /**
     * 请求 Content-Type（@RequestMapping.consumes）
     */
    private String[] consumes;

    /**
     * 响应 Content-Type（@RequestMapping.produces）
     */
    private String[] produces;

}
