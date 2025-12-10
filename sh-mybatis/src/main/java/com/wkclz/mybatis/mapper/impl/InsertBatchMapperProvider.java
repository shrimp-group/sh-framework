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
public class InsertBatchMapperProvider extends BaseMapperProvider {


    /**
     * 批量插入数据，空值也插入
     * @param entities 实体列表
     * @return SQL字符串
     */
    public String insertBatch(List<BaseEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return "";
        }
        BaseEntity firstEntity = entities.get(0);
        DbEntityProperty property = getDbEntityProperty(firstEntity.getClass());
        String tableName = property.getTableName();

        List<JavaField> insertFields = property.getInsertFields();

        StringBuilder columns = new StringBuilder();
        for (int i = 0; i < insertFields.size(); i++) {
            if (i > 0) {
                columns.append(", ");
            }
            columns.append(insertFields.get(i).getColumnName());
        }
        
        StringBuilder values = new StringBuilder();
        // 直接构建 values 部分，不使用 foreach 标签
        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) {
                values.append(", ");
            }
            values.append("(");
            for (int j = 0; j < insertFields.size(); j++) {
                if (j > 0) {
                    values.append(", ");
                }
                // 使用正确的MyBatis OGNL表达式格式引用List中的元素
                values.append("#{entities[").append(i).append("].").append(insertFields.get(j).getFieldName()).append("}");
            }
            values.append(")");
        }
        
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES " + values;
        
        log.debug("InsertBatch SQL: {}", sql);
        return sql;
    }

}