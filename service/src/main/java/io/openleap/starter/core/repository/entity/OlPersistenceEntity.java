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

import io.openleap.starter.core.util.OlUuid;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

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
    private UUID id;


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
    private UUID createdBy;

    @Column(
            name = "updated_at"
    )
    @LastModifiedDate
    private Instant updatedAt;

    @Column(
            name = "updated_by"
    )
    @LastModifiedBy
    private UUID updatedBy;

    public OlPersistenceEntity() {
    }

    protected OlPersistenceEntity(OlPersistenceEntityBuilder<?, ?> b) {
        this.pKey = b.pKey;
        this.id = b.id;
        this.version = b.version;
        this.createdAt = b.createdAt;
        this.createdBy = b.createdBy;
        this.updatedAt = b.updatedAt;
        this.updatedBy = b.updatedBy;
    }

    @PrePersist
    protected void ensureUuid() {
        if (this.id == null) {
            this.id = OlUuid.create();
        }
    }


    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getpKey() {
        return pKey;
    }

    public void setpKey(Long pKey) {
        this.pKey = pKey;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getShortId() {
        if (this.id == null) return null;
        return OlUuid.toShortBase64(this.id);
    }

    public static abstract class OlPersistenceEntityBuilder<C extends OlPersistenceEntity, B extends OlPersistenceEntityBuilder<C, B>> {
        private Long pKey;
        private UUID id;
        private long version;
        private Instant createdAt;
        private UUID createdBy;
        private Instant updatedAt;
        private UUID updatedBy;

        public B pKey(Long pKey) {
            this.pKey = pKey;
            return self();
        }

        public B id(UUID id) {
            this.id = id;
            return self();
        }

        public B version(long version) {
            this.version = version;
            return self();
        }

        public B createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return self();
        }

        public B createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return self();
        }

        public B updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return self();
        }

        public B updatedBy(UUID updatedBy) {
            this.updatedBy = updatedBy;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "OlPersistenceEntity.OlPersistenceEntityBuilder(pKey=" + this.pKey + ", id=" + this.id + ", version=" + this.version + ", createdAt=" + this.createdAt + ", createdBy=" + this.createdBy + ", lastModifiedAt=" + this.updatedAt + ", lastModifiedBy=" + this.updatedBy + ")";
        }
    }
}
