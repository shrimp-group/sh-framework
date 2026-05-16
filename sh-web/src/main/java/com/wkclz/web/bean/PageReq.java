package com.wkclz.web.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "分页请求")
public class PageReq implements Serializable {

    @Schema(description = "分页页码")
    private Long current;

    @Schema(description = "分页大小")
    private Long size;


}
