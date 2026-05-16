package com.wkclz.web.annotation;

import com.wkclz.web.annotation.validator.AtLeastOneNotNullValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @author shrimp
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneNotNullValidator.class)
@Documented
public @interface AtLeastOneNotNull {

    String message() default "两个字段至少需要填写一个";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // 需要校验的字段名数组
    String[] fields();

}