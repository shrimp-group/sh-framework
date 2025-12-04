package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class BaseMapperProvider {

    private static final String PRIMARY_KEY = "id";
    private static final String DELETED_FIELD = "deleted";
    private static final String VERSION_FIELD = "version";
    private static final String CREATE_TIME_FIELD = "create_time";
    private static final String UPDATE_TIME_FIELD = "update_time";
    private static final String UPDATE_BY_FIELD = "update_by";

    /**
     * 将类名转换为下划线格式的表名
     * @param entityClass 实体类
     * @return 下划线格式的表名
     */
    private String getTableName(Class<?> entityClass) {
        String className = entityClass.getSimpleName();
        // 大驼峰转下划线
        StringBuilder tableName = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                tableName.append('_');
            }
            tableName.append(Character.toLowerCase(c));
        }
        String result = tableName.toString();
        log.debug("ClassName: {}, TableName: {}", className, result);
        return result;
    }

    /**
     * 获取实体类的所有字段（包括父类字段，直到BaseEntity）
     * @param entityClass 实体类
     * @return 字段列表
     */
    private List<Field> getEntityFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        // 获取当前类的所有字段
        fields.addAll(Arrays.asList(entityClass.getDeclaredFields()));
        // 获取父类的所有字段，直到BaseEntity
        Class<?> superClass = entityClass.getSuperclass();
        while (superClass != null && !superClass.equals(Object.class)) {
            fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            if (superClass.equals(BaseEntity.class)) {
                break;
            }
            superClass = superClass.getSuperclass();
        }
        return fields;
    }

    /**
     * 将驼峰命名转换为下划线命名
     * @param camelCase 驼峰命名
     * @return 下划线命名
     */
    private String camelToUnderline(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder underline = new StringBuilder();
        underline.append(Character.toLowerCase(camelCase.charAt(0)));
        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                underline.append('_');
                underline.append(Character.toLowerCase(c));
            } else {
                underline.append(c);
            }
        }
        return underline.toString();
    }

    /**
     * 获取字段值
     * @param entity 实体对象
     * @param field 字段
     * @return 字段值
     */
    private Object getFieldValue(BaseEntity entity, Field field) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            log.error("获取字段值失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取字段值
     * @param entity 实体对象
     * @param fieldName 字段名
     * @return 字段值
     */
    private Object getFieldValue(BaseEntity entity, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 尝试从父类查找字段
            Class<?> superClass = entity.getClass().getSuperclass();
            while (superClass != null && !superClass.equals(Object.class)) {
                try {
                    Field field = superClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(entity);
                } catch (NoSuchFieldException | IllegalAccessException ex) {
                    superClass = superClass.getSuperclass();
                }
            }
            return null;
        }
    }

    /**
     * 构建查询条件，处理null和空字符串
     * @param entity 实体对象
     * @return 查询条件字符串
     */
    private String buildWhereClause(BaseEntity entity) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append(DELETED_FIELD + " = 0");

        List<Field> fields = getEntityFields(entity.getClass());

        for (Field field : fields) {
            String fieldName = field.getName();

            // 跳过特殊字段
            if ("timeFrom".equals(fieldName) || "timeTo".equals(fieldName) || "orderBy".equals(fieldName) ||
                "ids".equals(fieldName) || "keyword".equals(fieldName) || "pageNum".equals(fieldName) ||
                "pageSize".equals(fieldName) || "offset".equals(fieldName) || "limit".equals(fieldName) || "deleted".equals(fieldName)) {
                continue;
            }

            Object value = getFieldValue(entity, field);
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
                // 字符串类型，使用like查询
                whereClause.append(camelToUnderline(fieldName));
                whereClause.append(" LIKE CONCAT('%', #{");
                whereClause.append(fieldName);
                whereClause.append("}, '%')");
            } else if (value instanceof List) {
                // 列表类型，使用in查询
                List<?> listValue = (List<?>) value;
                if (listValue.isEmpty()) {
                    continue;
                }
                whereClause.append(camelToUnderline(fieldName));
                whereClause.append(" IN <foreach collection=\"" + fieldName + "\" item=\"item\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
            } else {
                // 其他类型，使用等于查询
                whereClause.append(camelToUnderline(fieldName));
                whereClause.append(" = #{");
                whereClause.append(fieldName);
                whereClause.append("}");
            }
        }

        // 处理时间范围查询
        Object timeFrom = getFieldValue(entity, "timeFrom");
        if (timeFrom != null) {
            whereClause.append(" AND ");
            whereClause.append(CREATE_TIME_FIELD);
            whereClause.append(" >= #{timeFrom}");
        }

        Object timeTo = getFieldValue(entity, "timeTo");
        if (timeTo != null) {
            whereClause.append(" AND ");
            whereClause.append(CREATE_TIME_FIELD);
            whereClause.append(" <= #{timeTo}");
        }

        return whereClause.toString();
    }

    /**
     * 插入单条数据，跳过空字段
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String insert(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        List<Field> fields = getEntityFields(entity.getClass());
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        
        for (Field field : fields) {
            String fieldName = field.getName();
            // 跳过特殊字段
            if ("timeFrom".equals(fieldName) || "timeTo".equals(fieldName) || "orderBy".equals(fieldName) ||
                "ids".equals(fieldName) || "keyword".equals(fieldName) || "pageNum".equals(fieldName) ||
                "pageSize".equals(fieldName) || "offset".equals(fieldName) || "limit".equals(fieldName)) {
                continue;
            }
            
            Object value = getFieldValue(entity, field);
            // 跳过空值字段
            if (value == null) {
                continue;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            
            if (columns.length() > 0) {
                columns.append(", ");
                values.append(", ");
            }
            
            columns.append(camelToUnderline(fieldName));
            values.append("#{");
            values.append(fieldName);
            values.append("}");
        }
        
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        
        log.debug("Insert SQL: {}", sql);
        return sql;
    }

    /**
     * 批量插入数据，空值也插入
     * @param entities 实体列表
     * @return SQL字符串
     */
    public String insertBatch(List<BaseEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return "";
        }
        
        BaseEntity firstEntity = entities.get(0);
        String tableName = getTableName(firstEntity.getClass());
        
        List<Field> fields = getEntityFields(firstEntity.getClass());
        List<String> columnNames = new ArrayList<>();
        
        for (Field field : fields) {
            String fieldName = field.getName();
            // 跳过特殊字段
            if ("timeFrom".equals(fieldName) || "timeTo".equals(fieldName) || "orderBy".equals(fieldName) ||
                "ids".equals(fieldName) || "keyword".equals(fieldName) || "pageNum".equals(fieldName) ||
                "pageSize".equals(fieldName) || "offset".equals(fieldName) || "limit".equals(fieldName)) {
                continue;
            }
            
            columnNames.add(camelToUnderline(fieldName));
        }
        
        StringBuilder columns = new StringBuilder();
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) {
                columns.append(", ");
            }
            columns.append(columnNames.get(i));
        }
        
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) {
                values.append(", ");
            }
            values.append("(");
            for (int j = 0; j < columnNames.size(); j++) {
                if (j > 0) {
                    values.append(", ");
                }
                values.append("#{entities[");
                values.append(i);
                values.append(".");
                values.append(getFieldNameFromColumnName(columnNames.get(j)));
                values.append("}");
            }
            values.append(")");
        }
        
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES " + values;
        
        log.debug("InsertBatch SQL: {}", sql);
        return sql;
    }
    
    /**
     * 根据下划线列名获取驼峰字段名
     * @param columnName 下划线列名
     * @return 驼峰字段名
     */
    private String getFieldNameFromColumnName(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    /**
     * 根据ID删除单条数据，采用逻辑删除
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String deleteById(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        // 获取id和version字段值
        Object id = getFieldValue(entity, PRIMARY_KEY);
        Object version = getFieldValue(entity, VERSION_FIELD);
        
        if (id == null) {
            return "";
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE " + tableName + " SET " + DELETED_FIELD + " = NOW(), " + UPDATE_TIME_FIELD + " = NOW()");
        
        // 处理updateBy字段
        Object updateBy = getFieldValue(entity, UPDATE_BY_FIELD);
        if (updateBy != null) {
            sql.append(", " + UPDATE_BY_FIELD + " = #{" + UPDATE_BY_FIELD + "}");
        }
        
        // 添加乐观锁条件
        sql.append(" WHERE " + PRIMARY_KEY + " = #{" + PRIMARY_KEY + "}" + " AND " + DELETED_FIELD + " = 0");
        if (version != null) {
            sql.append(" AND version = #{version}");
        }
        
        log.debug("DeleteById SQL: {}", sql.toString());
        return sql.toString();
    }

    /**
     * 根据ID列表批量删除数据，采用逻辑删除
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String deleteByIds(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        // 获取ids字段值
        Object ids = getFieldValue(entity, "ids");
        
        if (ids == null || !(ids instanceof List)) {
            return "";
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE " + tableName + " SET " + DELETED_FIELD + " = NOW(), " + UPDATE_TIME_FIELD + " = NOW()");
        
        // 处理updateBy字段
        Object updateBy = getFieldValue(entity, UPDATE_BY_FIELD);
        if (updateBy != null) {
            sql.append(", " + UPDATE_BY_FIELD + " = #{" + UPDATE_BY_FIELD + "}");
        }
        
        // 构建ids IN条件
        sql.append(" WHERE " + PRIMARY_KEY + " IN <foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach> AND " + DELETED_FIELD + " = 0");
        
        log.debug("DeleteByIds SQL: {}", sql.toString());
        return sql.toString();
    }

    /**
     * 根据ID更新单条数据（全字段更新），带乐观锁
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String updateById(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        List<Field> fields = getEntityFields(entity.getClass());
        StringBuilder updateSet = new StringBuilder();
        
        for (Field field : fields) {
            String fieldName = field.getName();
            // 跳过主键、特殊字段以及deleted字段
            if (PRIMARY_KEY.equals(fieldName) || "timeFrom".equals(fieldName) || "timeTo".equals(fieldName) || "orderBy".equals(fieldName) ||
                "ids".equals(fieldName) || "keyword".equals(fieldName) || "pageNum".equals(fieldName) ||
                "pageSize".equals(fieldName) || "offset".equals(fieldName) || "limit".equals(fieldName) || "deleted".equals(fieldName) || VERSION_FIELD.equals(fieldName)) {
                continue;
            }
            
            if (updateSet.length() > 0) {
                updateSet.append(", ");
            }
            
            updateSet.append(camelToUnderline(fieldName));
            updateSet.append(" = #{");
            updateSet.append(fieldName);
            updateSet.append("}");
        }
        
        // 添加更新时间和version自增
        updateSet.append(", " + UPDATE_TIME_FIELD + " = NOW()");
        updateSet.append(", version = version + 1");
        
        // 获取id和version字段值
        Object id = getFieldValue(entity, PRIMARY_KEY);
        Object version = getFieldValue(entity, VERSION_FIELD);
        
        if (id == null) {
            return "";
        }
        
        String sql = "UPDATE " + tableName + " SET " + updateSet + " WHERE " + PRIMARY_KEY + " = #{" + PRIMARY_KEY + "}" + " AND " + DELETED_FIELD + " = 0";
        if (version != null) {
            sql += " AND version = #{version}";
        }
        
        log.debug("UpdateById SQL: {}", sql);
        return sql;
    }

    /**
     * 根据ID更新单条数据（只更新非空字段），带乐观锁
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String updateByIdSelective(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        List<Field> fields = getEntityFields(entity.getClass());
        StringBuilder updateSet = new StringBuilder();
        
        for (Field field : fields) {
            String fieldName = field.getName();
            // 跳过主键、特殊字段以及deleted字段
            if (PRIMARY_KEY.equals(fieldName) || "timeFrom".equals(fieldName) || "timeTo".equals(fieldName) || "orderBy".equals(fieldName) ||
                "ids".equals(fieldName) || "keyword".equals(fieldName) || "pageNum".equals(fieldName) ||
                "pageSize".equals(fieldName) || "offset".equals(fieldName) || "limit".equals(fieldName) || "deleted".equals(fieldName) || VERSION_FIELD.equals(fieldName)) {
                continue;
            }
            
            Object value = getFieldValue(entity, field);
            // 跳过空值字段
            if (value == null) {
                continue;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            
            if (updateSet.length() > 0) {
                updateSet.append(", ");
            }
            
            updateSet.append(camelToUnderline(fieldName));
            updateSet.append(" = #{");
            updateSet.append(fieldName);
            updateSet.append("}");
        }
        
        // 添加更新时间和version自增
        updateSet.append(", " + UPDATE_TIME_FIELD + " = NOW()");
        updateSet.append(", version = version + 1");
        
        // 获取id和version字段值
        Object id = getFieldValue(entity, PRIMARY_KEY);
        Object version = getFieldValue(entity, VERSION_FIELD);
        
        if (id == null) {
            return "";
        }
        
        String sql = "UPDATE " + tableName + " SET " + updateSet + " WHERE " + PRIMARY_KEY + " = #{" + PRIMARY_KEY + "}" + " AND " + DELETED_FIELD + " = 0";
        if (version != null) {
            sql += " AND version = #{version}";
        }
        
        log.debug("UpdateByIdSelective SQL: {}", sql);
        return sql;
    }

    /**
     * 批量更新数据，不带乐观锁
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String updateBatch(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        List<Field> fields = getEntityFields(entity.getClass());
        StringBuilder updateSet = new StringBuilder();
        
        for (Field field : fields) {
            String fieldName = field.getName();
            // 跳过主键、特殊字段以及deleted字段
            if (PRIMARY_KEY.equals(fieldName) || "timeFrom".equals(fieldName) || "timeTo".equals(fieldName) || "orderBy".equals(fieldName) ||
                "ids".equals(fieldName) || "keyword".equals(fieldName) || "pageNum".equals(fieldName) ||
                "pageSize".equals(fieldName) || "offset".equals(fieldName) || "limit".equals(fieldName) || "deleted".equals(fieldName) || VERSION_FIELD.equals(fieldName)) {
                continue;
            }
            
            Object value = getFieldValue(entity, field);
            // 跳过空值字段
            if (value == null) {
                continue;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            
            if (updateSet.length() > 0) {
                updateSet.append(", ");
            }
            
            updateSet.append(camelToUnderline(fieldName));
            updateSet.append(" = #{");
            updateSet.append(fieldName);
            updateSet.append("}");
        }
        
        // 添加更新时间
        updateSet.append(", " + UPDATE_TIME_FIELD + " = NOW()");
        
        // 构建ids IN条件
        String sql = "UPDATE " + tableName + " SET " + updateSet + " WHERE " + PRIMARY_KEY + " IN <foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach> AND " + DELETED_FIELD + " = 0";
        
        log.debug("UpdateBatch SQL: {}", sql);
        return sql;
    }

    /**
     * 根据ID查询单条数据
     * @param params 包含id和entityClass的参数
     * @return SQL字符串
     */
    public String selectById(java.util.Map<String, Object> params) {
        Long id = (Long) params.get("id");
        Class<?> entityClass = (Class<?>) params.get("entityClass");
        
        if (id == null || entityClass == null) {
            return "";
        }
        
        String tableName = getTableName(entityClass);
        String sql = "SELECT * FROM " + tableName + " WHERE " + PRIMARY_KEY + " = #{id} AND " + DELETED_FIELD + " = 0";
        
        log.debug("SelectById SQL: {}", sql);
        return sql;
    }

    /**
     * 根据ID列表查询多条数据
     * @param params 包含ids和entityClass的参数
     * @return SQL字符串
     */
    public String selectByIds(java.util.Map<String, Object> params) {
        List<Long> ids = (List<Long>) params.get("ids");
        Class<?> entityClass = (Class<?>) params.get("entityClass");
        
        if (ids == null || ids.isEmpty() || entityClass == null) {
            return "";
        }
        
        String tableName = getTableName(entityClass);
        String sql = "SELECT * FROM " + tableName + " WHERE " + PRIMARY_KEY + " IN <foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach> AND " + DELETED_FIELD + " = 0";
        
        log.debug("SelectByIds SQL: {}", sql);
        return sql;
    }

    /**
     * 查询所有数据
     * @param params 包含entityClass的参数
     * @return SQL字符串
     */
    public String selectAll(java.util.Map<String, Object> params) {
        Class<?> entityClass = (Class<?>) params.get("entityClass");
        
        if (entityClass == null) {
            return "";
        }
        
        String tableName = getTableName(entityClass);
        String sql = "SELECT * FROM " + tableName + " WHERE " + DELETED_FIELD + " = 0 ORDER BY " + PRIMARY_KEY + " DESC";
        
        log.debug("SelectAll SQL: {}", sql);
        return sql;
    }

    /**
     * 根据实体条件查询数据
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String selectByEntity(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        String whereClause = buildWhereClause(entity);
        String sql = "SELECT * FROM " + tableName + " WHERE " + whereClause;
        
        // 处理排序
        Object orderBy = getFieldValue(entity, "orderBy");
        if (orderBy != null && !((String) orderBy).trim().isEmpty()) {
            sql += " ORDER BY " + orderBy;
        } else {
            sql += " ORDER BY " + PRIMARY_KEY + " DESC";
        }
        
        log.debug("SelectByEntity SQL: {}", sql);
        return sql;
    }

    /**
     * 根据实体条件分页查询数据
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String selectByEntityWithPage(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        String whereClause = buildWhereClause(entity);
        String sql = "SELECT * FROM " + tableName + " WHERE " + whereClause;
        
        // 处理排序
        Object orderBy = getFieldValue(entity, "orderBy");
        if (orderBy != null && !((String) orderBy).trim().isEmpty()) {
            sql += " ORDER BY " + orderBy;
        } else {
            sql += " ORDER BY " + PRIMARY_KEY + " DESC";
        }
        
        // 处理分页
        sql += " LIMIT #{offset}, #{limit}";
        
        log.debug("SelectByEntityWithPage SQL: {}", sql);
        return sql;
    }

    /**
     * 根据实体条件统计数据数量
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String selectCountByEntity(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        String whereClause = buildWhereClause(entity);
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause;
        
        log.debug("SelectCountByEntity SQL: {}", sql);
        return sql;
    }

    /**
     * 根据实体条件查询单条数据
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String selectOneByEntity(BaseEntity entity) {
        String tableName = getTableName(entity.getClass());
        
        String whereClause = buildWhereClause(entity);
        String sql = "SELECT * FROM " + tableName + " WHERE " + whereClause + " LIMIT 1";
        
        log.debug("SelectOneByEntity SQL: {}", sql);
        return sql;
    }
}