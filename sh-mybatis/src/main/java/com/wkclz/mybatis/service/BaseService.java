package com.wkclz.mybatis.service;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.base.PageData;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BaseService 实现类，提供单表操作的服务层实现
 *
 * @param <T> 实体类类型
 * @param <M> Mapper接口类型，必须继承自BaseMapper<T>
 */
@Service
@Transactional
public abstract class BaseService<T extends BaseEntity, M extends BaseMapper<T>> {

    // 每次处理的最大条数
    private static final int BATCH_SIZE = 1000;

    @Autowired
    protected M mapper;

    /**
     * 插入单条数据
     * @param entity 实体对象
     * @return 插入结果
     */
    public int insert(T entity) {
        return mapper.insert(entity);
    }

    /**
     * 批量插入数据，每次最多处理1000条
     * @param entities 实体对象列表
     * @return 插入结果
     */
    public int insertBatch(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        int totalInserted = 0;
        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, entities.size());
            List<T> batchEntities = entities.subList(i, endIndex);
            totalInserted += mapper.insertBatch(batchEntities);
        }
        return totalInserted;
    }

    /**
     * 根据ID删除单条数据
     * @param entity 实体对象
     * @return 删除结果
     */
    public int deleteById(T entity) {
        return mapper.deleteById(entity);
    }

    /**
     * 根据ID列表批量删除数据
     * @param entity 实体对象
     * @return 删除结果
     */
    public int deleteByIds(T entity) {
        return mapper.deleteByIds(entity);
    }

    /**
     * 根据ID更新单条数据（全字段更新）
     * @param entity 实体对象
     * @return 更新结果
     */
    public int updateById(T entity) {
        return mapper.updateById(entity);
    }

    /**
     * 根据ID更新单条数据（只更新非空字段）
     * @param entity 实体对象
     * @return 更新结果
     */
    public int updateByIdSelective(T entity) {
        return mapper.updateByIdSelective(entity);
    }

    /**
     * 批量更新数据
     * @param entity 实体对象
     * @return 更新结果
     */
    public int updateBatch(T entity) {
        return mapper.updateBatch(entity);
    }

    /**
     * 根据ID查询单条数据
     * @param id 主键ID
     * @return 实体对象
     */
    public T selectById(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 根据ID列表查询多条数据
     * @param ids 主键ID列表
     * @return 实体对象列表
     */
    public List<T> selectByIds(List<Long> ids) {
        return mapper.selectByIds(ids);
    }

    /**
     * 查询所有数据
     * @return 实体对象列表
     */
    public List<T> selectAll() {
        return mapper.selectAll();
    }

    /**
     * 根据实体条件查询数据
     * @param entity 实体对象
     * @return 实体对象列表
     */
    public List<T> selectByEntity(T entity) {
        return mapper.selectByEntity(entity);
    }

    /**
     * 根据实体条件查询单条数据
     * @param entity 实体对象
     * @return 实体对象
     */
    public T selectOneByEntity(T entity) {
        return mapper.selectOneByEntity(entity);
    }

    /**
     * 根据实体条件统计数据数量
     * @param entity 实体对象
     * @return 数据数量
     */
    public long selectCountByEntity(T entity) {
        return mapper.selectCountByEntity(entity);
    }

    /**
     * 单表分页查询，返回PageData对象
     * @param entity 实体对象，包含分页条件
     * @return 分页结果PageData对象
     */
    public PageData<T> selectPage(T entity) {
        // 初始化分页参数
        entity.init();

        // 查询数据列表
        List<T> records = mapper.selectByEntityWithLimit(entity);

        // 查询总数据量
        long total = mapper.selectCountByEntity(entity);
        
        // 设置总数据量到实体对象，用于PageData转换
        entity.setCount(total);
        entity.setTotal(total);
        
        // 使用PageData封装结果
        return PageData.fromEntity(entity, records);
    }

}
