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
package io.openleap.starter.core.messaging.config;

import io.openleap.starter.core.config.IdentityHolder;
import io.openleap.starter.core.config.OlStarterServiceProperties;
import io.openleap.starter.core.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Extracts identity information from incoming AMQP messages and stores it in IdentityHolder.
 * In simplesec mode, expects an 'x-jwt' header; in nosec, expects plain headers.
 * Validates presence of tenantId and userId; rejects message if missing or invalid.
 */
import org.springframework.stereotype.Component;

@Component
public class MessagingIdentityPostProcessor implements MessagePostProcessor {

    private static final Logger log = LoggerFactory.getLogger(MessagingIdentityPostProcessor.class);

    public static final String HDR_TENANT = "x-tenant-id";
    public static final String HDR_USER = "x-user-id";
    public static final String HDR_SCOPES = "x-scopes";
    public static final String HDR_ROLES = "x-roles";
    public static final String HDR_JWT = "x-jwt";

    @Autowired(required = false)
    private OlStarterServiceProperties olStarterServiceProperties;

    @Override
    public Message postProcessMessage(Message message) throws AmqpRejectAndDontRequeueException {
        OlStarterServiceProperties.Security.Mode mode = resolveMode();
        try {
            if (mode == OlStarterServiceProperties.Security.Mode.iamsec) {
                applyFromJwt(message.getMessageProperties());
            } else {
                applyFromHeaders(message.getMessageProperties());
            }
            // Validate mandatory
            if (IdentityHolder.getTenantId() == null || IdentityHolder.getUserId() == null) {
                throw new AmqpRejectAndDontRequeueException("Missing tenantId or userId in message headers");
            }
            return message;
        } catch (AmqpRejectAndDontRequeueException e) {
            // Ensure clear before throwing
            IdentityHolder.clear();
            throw e;
        } catch (RuntimeException e) {
            IdentityHolder.clear();
            throw e;
        }
    }

    private void applyFromHeaders(MessageProperties props) {
        Map<String, Object> headers = safeHeaders(props);
        Optional<UUID> tenant = firstUuid(headers, HDR_TENANT, "X-Tenant-Id");
        Optional<UUID> user = firstUuid(headers, HDR_USER, "X-User-Id");
        tenant.ifPresent(IdentityHolder::setTenantId);
        user.ifPresent(IdentityHolder::setUserId);
        IdentityHolder.setScopes(parseSet(headers.get(HDR_SCOPES), headers.get("X-Scopes")));
        IdentityHolder.setRoles(parseSet(headers.get(HDR_ROLES), headers.get("X-Roles")));
    }

    private void applyFromJwt(MessageProperties props) {
        Map<String, Object> headers = safeHeaders(props);
        String jwt = stringHeader(headers.get(HDR_JWT), headers.get("X-JWT"));
        Map<String, Object> claims = JwtUtils.decodePayloadClaims(jwt);
        // tenant/user
        findUuidClaim(claims, "tenantId", "tenantid", "tenant_id").ifPresent(IdentityHolder::setTenantId);
        findUuidClaim(claims, "userId", "userid", "user_id", "sub").ifPresent(IdentityHolder::setUserId);
        // roles/scopes
        IdentityHolder.setRoles(claimAsStringSet(claims, "roles"));
        IdentityHolder.setScopes(scopesFromClaim(claims));
    }

    private static Map<String, Object> safeHeaders(MessageProperties props) {
        Map<String, Object> h = props.getHeaders();
        return h != null ? h : Collections.emptyMap();
    }

    private static String stringHeader(Object... candidates) {
        for (Object o : candidates) {
            if (o != null) {
                String s = o.toString().trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }

    private static Optional<UUID> firstUuid(Map<String, Object> headers, String... keys) {
        for (String k : keys) {
            Object v = headers.get(k);
            if (v != null) {
                try { return Optional.of(UUID.fromString(v.toString().trim())); } catch (Exception ignored) {}
            }
        }
        return Optional.empty();
    }

    private static Optional<UUID> findUuidClaim(Map<String, Object> claims, String... keys) {
        for (String k : keys) {
            Object v = claims.get(k);
            if (v != null) {
                try { return Optional.of(UUID.fromString(v.toString().trim())); } catch (Exception ignored) {}
            }
        }
        return Optional.empty();
    }

    private static Set<String> parseSet(Object... candidates) {
        Set<String> out = new HashSet<>();
        for (Object c : candidates) {
            if (c == null) continue;
            String s = c.toString();
            String[] parts = s.split("[ ,]");
            for (String p : parts) {
                if (p != null) {
                    String t = p.trim();
                    if (!t.isEmpty()) out.add(t);
                }
            }
            if (!out.isEmpty()) break;
        }
        return out;
    }

    private static Set<String> scopesFromClaim(Map<String, Object> claims) {
        Set<String> out = new HashSet<>();
        Object scope = claims.get("scope");
        if (scope instanceof String s) {
            out.addAll(Arrays.asList(s.split(" ")));
        } else if (scope instanceof Collection<?> c) {
            for (Object o : c) if (o != null) out.add(o.toString());
        }
        Object scopes = claims.get("scopes");
        if (scopes instanceof String s) {
            out.addAll(Arrays.asList(s.split("[ ,]")));
        } else if (scopes instanceof Collection<?> c) {
            for (Object o : c) if (o != null) out.add(o.toString());
        }
        return out;
    }

    private static Set<String> claimAsStringSet(Map<String, Object> claims, String name) {
        Set<String> out = new HashSet<>();
        Object v = claims.get(name);
        if (v instanceof String s) {
            out.addAll(Arrays.asList(s.split("[ ,]")));
        } else if (v instanceof Collection<?> c) {
            for (Object o : c) if (o != null) out.add(o.toString());
        }
        return out;
    }

    private OlStarterServiceProperties.Security.Mode resolveMode() {
        try {
            if (olStarterServiceProperties != null && olStarterServiceProperties.getSecurity() != null && olStarterServiceProperties.getSecurity().getMode() != null) {
                return olStarterServiceProperties.getSecurity().getMode();
            }
        } catch (Exception ignored) { }
        return OlStarterServiceProperties.Security.Mode.nosec;
    }
}
