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
package io.openleap.starter.testkit.web;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.hamcrest.Matchers.startsWith;

/**
 * Utilities to assert RFC 7807 Problem+JSON and versioning conventions.
 * Intended for use with Spring MockMvc.
 */
public final class WebMvcCompliance {
    private WebMvcCompliance() {}

    /** Expect Content-Type application/problem+json */
    public static ResultMatcher problemJsonContentType() {
        return MockMvcResultMatchers.header().string("Content-Type", startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    /** Expect ProblemDetail type URI */
    public static ResultMatcher problemType(String typeUri) {
        return MockMvcResultMatchers.jsonPath("$.type").value(typeUri);
    }

    /** Expect ProblemDetail status */
    public static ResultMatcher problemStatus(int status) {
        return MockMvcResultMatchers.jsonPath("$.status").value(status);
    }

    /** Expect presence of traceId in extensions if your handler includes it */
    public static ResultMatcher hasTraceId() {
        return MockMvcResultMatchers.jsonPath("$.traceId").exists();
    }
}
