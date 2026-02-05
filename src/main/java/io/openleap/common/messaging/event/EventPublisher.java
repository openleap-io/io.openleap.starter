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

import io.openleap.common.messaging.MessageCoverageTracker;
import io.openleap.common.messaging.RoutingKey;
import io.openleap.common.messaging.config.OpenleapMessagingProperties;
import io.openleap.common.messaging.entity.OutboxEvent;
import io.openleap.common.messaging.entity.OutboxEventId;
import io.openleap.common.messaging.repository.OutboxRepository;
import io.openleap.common.messaging.service.OutboxOrchestrator;
import io.openleap.common.util.OpenleapUuid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;

/**
 * Transactional messaging publisher that writes to the Outbox table.
 * A separate dispatcher will forward records to RabbitMQ.
 */
@Service
public class EventPublisher {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;
    private final OutboxOrchestrator outboxOrchestrator;

    // Optional coverage tracker: when enabled, records messages actually enqueued (runtime truth)
    @Autowired(required = false)
    private MessageCoverageTracker coverageTracker;

    private boolean coverageEnabled;

    private boolean wakeupAfterCommit;

    private String eventsExchange;

    @Autowired
    public EventPublisher(OpenleapMessagingProperties config, OutboxRepository outboxRepository, JsonMapper jsonMapper, OutboxOrchestrator outboxOrchestrator) {
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
        this.outboxOrchestrator = outboxOrchestrator;
        if (config != null && config.getOutbox() != null) {
            this.coverageEnabled = config.isCoverage();
            this.wakeupAfterCommit = config.getOutbox().getDispatcher().isWakeupAfterCommit();
            this.eventsExchange = config.getEventsExchange();
        }
    }

    // Backwards-compatible constructor for tests or contexts that don't need immediate dispatch
    public EventPublisher(OpenleapMessagingProperties config, OutboxRepository outboxRepository, JsonMapper jsonMapper) {
        this(config, outboxRepository, jsonMapper, null);
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
            hdrs.putIfAbsent("eventId", OpenleapUuid.create().toString());

            OutboxEvent e = new OutboxEvent();
            e.setBusinessId(OutboxEventId.create());
            e.setExchangeKey(exchangeKey);
            e.setRoutingKey(routingKey.key());
            e.setOccurredAt(Instant.now());
            e.setPublished(false);
            e.setAttempts(0);
            e.setNextAttemptAt(null);
            e.setPayloadJson(jsonMapper.writeValueAsString(payload));
            e.setHeadersJson(hdrs.isEmpty() ? null : jsonMapper.writeValueAsString(hdrs));
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
