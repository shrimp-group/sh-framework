package com.wkclz.core.annotation;

import java.lang.annotation.*;

/**
 * Created by wangkc on 2018/06/07.
 * 标识路由，提取注释
 */
@Documented
@Inherited
// 作用域是类或者接口,或者方法 // 不限制使用位置
@Target({ElementType.TYPE})
// 注解类型：VM将在运行期间保留注解，因此可以通过反射机制读取注解的信息
@Retention(RetentionPolicy.RUNTIME)
public @interface Router {

    // 注解只有一个变量时 变量名必须为value

    // 需要有多个变量

    // 模块
    String module();

    // 前缀
    String prefix();

}
