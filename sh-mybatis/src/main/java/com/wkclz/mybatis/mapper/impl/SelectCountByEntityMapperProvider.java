package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.mybatis.bean.DbEntityProperty;
import lombok.extern.slf4j.Slf4j;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class SelectCountByEntityMapperProvider extends BaseMapperProvider {

    /**
     * 根据实体条件统计数据数量
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String selectCountByEntity(BaseEntity entity) throws IllegalAccessException {
        DbEntityProperty property = getDbEntityProperty(entity.getClass());
        String tableName = property.getTableName();

        String whereClause = buildWhereClause(entity);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableName).append(" WHERE ").append(whereClause);
        
        log.debug("SelectCountByEntity SQL: {}", sql);
        return sql.toString();
    }

}