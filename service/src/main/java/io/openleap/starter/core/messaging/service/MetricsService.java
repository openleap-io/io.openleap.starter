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

import io.openleap.starter.core.config.OlStarterServiceProperties;
import io.openleap.starter.core.repository.OutboxRepository;
import io.openleap.starter.core.repository.entity.OutboxEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides lightweight operational metrics for the service, including:
 * - Outbox backlog and parked (producer DLQ) sizes from the database
 * - Optional RabbitMQ queue depths (main and DLQ) via passive declare, if queue names are configured
 *
 * Author: Dr. Sören Kemmann
 */
@Service
public class MetricsService {

    private final OutboxRepository outboxRepository;

    private final RabbitTemplate rabbitTemplate;

    private String mainQueueName;

    private String dlqQueueName;

    public MetricsService(OlStarterServiceProperties config, OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        if (config != null) {
            this.mainQueueName = config.getMessaging().getMetrics().getQueues().getMain();
            this.dlqQueueName = config.getMessaging().getMetrics().getQueues().getDlq();
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> m = new HashMap<>();
        // Outbox metrics
        List<OutboxEvent> pendingNow = outboxRepository.findPending();
        List<OutboxEvent> allUnpublished = outboxRepository.findByPublishedFalse();
        long parked = allUnpublished.stream().filter(o -> o.getNextAttemptAt() == null && o.getAttempts() > 0).count();
        m.put("outbox_pending", pendingNow.size());
        m.put("outbox_unpublished", allUnpublished.size());
        m.put("outbox_parked", parked);

        // Broker queue metrics (optional if queue names configured)
        if (mainQueueName != null && !mainQueueName.isBlank()) {
            m.put("broker_main_queue_depth", getQueueDepth(mainQueueName.trim()));
        }
        if (dlqQueueName != null && !dlqQueueName.isBlank()) {
            m.put("broker_dlq_depth", getQueueDepth(dlqQueueName.trim()));
        }
        return m;
    }

    private Integer getQueueDepth(String queue) {
        try {
            return rabbitTemplate.execute(channel -> {
                com.rabbitmq.client.AMQP.Queue.DeclareOk ok = channel.queueDeclarePassive(queue);
                return ok.getMessageCount();
            });
        } catch (Exception e) {
            return null; // queue may not exist or not accessible; omit metric
        }
    }
}
