package com.wkclz.tool.bean;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author shrimp
 */
@Data
public class JavaField implements Serializable {

    private String fieldName;
    private String columnName;
    private Field field;
    private Method getter;
    private Method setter;
    private Class<?> clazz;

}
