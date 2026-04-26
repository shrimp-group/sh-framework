package com.wkclz.core.base;

import com.wkclz.core.annotation.FieldDesc;
import com.wkclz.core.exception.SystemException;
import com.wkclz.tool.utils.BeanUtil;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BaseEntity extends DbColumnEntity {

    private static final long DEFAULT_CURRENT = 1L;
    private static final long DEFAULT_SIZE = 10L;

    @FieldDesc("用户编码")
    private String userCode;
    @FieldDesc("租户编码")
    private String tenantCode;

    /**
     * 查询辅助
     */
    @FieldDesc("查询排序规则")
    private String orderBy;
    @FieldDesc("主键ID数组")
    private List<Long> ids;
    @FieldDesc("模糊查询关键字")
    private String keyword;
    @FieldDesc("创建时间从")
    private LocalDateTime timeFrom;
    @FieldDesc("创建时间到")
    private LocalDateTime timeTo;

    /**
     * 分页辅助
     */
    @FieldDesc("分页页码")
    private Long current;
    @FieldDesc("分页大小")
    private Long size;
    @FieldDesc("偏移量")
    private Long offset;
    @FieldDesc("总数据量")
    private Long total;
    @FieldDesc("统计数")
    private Long count;


    /**
     * debug 模式参数
     */
    private Integer debug;




    public void init() {
        if (this.current == null || this.current < 1) {
            this.current = DEFAULT_CURRENT;
        }
        if (this.size == null || this.size < 1) {
            this.size = DEFAULT_SIZE;
        }
        this.offset = (this.current - 1) * this.size;
    }


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
