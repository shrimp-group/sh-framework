package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.mybatis.mapper.BaseMapper;

import java.util.List;

public class BaseMapperProvider<T extends BaseEntity> implements BaseMapper<T> {

    @Override
    public int insert(T entity) {
        return 0;
    }

    @Override
    public int insertBatch(List<T> entities) {
        return 0;
    }

    @Override
    public int deleteById(T entity) {
        return 0;
    }

    @Override
    public int deleteByIds(T entity) {
        return 0;
    }

    @Override
    public int updateById(T entity) {
        return 0;
    }

    @Override
    public int updateByIdSelective(T entity) {
        return 0;
    }

    @Override
    public int updateBatch(T entity) {
        return 0;
    }

    @Override
    public T selectById(Long id) {
        return null;
    }

    @Override
    public List<T> selectByIds(List<Long> ids) {
        return List.of();
    }

    @Override
    public List<T> selectAll() {
        return List.of();
    }

    @Override
    public List<T> selectByEntity(T entity) {
        return List.of();
    }

    @Override
    public List<T> selectByEntityWithPage(T entity) {
        return List.of();
    }

    @Override
    public long selectCountByEntity(T entity) {
        return 0;
    }

    @Override
    public T selectOneByEntity(T entity) {
        return null;
    }
}