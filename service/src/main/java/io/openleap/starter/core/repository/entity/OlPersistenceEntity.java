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

import java.io.Serializable;

@MappedSuperclass
public abstract class OlPersistenceEntity implements Serializable {

    @Id
    @Column(
            name = "id", nullable = false
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE
    )
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OlPersistenceEntity that = (OlPersistenceEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        // Use class hashCode for consistent behavior across entity states
        return getClass().hashCode();
    }

}
