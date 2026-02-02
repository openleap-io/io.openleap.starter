package io.openleap.common.messaging;

import io.openleap.common.persistence.entity.OutboxEvent;
import io.openleap.common.persistence.entity.OutboxEventId;

public class OutboxTestData {

    private OutboxTestData() {
        // Prevent instantiation
    }

    public static OutboxEvent createEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setBusinessId(OutboxEventId.create());
        event.setExchangeKey("test-exchange");
        event.setRoutingKey("test-rk");
        event.setPayloadJson("{\"data\":\"test\"}");
        event.setHeadersJson("{\"x-trace-id\":\"123\"}");
        return event;
    }
}
