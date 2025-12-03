package com.wkclz.mybatis.mapper;

import com.wkclz.core.base.BaseEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * BaseMapper 接口，提供数据库基本单表操作
 *
 * @param <T> 实体类类型
 */
public interface BaseMapper<T extends BaseEntity> {

    int insert(T entity);
    int insertBatch(List<T> entities);
    int deleteById(T entity);
    int deleteByIds(T entity);
    int updateById(T entity);
    int updateByIdSelective(T entity);
    int updateBatch(T entity);
    T selectById(@Param("id") Long id);
    List<T> selectByIds(@Param("ids") List<Long> ids);
    List<T> selectAll();
    List<T> selectByEntity(T entity);
    List<T> selectByEntityWithPage(T entity);
    long selectCountByEntity(T entity);
    T selectOneByEntity(T entity);

}