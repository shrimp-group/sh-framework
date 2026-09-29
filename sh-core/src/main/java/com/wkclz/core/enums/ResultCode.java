package com.wkclz.core.enums;

/**
 * Common result codes
 * @author shrimp
 */
public enum ResultCode {

    SUCCESS(200, "Success"),
    VALIDATION_ERROR(400, "Parameter validation error"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource Not Found"),
    // 记录级语义码，复用 HTTP 状态码：与通用码同码时由 message 区分具体场景
    RECORD_NOT_EXIST(404, "Record Not Found"),
    RECORD_DUPLICATE(409, "Duplicate Record"),
    ERROR(500, "Internal Server Error"),
    ;

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
