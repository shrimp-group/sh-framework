package com.wkclz.mybatis.helper;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.base.PageData;

import java.util.List;
import java.util.function.Function;

/**
 * @author shrimp
 */
public class PageQuery {

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

}