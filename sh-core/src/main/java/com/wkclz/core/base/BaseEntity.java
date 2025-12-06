package com.wkclz.core.base;

import com.wkclz.core.annotation.Desc;
import com.wkclz.tool.utils.BeanUtil;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BaseEntity extends DbColumnEntity {

    @Desc("用户编码")
    private String userCode;
    @Desc("租户编码")
    private String tenantCode;

    /**
     * 查询辅助
     */
    @Desc("查询排序规则")
    private String orderBy;
    @Desc("主键ID数组")
    private List<Long> ids;
    @Desc("模糊查询关键字")
    private String keyword;
    @Desc("创建时间从")
    private LocalDateTime timeFrom;
    @Desc("创建时间到")
    private LocalDateTime timeTo;

    /**
     * 分页辅助
     */
    @Desc("分页页码")
    private Long current;
    @Desc("分页大小")
    private Long size;
    @Desc("偏移量")
    private Long offset;
    @Desc("总数据量")
    private Long total;
    @Desc("统计数")
    private Long count;


    /**
     * debug 模式参数
     */
    private Integer debug;




    public void init() {
        if (this.current == null || this.current < 1) {
            this.current = 1L;
        }
        if (this.size == null || this.size < 1) {
            this.size = 10L;
        }
        this.offset = (this.current -1 ) * this.size;
    }


    public static <T extends BaseEntity> T copy(T source, T target) {
        T newTarget = checkSourceAndTarget(source, target);
        if(newTarget == null) {
            return null;
        }
        BeanUtil.cpAll(source, newTarget);
        return target;
    }

    public static <T extends BaseEntity> T copyIfNotNull(T source, T target) {
        T newTarget = checkSourceAndTarget(source, target);
        if(newTarget == null) {
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
                // who care ?
            }
        }
        return target;
    }

}
