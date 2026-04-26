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
public class SelectByEntityWithLimitMapperProvider extends BaseMapperProvider {

    /**
     * 根据实体条件分页查询数据
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String selectByEntityWithLimit(BaseEntity entity) throws IllegalAccessException {
        Class<?> entityClass = entity.getClass();
        DbEntityProperty property = getDbEntityProperty(entityClass);
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;


        List<String> selectFields = property.getSelectListFields().stream().map(JavaField::getColumnName).toList();
        String selectFieldsStr = String.join(",", selectFields);
        String whereClause = buildWhereClause(entity);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectFieldsStr).append(" FROM ").append(tableName).append(" WHERE ").append(whereClause);
        
        // 处理排序（防 SQL 注入）
        sql.append(buildOrderByClause(entity.getOrderBy(), property, primaryKey + " DESC"));
        
        // 处理分页
        sql.append(" LIMIT #{offset}, #{size}");
        
        log.debug("SelectByEntityWithPage SQL: {}", sql);
        return sql.toString();
    }

}