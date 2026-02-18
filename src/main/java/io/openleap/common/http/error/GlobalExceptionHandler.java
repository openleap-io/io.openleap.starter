/*
 * This file is part of the openleap.io software project.
 *
 *  Copyright (C) 2025 Dr.-Ing. Sören Kemmann
 *
 * This software is dual-licensed under:
 *
 * 1. The European Union Public License v.1.2 (EUPL)
 *    https://joinup.ec.europa.eu/collection/eupl
 *
 *     You may use, modify and redistribute this file under the terms of the EUPL.
 *
 *  2. A commercial license available from:
 *
 *     B+B Unternehmensberatung GmbH & Co.KG
 *     Robert-Bunsen-Straße 10
 *     67098 Bad Dürkheim
 *     Germany
 *     Contact: license@bb-online.de
 *
 *  You may choose which license to apply.
 */
package io.openleap.common.http.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global fallback exception handler with lowest precedence.
 * <p>
 * Exception handlers in specific services should have higher precedence
 * than this global handler to allow domain-specific error handling.
 * This handler acts as a last resort catch-all.
 */
// TODO (itaseski): Consider moving to API package
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final String TRACE_ID = "traceId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} error(s)", ex.getBindingResult().getErrorCount());
        List<Map<String, String>> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toError)
                .toList();
        ErrorResponse body = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.message(),
                violations,
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), ErrorCode.VALIDATION_ERROR.status());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violations: {}", ex.getConstraintViolations().size());
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "field", v.getPropertyPath().toString(),
                        "message", v.getMessage()))
                .toList();
        ErrorResponse body = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.message(),
                violations,
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), ErrorCode.VALIDATION_ERROR.status());
    }

    // 400 - Bad Request scenarios
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Object> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        ErrorResponse body = new ErrorResponse(
                ErrorCode.BAD_REQUEST.name(),
                ErrorCode.BAD_REQUEST.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        if (ex.getStatusCode().is4xxClientError()) {
            log.warn("{}: {}", ex.getStatusCode(), ex.getReason());
        } else {
            log.error("{}: {}", ex.getStatusCode(), ex.getReason());
        }
        // Parse reason as CODE or CODE: details
        String reason = ex.getReason();
        String codeStr = reason;
        Object details = null;
        if (reason != null && reason.contains(":")) {
            int idx = reason.indexOf(':');
            codeStr = reason.substring(0, idx).trim();
            details = reason.substring(idx + 1).trim();
        }

        ErrorCode code = ErrorCode.from(ErrorCode.class, codeStr);
        HttpStatusCode statusCode = ex.getStatusCode();
        String message;
        if (code != null) {
            message = code.message();
            // If controller set a different status, keep it; otherwise use catalog default
            if (statusCode == null) statusCode = code.status();
        } else {
            // Fallback to generic mapping based on status
            message = ex.getReason();
        }
        ErrorResponse body = new ErrorResponse(
                code != null ? code.name() : (statusCode.is4xxClientError() ? ErrorCode.BAD_REQUEST.name() : ErrorCode.INTERNAL_ERROR.name()),
                message != null ? message : (statusCode.is4xxClientError() ? ErrorCode.BAD_REQUEST.message() : ErrorCode.INTERNAL_ERROR.message()),
                details,
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), statusCode);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        log.error("IO error encountered: {}", ex.getMessage(), ex);
        ErrorResponse body = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), ErrorCode.INTERNAL_ERROR.status());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.error("Illegal state encountered: {}", ex.getMessage(), ex);
        ErrorResponse body = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), ErrorCode.INTERNAL_ERROR.status());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        // log event
        ErrorResponse body = new ErrorResponse(
                ErrorCode.BAD_REQUEST.name(),
                ErrorCode.BAD_REQUEST.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), ErrorCode.BAD_REQUEST.status());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse body = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, String> toError(FieldError fe) {
        Map<String, String> map = new HashMap<>();
        map.put("field", fe.getField());
        map.put("message", fe.getDefaultMessage());
        return map;
    }
}
