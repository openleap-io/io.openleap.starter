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
package io.openleap.common.messaging.event;

import java.time.Instant;
import java.util.Map;

/**
 * Interface for all domain events in the OpenLeap ecosystem.
 * Follows the Thin Event (Notification) pattern.
 */
public interface DomainEvent {

    /**
     * @return The business identifier of the aggregate that was changed.
     */
    String getAggregateId();

    /**
     * @return The type of the aggregate (e.g., "Order", "User").
     */
    String getAggregateType();

    /**
     * @return The type of the event (e.g., "OrderCreated", "PaymentReceived").
     */
    String getChangeType();

    /**
     * @return The timestamp when the state change occurred.
     */
    Instant getOccurredAt();

    /**
     * @return The version of the aggregate after the change.
     */
    Long getVersion();

    /**
     * @return Optional metadata for the event.
     */
    Map<String, Object> getMetadata();

}
