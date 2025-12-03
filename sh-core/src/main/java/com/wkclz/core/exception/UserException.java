package com.wkclz.core.exception;

import com.wkclz.core.enums.ResultCode;

import java.text.MessageFormat;

/**
 * 用户异常类
 * 用于处理与用户操作相关的业务异常
 */
public class UserException extends CommonException {
    
    public UserException(String message) {
        super(message);
    }
    
    public UserException(ResultCode resultCode) {
        super(resultCode);
    }
    
    public UserException(int code, String message) {
        super(code, message);
    }
    
    public UserException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UserException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }
    
    public UserException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    // 静态方法，支持字符串模板
    public static UserException of(String message, Object... args) {
        return new UserException(MessageFormat.format(message, args));
    }
    
    public static UserException of(ResultCode resultCode) {
        return new UserException(resultCode);
    }
    
    public static UserException of(int code, String message, Object... args) {
        return new UserException(code, MessageFormat.format(message, args));
    }
}