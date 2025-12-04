package com.wkclz.core.base;

import com.wkclz.core.annotation.Desc;
import lombok.Data;

import java.util.List;

/**
 * 分页数据封装类
 * 用于统一封装分页查询结果
 *
 * @param <T> 分页数据类型
 */
@Data
public class PageData<T> {

    @Desc("当前页码")
    private Long current;

    @Desc("每页大小")
    private Long size;

    @Desc("偏移量")
    private Long offset;

    @Desc("总数据量")
    private Long total;

    @Desc("统计数")
    private Long count;

    @Desc("分页结果列表")
    private List<T> records;

    /**
     * 从BaseEntity初始化分页信息
     *
     * @param entity BaseEntity对象
     */
    public void initFromEntity(BaseEntity entity) {
        this.current = entity.getCurrent();
        this.size = entity.getSize();
        this.offset = entity.getOffset();
        this.total = entity.getTotal();
        this.count = entity.getCount();
    }

    /**
     * 将BaseEntity的分页信息和数据转换为PageData
     *
     * @param entity BaseEntity对象
     * @param records 分页结果列表
     * @param <T> 数据类型
     * @return PageData对象
     */
    public static <T> PageData<T> fromEntity(BaseEntity entity, List<T> records) {
        PageData<T> pageData = new PageData<>();
        pageData.initFromEntity(entity);
        pageData.setRecords(records);
        return pageData;
    }

    /**
     * 快速创建包含数据和总条数的PageData对象（默认页码1，每页大小10）
     *
     * @param records 分页结果列表
     * @param total 总数据量
     * @param <T> 数据类型
     * @return PageData对象
     */
    public static <T> PageData<T> of(List<T> records, Long total) {
        return of(records, total, 1L, 10L);
    }

    /**
     * 快速创建包含完整分页信息的PageData对象
     *
     * @param records 分页结果列表
     * @param total 总数据量
     * @param current 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return PageData对象
     */
    public static <T> PageData<T> of(List<T> records, Long total, Long current, Long size) {
        PageData<T> pageData = new PageData<>();
        pageData.setCurrent(current);
        pageData.setSize(size);
        pageData.setOffset((current - 1) * size);
        pageData.setTotal(total);
        pageData.setCount(total);
        pageData.setRecords(records);
        return pageData;
    }

    /**
     * 创建空的PageData对象
     *
     * @param <T> 数据类型
     * @return 空的PageData对象
     */
    public static <T> PageData<T> empty() {
        return empty(1L, 10L);
    }

    /**
     * 创建指定页码和大小的空PageData对象
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return 空的PageData对象
     */
    public static <T> PageData<T> empty(Long current, Long size) {
        PageData<T> pageData = new PageData<>();
        pageData.setCurrent(current);
        pageData.setSize(size);
        pageData.setOffset((current - 1) * size);
        pageData.setTotal(0L);
        pageData.setCount(0L);
        pageData.setRecords(java.util.Collections.emptyList());
        return pageData;
    }

    /**
     * 快速创建包含数据的PageData对象（自动计算总条数）
     *
     * @param records 分页结果列表
     * @param current 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return PageData对象
     */
    public static <T> PageData<T> of(List<T> records, Long current, Long size) {
        Long total = records == null ? 0L : (long) records.size();
        return of(records, total, current, size);
    }

    /**
     * 转换PageData的泛型类型和数据内容，保持分页信息不变
     *
     * @param source 源PageData对象
     * @param newRecords 新的数据列表
     * @param <T> 新的数据类型
     * @return 转换后的PageData对象
     */
    public static <T> PageData<T> convert(PageData<?> source, List<T> newRecords) {
        PageData<T> pageData = new PageData<>();
        pageData.setCurrent(source.getCurrent());
        pageData.setSize(source.getSize());
        pageData.setOffset(source.getOffset());
        pageData.setTotal(source.getTotal());
        pageData.setCount(source.getCount());
        pageData.setRecords(newRecords);
        return pageData;
    }

    /**
     * 带类型检查的转换方法，转换PageData的泛型类型和数据内容，保持分页信息不变
     *
     * @param source 源PageData对象
     * @param newRecords 新的数据列表
     * @param targetClass 目标数据类型
     * @param <T> 新的数据类型
     * @return 转换后的PageData对象
     */
    public static <T> PageData<T> convert(PageData<?> source, List<T> newRecords, Class<T> targetClass) {
        // 类型检查
        if (newRecords != null) {
            for (T item : newRecords) {
                if (item != null && !targetClass.isInstance(item)) {
                    throw new IllegalArgumentException("Record item type mismatch. Expected: " + targetClass.getName() + ", Actual: " + item.getClass().getName());
                }
            }
        }
        return convert(source, newRecords);
    }
}
