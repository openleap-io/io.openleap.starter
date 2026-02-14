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

package io.openleap.common.messaging.entity;

import io.openleap.common.domain.DomainEntity;
import io.openleap.common.persistence.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Transactional outbox record for reliable messaging publication.
 * Records are inserted within domain transactions and later dispatched to the broker.
 * Backed by fi_acc.outbox.
 */
@Entity
@Table(name = "outbox")
@Getter
@Setter
public class OutboxEvent extends AuditableEntity implements DomainEntity<OutboxEventId> {

    @Embedded
    private OutboxEventId businessId;

    @Column(nullable = false, length = 256)
    private String exchangeKey;

    @Column(nullable = false, length = 256)
    private String routingKey;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private int attempts;

    @Column
    private Instant nextAttemptAt;

    @Column(length = 4000)
    private String lastError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String headersJson;
}
