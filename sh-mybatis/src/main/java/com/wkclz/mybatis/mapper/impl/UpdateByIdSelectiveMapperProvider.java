package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class UpdateByIdSelectiveMapperProvider extends BaseMapperProvider {


    /**
     * 根据ID更新单条数据（只更新非空字段），带乐观锁
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String updateByIdSelective(BaseEntity entity) throws IllegalAccessException {
        if (entity == null) {
            throw ValidationException.of("实体对象不能为空");
        }
        DbEntityProperty property = getDbEntityProperty(entity.getClass());
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;
        String deleted = DbEntityProperty.DELETED_FIELD;
        String version = DbEntityProperty.VERSION_FIELD;

        // 获取id
        Object id = property.getIdField().getField().get(entity);
        if (id == null) {
            throw ValidationException.of("ID不能为空");
        }

        StringBuilder updateSet = new StringBuilder();
        for (JavaField field : property.getUpdateFields()) {
            String fieldName = field.getFieldName();

            Object value = field.getField().get(entity);
            // 跳过空值字段
            if (value == null) {
                continue;
            }
            if (!updateSet.isEmpty()) {
                updateSet.append(", ");
            }
            updateSet.append(field.getColumnName());
            updateSet.append(" = #{");
            updateSet.append(fieldName);
            updateSet.append("}");
        }

        // 添加version自增
        updateSet.append(", ").append(version).append(" = ").append(version).append(" + 1");

        // 处理updateBy字段
        JavaField updateByField = property.getUpdateByField();
        if (updateByField.getField().get(entity) != null) {
            updateSet.append(", ").append(updateByField.getColumnName()).append(" = #{").append(updateByField.getFieldName()).append("}");
        }

        // 获取id和version字段值
        Object versionValue = property.getVersionByField().getField().get(entity);
        String sql = "UPDATE " + tableName + " SET " + updateSet + " WHERE " + primaryKey + " = #{" + primaryKey + "}" + " AND " + deleted + " = 0";
        if (versionValue != null) {
            sql += " AND " + version + " = #{" + property.getVersionByField().getFieldName() + "}";
        }
        
        log.debug("UpdateByIdSelective SQL: {}", sql);
        return sql;
    }

}