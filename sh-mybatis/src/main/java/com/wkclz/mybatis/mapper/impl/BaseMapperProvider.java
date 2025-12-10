package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.exception.SystemException;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class BaseMapperProvider {

    private static final Map<Class<?>, DbEntityProperty> ENTITY_CACHE = new ConcurrentHashMap<>();
    protected static DbEntityProperty getDbEntityProperty(Class<?> entityClass) {
        return ENTITY_CACHE.computeIfAbsent(entityClass, k -> DbEntityProperty.createInstance(entityClass));
    }
    
    /**
     * 从ProviderContext中获取泛型类型参数
     * @param context ProviderContext对象
     * @return 泛型类型参数
     */
    protected Class<?> getEntityClassFromContext(ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        Type[] genericInterfaces = mapperType.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class && "BaseMapper".equals(((Class<?>) rawType).getSimpleName())) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                        return (Class<?>) actualTypeArguments[0];
                    }
                }
            }
        }
        return null;
    }


    /**
     * 获取字段值
     * @param entity 实体对象
     * @param fieldName 字段名
     * @return 字段值
     */
    protected Object getFieldValue(BaseEntity entity, String fieldName) {
        try {
            DbEntityProperty property = getDbEntityProperty(entity.getClass());
            // 查找字段
            for (JavaField field : property.getFields()) {
                if (field.getFieldName().equals(fieldName)) {
                    return field.getField().get(entity);
                }
            }
            return null;
        } catch (IllegalAccessException e) {
            throw SystemException.of(500, "获取字段值失败: entity={}, fieldName={}", entity, fieldName, e);
        }
    }



    /**
     * 构建IN子句
     * @param listValue 列表值
     * @param fieldName 字段名
     * @return IN子句字符串
     */
    protected String buildInClause(List<?> listValue, String fieldName) {
        if (listValue == null || listValue.isEmpty()) {
            return "()";
        }
        StringBuilder inClause = new StringBuilder();
        inClause.append("(");
        for (int i = 0; i < listValue.size(); i++) {
            if (i > 0) {
                inClause.append(", ");
            }
            inClause.append("#{")
                    .append(fieldName)
                    .append("[")
                    .append(i)
                    .append("]}");
        }
        inClause.append(")");
        return inClause.toString();
    }

    /**
     * 构建查询条件，处理null和空字符串
     * @param entity 实体对象
     * @return 查询条件字符串
     */
    protected String buildWhereClause(BaseEntity entity) throws IllegalAccessException {
        DbEntityProperty property = getDbEntityProperty(entity.getClass());
        String deleted = DbEntityProperty.DELETED_FIELD;

        StringBuilder whereClause = new StringBuilder();
        whereClause.append(deleted).append(" = 0");

        for (JavaField field : property.getSelectListFields()) {
            String fieldName = field.getFieldName();
            String columnName = field.getColumnName();

            Object value = field.getField().get(entity);
            // 跳过空值字段
            if (value == null) {
                continue;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }

            whereClause.append(" AND ");

            // 判断字段类型，处理不同类型的查询条件
            if (value instanceof String) {
                // 字符串类型，使用等于查询
                whereClause.append(columnName).append(" = #{").append(fieldName).append("}");
            } else if (value instanceof List<?> listValue) {
                // 列表类型，使用in查询
                if (listValue.isEmpty()) {
                    continue;
                }
                whereClause.append(columnName).append(" IN ").append(buildInClause(listValue, fieldName));
            } else {
                // 其他类型，使用等于查询
                whereClause.append(columnName).append(" = #{").append(fieldName).append("}");
            }
        }

        // 处理时间范围查询
        Object timeFrom = getFieldValue(entity, "timeFrom");
        if (timeFrom != null) {
            whereClause.append(" AND ");
            whereClause.append(DbEntityProperty.CREATE_TIME_FIELD);
            whereClause.append(" >= #{timeFrom}");
        }

        Object timeTo = getFieldValue(entity, "timeTo");
        if (timeTo != null) {
            whereClause.append(" AND ");
            whereClause.append(DbEntityProperty.CREATE_TIME_FIELD);
            whereClause.append(" <= #{timeTo}");
        }

        return whereClause.toString();
    }

}