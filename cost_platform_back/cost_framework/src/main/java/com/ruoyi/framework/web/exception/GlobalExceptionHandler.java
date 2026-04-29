package com.ruoyi.framework.web.exception;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.exception.DemoModeException;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.html.EscapeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 全局异常处理器
 *
 * @author ruoyi
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_TYPE_TAG = "errorType";
    private static final String TRACE_ID_TAG = "traceId";
    private static final String ERROR_TYPE_PARAM = "PARAM";
    private static final String ERROR_TYPE_PERMISSION = "PERMISSION";
    private static final String ERROR_TYPE_BUSINESS = "BUSINESS";
    private static final String ERROR_TYPE_DATA_CONSTRAINT = "DATA_CONSTRAINT";
    private static final String ERROR_TYPE_SYSTEM = "SYSTEM";
    private static final DateTimeFormatter TRACE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 权限校验异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限校验失败'{}'", requestURI, e.getMessage());
        return classifiedError(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权", ERROR_TYPE_PERMISSION);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public AjaxResult handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生唯一约束冲突.", requestURI, e);
        return classifiedError(resolveDataIntegrityMessage(e), ERROR_TYPE_DATA_CONSTRAINT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public AjaxResult handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生数据完整性异常.", requestURI, e);
        return classifiedError(resolveDataIntegrityMessage(e), ERROR_TYPE_DATA_CONSTRAINT);
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                          HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return classifiedError(e.getMessage(), ERROR_TYPE_PARAM);
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return StringUtils.isNotNull(code)
                ? classifiedError(code, e.getMessage(), ERROR_TYPE_BUSINESS)
                : classifiedError(e.getMessage(), ERROR_TYPE_BUSINESS);
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return classifiedError(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()), ERROR_TYPE_PARAM);
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                               HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value)) {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        return classifiedError(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'",
                e.getName(), e.getRequiredType().getName(), value), ERROR_TYPE_PARAM);
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return systemError("请求地址'" + requestURI + "',发生未知异常.", e);
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return systemError("请求地址'" + requestURI + "',发生系统异常.", e);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return classifiedError(message, ERROR_TYPE_PARAM);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return classifiedError(message, ERROR_TYPE_PARAM);
    }

    /**
     * 演示模式异常
     */
    @ExceptionHandler(DemoModeException.class)
    public AjaxResult handleDemoModeException(DemoModeException e) {
        return classifiedError("演示模式，不允许操作", ERROR_TYPE_BUSINESS);
    }

    private AjaxResult classifiedError(String message, String errorType) {
        return AjaxResult.error(message).put(ERROR_TYPE_TAG, errorType);
    }

    private AjaxResult classifiedError(int code, String message, String errorType) {
        return AjaxResult.error(code, message).put(ERROR_TYPE_TAG, errorType);
    }

    private AjaxResult systemError(String logMessage, Throwable throwable) {
        String traceId = buildTraceId();
        log.error("{} traceId={}", logMessage, traceId, throwable);
        return AjaxResult.error("系统处理失败，请联系管理员并提供追踪编号：" + traceId)
                .put(ERROR_TYPE_TAG, ERROR_TYPE_SYSTEM)
                .put(TRACE_ID_TAG, traceId);
    }

    static String buildTraceId() {
        return "ERR-" + TRACE_TIME_FORMATTER.format(LocalDateTime.now()) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    static String resolveDataIntegrityMessage(Throwable throwable) {
        Throwable rootCause = getRootCause(throwable);
        String detail = rootCause == null ? throwable.getMessage() : rootCause.getMessage();
        String normalized = detail == null ? "" : detail.toLowerCase();

        if (hasCause(throwable, DuplicateKeyException.class) || normalized.contains("duplicate entry")) {
            return "数据保存失败：编码、名称或业务唯一标识已存在，请调整后重试。";
        }
        if (normalized.contains("foreign key constraint fails") || normalized.contains("constraint fails")) {
            return "当前数据已被其他业务记录引用，不能直接删除或修改，请先解除关联后重试。";
        }
        if (normalized.contains("cannot be null") || normalized.contains("not null")) {
            return "数据保存失败：存在必填字段为空，请补齐后重试。";
        }
        if (normalized.contains("data too long") || normalized.contains("value too long")) {
            return "数据保存失败：字段内容过长，请缩短后重试。";
        }
        if (normalized.contains("out of range")) {
            return "数据保存失败：数字超出允许范围，请调整后重试。";
        }
        return "数据保存失败：当前数据不满足数据库约束，请检查编码、引用关系和必填项后重试。";
    }

    private static Throwable getRootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }
        return result;
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
