package com.wkclz.core.base;

import com.wkclz.core.exception.SystemException;
import com.wkclz.tool.utils.BeanUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BaseEntity extends DbColumnEntity implements Pageable {

    @Schema(description = "创建人姓名")
    private String createByName;
    @Schema(description = "更新人姓名")
    private String updateByName;

    @Schema(description = "用户编码")
    private String userCode;
    @Schema(description = "租户编码")
    private String tenantCode;

    /**
     * 查询辅助
     */
    @Schema(description = "查询排序规则")
    private String orderBy;
    @Schema(description = "主键ID数组")
    private List<Long> ids;
    @Schema(description = "模糊查询关键字")
    private String keyword;
    @Schema(description = "创建时间从")
    private LocalDateTime timeFrom;
    @Schema(description = "创建时间到")
    private LocalDateTime timeTo;

    /**
     * 分页辅助
     */
    @Schema(description = "分页页码")
    private Long current;
    @Schema(description = "分页大小")
    private Long size;
    @Schema(description = "偏移量")
    private Long offset;
    @Schema(description = "总数据量")
    private Long total;
    @Schema(description = "统计数")
    private Long count;


    /**
     * debug 模式参数
     */
    private Integer debug;


    public static <T extends BaseEntity> T copy(T source, T target) {
        T newTarget = checkSourceAndTarget(source, target);
        if (newTarget == null) {
            return null;
        }
        BeanUtil.cpAll(source, newTarget);
        return newTarget;
    }

    public static <T extends BaseEntity> T copyIfNotNull(T source, T target) {
        T newTarget = checkSourceAndTarget(source, target);
        if (newTarget == null) {
            return null;
        }
        BeanUtil.cpNotNull(source, newTarget);
        return newTarget;
    }

    // 生成 new target
    private static <T extends BaseEntity> T checkSourceAndTarget(T source, T target) {
        if (source == null) {
            return null;
        }
        if (target == null) {
            try {
                // noinspection unchecked
                target = (T)source.getClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new SystemException("Failed to create new instance of " + source.getClass().getName(), e);
            }
        }
        return target;
    }

}
