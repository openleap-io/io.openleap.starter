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
package io.openleap.starter.core.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openleap.starter.core.config.OlStarterServiceProperties;
import io.openleap.starter.core.messaging.RoutingKey;
import io.openleap.starter.core.messaging.event.EventPayload;
import io.openleap.starter.core.messaging.event.EventPublisher;
import io.openleap.starter.core.repository.OutboxRepository;
import io.openleap.starter.core.repository.entity.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventPublisherUnitTest {

    private OutboxRepository outboxRepository;
    private ObjectMapper objectMapper;
    private EventPublisher publisher;
    private OlStarterServiceProperties config;

    @BeforeEach
    void setup() {
        outboxRepository = Mockito.mock(OutboxRepository.class);
        objectMapper = new ObjectMapper();
        // Ensure OffsetDateTime and other Java Time types serialize to ISO-8601 strings
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        config = Mockito.mock(OlStarterServiceProperties.class);
        publisher = new EventPublisher(config, outboxRepository, objectMapper);
    }

    @Test
    void enqueue_persistsOutboxRecord_withHeadersAndPayload() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        Mockito.when(outboxRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> headers = new java.util.HashMap<>();
        headers.put("traceId", "t-1");
        headers.put("eventId", "e-1");
        headers.put("producer", "tech.starter-test");
        headers.put("tenantId", UUID.randomUUID().toString());
        headers.put("schemaRef", "tech/dms/document.created.schema.json");

        EventPayload payload = new EventPayload(
                "testAggregate",
                "create",
                List.of(UUID.randomUUID().toString()),
                1L,
                OffsetDateTime.now()
        );

        publisher.enqueue("fi.acc.test.exchange",
                new RoutingKey("fi.acc.test.messaging", "Test key", "https://schemas.openleap.io/tech/dms/document.created.schema.json", null),
                payload,
                headers);

        Mockito.verify(outboxRepository).save(captor.capture());
        OutboxEvent e = captor.getValue();
        assertNotNull(e.getBusinessId());
        assertEquals("fi.acc.test.messaging", e.getRoutingKey());
        assertFalse(e.isPublished());
        assertNotNull(e.getPayloadJson());
        // Validate essential payload fields serialized
        assertTrue(e.getPayloadJson().contains("\"aggregateType\":\"testAggregate\""));
        assertTrue(e.getPayloadJson().contains("\"changeType\":\"create\""));
        assertNotNull(e.getHeadersJson());
        assertTrue(e.getHeadersJson().contains("traceId"));
        assertTrue(e.getHeadersJson().contains("producer"));
        assertTrue(e.getHeadersJson().contains("tenantId"));
    }
}
