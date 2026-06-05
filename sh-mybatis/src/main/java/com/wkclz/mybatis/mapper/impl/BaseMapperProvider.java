package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.exception.SystemException;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import com.wkclz.tool.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
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
     * 获取字段值，优先使用 getter Method，getter 为 null 时回退到 Field.get()
     * @param field JavaField 对象
     * @param entity 实体对象
     * @return 字段值
     */
    protected static Object getFieldValue(JavaField field, Object entity) {
        try {
            if (field.getGetter() != null) {
                return field.getGetter().invoke(entity);
            }
            return field.getField().get(entity);
        } catch (IllegalAccessException e) {
            throw SystemException.of(500, "获取字段值失败: field={}, entity={}", field.getFieldName(), entity, e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof RuntimeException re) {
                throw re;
            }
            throw SystemException.of(500, "获取字段值失败: field={}, entity={}", field.getFieldName(), entity, target);
        }
    }

    /**
     * 获取字段值
     * @param entity 实体对象
     * @param fieldName 字段名
     * @return 字段值
     */
    protected Object getFieldValue(BaseEntity entity, String fieldName) {
        DbEntityProperty property = getDbEntityProperty(entity.getClass());
        JavaField field = property.getFieldMap().get(fieldName);
        if (field == null) {
            return null;
        }
        return getFieldValue(field, entity);
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

            Object value = getFieldValue(field, entity);
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
        LocalDateTime timeFrom = entity.getTimeFrom();
        if (timeFrom != null) {
            whereClause.append(" AND ");
            whereClause.append(DbEntityProperty.CREATE_TIME_FIELD);
            whereClause.append(" >= #{timeFrom}");
        }

        LocalDateTime timeTo = entity.getTimeTo();
        if (timeTo != null) {
            whereClause.append(" AND ");
            whereClause.append(DbEntityProperty.CREATE_TIME_FIELD);
            whereClause.append(" <= #{timeTo}");
        }

        return whereClause.toString();
    }


    /**
     * 构建安全的 ORDER BY 子句，防止 SQL 注入
     * 仅允许实体字段名（驼峰或下划线）和 ASC/DESC 关键字
     * @param orderBy 排序字符串，如 "name ASC, id DESC"
     * @param property 实体属性信息
     * @param defaultOrderBy 默认排序（当 orderBy 为空时使用）
     * @return 安全的 ORDER BY 子句
     */
    protected String buildOrderByClause(String orderBy, DbEntityProperty property, String defaultOrderBy) {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return " ORDER BY " + defaultOrderBy;
        }

        // 收集所有合法的列名（驼峰+下划线形式）
        java.util.Set<String> validColumns = new java.util.HashSet<>();
        for (JavaField field : property.getFields()) {
            validColumns.add(field.getColumnName().toLowerCase());
            validColumns.add(field.getFieldName().toLowerCase());
        }
        validColumns.add(DbEntityProperty.PRIMARY_KEY);
        validColumns.add(DbEntityProperty.CREATE_TIME_FIELD);
        validColumns.add(DbEntityProperty.UPDATE_TIME_FIELD);

        // 校验每个排序项
        String[] orderItems = orderBy.split(",");
        StringBuilder safeOrderBy = new StringBuilder();
        for (String item : orderItems) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 拆分字段名和排序方向
            String[] parts = trimmed.split("\\s+");
            String columnName = parts[0];

            // 将驼峰转为下划线进行校验
            String checkName = StringUtil.camelToUnderline(columnName).toLowerCase();

            if (!validColumns.contains(checkName) && !validColumns.contains(columnName.toLowerCase())) {
                log.warn("OrderBy 包含非法字段，已忽略: {}", columnName);
                continue;
            }

            if (!safeOrderBy.isEmpty()) {
                safeOrderBy.append(", ");
            }
            safeOrderBy.append(checkName);

            // 校验排序方向
            if (parts.length > 1) {
                String direction = parts[parts.length - 1].toUpperCase();
                if ("ASC".equalsIgnoreCase(direction) || "DESC".equalsIgnoreCase(direction)) {
                    safeOrderBy.append(" ").append(direction);
                }
            }
        }

        if (safeOrderBy.isEmpty()) {
            return " ORDER BY " + defaultOrderBy;
        }
        return " ORDER BY " + safeOrderBy;
    }

}