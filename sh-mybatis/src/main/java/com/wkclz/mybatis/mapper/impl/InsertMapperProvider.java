package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class InsertMapperProvider extends BaseMapperProvider {


    /**
     * 插入单条数据，跳过空字段
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String insert(BaseEntity entity) throws IllegalAccessException {
        DbEntityProperty property = getDbEntityProperty(entity.getClass());
        String tableName = property.getTableName();
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        for (JavaField f : property.getInsertFields()) {
            Object value = f.getField().get(entity);
            // 跳过空值字段
            if (value == null) {
                continue;
            }
            if (!columns.isEmpty()) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(f.getColumnName());
            values.append("#{");
            values.append(f.getFieldName());
            values.append("}");
        }
        
        // 添加缺失的自动赋值字段
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        
        log.debug("Insert SQL: {}", sql);
        return sql;
    }

}