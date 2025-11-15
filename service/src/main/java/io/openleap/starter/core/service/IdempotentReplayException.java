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
 *
 *  Author: Dr. Sören Kemmann
 */

package io.openleap.starter.core.service;

/**
 * Exception thrown when an idempotency key is reused with a different payload.
 * Per PRQ/VR spec, this should result in HTTP 409 Conflict with error code IDEMPOTENT_REPLAY.
 */
public class IdempotentReplayException extends RuntimeException {
    
    public IdempotentReplayException(String message) {
        super(message);
    }
    
    public IdempotentReplayException(String message, Throwable cause) {
        super(message, cause);
    }
}
