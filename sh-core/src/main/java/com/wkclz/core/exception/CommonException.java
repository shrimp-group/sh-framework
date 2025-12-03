package com.wkclz.core.exception;

import com.wkclz.core.enums.ResultCode;

import java.text.MessageFormat;

/**
 * 业务异常基类
 * 所有业务异常都应该继承此类
 */
public class CommonException extends RuntimeException {
    
    private Integer code;
    
    public CommonException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }
    
    public CommonException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
    
    public CommonException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public CommonException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CommonException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
    }
    
    public CommonException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    // 静态方法，支持字符串模板
    public static CommonException of(String message, Object... args) {
        return new CommonException(MessageFormat.format(message, args));
    }
    
    public static CommonException of(ResultCode resultCode) {
        return new CommonException(resultCode);
    }
    
    public static CommonException of(int code, String message, Object... args) {
        return new CommonException(code, MessageFormat.format(message, args));
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
}