package com.yiyun.common;

import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 捕获 Controller 层抛出的异常，统一封装成 Result 返回前端，
 * 避免抛出原始堆栈信息，提升用户体验和安全性。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 处理运行时异常（业务异常，如登录失败、用户不存在等），返回 500 + 异常消息 */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 处理 @Valid 参数校验异常（JSON 请求体）
     * <p>
     * 当 Controller 方法参数标注 @Valid 且校验不通过时触发，
     * 提取第一个字段的校验错误消息返回。
     *
     * @param e 校验异常对象
     * @return 400 状态码 + 校验错误消息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 获取第一个校验失败的字段
        FieldError fieldError = e.getBindingResult().getFieldError();
        // 取该字段的默认错误消息，没有则用默认提示
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(400, message);
    }

    /** 处理表单参数绑定异常，返回 400 + 校验错误消息 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(400, message);
    }
}