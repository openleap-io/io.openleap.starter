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
package io.openleap.starter.core.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageCoverageTracker {

    private final Set<String> expectedMessages = ConcurrentHashMap.newKeySet();
    private final Set<String> sentMessages = ConcurrentHashMap.newKeySet();

    public void registerExpectedMessage(String exchange, String routingKey) {
        expectedMessages.add(toKey(exchange, routingKey));
    }

    public void recordSentMessage(String exchange, String routingKey) {
        String key = toKey(exchange, routingKey);
        sentMessages.add(key);
    }

    private String toKey(String exchange, String routingKey) {
        return exchange + ":" + routingKey;
    }

    public MessageCoverageReport getReport() {
        Set<String> covered = new HashSet<>(sentMessages);
        covered.retainAll(expectedMessages);

        Set<String> uncovered = new HashSet<>(expectedMessages);
        uncovered.removeAll(sentMessages);

        double coverage = expectedMessages.isEmpty() ? 100.0 :
                (covered.size() * 100.0) / expectedMessages.size();

        return new MessageCoverageReport(
                expectedMessages.size(),
                covered.size(),
                uncovered,
                coverage
        );
    }

    public void reset() {
        sentMessages.clear();
    }
}

