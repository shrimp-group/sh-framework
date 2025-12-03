package com.wkclz.core.exception;

import com.wkclz.core.enums.ResultCode;

import java.text.MessageFormat;

/**
 * API异常类
 * 用于处理API调用过程中出现的异常
 */
public class ApiException extends CommonException {
    
    public ApiException(String message) {
        super(message);
    }
    
    public ApiException(ResultCode resultCode) {
        super(resultCode);
    }
    
    public ApiException(int code, String message) {
        super(code, message);
    }
    
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ApiException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }
    
    public ApiException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    // 静态方法，支持字符串模板
    public static ApiException of(String message, Object... args) {
        return new ApiException(MessageFormat.format(message, args));
    }
    
    public static ApiException of(ResultCode resultCode) {
        return new ApiException(resultCode);
    }
    
    public static ApiException of(int code, String message, Object... args) {
        return new ApiException(code, MessageFormat.format(message, args));
    }
}