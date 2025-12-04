package com.wkclz.mybatis.service.impl;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.base.PageData;
import com.wkclz.mybatis.exception.MyBatisException;
import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.mybatis.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BaseService 实现类，提供单表操作的服务层实现
 *
 * @param <T> 实体类类型
 */
@Service
@Transactional
public class BaseServiceImpl<T extends BaseEntity> implements BaseService<T> {

    /**
     * 获取对应的Mapper
     * @return Mapper对象
     */
    @Override
    public BaseMapper<T> getMapper() {
        // 子类需要实现此方法，返回对应的Mapper实例
        throw new MyBatisException("子类必须实现getMapper()方法");
    }

    /**
     * 插入单条数据
     * @param entity 实体对象
     * @return 插入结果
     */
    @Override
    public int insert(T entity) {
        return getMapper().insert(entity);
    }

    /**
     * 批量插入数据
     * @param entities 实体对象列表
     * @return 插入结果
     */
    @Override
    public int insertBatch(List<T> entities) {
        return getMapper().insertBatch(entities);
    }

    /**
     * 根据ID删除单条数据
     * @param entity 实体对象
     * @return 删除结果
     */
    @Override
    public int deleteById(T entity) {
        return getMapper().deleteById(entity);
    }

    /**
     * 根据ID列表批量删除数据
     * @param entity 实体对象
     * @return 删除结果
     */
    @Override
    public int deleteByIds(T entity) {
        return getMapper().deleteByIds(entity);
    }

    /**
     * 根据ID更新单条数据（全字段更新）
     * @param entity 实体对象
     * @return 更新结果
     */
    @Override
    public int updateById(T entity) {
        return getMapper().updateById(entity);
    }

    /**
     * 根据ID更新单条数据（只更新非空字段）
     * @param entity 实体对象
     * @return 更新结果
     */
    @Override
    public int updateByIdSelective(T entity) {
        return getMapper().updateByIdSelective(entity);
    }

    /**
     * 批量更新数据
     * @param entity 实体对象
     * @return 更新结果
     */
    @Override
    public int updateBatch(T entity) {
        return getMapper().updateBatch(entity);
    }

    /**
     * 根据ID查询单条数据
     * @param id 主键ID
     * @return 实体对象
     */
    @Override
    public T selectById(Long id) {
        return getMapper().selectById(id);
    }

    /**
     * 根据ID列表查询多条数据
     * @param ids 主键ID列表
     * @return 实体对象列表
     */
    @Override
    public List<T> selectByIds(List<Long> ids) {
        return getMapper().selectByIds(ids);
    }

    /**
     * 查询所有数据
     * @return 实体对象列表
     */
    @Override
    public List<T> selectAll() {
        return getMapper().selectAll();
    }

    /**
     * 根据实体条件查询数据
     * @param entity 实体对象
     * @return 实体对象列表
     */
    @Override
    public List<T> selectByEntity(T entity) {
        return getMapper().selectByEntity(entity);
    }

    /**
     * 根据实体条件查询单条数据
     * @param entity 实体对象
     * @return 实体对象
     */
    @Override
    public T selectOneByEntity(T entity) {
        return getMapper().selectOneByEntity(entity);
    }

    /**
     * 根据实体条件统计数据数量
     * @param entity 实体对象
     * @return 数据数量
     */
    @Override
    public long selectCountByEntity(T entity) {
        return getMapper().selectCountByEntity(entity);
    }

    /**
     * 单表分页查询，返回PageData对象
     * @param entity 实体对象，包含分页条件
     * @return 分页结果PageData对象
     */
    @Override
    public PageData<T> selectPage(T entity) {
        // 初始化分页参数
        entity.init();
        
        // 查询数据列表
        List<T> records = getMapper().selectByEntityWithLimit(entity);
        
        // 查询总数据量
        long total = getMapper().selectCountByEntity(entity);
        
        // 设置总数据量到实体对象，用于PageData转换
        entity.setCount(total);
        entity.setTotal(total);
        
        // 使用PageData封装结果
        return PageData.fromEntity(entity, records);
    }

}
