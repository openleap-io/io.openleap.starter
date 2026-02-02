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

package io.openleap.common.idempotency;

import io.openleap.common.persistence.entity.PersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Idempotency record to prevent duplicate processing of identical requests.
 */
@Entity
@Table(name = "idempotency")
@Getter
@Setter
public class IdempotencyRecordEntity extends PersistenceEntity {

    @Column(nullable = false, unique = true, length = 128)
    private String idemKey;

    @Column(nullable = false, length = 128)
    private String payloadHash;

    @Column(nullable = false)
    private UUID entryUuid;
}
