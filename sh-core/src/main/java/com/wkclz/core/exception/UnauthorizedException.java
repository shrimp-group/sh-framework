package com.wkclz.core.exception;

import com.wkclz.core.enums.ResultCode;

import java.text.MessageFormat;

/**
 * 未授权异常类
 * 用于处理未授权访问的情况
 */
public class UnauthorizedException extends CommonException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(ResultCode resultCode) {
        super(resultCode);
    }
    
    public UnauthorizedException(int code, String message) {
        super(code, message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UnauthorizedException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }
    
    public UnauthorizedException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    // 静态方法，支持字符串模板
    public static UnauthorizedException of(String message, Object... args) {
        return new UnauthorizedException(MessageFormat.format(message, args));
    }
    
    public static UnauthorizedException of(ResultCode resultCode) {
        return new UnauthorizedException(resultCode);
    }
    
    public static UnauthorizedException of(int code, String message, Object... args) {
        return new UnauthorizedException(code, MessageFormat.format(message, args));
    }
}