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
package io.openleap.starter.testkit.identity;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * JUnit 5 annotation to seed IdentityHolder with a tenant/user for a test.
 * Usage:
 *  @WithTenant(tenant="00000000-0000-0000-0000-000000000001")
 *  void test() { ... }
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(IdentityTestExtension.class)
@Documented
public @interface WithTenant {
    /** Tenant UUID string (required). */
    String tenant();
    /** Optional user UUID string. */
    String user() default "";
    /** Optional roles to assign to the user. */
    String[] roles() default {};
    /** Optional scopes to assign to the user. */
    String[] scopes() default {};
}
