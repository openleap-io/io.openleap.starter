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
 * 2. A commercial license available from:
 *
 *     B+B Unternehmensberatung GmbH & Co.KG
 *     Robert-Bunsen-Straße 10
 *     67098 Bad Dürkheim
 *     Germany
 *     Contact: license@bb-online.de
 *
 *  You may choose which license to apply.
 */
package io.openleap.starter.core.api.dto;

import java.util.List;

/**
 * Standardized DTO for paginated API responses.
 *
 * @param <T> The type of the content items.
 */
public record OlPageableResponseDto<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int pageNumber,
    int pageSize,
    boolean last
) {
    /**
     * Factory method for creating a paginated response.
     */
    public static <T> OlPageableResponseDto<T> of(
            List<T> content,
            long totalElements,
            int totalPages,
            int pageNumber,
            int pageSize,
            boolean last) {
        return new OlPageableResponseDto<>(content, totalElements, totalPages, pageNumber, pageSize, last);
    }
}
