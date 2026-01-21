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

import io.openleap.starter.core.lock.aspect.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
public class OutboxOrchestrator {

    private final OutboxProcessor outboxProcessor;

    @Value("${ol.starter.service.messaging.outbox.dispatcher.enabled:true}")
    private boolean enabled;

    @Value("${ol.starter.service.messaging.outbox.dispatcher.fixedDelay:1000}")
    private long fixedDelayMs;

    // Serialize dispatcher execution within this JVM to avoid double sending
    private final ReentrantLock dispatchLock = new ReentrantLock();

    // Signal for immediate wakeups
    private final Semaphore wakeupSignal = new Semaphore(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    public OutboxOrchestrator(OutboxProcessor outboxProcessor) {
        this.outboxProcessor = outboxProcessor;
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

    // TODO (itaseski): Maybe use scheduled executor with fixed delay instead of running a loop?
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
                execute();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                log.error("[Outbox] Worker loop error: {}", t.toString());
            }
        }
    }

    // TODO (itaseski): Consider adding backoff to the distributed lock acquisition?
    @DistributedLock(key = "outbox-dispatcher")
    public void execute() {
        if (!enabled) {
            log.debug("[Outbox] Dispatcher disabled via property");
            return;
        }

        // Layer 1: In-process guard (ReentrantLock) to prevent local thread contention
        // and avoid unnecessary database/network round-trips for the distributed lock.
        if (!dispatchLock.tryLock()) {
            log.debug("[Outbox] Dispatch already running, skipping concurrent invocation");
            return;
        }
        try {
            outboxProcessor.processOutbox();
        } finally {
            // TODO (itaseski): Check if transaction synchronization is needed for unlocking
            dispatchLock.unlock();
        }
    }
}
