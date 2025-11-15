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
package io.openleap.starter.testkit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.openleap.starter.core.event.MessageTopologyConfiguration;
import io.openleap.starter.core.event.RoutingKey;
import io.openleap.starter.core.repository.entity.OlPersistenceEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Reusable ArchUnit rules to verify compliance with the OpenLeap guideline.
 *
 * Usage:
 *  ArchitectureRules.load("io.openleap.acc.customer");
 *  ArchitectureRules.entitiesExtendOlPersistenceEntity().check(classes);
 */
public final class ArchitectureRules {

    private ArchitectureRules() {}

    /** Routing key regex: <suite>.<domain>.<aggregate>.<kind>.<action>.v<major> */
    public static final String ROUTING_KEY_REGEX =
            "^(?<suite>[a-z0-9]+)\\.(?<domain>[a-z0-9]+)\\.(?<aggregate>[a-z0-9]+)\\.(?<kind>command|event)\\.(?<action>[a-z0-9_]+)\\.v(?<major>\\d+)$";

    /** Load classes from the given base package for ad-hoc checks. */
    public static JavaClasses load(String basePackage) {
        return new ClassFileImporter().importPackages(basePackage);
    }

    /** All @Entity classes must extend OlPersistenceEntity. */
    public static ArchRule entitiesExtendOlPersistenceEntity() {
        return classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().beAssignableTo(OlPersistenceEntity.class)
                .as("JPA entities must extend OlPersistenceEntity");
    }

    /** Entities must not be returned from REST controllers. */
    public static ArchRule controllersDoNotReturnEntities() {
        return methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .should(notReturnEntityTypes())
                .as("REST controller methods must not return @Entity types (use DTOs)");
    }

    private static ArchCondition<JavaMethod> notReturnEntityTypes() {
        return new ArchCondition<JavaMethod>("not return @Entity types") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                var returnType = method.getReturnType().toErasure();

                // Check if return type itself is an Entity
                if (returnType.isAnnotatedWith(jakarta.persistence.Entity.class)) {
                    events.add(SimpleConditionEvent.violated(method,
                            String.format("Method %s returns @Entity type %s",
                                    method.getFullName(), returnType.getName())));
                }

                // Check if return type is a collection/wrapper containing Entities
                checkForEntityInGenericTypes(method, returnType, events);
            }

            private void checkForEntityInGenericTypes(JavaMethod method, JavaClass type, ConditionEvents events) {
                type.getTypeParameters().forEach(typeParam -> {
                    JavaClass actualType = typeParam.toErasure();
                    if (actualType.isAnnotatedWith(jakarta.persistence.Entity.class)) {
                        events.add(SimpleConditionEvent.violated(method,
                                String.format("Method %s returns type containing @Entity: %s",
                                        method.getFullName(), actualType.getName())));
                    }
                });
            }
        };
    }

    /** Controllers should return ResponseEntity or DTOs under *.dto.* packages (heuristic). */
    public static ArchRule controllersPreferDtoOrResponseEntity() {
        return classes().that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().resideInAnyPackage("..dto..", ResponseEntity.class.getPackageName())
                .as("Controllers should depend on DTOs or ResponseEntity");
    }

    /**
     * Architecture compliance: Instantiation of RoutingKey is only allowed in classes
     * annotated with @MessageTopologyConfiguration.
     */
    public static ArchRule routingKeysInstantiatedOnlyInTopologyClasses() {
        ArchCondition<JavaClass> condition = new ArchCondition<JavaClass>(
                "not instantiate RoutingKey outside @MessageTopologyConfiguration classes") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                // Allowed: classes annotated with @MessageTopologyConfiguration
                if (javaClass.isAnnotatedWith(MessageTopologyConfiguration.class)) {
                    return;
                }
                for (JavaConstructorCall call : javaClass.getConstructorCallsFromSelf()) {
                    if (call.getTarget().getOwner().isAssignableTo(RoutingKey.class)) {
                        events.add(SimpleConditionEvent.violated(call,
                                String.format("%s instantiates RoutingKey via %s",
                                        javaClass.getName(), call.getDescription())));
                    }
                }
            }
        };
        return classes().should(condition)
                .as("RoutingKey may only be instantiated inside classes annotated with @MessageTopologyConfiguration");
    }
}
