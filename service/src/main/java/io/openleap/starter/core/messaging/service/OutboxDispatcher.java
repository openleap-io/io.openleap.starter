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
package io.openleap.starter.core.messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openleap.starter.core.repository.OutboxRepository;
import io.openleap.starter.core.repository.entity.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Background dispatcher that publishes outbox records to RabbitMQ with retry/backoff.
 * Implements a simple producer-side DLQ by parking records after max attempts.
 * Uses publisher confirms to mark records as SENT only after broker ack.
 */
@Slf4j
@Component
public class OutboxDispatcher {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ol.starter.service.messaging.outbox.dispatcher.maxAttempts:10}")
    private int maxAttempts;

    @Value("${ol.starter.service.messaging.outbox.dispatcher.confirmTimeoutMillis:5000}")
    private long confirmTimeoutMillis;

    @Value("${ol.starter.service.messaging.outbox.dispatcher.enabled:true}")
    private boolean enabled;

    @Value("${ol.starter.service.message.deleteOnAck:false}")
    private boolean deleteOnAck;

    // Serialize dispatcher execution within this JVM to avoid double sending
    private final ReentrantLock dispatchLock = new ReentrantLock();

    @Value("${ol.starter.service.messaging.outbox.dispatcher.fixedDelay:1000}")
    private long fixedDelayMs;

    // Signal for immediate wakeups
    private final Semaphore wakeupSignal = new Semaphore(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    public OutboxDispatcher(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Backup scheduled tick: if fixedDelay is set to 0, the scheduled wakeup is effectively disabled.
     * Otherwise, it only signals the worker to perform a dispatch pass.
     */
    @Scheduled(fixedDelayString = "${ol.service.messaging.outbox.dispatcher.fixedDelay:1000}")
    public void scheduledTick() {
        if (!enabled) return;
        if (fixedDelayMs == 0) {
            // Disabled by configuration
            return;
        }
        wakeup();
    }

    /**
     * Public method to request immediate dispatch from business logic.
     */
    public void wakeup() {
        if (!enabled) return;
        if (wakeupSignal.availablePermits() == 0) {
            wakeupSignal.release();
        }
        ensureWorkerStarted();
    }

    private synchronized void ensureWorkerStarted() {
        if (running.get()) return;
        running.set(true);
        worker = new Thread(this::runLoop, "outbox-dispatcher");
        worker.setDaemon(true);
        worker.start();
    }

    private void runLoop() {
        while (running.get()) {
            try {
                if (fixedDelayMs > 0) {
                    wakeupSignal.tryAcquire(fixedDelayMs, TimeUnit.MILLISECONDS);
                } else {
                    // No periodic wakeups; only react to signals
                    wakeupSignal.acquire();
                }
                // Execute dispatch pass
                dispatch();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                log.error("[Outbox] Worker loop error: {}", t.toString());
            }
        }
    }

    @Transactional
    public void dispatch() {
        if (!enabled) {
            log.debug("[Outbox] Dispatcher disabled via property");
            return;
        }
        // Ensure only one thread executes dispatch at a time (in-process guard)
        if (!dispatchLock.tryLock()) {
            log.debug("[Outbox] Dispatch already running, skipping concurrent invocation");
            return;
        }
        boolean unlockInFinally = true;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    dispatchLock.unlock();
                }
            });
            unlockInFinally = false;
        }
        try {
            List<OutboxEvent> pending = outboxRepository.findPending();
            log.debug("[Outbox] Found pending size={} (enabled={})", pending.size(), enabled);
            for (OutboxEvent ob : pending) {
                // If max attempts exceeded previously and nextAttemptAt is null, consider it parked (DLQ state)
                if (ob.getAttempts() >= maxAttempts && ob.getNextAttemptAt() == null) {
                    // parked - skip
                    continue;
                }
                try {
                    String rk = ob.getRoutingKey();

                    String payload = ob.getPayloadJson();
                    Map<String, Object> headers = parseHeaders(ob.getHeadersJson());
                    CorrelationData cd = new CorrelationData(ob.getId() != null ? ob.getId().toString() : null);
                    rabbitTemplate.convertAndSend(ob.getExchangeKey(), ob.getRoutingKey(), payload, message -> {
                        if (headers != null) {
                            for (Map.Entry<String, Object> e : headers.entrySet()) {
                                message.getMessageProperties().setHeader(e.getKey(), e.getValue());
                            }
                        }
                        // Do not force contentType here; let the MessageConverter decide (JSON or Avro)
                        return message;
                    }, cd);
                    // Wait for publisher confirm (synchronous wait)
                    CorrelationData.Confirm confirm = cd.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
                    if (confirm != null && confirm.isAck()) {
                        if (deleteOnAck) {
                            outboxRepository.delete(ob);
                            log.info("[Outbox] Published and deleted (ack) routingKey={} id={}", rk, ob.getId());
                        } else {
                            ob.setPublished(true);
                            ob.setNextAttemptAt(null);
                            ob.setLastError(null);
                            outboxRepository.save(ob);
                            log.info("[Outbox] Published (ack) routingKey={} id={}", rk, ob.getId());
                        }
                    } else {
                        String cause = confirm != null ? confirm.getReason() : "No confirm (timeout)";
                        handlePublishFailure(ob, cause);
                    }
                } catch (Exception ex) {
                    handlePublishFailure(ob, ex.getMessage());
                }
            }
            // JPA @Transactional will flush updates or deletions
        } finally {
            if (unlockInFinally) {
                dispatchLock.unlock();
            }
        }
    }

    private void handlePublishFailure(OutboxEvent ob, String error) {
        int attempts = ob.getAttempts() + 1;
        ob.setAttempts(attempts);
        String safeError = (error == null || error.isBlank()) ? "PUBLISH_FAILED" : error;
        ob.setLastError(safeError);
        if (attempts >= maxAttempts) {
            ob.setNextAttemptAt(null); // park (DLQ)
            log.error("[Outbox] Parking record after maxAttempts={} id={} error={}", maxAttempts, ob.getId(), safeError);
        } else {
            Duration backoff = Duration.ofSeconds(Math.min(60, (long) Math.pow(2, Math.min(6, attempts))));
            ob.setNextAttemptAt(Instant.now().plus(backoff));
            log.warn("[Outbox] Publish failed (attempts={}): {}", attempts, safeError);
        }
        outboxRepository.save(ob);
    }

    private Map<String, Object> parseHeaders(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            return null;
        }
    }
}
