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

import io.openleap.starter.core.config.IdentityHolder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JUnit 5 extension to seed and clear IdentityHolder before/after tests.
 */
public class IdentityTestExtension implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
        WithTenant ann = context.getElement()
                .flatMap(el -> el.getAnnotation(WithTenant.class) != null ? Optional.of(el.getAnnotation(WithTenant.class)) : Optional.empty())
                .orElseGet(() -> context.getRequiredTestClass().getAnnotation(WithTenant.class));
        if (ann != null) {
            UUID tenant = UUID.fromString(ann.tenant());
            IdentityHolder.setTenantId(tenant);
            if (!ann.user().isEmpty()) {
                IdentityHolder.setUserId(UUID.fromString(ann.user()));
            }
            if (ann.roles().length > 0) {
                IdentityHolder.setRoles(Arrays.stream(ann.roles()).collect(Collectors.toSet()));
            }
            if (ann.scopes().length > 0) {
                IdentityHolder.setScopes(Arrays.stream(ann.scopes()).collect(Collectors.toSet()));
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        IdentityHolder.clear();
    }
}
