package com.wkclz.mybatis.mapper.impl;

import com.wkclz.mybatis.bean.DbEntityProperty;
import com.wkclz.tool.bean.JavaField;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.util.List;
import java.util.Map;

/**
 * BaseMapperProvider 实现类，提供数据库基本操作的 SQL 构建
 */
@Slf4j
public class SelectByIdMapperProvider extends BaseMapperProvider {



    /**
     * 根据ID查询单条数据
     * @param params 包含id的参数
     * @param context ProviderContext对象，可以获取当前Mapper接口类型
     * @return SQL字符串
     */
    public String selectById(Map<String, Object> params, ProviderContext context) {
        Long id = (Long) params.get("id");
        Class<?> entityClass = getEntityClassFromContext(context);
        
        if (id == null || entityClass == null) {
            return "";
        }

        DbEntityProperty property = getDbEntityProperty(entityClass);
        String tableName = property.getTableName();
        String primaryKey = DbEntityProperty.PRIMARY_KEY;
        String deleted = DbEntityProperty.DELETED_FIELD;

        List<String> selectFields = property.getSelectObjFields().stream().map(JavaField::getColumnName).toList();
        String selectFieldsStr = String.join(",", selectFields);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(selectFieldsStr).append(" FROM ").append(tableName).append(" WHERE ").append(primaryKey).append(" = #{id} AND ").append(deleted).append(" = 0");
        
        log.debug("SelectById SQL: {}", sql);
        return sql.toString();
    }

}