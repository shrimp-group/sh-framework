package com.wkclz.core.base;

/**
 * 分页接口，定义分页参数的获取与初始化
 * <p>
 * 实现此接口的类应提供分页页码（current）和分页大小（size）的获取方法，
 * 并可通过默认的 {@link #init()} 方法进行参数校验与默认值设置。
 * </p>
 *
 * @author wkclz
 */
public interface Pageable {

    /**
     * 默认分页页码
     */
    long DEFAULT_CURRENT = 1L;

    /**
     * 默认分页大小
     */
    long DEFAULT_SIZE = 10L;

    /**
     * 获取当前页码
     *
     * @return 当前页码，从 1 开始
     */
    Long getCurrent();

    /**
     * 设置当前页码
     *
     * @param current 当前页码
     */
    void setCurrent(Long current);

    /**
     * 获取分页大小
     *
     * @return 每页数据条数
     */
    Long getSize();

    /**
     * 设置分页大小
     *
     * @param size 每页数据条数
     */
    void setSize(Long size);

    /**
     * 获取偏移量
     *
     * @return 偏移量，用于数据库查询
     */
    Long getOffset();

    /**
     * 设置偏移量
     *
     * @param offset 偏移量
     */
    void setOffset(Long offset);

    /**
     * 初始化分页参数
     * <p>
     * 处理空值和非法值：
     * <ul>
     *   <li>如果 current 为 null 或小于 1，则设置为默认值 1</li>
     *   <li>如果 size 为 null 或小于 1，则设置为默认值 10</li>
     *   <li>根据 current 和 size 计算 offset</li>
     * </ul>
     * </p>
     */
    default void init() {
        Long current = getCurrent();
        if (current == null || current < 1) {
            setCurrent(DEFAULT_CURRENT);
        }

        Long size = getSize();
        if (size == null || size < 1) {
            setSize(DEFAULT_SIZE);
        }

        // 计算偏移量：(current - 1) * size
        long offset = (getCurrent() - 1) * getSize();
        setOffset(offset);
    }

}