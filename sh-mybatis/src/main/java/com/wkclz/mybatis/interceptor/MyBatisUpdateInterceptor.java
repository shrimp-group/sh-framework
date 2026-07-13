package com.wkclz.mybatis.interceptor;

import com.wkclz.core.base.DbColumnEntity;
import com.wkclz.core.user.PrincipalContext;
import com.wkclz.mybatis.bean.MyBatisConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Properties;

/**
 * MyBatis拦截器，用于自动植入创建人和更新人信息
 */
@Slf4j
@Component
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class MyBatisUpdateInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        // 获取SQL命令类型
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        // 获取当前用户（从 IAM 契约层 PrincipalContext）
        String userCode = PrincipalContext.getUserCode();

        // 无用户信息时使用默认值 nobody
        if (userCode == null) {
            log.debug("当前无用户信息，createBy/updateBy 使用默认值 nobody");
            userCode = MyBatisConstants.DEFAULT_OPERATOR;
        }

        setOperatorUser(parameter, sqlCommandType, userCode);
        // 继续执行原始操作
        return invocation.proceed();
    }


    private static void setOperatorUser(Object obj, SqlCommandType sqlCommandType, String userCode) {
        if (obj instanceof MapperMethod.ParamMap map) {
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                setOperatorUser(value, sqlCommandType, userCode);
            }
            return;
        }
        if (obj instanceof Collection<?> list) {
            for (Object o : list) {
                setOperatorUser(o, sqlCommandType, userCode);
            }
            return;
        }
        // 检查对象是否继承自DbColumnEntity
        if (obj instanceof DbColumnEntity db) {
            // 清空时间字段，让数据库自动填充
            db.setCreateTime(null);
            db.setUpdateTime(null);
            db.setUpdateBy(userCode);
            if (SqlCommandType.INSERT == sqlCommandType) {
                db.setCreateBy(userCode);
            } else {
                db.setCreateBy(null);
            }
        }
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
