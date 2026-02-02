package io.openleap.common.messaging.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openleap.common.persistence.entity.OutboxEvent;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RabbitMqOutboxDispatcher implements OutboxDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final long confirmTimeoutMillis;

    public RabbitMqOutboxDispatcher(RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper,
                                    long confirmTimeoutMillis) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.confirmTimeoutMillis = confirmTimeoutMillis;
    }

    @Override
    public DispatchResult dispatch(OutboxEvent event) throws Exception {
        String payload = event.getPayloadJson();
        Map<String, Object> headers = parseHeaders(event.getHeadersJson());
        CorrelationData cd = new CorrelationData(event.getId() != null ? event.getId().toString() : null);

        rabbitTemplate.convertAndSend(event.getExchangeKey(), event.getRoutingKey(), payload, message -> {
            if (headers != null) {
                for (Map.Entry<String, Object> e : headers.entrySet()) {
                    message.getMessageProperties().setHeader(e.getKey(), e.getValue());
                }
            }
            // Do not force contentType here; let the MessageConverter decide (JSON or Avro)
            return message;
        }, cd);

        // TODO (itaseski): Handle interrupted exception?
        // Wait for publisher confirm (synchronous wait)
        CorrelationData.Confirm confirm = cd.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);

        if (confirm != null && confirm.isAck()) {
            return DispatchResult.ok();
        } else {
            // Capture the specific reason for the failure
            String cause = (confirm != null) ? confirm.getReason() : "No confirm (timeout)";
            return DispatchResult.fail(cause);
        }
    }

    // TODO (itaseski): Null handling seems dubious
    private Map<String, Object> parseHeaders(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            return null;
        }
    }
}
