package io.openleap.starter.core.messaging;

import io.openleap.starter.core.repository.entity.OutboxEvent;

import java.util.UUID;

public class OutboxTestData {

    private OutboxTestData() {
        // Prevent instantiation
    }

    public static OutboxEvent createEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setExchangeKey("test-exchange");
        event.setRoutingKey("test-rk");
        event.setPayloadJson("{\"data\":\"test\"}");
        event.setHeadersJson("{\"x-trace-id\":\"123\"}");
        return event;
    }
}
