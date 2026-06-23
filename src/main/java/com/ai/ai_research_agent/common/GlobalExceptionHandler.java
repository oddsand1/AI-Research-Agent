package com.ai.ai_research_agent.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg=e.getBindingResult().getFieldErrors().stream()
                .map(err->err.getField()+": "+err.getDefaultMessage())
                .reduce((a,b)->a+";"+b)
                .orElse("参数校验失败");
        return Result.fail(400,msg);
    }

    /**
     * 路径参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlerConstraintViolation(ConstraintViolationException e) {
        return Result.fail(400,e.getMessage());
    }


    /**
     * 业务异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e){
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(400,e.getMessage());
    }


    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e){
        log.error("系统异常",e);
        return Result.fail(500,e.getMessage());
    }
}