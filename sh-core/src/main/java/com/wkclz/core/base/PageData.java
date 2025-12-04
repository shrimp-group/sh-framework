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
}
