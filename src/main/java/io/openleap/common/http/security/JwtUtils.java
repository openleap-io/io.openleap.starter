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

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * Minimal JWT utility to decode claims without signature verification.
 * Intended for extracting identity information in "simplesec" mode.
 */
public final class JwtUtils {
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private JwtUtils() {
    }

    public static Map<String, Object> decodePayloadClaims(String jwt) {
        if (jwt == null) return Collections.emptyMap();
        String token = jwt.trim();
        if (token.isEmpty()) return Collections.emptyMap();
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return Collections.emptyMap();
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            String json = new String(decoded, StandardCharsets.UTF_8);
            return JSON_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
