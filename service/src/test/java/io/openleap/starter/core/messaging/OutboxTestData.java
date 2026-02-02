package io.openleap.starter.core.messaging;

import io.openleap.starter.core.repository.entity.OlOutboxEvent;
import io.openleap.starter.core.repository.entity.OlOutboxEventId;

import java.util.UUID;

public class OutboxTestData {

    private OutboxTestData() {
        // Prevent instantiation
    }

    public static OlOutboxEvent createEvent() {
        OlOutboxEvent event = new OlOutboxEvent();
        event.setBusinessId(OlOutboxEventId.create());
        event.setExchangeKey("test-exchange");
        event.setRoutingKey("test-rk");
        event.setPayloadJson("{\"data\":\"test\"}");
        event.setHeadersJson("{\"x-trace-id\":\"123\"}");
        return event;
    }
}
