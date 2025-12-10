package com.wkclz.mybatis.mapper;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import com.wkclz.mybatis.mapper.impl.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * BaseMapper 接口，提供数据库基本单表操作
 *
 * @param <T> 实体类类型
 */
public interface BaseMapper<T extends BaseEntity> {

    @Desc("插入单条数据")
    @InsertProvider(type = InsertMapperProvider.class, method = "insert")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(T entity);
    
    @Desc("批量插入数据，注意控制批量大小，避免单次插入过多数据")
    @InsertProvider(type = InsertBatchMapperProvider.class, method = "insertBatch")
    int insertBatch(@Param("entities") List<T> entities);


    
    @Desc("根据ID删除单条数据")
    @DeleteProvider(type = DeleteByIdMapperProvider.class, method = "deleteById")
    int deleteById(T entity);
    
    @Desc("根据ID列表批量删除数据")
    @DeleteProvider(type = DeleteByIdsMapperProvider.class, method = "deleteByIds")
    int deleteByIds(T entity);


    
    @Desc("根据ID更新单条数据（全字段更新），带乐观锁")
    @UpdateProvider(type = UpdateByIdMapperProvider.class, method = "updateById")
    int updateById(T entity);
    
    @Desc("根据ID更新单条数据（只更新非空字段），带乐观锁")
    @UpdateProvider(type = UpdateByIdSelectiveMapperProvider.class, method = "updateByIdSelective")
    int updateByIdSelective(T entity);
    
    @Desc("批量更新数据，不带乐观锁")
    @UpdateProvider(type = UpdateBatchMapperProvider.class, method = "updateBatch")
    int updateBatch(T entity);


    
    @Desc("根据ID查询单条数据")
    @SelectProvider(type = SelectByIdMapperProvider.class, method = "selectById")
    T selectById(@Param("id") Long id);
    
    @Desc("根据ID列表查询多条数据，不带 Blob 字段")
    @SelectProvider(type = SelectByIdsMapperProvider.class, method = "selectByIds")
    List<T> selectByIds(@Param("ids") List<Long> ids);
    
    @Desc("查询所有数据，不带 Blob 字段")
    @SelectProvider(type = SelectAllMapperProvider.class, method = "selectAll")
    List<T> selectAll();
    
    @Desc("根据实体条件查询数据，不带 Blob 字段")
    @SelectProvider(type = SelectByEntityMapperProvider.class, method = "selectByEntity")
    List<T> selectByEntity(T entity);
    
    @Desc("根据实体条件分页查询数据，不带 Blob 字段")
    @SelectProvider(type = SelectByEntityWithLimitMapperProvider.class, method = "selectByEntityWithLimit")
    List<T> selectByEntityWithLimit(T entity);
    
    @Desc("根据实体条件统计数据数量")
    @SelectProvider(type = SelectCountByEntityMapperProvider.class, method = "selectCountByEntity")
    long selectCountByEntity(T entity);
    
    @Desc("根据实体条件查询单条数据，确保查询条件唯一，否则会抛出异常")
    @SelectProvider(type = SelectOneByEntityMapperProvider.class, method = "selectOneByEntity")
    T selectOneByEntity(T entity);

}