package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.exception.ValidationException;
import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.annotation.ProviderContext;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class SelectByIdsMapperProvider extends BaseMapperProvider {


    /**
     * 根据ID列表查询多条数据
     * @param params 包含ids的参数
     * @param context ProviderContext对象，可以获取当前Mapper接口类型
     * @return SQL字符串
     */
    public String selectByIds(Map<String, Object> params, ProviderContext context) {
        Object idsObj = params.get("ids");
        if (idsObj == null) {
            throw ValidationException.of("ids不能为空");
        }
        if (!(idsObj instanceof List<?> idList)) {
            throw ValidationException.of("ids必须是List类型");
        }
        if (idList.isEmpty()) {
            throw ValidationException.of("ids列表不能为空");
        }
        Class<?> entityClass = getEntityClassFromContext(context);
        if (entityClass == null) {
            throw ValidationException.of("无法确认查询实体");
        }
        DbEntityProperty property = getDbEntityProperty(entityClass);
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;
        String deleted = DbEntityProperty.DELETED_FIELD;

        List<String> selectFields = property.getSelectListFields().stream().map(JavaField::getColumnName).toList();
        String selectFieldsStr = String.join(",", selectFields);
        
        // 构建ids IN条件
        StringBuilder inClause = new StringBuilder();
        inClause.append("(");
        for (int i = 0; i < idList.size(); i++) {
            if (i > 0) {
                inClause.append(", ");
            }
            inClause.append("#{ids[").append(i).append("]}");
        }
        inClause.append(")");
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectFieldsStr).append(" FROM ").append(tableName).append(" WHERE ").append(primaryKey).append(" IN ").append(inClause).append(" AND ").append(deleted).append(" = 0");
        
        log.debug("SelectByIds SQL: {}", sql);
        return sql.toString();
    }

}