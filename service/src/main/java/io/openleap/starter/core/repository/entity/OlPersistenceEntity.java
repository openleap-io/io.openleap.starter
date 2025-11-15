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

package io.openleap.starter.core.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class})
/**
 * Base persistence entity with technical identifiers and auditing columns.
 * Serves as mapped superclass for all JPA entities in this service.
 */
public abstract class OlPersistenceEntity implements Serializable {

    @Id
    @Column(
            name = "pk"
    )
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long pKey;

    @Column(name = "uuid")
    private UUID uuid;


    @Version
    @Column(
            name = "version"
    )
    private long version;

    @Column(
            name = "created_at"
    )
    @CreatedDate
    private Instant createdAt;

    @Column(
            name = "created_by"
    )
    @CreatedBy
    private String createdBy;

    @Column(
            name = "updated_at"
    )
    @LastModifiedDate
    private Instant updatedAt;

    @Column(
            name = "updated_by"
    )
    @LastModifiedBy
    private String updatedBy;

}
