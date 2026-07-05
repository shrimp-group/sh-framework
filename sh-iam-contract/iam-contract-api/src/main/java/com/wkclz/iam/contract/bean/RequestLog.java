package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 请求日志
 * 由 SsoFacadeContract.saveLog() 上报到 SSO 服务端
 *
 * @author shrimp
 */
@Data
@Schema(description = "请求日志")
public class RequestLog implements Serializable {

    @Schema(description = "请求 URI")
    private String uri;

    @Schema(description = "HTTP 方法")
    private String method;

    @Schema(description = "请求体")
    private String requestBody;

    @Schema(description = "响应状态码")
    private Integer responseStatus;

    @Schema(description = "响应体")
    private String responseBody;

    @Schema(description = "请求时间")
    private Long requestTime;

    @Schema(description = "响应时间")
    private Long responseTime;

    @Schema(description = "耗时(ms)")
    private Long duration;

    @Schema(description = "客户端 IP")
    private String clientIp;

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "应用编码")
    private String appCode;
}
