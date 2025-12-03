package com.wkclz.core.base;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Method;

@Data
public class JavaField implements Serializable {

    private String name;
    private Method getter;
    private Class clazz;

}
