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
import io.openleap.starter.core.repository.OutboxRepository;
import io.openleap.starter.core.repository.entity.OutboxEvent;
import io.openleap.starter.core.messaging.service.OutboxDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxDispatcherUnitTest {

    private OutboxRepository outboxRepository;
    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;

    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setup() {
        outboxRepository = Mockito.mock(OutboxRepository.class);
        rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        objectMapper = new ObjectMapper();
        dispatcher = new OutboxDispatcher(outboxRepository, rabbitTemplate, objectMapper);
    }

    @Test
    void dispatch_noop_whenDisabled() throws Exception {
        // Set enabled=false via reflection
        setField(dispatcher, "enabled", false);
        dispatcher.dispatch();
        Mockito.verify(outboxRepository, Mockito.never()).findPending();
    }

    @Test
    void dispatch_onPublishFailure_incrementsAttempts_andParksAtMax() throws Exception {
        // Prepare one pending outbox record
        OutboxEvent ob = new OutboxEvent();
        ob.setId(UUID.randomUUID());
        ob.setRoutingKey("fi.acc.test");
        ob.setPayloadJson("{\"x\":1}");
        ob.setHeadersJson("{\"h\":\"v\"}");
        ob.setAttempts(0);
        ob.setPublished(false);
        ob.setNextAttemptAt(Instant.now());

        Mockito.when(outboxRepository.findPending()).thenReturn(List.of(ob));
        // Force publish failure by throwing from convertAndSend
        Mockito.doThrow(new RuntimeException("broker down")).when(rabbitTemplate)
                .convertAndSend(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

        // Enable dispatcher and set maxAttempts=1 so it parks after first failure
        setField(dispatcher, "enabled", true);
        setField(dispatcher, "maxAttempts", 1);

        dispatcher.dispatch();

        assertEquals(1, ob.getAttempts());
        assertFalse(ob.isPublished());
        assertNull(ob.getNextAttemptAt(), "should be parked (DLQ) after max attempts");
        assertNotNull(ob.getLastError());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
