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
import io.openleap.starter.core.event.EventPublisher;
import io.openleap.starter.core.event.RoutingKey;
import io.openleap.starter.core.repository.OutboxRepository;
import io.openleap.starter.core.repository.entity.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventPublisherUnitTest {

    private OutboxRepository outboxRepository;
    private ObjectMapper objectMapper;
    private EventPublisher publisher;

    @BeforeEach
    void setup() {
        outboxRepository = Mockito.mock(OutboxRepository.class);
        objectMapper = new ObjectMapper();
        publisher = new EventPublisher(outboxRepository, objectMapper);
    }

    @Test
    void enqueue_persistsOutboxRecord_withHeadersAndPayload() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        Mockito.when(outboxRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> headers = Map.of("traceId", "t-1", "eventId", "e-1");
        Object payload = Map.of("id", 123, "name", "test");
        publisher.enqueue("fi.acc.test.exchange", new RoutingKey("fi.acc.test.event", "non"), payload, headers);

        Mockito.verify(outboxRepository).save(captor.capture());
        OutboxEvent e = captor.getValue();
        assertNotNull(e.getUuid());
        assertEquals("fi.acc.test.event", e.getRoutingKey());
        assertFalse(e.isPublished());
        assertNotNull(e.getPayloadJson());
        assertTrue(e.getPayloadJson().contains("\"name\":\"test\""));
        assertNotNull(e.getHeadersJson());
        assertTrue(e.getHeadersJson().contains("traceId"));
    }
}
