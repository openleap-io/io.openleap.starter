package io.openleap.core.messaging;


import io.openleap.core.messaging.entity.OutboxEvent;
import io.openleap.core.messaging.entity.OutboxEventId;

public class OutboxTestData {

    private OutboxTestData() {
        // Prevent instantiation
    }

    public static OutboxEvent createEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setBusinessId(OutboxEventId.create());
        event.setExchangeKey("test-exchange");
        event.setRoutingKey("test-rk");
        event.setPayloadJson("{\"data\":\"test\"}");
        event.setHeadersJson("{\"x-trace-id\":\"123\"}");
        return event;
    }
}
