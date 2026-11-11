package org.example.aispringboot.common;
//拦截控制器的异常，把异常加入http响应体

import org.example.aispringboot.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.Field;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 指定要拦截的异常类型(参数校验异常)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handlerException(MethodArgumentNotValidException e){
        //处理异常数据的处理
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), ResultCode.PARAM_ERROR.getMsg(),message);
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e){
        // 如果异常携带有额外的数据
        if(e.getData() != null){
            return Result.error(e.getCode(), e.getMessage(), e.getData());
        }
        return Result.error(e.getCode(), e.getMessage(),null);
    }

    // 处理唯一约束冲突（如同一用户同一天重复提交日记）
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKeyException(DuplicateKeyException e){
        return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "今天已经写过日记啦，每天只能记录一条哦", null);
    }
}