package com.wkclz.web.bean;

import com.wkclz.web.annotation.AtLeastOneNotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "删除请求")
@AtLeastOneNotNull(fields = {"id", "ids"}, message = "id 或 ids 必须填写其中一个")
public class RemoveReq implements Serializable {

    @NotNull(message = "主键ID不能为空")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "主键ID清单不能为空")
    @Schema(description = "主键ID清单", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;


}
