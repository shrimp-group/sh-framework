package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.exception.ValidationException;
import com.wkclz.core.user.UserContext;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.annotation.ProviderContext;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class DeleteByIdMapperProvider extends BaseMapperProvider {


    /**
     * 根据ID删除单条数据，采用逻辑删除
     * @param id id
     * @return SQL字符串
     */
    public String deleteById(Long id, ProviderContext context) throws IllegalAccessException {
        if (id == null) {
            throw ValidationException.of("id 不能为空");
        }
        Class<?> entityClass = getEntityClassFromContext(context);
        if (entityClass == null) {
            throw ValidationException.of("无法确认操作实体");
        }
        DbEntityProperty property = getDbEntityProperty(entityClass);
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;
        String deleted = DbEntityProperty.DELETED_FIELD;
        String version = DbEntityProperty.VERSION_FIELD;

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ").append(deleted).append(" = DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%m')");
        sql.append(", ").append(version).append(" = ").append(version).append(" + 1");

        // 处理updateBy字段
        JavaField updateByField = property.getUpdateByField();
        if (updateByField != null) {
            String userCode = UserContext.getUserCode();
            if (userCode != null) {
                sql.append(", ").append(updateByField.getColumnName()).append(" = #{updateBy}");
            }
        }

        sql.append(" WHERE ").append(primaryKey).append(" = #{");
        sql.append(primaryKey).append("} AND ").append(deleted).append(" = 0");

        log.debug("DeleteById SQL: {}", sql.toString());
        return sql.toString();
    }

}