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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Property-driven identity extraction for HTTP requests.
 * Modes:
 *  - simplesec: extract JWT (scopes, roles, tenantId, userId) from Spring Security context/JWT header
 *  - nosec: extract from plain headers (X-Tenant-Id, X-User-Id, X-Scopes, X-Roles)
 * Always clears IdentityHolder in finally.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // after TraceIdFilter
public class IdentityHttpFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdentityHttpFilter.class);

    public static final String HDR_TENANT = "X-Tenant-Id";
    public static final String HDR_USER = "X-User-Id";
    public static final String HDR_SCOPES = "X-Scopes";
    public static final String HDR_ROLES = "X-Roles";
    public static final String HDR_JWT = "X-JWT";

    @Autowired(required = false)
    private OlStarterServiceProperties olStarterServiceProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        OlStarterServiceProperties.Security.Mode mode = resolveMode();
        try {
            if (mode == OlStarterServiceProperties.Security.Mode.simplesec) {
                applyFromJwt(request);
            } else {
                applyFromHeaders(request);
            }
            // Bridge to MDC for logging
            putMdc("tenantId", uuidToStringSafe(IdentityHolder.getTenantId()));
            putMdc("userId", uuidToStringSafe(IdentityHolder.getUserId()));
            filterChain.doFilter(request, response);
        } finally {
            // Clear both MDC and IdentityHolder to avoid leakage across threads
            MDC.remove("tenantId");
            MDC.remove("userId");
            IdentityHolder.clear();
        }
    }

    private void applyFromHeaders(HttpServletRequest request) {
        // Values may be absent; we do not enforce validation at HTTP layer
        String tenantId = firstNonBlank(request.getHeader(HDR_TENANT), request.getHeader("tenantId"));
        String userId = firstNonBlank(request.getHeader(HDR_USER), request.getHeader("userId"));
        if (isNotBlank(tenantId)) {
            uuidOrNull(tenantId).ifPresent(IdentityHolder::setTenantId);
        }
        if (isNotBlank(userId)) {
            uuidOrNull(userId).ifPresent(IdentityHolder::setUserId);
        }
        IdentityHolder.setScopes(parseCsvHeader(request.getHeader(HDR_SCOPES)));
        IdentityHolder.setRoles(parseCsvHeader(request.getHeader(HDR_ROLES)));
    }

    private void applyFromJwt(HttpServletRequest request) {
        // Prefer Spring Security JwtAuthenticationToken when present
        Authentication auth = (Authentication) request.getUserPrincipal();
        Map<String, Object> claims = Collections.emptyMap();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            claims = jwt.getClaims();
        } else if (auth instanceof AbstractAuthenticationToken aat) {
            Object details = aat.getDetails();
        }
        if (claims.isEmpty()) {
            // Try Authorization: Bearer <jwt> or X-JWT header
            String authz = request.getHeader("Authorization");
            String jwtHeader = request.getHeader(HDR_JWT);
            String jwt = null;
            if (isNotBlank(authz) && authz.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
                jwt = authz.substring(7).trim();
            } else if (isNotBlank(jwtHeader)) {
                jwt = jwtHeader.trim();
            }
            if (isNotBlank(jwt)) {
                claims = JwtUtils.decodePayloadClaims(jwt);
            }
        }
        // Extract values
        Optional<UUID> tenant = findUuidClaim(claims, "tenantId", "tenantid", "tenant_id");
        Optional<UUID> user = findUuidClaim(claims, "userId", "userid", "user_id", "sub");
        tenant.ifPresent(IdentityHolder::setTenantId);
        user.ifPresent(IdentityHolder::setUserId);

        Set<String> roles = claimAsStringSet(claims, "roles");
        Set<String> scope = scopesFromClaim(claims);
        IdentityHolder.setRoles(roles);
        IdentityHolder.setScopes(scope);
    }

    private static Set<String> scopesFromClaim(Map<String, Object> claims) {
        // Support "scope" as space-delimited string or array, and "scopes"
        Set<String> out = new HashSet<>();
        Object scope = claims.get("scope");
        if (scope instanceof String s) {
            out.addAll(splitSpaceOrComma(s));
        } else if (scope instanceof Collection<?> c) {
            for (Object o : c) if (o != null) out.add(o.toString());
        }
        Object scopes = claims.get("scopes");
        if (scopes instanceof String s) {
            out.addAll(splitSpaceOrComma(s));
        } else if (scopes instanceof Collection<?> c) {
            for (Object o : c) if (o != null) out.add(o.toString());
        }
        return out;
    }

    private static Set<String> claimAsStringSet(Map<String, Object> claims, String name) {
        Set<String> out = new HashSet<>();
        Object v = claims.get(name);
        if (v instanceof String s) {
            out.addAll(splitSpaceOrComma(s));
        } else if (v instanceof Collection<?> c) {
            for (Object o : c) if (o != null) out.add(o.toString());
        }
        return out;
    }

    private static Set<String> parseCsvHeader(String header) {
        if (!isNotBlank(header)) return Collections.emptySet();
        return splitSpaceOrComma(header);
    }

    private static Set<String> splitSpaceOrComma(String in) {
        Set<String> out = new HashSet<>();
        if (in == null) return out;
        String[] parts = in.split("[ ,]");
        for (String p : parts) {
            if (p != null) {
                String t = p.trim();
                if (!t.isEmpty()) out.add(t);
            }
        }
        return out;
    }

    private static Optional<UUID> findUuidClaim(Map<String, Object> claims, String... keys) {
        for (String k : keys) {
            Object v = claims.get(k);
            if (v != null) {
                Optional<UUID> parsed = uuidOrNull(v.toString());
                if (parsed.isPresent()) return parsed;
            }
        }
        return Optional.empty();
    }

    private static Optional<UUID> uuidOrNull(String v) {
        try { return Optional.of(UUID.fromString(v.trim())); } catch (Exception e) { return Optional.empty(); }
    }

    private static boolean isNotBlank(String s) { return s != null && !s.trim().isEmpty(); }

    private OlStarterServiceProperties.Security.Mode resolveMode() {
        try {
            if (olStarterServiceProperties != null && olStarterServiceProperties.getSecurity() != null && olStarterServiceProperties.getSecurity().getMode() != null) {
                return olStarterServiceProperties.getSecurity().getMode();
            }
        } catch (Exception ignored) { }
        return OlStarterServiceProperties.Security.Mode.nosec;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) if (isNotBlank(v)) return v;
        return null;
    }

    private static void putMdc(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        } else {
            MDC.remove(key);
        }
    }

    private static String uuidToStringSafe(java.util.UUID u) {
        return u == null ? null : u.toString();
    }
}
