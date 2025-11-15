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
package io.openleap.starter.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides Spring Data JPA AuditorAware backed by IdentityHolder so that
 * @CreatedBy and @LastModifiedBy fields in entities are populated from the
 * current user identity seeded by tests (@WithTenant) or runtime filters.
 */
@Configuration
public class AuditingProviderConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            UUID userId = IdentityHolder.getUserId();
            return Optional.ofNullable(userId).map(UUID::toString);
        };
    }
}
