package com.wkclz.core.exception;

import com.wkclz.core.enums.ResultCode;

import java.text.MessageFormat;

/**
 * 系统异常类
 * 用于处理系统级别或未预期的异常情况
 */
public class SystemException extends CommonException {
    
    public SystemException(String message) {
        super(message);
    }
    
    public SystemException(ResultCode resultCode) {
        super(resultCode);
    }
    
    public SystemException(int code, String message) {
        super(code, message);
    }
    
    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SystemException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }
    
    public SystemException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    // 静态方法，支持字符串模板
    public static SystemException of(String message, Object... args) {
        return new SystemException(MessageFormat.format(message, args));
    }
    
    public static SystemException of(ResultCode resultCode) {
        return new SystemException(resultCode);
    }
    
    public static SystemException of(int code, String message, Object... args) {
        return new SystemException(code, MessageFormat.format(message, args));
    }
}