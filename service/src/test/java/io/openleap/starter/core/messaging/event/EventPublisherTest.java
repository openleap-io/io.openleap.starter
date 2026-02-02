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
package io.openleap.starter.core.messaging.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openleap.starter.core.ReflectionUtils;
import io.openleap.starter.core.config.OlStarterServiceProperties;
import io.openleap.starter.core.messaging.RoutingKey;
import io.openleap.starter.core.messaging.service.OutboxOrchestrator;
import io.openleap.starter.core.repository.OutboxRepository;
import io.openleap.starter.core.repository.entity.OlOutboxEvent;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OutboxOrchestrator outboxOrchestrator;

    @Mock
    private OlStarterServiceProperties config;

    private EventPublisher eventPublisher;

    @BeforeEach
    void setup() {
        eventPublisher = new EventPublisher(config, outboxRepository, objectMapper, outboxOrchestrator);

        ReflectionUtils.setField(eventPublisher, "eventsExchange", "test-exchange");
        ReflectionUtils.setField(eventPublisher, "wakeupAfterCommit", false); // Ignore the synchronization logic
    }

    @DisplayName("Should successfully save enriched event to outbox repository")
    @Test
    void enqueue_Success_WhenPayloadIsValid() throws Exception {
        // given
        RoutingKey routingKey = new RoutingKey("order.created", "Order Created Event", null, null);
        EventPayload payload = new EventPayload();
        Map<String, String> headers = new HashMap<>(Map.of("custom-header", "test"));
        String jsonHeaders = "{\"custom-header\":\"test\"}";
        String jsonPayload = "{\"type\":\"test\"}";

        when(objectMapper.writeValueAsString(any(EventPayload.class))).thenReturn(jsonPayload);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonHeaders);

        // when
        eventPublisher.enqueue(routingKey, payload, headers);

        // then
        ArgumentCaptor<OlOutboxEvent> eventCaptor = ArgumentCaptor.forClass(OlOutboxEvent.class);
        verify(outboxRepository, times(1)).save(eventCaptor.capture());

        OlOutboxEvent captured = eventCaptor.getValue();

        assertThat(captured)
                .as("The outbox event should be correctly mapped and enriched")
                .returns("test-exchange", OlOutboxEvent::getExchangeKey)
                .returns("order.created", OlOutboxEvent::getRoutingKey)
                .returns(jsonPayload, OlOutboxEvent::getPayloadJson)
                .returns(false, OlOutboxEvent::isPublished)
                .returns(0, OlOutboxEvent::getAttempts)
                .returns(null, OlOutboxEvent::getNextAttemptAt)
                // We switch the focus to the Headers string for the final check
                .extracting(OlOutboxEvent::getHeadersJson, InstanceOfAssertFactories.STRING)
                .contains("custom-header", "test");
    }

}