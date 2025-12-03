package com.wkclz.core.exception;

import com.wkclz.core.enums.ResultCode;

import java.text.MessageFormat;

/**
 * 数据验证异常类
 * 用于处理数据校验失败的情况
 */
public class ValidationException extends CommonException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(ResultCode resultCode) {
        super(resultCode);
    }
    
    public ValidationException(int code, String message) {
        super(code, message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ValidationException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }
    
    public ValidationException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    // 静态方法，支持字符串模板
    public static ValidationException of(String message, Object... args) {
        return new ValidationException(MessageFormat.format(message, args));
    }
    
    public static ValidationException of(ResultCode resultCode) {
        return new ValidationException(resultCode);
    }
    
    public static ValidationException of(int code, String message, Object... args) {
        return new ValidationException(code, MessageFormat.format(message, args));
    }
}