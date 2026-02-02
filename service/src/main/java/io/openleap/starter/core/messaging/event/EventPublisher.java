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
import io.openleap.starter.core.config.OlStarterServiceProperties;
import io.openleap.starter.core.messaging.MessageCoverageTracker;
import io.openleap.starter.core.messaging.RoutingKey;
import io.openleap.starter.core.repository.OutboxRepository;
import io.openleap.starter.core.repository.entity.OutboxEvent;
import io.openleap.starter.core.messaging.service.OutboxOrchestrator;
import io.openleap.starter.core.util.OlUuid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;

/**
 * Transactional messaging publisher that writes to the Outbox table.
 * A separate dispatcher will forward records to RabbitMQ.
 */
@Service
public class EventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final OutboxOrchestrator outboxOrchestrator;

    // Optional coverage tracker: when enabled, records messages actually enqueued (runtime truth)
    @Autowired(required = false)
    private MessageCoverageTracker coverageTracker;

    private boolean coverageEnabled;

    private boolean wakeupAfterCommit;

    private String eventsExchange;

    @Autowired
    public EventPublisher(OlStarterServiceProperties config, OutboxRepository outboxRepository, ObjectMapper objectMapper, OutboxOrchestrator outboxOrchestrator) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.outboxOrchestrator = outboxOrchestrator;
        if (config != null && config.getMessaging() != null && config.getMessaging().getOutbox() != null) {
            this.coverageEnabled = config.getMessaging().isCoverage();
            this.wakeupAfterCommit = config.getMessaging().getOutbox().getDispatcher().isWakeupAfterCommit();
            this.eventsExchange = config.getMessaging().getEventsExchange();
        }
    }

    // Backwards-compatible constructor for tests or contexts that don't need immediate dispatch
    public EventPublisher(OlStarterServiceProperties config, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this(config, outboxRepository, objectMapper, null);
    }

    @Transactional
    public void enqueue(RoutingKey routingKey, EventPayload payload, Map<String, String> headers) {
        this.enqueueInternal(eventsExchange, routingKey, payload, headers);
    }

    @Transactional
    public void enqueue(String exchangeKey, RoutingKey routingKey, EventPayload payload, Map<String, String> headers) {
        this.enqueueInternal(exchangeKey, routingKey, payload, headers);
    }

    public void enqueueInternal(String exchangeKey, RoutingKey routingKey, EventPayload payload, Map<String, String> headers) {
        try {
            // Enrich headers with traceId and eventId if missing
            Map<String, String> hdrs = headers == null ? new java.util.HashMap<>() : new java.util.HashMap<>(headers);
            String traceId = org.slf4j.MDC.get("traceId");
            if (traceId != null && !traceId.isBlank()) {
                hdrs.putIfAbsent("traceId", traceId);
            }
            hdrs.putIfAbsent("eventId", OlUuid.create().toString());

            OutboxEvent e = new OutboxEvent();
            e.setId(OlUuid.create());
            e.setExchangeKey(exchangeKey);
            e.setRoutingKey(routingKey.key());
            e.setOccurredAt(Instant.now());
            e.setPublished(false);
            e.setAttempts(0);
            e.setNextAttemptAt(null);
            e.setPayloadJson(objectMapper.writeValueAsString(payload));
            e.setHeadersJson(hdrs.isEmpty() ? null : objectMapper.writeValueAsString(hdrs));
            outboxRepository.save(e);

            // Record coverage with the actual configured exchange + routing key (if enabled)
            if (coverageEnabled && coverageTracker != null) {
                coverageTracker.recordSentMessage(exchangeKey, routingKey.key());
            }

            // Immediately trigger dispatch after the transaction commits (if enabled and dispatcher available)
            if (wakeupAfterCommit && outboxOrchestrator != null) {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            outboxOrchestrator.wakeup();
                        }
                    });
                } else {
                    // Fallback: no active transaction (shouldn't happen due to @Transactional)
                    outboxOrchestrator.execute();
                }
            }
        } catch (Exception ex) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "EVENT_ENQUEUE_FAILED");
        }
    }
}
