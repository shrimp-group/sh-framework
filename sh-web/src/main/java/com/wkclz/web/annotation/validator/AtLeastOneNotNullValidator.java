package com.wkclz.web.annotation.validator;

import com.wkclz.web.annotation.AtLeastOneNotNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Collection;

@Slf4j
public class AtLeastOneNotNullValidator implements ConstraintValidator<AtLeastOneNotNull, Object> {

    private String[] fields;

    @Override
    public void initialize(AtLeastOneNotNull constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        try {
            // 遍历指定的字段，只要有一个不为空，就返回 true
            for (String fieldName : fields) {
                Field field = value.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object fieldValue = field.get(value);
                if (fieldValue != null) {
                    // 如果是字符串，还可以进一步判断是否为空串
                    if (fieldValue instanceof String && ((String) fieldValue).trim().isEmpty()) {
                        continue;
                    }
                    // 如果 fieldValue 是列表或数组
                    if (fieldValue instanceof Collection<?> collection && collection.isEmpty()) {
                        continue;
                    }
                    if (fieldValue.getClass().isArray()) {
                        assert fieldValue instanceof Object[];
                        if (((Object[]) fieldValue).length == 0) {
                            continue;
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("双参数校验失败: {}", e.getMessage(), e);
        }
        // 两个都为空，校验失败
        return false;
    }
}