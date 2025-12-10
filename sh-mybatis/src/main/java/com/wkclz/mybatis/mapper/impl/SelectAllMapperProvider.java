package com.wkclz.mybatis.mapper.impl;

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
public class SelectAllMapperProvider extends BaseMapperProvider {

    /**
     * 查询所有数据
     * @param params 参数
     * @param context ProviderContext对象，可以获取当前Mapper接口类型
     * @return SQL字符串
     */
    public String selectAll(Map<String, Object> params, ProviderContext context) {
        Class<?> entityClass = getEntityClassFromContext(context);
        if (entityClass == null) {
            return "";
        }

        DbEntityProperty property = getDbEntityProperty(entityClass);
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;
        String deleted = DbEntityProperty.DELETED_FIELD;

        List<String> selectFields = property.getSelectListFields().stream().map(JavaField::getColumnName).toList();
        String selectFieldsStr = String.join(",", selectFields);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectFieldsStr).append(" FROM ").append(tableName).append(" WHERE ").append(deleted).append(" = 0 ORDER BY ").append(primaryKey).append(" DESC");
        
        log.debug("SelectAll SQL: {}", sql);
        return sql.toString();
    }

}