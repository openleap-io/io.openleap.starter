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
package io.openleap.starter.core.messaging.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Generic, minimal message payload used in informational events.
 * Concrete domain payloads should extend this type to keep a consistent envelope.
 */
public class EventPayload {
    private final String aggregateType;
    private final String changeType;
    private final List<String> entityIds;
    private final Long version;
    private final OffsetDateTime occurredAt;

    public EventPayload(String aggregateType,
                        String changeType,
                        List<String> entityIds,
                        Long version,
                        OffsetDateTime occurredAt) {
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType");
        this.changeType = Objects.requireNonNull(changeType, "changeType");
        this.entityIds = entityIds == null ? List.of() : List.copyOf(entityIds);
        this.version = version;
        this.occurredAt = occurredAt;
    }

    public String getAggregateType() { return aggregateType; }
    public String getChangeType() { return changeType; }
    public List<String> getEntityIds() { return entityIds; }
    public Long getVersion() { return version; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
