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

package io.openleap.common.messaging.event;

import io.openleap.common.ReflectionUtils;
import io.openleap.common.messaging.MessageCoverageTracker;
import io.openleap.common.messaging.RoutingKey;
import io.openleap.common.messaging.config.MessagingProperties;
import io.openleap.common.messaging.entity.OutboxEvent;
import io.openleap.common.messaging.repository.OutboxRepository;
import io.openleap.common.messaging.service.OutboxOrchestrator;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private OutboxOrchestrator outboxOrchestrator;

    @Mock
    private MessagingProperties config;

    @Mock
    private MessageCoverageTracker messageCoverageTracker;

    private EventPublisher eventPublisher;

    @BeforeEach
    void setup() {
        eventPublisher = new EventPublisher(config, outboxRepository, jsonMapper, outboxOrchestrator, Optional.of(messageCoverageTracker));

        ReflectionUtils.setField(eventPublisher, "eventsExchange", "test-exchange");
        ReflectionUtils.setField(eventPublisher, "wakeupAfterCommit", false); // Ignore the synchronization logic
    }

    @DisplayName("Should successfully save enriched event to outbox persistence")
    @Test
    void enqueue_Success_WhenPayloadIsValid() throws Exception {
        // given
        RoutingKey routingKey = new RoutingKey("order.created", "Order Created Event", null, null);
        BaseDomainEvent payload = BaseDomainEvent.builder().build();
        Map<String, String> headers = new HashMap<>(Map.of("custom-header", "test"));
        String jsonHeaders = "{\"custom-header\":\"test\"}";
        String jsonPayload = "{\"type\":\"test\"}";

        when(jsonMapper.writeValueAsString(any(BaseDomainEvent.class))).thenReturn(jsonPayload);
        when(jsonMapper.writeValueAsString(any(Map.class))).thenReturn(jsonHeaders);

        // when
        eventPublisher.enqueue(routingKey, payload, headers);

        // then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository, times(1)).save(eventCaptor.capture());

        OutboxEvent captured = eventCaptor.getValue();

        assertThat(captured)
                .as("The outbox event should be correctly mapped and enriched")
                .returns("test-exchange", OutboxEvent::getExchangeKey)
                .returns("order.created", OutboxEvent::getRoutingKey)
                .returns(jsonPayload, OutboxEvent::getPayloadJson)
                .returns(false, OutboxEvent::isPublished)
                .returns(0, OutboxEvent::getAttempts)
                .returns(null, OutboxEvent::getNextAttemptAt)
                // We switch the focus to the Headers string for the final check
                .extracting(OutboxEvent::getHeadersJson, InstanceOfAssertFactories.STRING)
                .contains("custom-header", "test");
    }

}
