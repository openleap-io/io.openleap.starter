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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for Spring @Configuration classes that declare message topology
 * (exchanges, queues, bindings, and canonical routing keys) for a service/domain.
 *
 * Usage:
 * <pre>
 * @MessageTopologyConfiguration("bp-events")
 * @Configuration
 * public class MessagingConfig { ... }
 * </pre>
 *
 * Notes:
 * - This is a marker only; it enables convention-based discovery and documentation.
 * - Tools and coverage mechanisms in the platform may scan for this annotation
 *   to collect declared routing keys and validate bindings.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MessageTopologyConfiguration {
    /**
     * Optional identifier/name for this topology configuration (e.g., "bp-events").
     */
    String value() default "";
}