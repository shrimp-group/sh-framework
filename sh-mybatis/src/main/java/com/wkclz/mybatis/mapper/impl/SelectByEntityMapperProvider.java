package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class SelectByEntityMapperProvider extends BaseMapperProvider {


    /**
     * 根据实体条件查询数据
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String selectByEntity(BaseEntity entity) throws IllegalAccessException {
        Class<?> entityClass = entity.getClass();
        DbEntityProperty property = getDbEntityProperty(entityClass);
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;

        List<String> selectFields = property.getSelectListFields().stream().map(JavaField::getColumnName).toList();
        String selectFieldsStr = String.join(",", selectFields);
        String whereClause = buildWhereClause(entity);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectFieldsStr).append(" FROM ").append(tableName).append(" WHERE ").append(whereClause);
        
        // 处理排序
        Object orderBy = getFieldValue(entity, "orderBy");
        if (orderBy != null && !((String) orderBy).trim().isEmpty()) {
            sql.append(" ORDER BY ").append(orderBy);
        } else {
            sql.append(" ORDER BY ").append(primaryKey).append(" DESC");
        }
        
        log.debug("SelectByEntity SQL: {}", sql);
        return sql.toString();
    }

}