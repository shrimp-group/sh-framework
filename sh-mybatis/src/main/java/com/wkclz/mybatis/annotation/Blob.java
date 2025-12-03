package com.wkclz.mybatis.annotation;

import java.lang.annotation.*;


/**
 * Blob 字段标识。被标识的字段，不出现在 List 查询中
 * @author shrimp
 */
@Documented
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Blob {

}
