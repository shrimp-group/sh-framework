package com.wkclz.mybatis.service;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.base.PageData;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.session.RowBounds;
import java.util.List;

/**
 * BaseService 接口，提供单表操作的服务层封装
 *
 * @param <T> 实体类类型
 */
public interface BaseService<T extends BaseEntity> {

    /**
     * 获取对应的Mapper
     * @return Mapper对象
     */
    BaseMapper<T> getMapper();

    /**
     * 插入单条数据
     * @param entity 实体对象
     * @return 插入结果
     */
    int insert(T entity);

    /**
     * 批量插入数据
     * @param entities 实体对象列表
     * @return 插入结果
     */
    int insertBatch(List<T> entities);

    /**
     * 根据ID删除单条数据
     * @param entity 实体对象
     * @return 删除结果
     */
    int deleteById(T entity);

    /**
     * 根据ID列表批量删除数据
     * @param entity 实体对象
     * @return 删除结果
     */
    int deleteByIds(T entity);

    /**
     * 根据ID更新单条数据（全字段更新）
     * @param entity 实体对象
     * @return 更新结果
     */
    int updateById(T entity);

    /**
     * 根据ID更新单条数据（只更新非空字段）
     * @param entity 实体对象
     * @return 更新结果
     */
    int updateByIdSelective(T entity);

    /**
     * 批量更新数据
     * @param entity 实体对象
     * @return 更新结果
     */
    int updateBatch(T entity);

    /**
     * 根据ID查询单条数据
     * @param id 主键ID
     * @return 实体对象
     */
    T selectById(Long id);

    /**
     * 根据ID列表查询多条数据
     * @param ids 主键ID列表
     * @return 实体对象列表
     */
    List<T> selectByIds(List<Long> ids);

    /**
     * 查询所有数据
     * @return 实体对象列表
     */
    List<T> selectAll();

    /**
     * 根据实体条件查询数据
     * @param entity 实体对象
     * @return 实体对象列表
     */
    List<T> selectByEntity(T entity);

    /**
     * 根据实体条件查询单条数据
     * @param entity 实体对象
     * @return 实体对象
     */
    T selectOneByEntity(T entity);

    /**
     * 根据实体条件统计数据数量
     * @param entity 实体对象
     * @return 数据数量
     */
    long selectCountByEntity(T entity);

    /**
     * 单表分页查询，返回PageData对象
     * @param entity 实体对象，包含分页条件
     * @return 分页结果PageData对象
     */
    PageData<T> selectPage(T entity);
}