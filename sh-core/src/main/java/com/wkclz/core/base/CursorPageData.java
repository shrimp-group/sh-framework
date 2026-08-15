package com.wkclz.core.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * C 端游标分页响应（游标分页不统计总数，无 total）
 *
 * @param <T> 分页数据类型
 */
@Data
@Schema(description = "C 端游标分页响应")
public class CursorPageData<T> implements Serializable {

    @Schema(description = "数据列表（id 倒序）")
    private List<T> records;

    @Schema(description = "是否还有下一页")
    private Boolean hasMore;

    /**
     * 快速创建游标分页结果
     *
     * @param records 数据列表（调用方已按 size 截断）
     * @param hasMore 是否还有下一页
     */
    public static <T> CursorPageData<T> of(List<T> records, boolean hasMore) {
        CursorPageData<T> pageData = new CursorPageData<>();
        pageData.setRecords(records);
        pageData.setHasMore(hasMore);
        return pageData;
    }

    /**
     * 创建空游标分页结果
     */
    public static <T> CursorPageData<T> empty() {
        return of(Collections.emptyList(), false);
    }
}
