package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.exception.ValidationException;
import com.wkclz.core.user.UserContext;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.util.List;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class DeleteByIdsMapperProvider extends BaseMapperProvider {

    /**
     * 根据ID列表批量删除数据，采用逻辑删除
     * @param ids 实体对象
     * @return SQL字符串
     */
    public String deleteByIds(List<Long> ids, ProviderContext context) throws IllegalAccessException {
        if (CollectionUtils.isEmpty(ids)) {
            throw ValidationException.of("ids不能为空");
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

        // 构建ids IN条件
        String inClause = buildInClause(ids, "ids");
        sql.append(" WHERE ").append(primaryKey).append(" IN ").append(inClause).append(" AND ").append(deleted).append(" = 0");
        
        log.debug("DeleteByIds SQL: {}", sql.toString());
        return sql.toString();
    }

}