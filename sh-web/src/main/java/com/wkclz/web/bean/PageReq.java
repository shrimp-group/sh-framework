package com.wkclz.web.bean;

import com.wkclz.core.base.Pageable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Data
@Slf4j
@Schema(description = "分页请求")
public class PageReq implements Pageable, Serializable {

    @Schema(description = "分页页码")
    private Long current;

    @Schema(description = "分页大小")
    private Long size;

    @Schema(description = "偏移量", hidden = true)
    private Long offset;

    @Override
    public void init() {
        Long current = getCurrent();
        if (current == null || current < 1) {
            log.debug("分页参数 current 为空或非法值: {}, 设置为默认值: {}", current, DEFAULT_CURRENT);
            setCurrent(DEFAULT_CURRENT);
        }

        Long size = getSize();
        if (size == null || size < 1) {
            log.debug("分页参数 size 为空或非法值: {}, 设置为默认值: {}", size, DEFAULT_SIZE);
            setSize(DEFAULT_SIZE);
        }

        // 计算偏移量：(current - 1) * size
        long offset = (getCurrent() - 1) * getSize();
        setOffset(offset);
        log.debug("分页参数初始化完成, current: {}, size: {}, offset: {}", getCurrent(), getSize(), offset);
    }

}
