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
package io.openleap.starter.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Simple HTTP filter that ensures each request has a traceId in the logging MDC
 * and that the same value is propagated back via response header X-Trace-Id.
 * <p>
 * Incoming preference order:
 * - X-Trace-Id header (if present and non-blank)
 * - traceId header (fallback)
 * - generated UUID
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = header(request, TRACE_ID_HEADER);
        if (incoming == null || incoming.isBlank()) {
            incoming = header(request, TRACE_ID_MDC_KEY);
        }
        String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming.trim();
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        try {
            // Proceed
            filterChain.doFilter(request, response);
        } finally {
            // Always set header and clear MDC
            response.setHeader(TRACE_ID_HEADER, traceId);
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private static String header(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return v != null ? v.trim() : null;
    }
}
