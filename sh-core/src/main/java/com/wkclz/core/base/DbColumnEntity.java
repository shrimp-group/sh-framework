package com.wkclz.core.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体，数据库规范字段
 */
@Data
public class DbColumnEntity implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "排序号，越大越往后")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人code")
    private String createBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人code")
    private String updateBy;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "数据版本")
    private Integer version;

}
