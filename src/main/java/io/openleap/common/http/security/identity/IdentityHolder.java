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
package io.openleap.common.http.security.identity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Holds identity information for the current thread/request lifecycle.
 * Use carefully and always clear after request is processed to avoid memory leaks in thread pools.
 */
public final class IdentityHolder {
    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> ROLES = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> SCOPES = new ThreadLocal<>();

    private IdentityHolder() {}

    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            TENANT.remove();
        } else {
            TENANT.set(tenantId);
        }
    }

    public static UUID getTenantId() {
        return TENANT.get();
    }

    public static void setUserId(UUID userId) {
        if (userId == null) {
            USER.remove();
        } else {
            USER.set(userId);
        }
    }

    public static UUID getUserId() {
        return USER.get();
    }

    public static void setRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            ROLES.remove();
        } else {
            ROLES.set(normalize(roles));
        }
    }

    public static Set<String> getRoles() {
        Set<String> v = ROLES.get();
        return v == null ? Collections.emptySet() : v;
    }

    public static void setScopes(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            SCOPES.remove();
        } else {
            SCOPES.set(normalize(scopes));
        }
    }

    public static Set<String> getScopes() {
        Set<String> v = SCOPES.get();
        return v == null ? Collections.emptySet() : v;
    }

    public static void clear() {
        TENANT.remove();
        USER.remove();
        ROLES.remove();
        SCOPES.remove();
    }

    private static Set<String> normalize(Set<String> in) {
        Set<String> out = new HashSet<>();
        for (String s : in) {
            if (s != null) {
                String t = s.trim();
                if (!t.isEmpty()) out.add(t);
            }
        }
        return out;
    }
}
