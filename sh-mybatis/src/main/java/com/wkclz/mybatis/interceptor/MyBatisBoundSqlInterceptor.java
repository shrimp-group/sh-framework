package com.wkclz.mybatis.interceptor;

import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.mybatis.bean.MyBatisConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis拦截器，将 updateBy 注入到 BoundSql 附加参数中，
 * 使 SQL 中的 #{updateBy} 占位符可正确解析（用于非实体参数的 deleteById/deleteByIds 等方法）
 */
@Slf4j
@Component
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class MyBatisBoundSqlInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();

        // 仅当 SQL 中包含 #{updateBy} 占位符时才注入（即 deleteById/deleteByIds 场景）
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        boolean hasUpdateBy = parameterMappings.stream().anyMatch(pm -> "updateBy".equals(pm.getProperty()));
        if (hasUpdateBy) {
            String userCode = PrincipalContext.getUserCode();
            if (userCode == null) {
                userCode = MyBatisConstants.DEFAULT_OPERATOR;
            }
            boundSql.setAdditionalParameter("updateBy", userCode);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
