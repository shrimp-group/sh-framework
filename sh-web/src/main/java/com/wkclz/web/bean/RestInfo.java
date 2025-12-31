package com.wkclz.web.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class RestInfo implements Serializable {

    private String appCode;
    private String code;
    private String module;
    private String method;
    private String uri;
    private String name;
    private String desc;
    private Integer writeFlag;

}
