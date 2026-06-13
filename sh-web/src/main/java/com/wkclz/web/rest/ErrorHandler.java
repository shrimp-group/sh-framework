package com.wkclz.web.rest;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import com.wkclz.core.base.R;
import com.wkclz.core.exception.CommonException;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.spring.config.SpringContextHolder;
import com.wkclz.spring.config.SystemConfig;
import com.wkclz.spring.utils.MailUtil;
import com.wkclz.web.helper.LocalThreadHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLSyntaxErrorException;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * 全局异常捕捉处理
 * @author shrimp
 */
@RestControllerAdvice
public class ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    // 记录请求信息，方便在异常时获取并提示
    public static final String REQUEST_LOG = "HTTP:REQUEST_LOG";
    public static final String REQUEST_ERROR = "HTTP:REQUEST_ERROR";

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public R httpHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        printErrorLog(request, response, status, e);
        return R.error(status.value(), status.getReasonPhrase());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R httpRequestMethodHandler(HttpRequestMethodNotSupportedException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        printErrorLog(request, response, status, e);
        return R.error(status.value(), status.getReasonPhrase());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public R httpNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        printErrorLog(request, response, status, e);
        return R.error(status.value(), status.getReasonPhrase());
    }

    @ExceptionHandler(SQLSyntaxErrorException.class)
    public R httpSQLSyntaxErrorException(SQLSyntaxErrorException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        printErrorLog(request, response, status, e);
        return R.error(status.value(), status.getReasonPhrase());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public R httpBadSqlGrammarException(BadSqlGrammarException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        printErrorLog(request, response, status, e);
        return R.error(status.value(), status.getReasonPhrase());
    }

    @ExceptionHandler(UncategorizedSQLException.class)
    public R httpUncategorizedSQLException(UncategorizedSQLException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        printErrorLog(request, response, status, e);
        return R.error(status.value(), status.getReasonPhrase());
    }


    @ExceptionHandler(MysqlDataTruncation.class)
    public R httpMysqlDataTruncation(MysqlDataTruncation e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        printErrorLog(request, response, status, e);
        return R.error(status.value(), status.getReasonPhrase());
    }

    @ExceptionHandler(ValidationException.class)
    public R validationExceptionHandler(ValidationException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        printErrorLog(request, response, status, e);
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request, HttpServletResponse response) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        HttpStatus status = HttpStatus.BAD_REQUEST;
        printErrorLog(request, response, status, e);
        return R.error(status.value(), message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        printErrorLog(request, response, status, e);
        return R.error(status.value(), message);
    }

    @ExceptionHandler(BindException.class)
    public R bindExceptionHandler(BindException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        printErrorLog(request, response, status, e);
        return R.error(status.value(), message);
    }

    @ExceptionHandler(CommonException.class)
    public R sysExceptionHandler(CommonException e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        printErrorLog(request, response, status, e);
        return R.error(-1, e.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    public R errorHandler(Exception e, HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        CommonException commonException = getCommonException(e);
        if (commonException != null) {
            printErrorLog(request, response, status, e);
            return R.error(commonException);
        }

        String message = e.getMessage();
        // 安全考虑：不向客户端暴露完整堆栈信息，仅在日志中记录
        if (message == null || message.trim().isEmpty() || "null".equals(message)) {
            message = "Internal Server Error";
        }
        printErrorLog(request, response, status, e);
        return R.error(message);
    }


    /**
     * Throwable 找 CommonException，找二级原因
     */
    private static CommonException getCommonException(Throwable throwable) {
        for (int i = 0; i < 3; i++) {
            if (throwable == null) {
                return null;
            }
            if (throwable instanceof CommonException commonException) {
                return commonException;
            }
            throwable = throwable.getCause();
        }
        return null;
    }

    private static void printErrorLog(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            Exception e) {

        response.setStatus(status.value());
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String errorMsg = e.getMessage();
        if (errorMsg == null) {
            errorMsg = e.toString();
        }

        // 异常信息通过 MDC (ThreadLocal 返回给 Filter 使用)
        LocalThreadHelper.set(REQUEST_ERROR, errorMsg);

        if (e instanceof UserException) {
            logger.error("biz error: {} {}, {}", method, uri, errorMsg);
            return;
        }

        logger.error("sys request: {} {}, {}", method, uri, errorMsg, e);

        // 发送邮件消息
        SystemConfig bean = SpringContextHolder.getBean(SystemConfig.class);

        if (!bean.isAlarmEmailEnabled()) {
            return;
        }

        String applicationName = bean.getApplicationName();
        String now = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        String subject = String.format("【%s】日志异常警告@%s: %s", applicationName, now, errorMsg);
        String html = buildHtml(errorMsg, now, e, request, bean);

        try {
            MailUtil mu = new MailUtil();
            mu.setEmailHost(bean.getAlarmEmailHost());
            mu.setEmailFrom(bean.getAlarmEmailFrom());
            mu.setEmailPassword(bean.getAlarmEmailPassword());
            mu.setToEmails(bean.getAlarmEmailTo());

            mu.setSubject(subject);
            mu.setContent(html);
            mu.sendEmail();
        } catch (Exception exception) {
            logger.error("发送邮件异常: {}", exception.getMessage());
        }

    }



    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static String buildHtml(String errorMsg, String now, Exception e, HttpServletRequest request, SystemConfig bean) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String applicationName = bean.getApplicationName();

        // 请求 Filter 拦截器可能记录了请求信息，若存在，则打印出来
        Object requestLog = LocalThreadHelper.get(REQUEST_LOG);

        String html = """
            <html>
                <body>
                    <div>系统: ${applicationName}</div>
                    <div>时间: ${now}</div>
                    <div>URL: ${url}</div>
                    <div>请求详情: </div>
                    <pre>${requestLog}</pre>
                    <pre>异常摘要: ${errorMsg}</pre>
                    <div>异常内容: </div>
                    <pre>${stackTrace}</pre>
                </body>
            </html>
            """;
        html = html.replace("${applicationName}", escapeHtml(applicationName));
        html = html.replace("${now}", now);
        html = html.replace("${url}", escapeHtml(method + ":" + uri));
        html = html.replace("${requestLog}", requestLog == null ? "无请求详情" : escapeHtml(JSONUtil.toJsonPrettyStr(requestLog)));
        html = html.replace("${errorMsg}", escapeHtml(errorMsg));
        html = html.replace("${stackTrace}", escapeHtml(e.getMessage()) + "<br />" + escapeHtml(ExceptionUtils.getStackTrace(e)));
        return html;
    }

}



