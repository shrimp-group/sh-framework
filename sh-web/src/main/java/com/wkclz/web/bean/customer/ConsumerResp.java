package com.wkclz.web.bean.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * C 端响应基类（面向小程序/H5/App 等消费者端，仅含主键，避免带出审计字段）
 */
@Data
@Schema(description = "C 端响应基类")
public class ConsumerResp implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

}
