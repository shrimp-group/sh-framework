package com.wkclz.core.base;

import com.wkclz.core.annotation.FieldDesc;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体，数据库规范字段
 */
@Data
public class DbColumnEntity implements Serializable {

    @FieldDesc("主键ID")
    private Long id;

    @FieldDesc("排序号，越大越往后")
    private Integer sort;

    @FieldDesc("创建时间")
    private LocalDateTime createTime;

    @FieldDesc("创建人code")
    private String createBy;

    @FieldDesc("更新时间")
    private LocalDateTime updateTime;

    @FieldDesc("更新人code")
    private String updateBy;

    @FieldDesc("备注")
    private String remark;

    @FieldDesc("数据版本")
    private Integer version;

}