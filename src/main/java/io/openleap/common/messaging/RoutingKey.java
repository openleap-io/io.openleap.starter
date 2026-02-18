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
package io.openleap.common.messaging;

import java.util.Objects;

public record RoutingKey(
        String key,
        String description,
        String jsonSchemaUrl,
        String avroSchemaUrl
) {

    public RoutingKey {
        Objects.requireNonNull(key, "Routing key cannot be null");
    }

    public static RoutingKey of(String key) {
        return new RoutingKey(key, null, null, null);
    }

    public static RoutingKey of(String key, String description) {
        return new RoutingKey(key, description, null, null);
    }

    public static RoutingKey withJsonSchema(String key, String description, String jsonSchemaUrl) {
        return new RoutingKey(key, description, jsonSchemaUrl, null);
    }

    public static RoutingKey withAvroSchema(String key, String description, String avroSchemaUrl) {
        return new RoutingKey(key, description, null, avroSchemaUrl);
    }

}
