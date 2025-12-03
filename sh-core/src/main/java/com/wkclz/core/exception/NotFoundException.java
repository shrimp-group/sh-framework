package com.wkclz.core.exception;

import com.wkclz.core.enums.ResultCode;

import java.text.MessageFormat;

/**
 * 资源未找到异常类
 * 用于处理资源未找到的情况
 */
public class NotFoundException extends CommonException {
    
    public NotFoundException(String message) {
        super(message);
    }
    
    public NotFoundException(ResultCode resultCode) {
        super(resultCode);
    }
    
    public NotFoundException(int code, String message) {
        super(code, message);
    }
    
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public NotFoundException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }
    
    public NotFoundException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    // 静态方法，支持字符串模板
    public static NotFoundException of(String message, Object... args) {
        return new NotFoundException(MessageFormat.format(message, args));
    }
    
    public static NotFoundException of(ResultCode resultCode) {
        return new NotFoundException(resultCode);
    }
    
    public static NotFoundException of(int code, String message, Object... args) {
        return new NotFoundException(code, MessageFormat.format(message, args));
    }
}