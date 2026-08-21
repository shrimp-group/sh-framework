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
    private Integer hasMore;

    @Schema(description = "下一页游标（本页最后一条记录 id），无更多时为 null")
    private Long nextCursor;

    /**
     * 快速创建游标分页结果
     *
     * @param records 数据列表（调用方已按 size 截断）
     * @param hasMore 是否还有下一页
     */
    public static <T> CursorPageData<T> of(List<T> records, Integer hasMore, Long nextCursor) {
        CursorPageData<T> pageData = new CursorPageData<>();
        pageData.setRecords(records);
        pageData.setHasMore(hasMore);
        pageData.setNextCursor(nextCursor);
        return pageData;
    }

    /**
     * 创建空游标分页结果
     */
    public static <T> CursorPageData<T> empty() {
        return of(Collections.emptyList(), 0, null);
    }
}
