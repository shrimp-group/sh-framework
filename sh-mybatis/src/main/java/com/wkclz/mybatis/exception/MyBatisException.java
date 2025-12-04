package com.wkclz.mybatis.exception;

import com.wkclz.core.exception.CommonException;
import com.wkclz.core.enums.ResultCode;

/**
 * MyBatis操作异常类
 * 用于封装MyBatis数据库操作过程中的异常信息
 */
public class MyBatisException extends CommonException {

    public MyBatisException() {
        super(ResultCode.ERROR);
    }

    public MyBatisException(String message) {
        super(message);
    }

    public MyBatisException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyBatisException(Throwable cause) {
        super("MyBatis操作异常", cause);
    }

    public MyBatisException(ResultCode resultCode) {
        super(resultCode);
    }

    public MyBatisException(ResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }

    public MyBatisException(int code, String message) {
        super(code, message);
    }

    public MyBatisException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
