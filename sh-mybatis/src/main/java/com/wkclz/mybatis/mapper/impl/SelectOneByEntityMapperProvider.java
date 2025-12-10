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
public class SelectOneByEntityMapperProvider extends BaseMapperProvider {

    /**
     * 根据实体条件查询单条数据
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String selectOneByEntity(BaseEntity entity) throws IllegalAccessException {
        Class<?> entityClass = entity.getClass();
        DbEntityProperty property = getDbEntityProperty(entity.getClass());
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;


        List<String> selectFields = property.getFields().stream().map(JavaField::getColumnName).toList();
        String selectFieldsStr = String.join(",", selectFields);

        String whereClause = buildWhereClause(entity);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectFieldsStr).append(" FROM ").append(tableName).append(" WHERE ").append(whereClause).append(" LIMIT 1");
        
        log.debug("SelectOneByEntity SQL: {}", sql);
        return sql.toString();
    }
}