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

import io.openleap.common.messaging.repository.OutboxRepository;
import io.openleap.common.messaging.entity.OutboxEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Administrative helper for Outbox DLQ handling: list parked (failed) records and trigger replay.
 */
@Service
public class OutboxAdminService {

    private final OutboxRepository outboxRepository;

    @Value("${ol.starter.idempotency.messaging.outbox.dispatcher.maxAttempts:10}")
    private int maxAttempts;

    public OutboxAdminService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> listFailed(int limit) {
        List<OutboxEvent> all = outboxRepository.findByPublishedFalse();
        return all.stream()
                .filter(o -> o.getAttempts() >= maxAttempts && o.getNextAttemptAt() == null)
                .sorted(Comparator.comparing(OutboxEvent::getCreatedAt))
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .toList();
    }

    @Transactional
    public int replayFailed(int limit) {
        int count = 0;
        List<OutboxEvent> all = outboxRepository.findByPublishedFalse();
        for (OutboxEvent o : all) {
            if (o.getAttempts() >= maxAttempts && o.getNextAttemptAt() == null) {
                if (limit > 0 && count >= limit) break;
                // Reset attempts and schedule immediate retry
                o.setAttempts(0);
                o.setNextAttemptAt(Instant.now());
                o.setLastError(null);
                count++;
            }
        }
        return count;
    }
}
