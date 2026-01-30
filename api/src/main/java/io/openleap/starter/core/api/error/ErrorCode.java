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
package io.openleap.starter.core.api.error;

import org.springframework.http.HttpStatus;

/**
 * Canonical error code catalog. Each code has a default HTTP status and default message.
 * Services should use these codes in ResponseStatusException reasons. GlobalExceptionHandler maps them
 * to a consistent response body {code, message, details, traceId, timestamp}.
 */
public enum ErrorCode implements OlErrorCode {
    // Generic
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad Request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found"),
    CONFLICT(HttpStatus.CONFLICT, "Conflict"),

    // Domain-specific
    SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "Snapshot not found"),
    PERIOD_CLOSED(HttpStatus.CONFLICT, "Period is closed"),

    // Common validation specifics
    MISSING_REQUEST(HttpStatus.BAD_REQUEST, "Missing request"),
    INVALID_MONTH(HttpStatus.BAD_REQUEST, "Invalid month"),
    INVALID_PERIOD_FORMAT(HttpStatus.BAD_REQUEST, "Invalid period format. Expect YYYY-MM"),
    BASE_CURRENCY_REQUIRED(HttpStatus.BAD_REQUEST, "Base currency required"),

    // Events/Outbox
    EVENT_ENQUEUE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to enqueue messaging");

    private final HttpStatus defaultStatus;

    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.defaultStatus = status;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public HttpStatus status() {
        return defaultStatus;
    }

    @Override
    public String message() {
        return defaultMessage;
    }

}
