package com.wkclz.mybatis.helper;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.base.PageData;
import com.wkclz.core.base.Pageable;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;

/**
 * 分页查询工具类
 * <p>
 * 基于 PageHelper 实现的分页查询辅助类，支持通过 {@link BaseEntity} 或 {@link Pageable} 接口
 * 获取分页参数，并自动管理分页上下文。
 * </p>
 *
 * @author shrimp
 */
@Slf4j
public class PageQuery {

    /**
     * 基于 BaseEntity 的分页查询
     * <p>
     * 从 BaseEntity 中获取分页参数（current、size），使用 PageHelper 进行分页处理，
     * 并将查询结果封装为 {@link PageData} 返回。
     * </p>
     *
     * @param param    查询参数，必须继承 {@link BaseEntity}，包含分页参数
     * @param function 查询函数，接收查询参数并返回结果列表
     * @param <T>      实体类型，必须继承 {@link BaseEntity}
     * @return 分页数据对象，包含结果列表和总记录数
     * @throws IllegalArgumentException 如果 param 或 function 为 null
     */
    public static <T extends BaseEntity> PageData<T> page(T param, Function<T, List<T>> function) {
        try {
            param.init();
            int current = param.getCurrent().intValue();
            int size = param.getSize().intValue();
            PageHelper.startPage(current, size);

            Page listPage = (Page)function.apply(param);
            PageData<T> pageData = PageData.of(listPage.getResult(), listPage.getTotal());
            return pageData;
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 基于 Pageable 接口的分页查询（分页参数与查询参数分离）
     * <p>
     * 从 {@link Pageable} 接口获取分页参数（current、size），使用 PageHelper 进行分页处理，
     * 并将查询结果封装为 {@link PageData} 返回。此方法允许分页参数与查询参数分离，
     * 提供更灵活的分页控制。
     * </p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Pageable pageable = new PageRequest(1L, 10L);
     * UserQueryParam param = new UserQueryParam();
     * PageData<User> result = PageQuery.page(pageable, param, queryParam -> userMapper.selectByParam(queryParam));
     * }</pre>
     *
     * @param pageable  分页参数接口，提供 current 和 size 参数
     * @param param     查询参数对象，不强制继承 BaseEntity
     * @param function  查询函数，接收查询参数并返回结果列表
     * @param <T>       实体类型
     * @param <P>       查询参数类型
     * @return 分页数据对象，包含结果列表和总记录数
     * @throws IllegalArgumentException 如果 pageable、param 或 function 为 null
     */
    public static <T, P> PageData<T> page(Pageable pageable, P param, Function<P, List<T>> function) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }
        if (param == null) {
            throw new IllegalArgumentException("Param cannot be null");
        }
        if (function == null) {
            throw new IllegalArgumentException("Function cannot be null");
        }

        try {
            // 初始化分页参数
            pageable.init();
            int current = pageable.getCurrent().intValue();
            int size = pageable.getSize().intValue();

            log.debug("Starting page query with current={}, size={}", current, size);
            PageHelper.startPage(current, size);

            // 执行查询
            Page listPage = (Page) function.apply(param);
            PageData<T> pageData = PageData.of(listPage.getResult(), listPage.getTotal());

            log.debug("Page query completed, total records={}", pageData.getTotal());
            return pageData;
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 基于 Pageable 接口的分页查询（分页参数与查询参数合一）
     * <p>
     * 当查询参数本身实现了 {@link Pageable} 接口时使用。从查询参数中获取分页参数（current、size），
     * 使用 PageHelper 进行分页处理，并将查询结果封装为 {@link PageData} 返回。
     * </p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * UserQueryParam param = new UserQueryParam(); // 假设 UserQueryParam 实现了 Pageable
     * param.setCurrent(1L);
     * param.setSize(10L);
     * PageData<User> result = PageQuery.page(param, queryParam -> userMapper.selectByParam(queryParam));
     * }</pre>
     *
     * @param param     查询参数对象，必须实现 {@link Pageable} 接口，包含分页参数
     * @param function  查询函数，接收查询参数并返回结果列表
     * @param <T>       实体类型
     * @param <P>       查询参数类型，必须实现 {@link Pageable} 接口
     * @return 分页数据对象，包含结果列表和总记录数
     * @throws IllegalArgumentException 如果 param 或 function 为 null
     */
    public static <T, P extends Pageable> PageData<T> page(P param, Function<P, List<T>> function) {
        if (param == null) {
            throw new IllegalArgumentException("Param cannot be null");
        }
        if (function == null) {
            throw new IllegalArgumentException("Function cannot be null");
        }

        try {
            // 初始化分页参数
            param.init();
            int current = param.getCurrent().intValue();
            int size = param.getSize().intValue();

            log.debug("Starting page query with current={}, size={}", current, size);
            PageHelper.startPage(current, size);

            // 执行查询
            Page listPage = (Page) function.apply(param);
            PageData<T> pageData = PageData.of(listPage.getResult(), listPage.getTotal());

            log.debug("Page query completed, total records={}", pageData.getTotal());
            return pageData;
        } finally {
            PageHelper.clearPage();
        }
    }

}