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
package io.openleap.common.http.security;

import io.openleap.common.http.security.identity.IdentityHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Once-per-request filter that extracts tenant/user ids from headers and stores them in IdentityHolder.
 * Active only when profile "nosec" is enabled. Headers are optional; invalid values are logged and ignored.
 */
@Component
@Profile("nosec")
public class NosecHeaderFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(NosecHeaderFilter.class);
    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String USER_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Support both canonical headers and simple variants used by tools (e.g., Insomnia): tenantId/userId
        String tenantId = firstNonBlank(request.getHeader(TENANT_HEADER), request.getHeader("tenantId"));
        String userId = firstNonBlank(request.getHeader(USER_HEADER), request.getHeader("userId"));
        try {
            // Tenant header is optional; if present, try to parse UUID; on failure, log and ignore
            if (tenantId != null && !tenantId.isBlank()) {
                try {
                    UUID tenantUuid = UUID.fromString(tenantId.trim());
                    IdentityHolder.setTenantId(tenantUuid);
                } catch (Exception e) {
                    log.warn("Invalid tenant header value: {}", tenantId);
                }
            }
            // User header is optional as well
            if (userId != null && !userId.isBlank()) {
                try {
                    UUID userUuid = UUID.fromString(userId.trim());
                    IdentityHolder.setUserId(userUuid);
                } catch (Exception e) {
                    log.warn("Invalid user header value: {}", userId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // Ensure the ThreadLocal is cleared to prevent leaks across requests
            IdentityHolder.clear();
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
