package com.wkclz.mybatis.interceptor;

import com.wkclz.tool.utils.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MyBatis拦截器，用于将查询参数中的空字符串替换为null
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MyBatisQueryInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        
        // 只处理查询操作
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if (SqlCommandType.SELECT != sqlCommandType) {
            return invocation.proceed();
        }

        Object parameter = invocation.getArgs()[1];
        // 处理参数，将空字符串替换为null
        processParameter(parameter);
        
        // 继续执行原始操作
        return invocation.proceed();
    }

    /**
     * 处理参数，将空字符串替换为null
     * @param parameter 参数对象
     */
    private void processParameter(Object parameter) {
        if (parameter == null) {
            return;
        }
        
        // 处理ParamMap类型参数
        if (parameter instanceof MapperMethod.ParamMap paramMap) {
            for (Object key : paramMap.keySet()) {
                Object value = paramMap.get(key);
                if (value instanceof String valueString) {
                    if (valueString.isEmpty()) {
                        paramMap.put(key, null);
                    }
                    return;
                }
                processParameter(value);
            }
            return;
        }
        
        // 处理Collection类型参数
        if (parameter instanceof Collection<?> collection) {
            List<Object> processedList = new ArrayList<>();
            for (Object item : collection) {
                processParameter(item);
            }
            return;
        }
        
        // 处理数组类型参数
        if (parameter.getClass().isArray()) {
            Object[] array = (Object[]) parameter;
            for (Object o : array) {
                replaceEmptyStringWithNull(o);
            }
            return;
        }

        if (parameter instanceof String) {
            return;
        }
        
        // 处理普通对象类型参数
        replaceEmptyStringWithNull(parameter);
    }

    /**
     * 递归将对象中的空字符串替换为null
     * @param obj 需要处理的对象
     */
    private void replaceEmptyStringWithNull(Object obj) {
        if (obj == null) {
            return;
        }

        // 基本类型，包装类型，不处理
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj instanceof Character || obj instanceof Date) {
            return;
        }

        BeanUtil.removeBlank(obj);
    }

    @Override
    public Object plugin(Object target) {
        // 生成代理对象
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 设置属性
    }
}
