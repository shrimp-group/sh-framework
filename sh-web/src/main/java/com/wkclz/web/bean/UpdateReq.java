package com.wkclz.web.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "更新请求")
public class UpdateReq implements Serializable {

    @NotNull(message = "主键ID不能为空")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "数据版本version不能为空")
    @Schema(description = "数据版本", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;

}
