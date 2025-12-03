package com.wkclz.core.base;

import com.wkclz.core.enums.ResultCode;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class R<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    private LocalDateTime requestTime;
    private LocalDateTime responseTime;
    private Long costTime;

    public R() {
    }

    public R(ResultCode rc, T data) {
        this.code = rc.getCode();
        this.msg = rc.getMessage();
        this.data = data;
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    
    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS, null);
    }
    
    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS, data);
    }

    public static <T> R<T> warn() {
        return new R<>(ResultCode.VALIDATION_ERROR, null);
    }

    public static <T> R<T> warn(String message) {
        return new R<>(ResultCode.VALIDATION_ERROR.getCode(), message, null);
    }

    public static <T> R<T> warn(String template, Object... args) {
        return new R<>(ResultCode.VALIDATION_ERROR.getCode(), format(template, args), null);
    }

    public static <T> R<T> error() {
        return new R<>(ResultCode.ERROR, null);
    }

    public static <T> R<T> error(String message) {
        return new R<>(ResultCode.ERROR.getCode(), message, null);
    }

    public static <T> R<T> error(String template, Object... args) {
        return new R<>(ResultCode.ERROR.getCode(), format(template, args), null);
    }

    private static String format(String template, Object... args) {
        if (template == null) {
            return null;
        }
        
        String result = template;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", String.valueOf(arg));
        }
        return result;
    }
}