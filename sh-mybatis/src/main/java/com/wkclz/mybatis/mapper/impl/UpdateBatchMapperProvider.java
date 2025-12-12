package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import com.wkclz.tool.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class UpdateBatchMapperProvider extends BaseMapperProvider {



    /**
     * 批量更新数据，不带乐观锁
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String updateBatch(BaseEntity entity) throws IllegalAccessException {
        DbEntityProperty property = getDbEntityProperty(entity.getClass());
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;
        String deleted = DbEntityProperty.DELETED_FIELD;
        String version = DbEntityProperty.VERSION_FIELD;

        StringBuilder updateSet = new StringBuilder();
        
        for (JavaField field : property.getUpdateFields()) {
            String fieldName = field.getFieldName();
            String columnName = field.getColumnName();

            Object value = field.getField().get(entity);
            if (value == null) {
                continue;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            
            if (!updateSet.isEmpty()) {
                updateSet.append(", ");
            }
            
            updateSet.append(columnName);
            updateSet.append(" = #{");
            updateSet.append(fieldName);
            updateSet.append("}");
        }

        // 处理updateBy字段
        JavaField updateByField = property.getUpdateByField();
        if (updateByField.getField().get(entity) != null) {
            updateSet.append(", ").append(updateByField.getColumnName()).append(" = #{").append(updateByField.getFieldName()).append("}");
        }
        // 添加version自增
        updateSet.append(", ").append(version).append(" = ").append(version).append(" + 1");
        
        // 构建ids IN条件
        Object idsObj = property.getIdsField().getField().get(entity);
        if (!(idsObj instanceof List<?> ids)) {
            return "";
        }
        String inClause = buildInClause(ids, "ids");

        String sql = "UPDATE " + tableName + " SET " + updateSet + " WHERE " + primaryKey + " IN " + inClause + " AND " + deleted + " = 0";
        
        log.debug("UpdateBatch SQL: {}", sql);
        return sql;
    }

}