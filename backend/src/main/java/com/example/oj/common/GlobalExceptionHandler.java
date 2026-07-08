package com.example.oj.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Result<Void> handleValidationException(Exception e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), resolveValidationMessage(e));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }

    private String resolveValidationMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException exception) {
            return firstFieldErrorMessage(exception.getBindingResult().getFieldError());
        }
        if (e instanceof BindException exception) {
            return firstFieldErrorMessage(exception.getBindingResult().getFieldError());
        }
        if (e instanceof ConstraintViolationException exception) {
            Optional<String> message = exception.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .filter(item -> item != null && !item.isBlank())
                    .findFirst();
            return message.orElse(ErrorCode.PARAM_ERROR.getMessage());
        }
        return ErrorCode.PARAM_ERROR.getMessage();
    }

    private String firstFieldErrorMessage(FieldError fieldError) {
        if (fieldError == null || fieldError.getDefaultMessage() == null || fieldError.getDefaultMessage().isBlank()) {
            return ErrorCode.PARAM_ERROR.getMessage();
        }
        return fieldError.getDefaultMessage();
    }
}
