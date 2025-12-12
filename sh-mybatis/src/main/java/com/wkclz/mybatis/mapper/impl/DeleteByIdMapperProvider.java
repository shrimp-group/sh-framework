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
public class DeleteByIdMapperProvider extends BaseMapperProvider {


    /**
     * 根据ID删除单条数据，采用逻辑删除
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String deleteById(BaseEntity entity) throws IllegalAccessException {
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
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ").append(deleted).append(" = DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%m')");
        sql.append(", ").append(version).append(" = ").append(version).append(" + 1");

        // 处理updateBy字段
        JavaField updateByField = property.getUpdateByField();
        if (updateByField.getField().get(entity) != null) {
            sql.append(", ").append(updateByField.getColumnName()).append(" = #{");
            sql.append(updateByField.getFieldName()).append("}");
        }
        sql.append(" WHERE ").append(primaryKey).append(" = #{");
        sql.append(primaryKey).append("} AND ").append(deleted).append(" = 0");

        log.debug("DeleteById SQL: {}", sql.toString());
        return sql.toString();
    }

}