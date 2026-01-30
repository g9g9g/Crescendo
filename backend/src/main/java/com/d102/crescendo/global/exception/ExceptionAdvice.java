package com.d102.crescendo.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public ErrorResponse businessExceptionHandler(BusinessException exception) {
        BusinessError businessError = exception.getBusinessError();
        log.warn("[{}] : {}", businessError.name(), businessError.getMessage());
        log.error("Unhandled BusinessException occurred", exception);
        return ErrorResponse
                .builder(exception, businessError.getHttpStatus(), businessError.getMessage())
                .title(businessError.name())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
        FieldError firstError = exception.getBindingResult().getFieldErrors().get(0);
        String fieldName = firstError.getField().toUpperCase();
        String errorMessage = firstError.getDefaultMessage();

        log.warn("[VALIDATION_ERROR] {} - {}", fieldName, errorMessage);

        return ErrorResponse
                .builder(exception, HttpStatus.BAD_REQUEST, errorMessage)
                .title("INVALID_" + fieldName + "_FORMAT")
                .build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException exception) {
        log.warn("[REQUEST_BODY_ERROR] : 요청 본문이 누락되었거나 형식이 올바르지 않습니다.");
        log.error("HttpMessageNotReadableException occurred", exception);

        return ErrorResponse
                .builder(exception, HttpStatus.BAD_REQUEST, "요청 본문이 누락되었거나 형식이 올바르지 않습니다.")
                .title("INVALID_REQUEST_BODY")
                .build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ErrorResponse runtimeExceptionHandler(RuntimeException exception) {

        log.warn("[{}", exception.getMessage());
        log.error("Unhandled runtimeException occurred", exception);
        return ErrorResponse
                .builder(exception, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류")
                .title("서버 내부 오류")
                .build();
    }
}