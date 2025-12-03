package com.wkclz.core.exception;

import com.wkclz.core.enums.ResultCode;

import java.text.MessageFormat;

/**
 * 应用异常类
 * 用于处理应用程序级别的业务异常
 */
public class ApplicationException extends CommonException {
    
    public ApplicationException(String message) {
        super(message);
    }
    
    public ApplicationException(ResultCode resultCode) {
        super(resultCode);
    }
    
    public ApplicationException(int code, String message) {
        super(code, message);
    }
    
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ApplicationException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }
    
    public ApplicationException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    // 静态方法，支持字符串模板
    public static ApplicationException of(String message, Object... args) {
        return new ApplicationException(MessageFormat.format(message, args));
    }
    
    public static ApplicationException of(ResultCode resultCode) {
        return new ApplicationException(resultCode);
    }
    
    public static ApplicationException of(int code, String message, Object... args) {
        return new ApplicationException(code, MessageFormat.format(message, args));
    }
}