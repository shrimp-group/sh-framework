package com.wkclz.core.base;

import com.wkclz.core.annotation.Desc;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体，数据库规范字段
 */
@Data
public class DbColumnEntity implements Serializable {

    @Desc("主键ID")
    private Long id;

    @Desc("排序号，越大越往后")
    private Integer sort;

    @Desc("创建时间")
    private LocalDateTime createTime;

    @Desc("创建人code")
    private String createBy;

    @Desc("更新时间")
    private LocalDateTime updateTime;

    @Desc("更新人code")
    private String updateBy;

    @Desc("备注")
    private String remark;

    @Desc("数据版本")
    private Integer version;

}