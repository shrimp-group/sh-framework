package com.wkclz.tool.bean;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Method;

@Data
public class JavaField implements Serializable {

    private String name;
    private Method getter;
    private Class clazz;

}
