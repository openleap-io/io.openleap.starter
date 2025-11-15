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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Sets PostgreSQL session variable app.tenant_id for the current transaction when a tenant is present.
 * Uses "set local" so the value is scoped to the current transaction only.
 *
 * Guarded at runtime: on non-PostgreSQL databases (e.g., H2) the statement will fail and is ignored.
 */
@Aspect
@Component
public class TenantRlsAspect {

    private final JdbcTemplate jdbcTemplate;

    public TenantRlsAspect(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || within(@org.springframework.transaction.annotation.Transactional *) || execution(* io.openleap.fi.acc.service..*(..))")
    public Object setTenantIdForTx(ProceedingJoinPoint pjp) throws Throwable {
        UUID tenant = IdentityHolder.getTenantId();
        if (tenant != null) {
            try {
                // Scope to current transaction if any; on H2 this will throw and is ignored.
                jdbcTemplate.execute("set local app.tenant_id = '" + tenant + "'");
            } catch (Exception ignored) {
                // Non-PostgreSQL or unavailable variable; ignore
            }
        }
        return pjp.proceed();
    }
}
