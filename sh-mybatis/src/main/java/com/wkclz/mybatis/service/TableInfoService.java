package com.wkclz.mybatis.service;

import com.wkclz.mybatis.bean.ColumnInfo;
import com.wkclz.mybatis.bean.ColumnQuery;
import com.wkclz.mybatis.bean.IndexInfo;
import com.wkclz.mybatis.bean.TableInfo;
import com.wkclz.mybatis.config.ShMyBatisConfig;
import com.wkclz.mybatis.mapper.TableInfoMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @author shrimp
 */
@Service
public class TableInfoService {

    @Resource
    private ShMyBatisConfig config;
    @Resource
    private TableInfoMapper tableInfoMapper;

    public List<TableInfo> getTables(TableInfo entity) {
        if (entity == null) {
            entity = new TableInfo();
        }
        if (StringUtils.isBlank(entity.getTableSchema())) {
            entity.setTableSchema(config.getTableSchema());
        }
        return tableInfoMapper.getTables(entity);
    }

    public List<ColumnInfo> getColumns(TableInfo entity) {
        if (entity == null) {
            entity = new TableInfo();
        }
        if (StringUtils.isBlank(entity.getTableSchema())) {
            entity.setTableSchema(config.getTableSchema());
        }
        return tableInfoMapper.getColumns(entity);
    }

    public List<IndexInfo> getIndexs(TableInfo entity) {
        if (entity == null) {
            entity = new TableInfo();
        }
        if (StringUtils.isBlank(entity.getTableSchema())) {
            entity.setTableSchema(config.getTableSchema());
        }
        return tableInfoMapper.getIndexs(entity);
    }


    public List<ColumnQuery> getColumnInfos4Options(ColumnQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(query.getTableSchema())) {
            query.setTableSchema(config.getTableSchema());
        }
        return tableInfoMapper.getColumnInfos4Options(query);
    }

}
