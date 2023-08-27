package com.example.account_project.Exception;

import com.example.account_project.dto.ErrorResponse;
import com.example.account_project.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.example.account_project.type.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.example.account_project.type.ErrorCode.INVALID_REQUEST;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountException.class)
    public ErrorResponse handlerAccountException(AccountException e) {
        log.error("{} 가 발생했습니다.", e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handlerMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException 가 발생했습니다.", e);

        return new ErrorResponse(INVALID_REQUEST, INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handlerDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException 가 발생했습니다.", e);

        return new ErrorResponse(INVALID_REQUEST, INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handlerException(Exception e) {
        log.error("예외가 발생했습니다.", e);

        return new ErrorResponse(INTERNAL_SERVER_ERROR
                ,INTERNAL_SERVER_ERROR.getDescription());
    }
}
