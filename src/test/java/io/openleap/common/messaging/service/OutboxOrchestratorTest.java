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
package io.openleap.common.messaging.service;

import io.openleap.common.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxOrchestratorTest {

    @Mock
    private OutboxProcessor outboxProcessor;

    private OutboxOrchestrator orchestrator;

    @BeforeEach
    void setup() {
        orchestrator = new OutboxOrchestrator(outboxProcessor);

        ReflectionUtils.setField(orchestrator, "enabled", true);
        ReflectionUtils.setField(orchestrator, "fixedDelayMs", 1000L);
    }

    @Test
    @DisplayName("Should trigger outbox processing when scheduled tick occurs")
    void scheduledTick_StartsProcessing_WhenEnabled() {
        // when
        orchestrator.scheduledTick();

        // then
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(outboxProcessor, atLeastOnce()).processOutbox();
        });
    }

    @Test
    @DisplayName("Should trigger immediate dispatch when wakeup is requested")
    void wakeup_TriggersImmediateDispatch_WhenCalled() {
        // when
        orchestrator.wakeup();

        // then
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(outboxProcessor, atLeastOnce()).processOutbox();
        });
    }

    @Test
    @DisplayName("Should skip processing when dispatcher is disabled via configuration")
    void execute_DoesNothing_WhenDisabled() {
        // given
        ReflectionUtils.setField(orchestrator, "enabled", false);

        // when
        orchestrator.execute();

        // then
        verify(outboxProcessor, never()).processOutbox();
    }

    @Test
    @DisplayName("Should prevent concurrent local execution using ReentrantLock")
    void execute_SkipsInvocation_WhenAlreadyRunning() throws Exception {
        // given: Simulate a long-running process to hold the lock
        doAnswer(invocation -> {
            Thread.sleep(500);
            return null;
        }).when(outboxProcessor).processOutbox();

        // when: Run the first call in a separate thread to hold the ReentrantLock
        Thread backgroundThread = new Thread(() -> orchestrator.execute());
        backgroundThread.start();

        // Small sleep to ensure backgroundThread has acquired the lock
        Thread.sleep(100);

        // Attempt second execution while first is still running
        orchestrator.execute();

        backgroundThread.join();

        // then: processOutbox should only have been called once due to the lock
        verify(outboxProcessor, times(1)).processOutbox();
    }

}