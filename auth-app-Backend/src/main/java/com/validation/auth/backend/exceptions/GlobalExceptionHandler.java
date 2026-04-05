package com.validation.auth.backend.exceptions;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.validation.auth.backend.dtos.ApiErrorDetail;
import com.validation.auth.backend.dtos.ApiErrorResponse;

import jakarta.validation.ConstraintViolationException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private  final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            CredentialsExpiredException.class,
            DisabledException.class
    })

        public ResponseEntity<ApiErrorResponse> handleAuthException(Exception e, HttpServletRequest request) {
        logger.info("Exception  : {}", e.getClass().getName());
        return build(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "Authentication Failed",
            "Your session expired or credentials are invalid. Please log in again.",
            request,
            List.of()
        );

    }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
        ) {
        List<ApiErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
            .map(this::toDetail)
            .toList();
        return build(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            "Validation Error",
            "Please check the highlighted fields and try again.",
            request,
            details
        );
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
        ) {
        List<ApiErrorDetail> details = exception.getConstraintViolations().stream()
            .map(violation -> new ApiErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
        return build(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            "Validation Error",
            "Please check the highlighted fields and try again.",
            request,
            details
        );
        }

    //resource not found exception handler :: method
    @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception, HttpServletRequest request){
        return build(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "Not Found",
            "The requested item was not found.",
            request,
            List.of(new ApiErrorDetail("resource", exception.getMessage()))
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiErrorResponse> hanleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request){
        return build(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            "Bad Request",
            exception.getMessage(),
            request,
            List.of()
        );
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleUnhandledException(Exception exception, HttpServletRequest request) {
        logger.error("Unhandled exception", exception);
        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "Internal Error",
            "Something went wrong on our side. Please try again shortly.",
            request,
            List.of()
        );
        }

        private ApiErrorDetail toDetail(FieldError fieldError) {
        return new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
        }

        private ResponseEntity<ApiErrorResponse> build(
            HttpStatusCode status,
            String code,
            String title,
            String message,
            HttpServletRequest request,
            List<ApiErrorDetail> details
        ) {
        ApiErrorResponse response = ApiErrorResponse.of(
            status.value(),
            code,
            title,
            message,
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(status).body(response);
    }

}
