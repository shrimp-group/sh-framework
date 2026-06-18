package com.wkclz.web.bean;

import com.wkclz.web.annotation.AtLeastOneNotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "删除请求")
@AtLeastOneNotNull(fields = {"id", "ids"}, message = "id 或 ids 必须填写其中一个")
public class RemoveReq implements Serializable {

    @Schema(description = "主键ID（与 ids 二选一）")
    private Long id;

    @Schema(description = "主键ID清单（与 id 二选一）")
    private List<Long> ids;


}
