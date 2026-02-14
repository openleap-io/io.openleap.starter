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
package io.openleap.common.http.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Optional OpenTelemetry setup. Disabled by default. Enable with:
 *   ol.tracing.otel.enabled=true
 * and configure endpoint via ol.tracing.otel.endpoint property.
 */
@Configuration
@EnableConfigurationProperties(TracingProperties.class)
public class OtelConfig {

    @Bean
    @ConditionalOnProperty(value = "ol.tracing.otel.enabled", havingValue = "true")
    public OpenTelemetry openTelemetry(TracingProperties tracingProperties) {
        String endpoint = System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT",
                tracingProperties.getOtel().getEndpoint());

        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofSeconds(10))
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(Resource.getDefault())
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        GlobalOpenTelemetry.set(sdk);
        return sdk;
    }
}
