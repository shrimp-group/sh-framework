package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class DeleteByIdsMapperProvider extends BaseMapperProvider {

    /**
     * 根据ID列表批量删除数据，采用逻辑删除
     * @param entity 实体对象
     * @return SQL字符串
     */
    public String deleteByIds(BaseEntity entity) throws IllegalAccessException {
        if (entity == null) {
            throw ValidationException.of("实体对象不能为空");
        }
        DbEntityProperty property = getDbEntityProperty(entity.getClass());
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;
        String deleted = DbEntityProperty.DELETED_FIELD;
        String version = DbEntityProperty.VERSION_FIELD;

        // 获取ids字段值
        Object ids = property.getIdsField().getField().get(entity);
        if (ids == null) {
            throw ValidationException.of("ids不能为空");
        }
        if (!(ids instanceof List<?> idList)) {
            throw ValidationException.of("ids必须是List类型");
        }
        if (idList.isEmpty()) {
            throw ValidationException.of("ids列表不能为空");
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ").append(deleted).append(" = DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%m')");
        sql.append(", ").append(version).append(" = ").append(version).append(" + 1");
        
        // 处理updateBy字段
        JavaField updateByField = property.getUpdateByField();
        if (updateByField.getField().get(entity) != null) {
            sql.append(", ").append(updateByField.getColumnName()).append(" = #{").append(updateByField.getFieldName()).append("}");
        }

        // 构建ids IN条件
        String inClause = buildInClause(idList, "ids");
        sql.append(" WHERE ").append(primaryKey).append(" IN ").append(inClause).append(" AND ").append(deleted).append(" = 0");
        
        log.debug("DeleteByIds SQL: {}", sql.toString());
        return sql.toString();
    }

}