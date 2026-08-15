package com.wkclz.web.bean;

import com.wkclz.web.annotation.AtLeastOneNotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Schema(description = "删除请求")
@AtLeastOneNotNull(fields = {"id", "ids"}, message = "id 或 ids 必须填写其中一个")
public class RemoveReq implements Serializable {

    @Schema(description = "主键ID（与 ids 二选一）")
    private Long id;

    @Schema(description = "主键ID清单（与 id 二选一）")
    private List<Long> ids;

    /**
     * 获取全部待删除主键 ID（兼容单个 id 与批量 ids，自动合并、去重、忽略 null）
     *
     * @return 去重后的主键 ID 列表（可能为空）
     */
    public List<Long> getAllIds() {
        List<Long> result = new ArrayList<>();
        if (id != null) {
            result.add(id);
        }
        if (ids != null) {
            for (Long item : ids) {
                if (item != null && !result.contains(item)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

}
