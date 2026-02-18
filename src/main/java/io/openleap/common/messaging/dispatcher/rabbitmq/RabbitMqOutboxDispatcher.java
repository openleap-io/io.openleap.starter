package io.openleap.common.messaging.dispatcher.rabbitmq;

import io.openleap.common.messaging.dispatcher.DispatchResult;
import io.openleap.common.messaging.dispatcher.OutboxDispatcher;
import io.openleap.common.messaging.entity.OutboxEvent;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitMqOutboxDispatcher implements OutboxDispatcher {

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;
    private final long confirmTimeoutMillis;

    public RabbitMqOutboxDispatcher(RabbitTemplate rabbitTemplate,
                                    JsonMapper jsonMapper,
                                    long confirmTimeoutMillis) {
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
        this.confirmTimeoutMillis = confirmTimeoutMillis;
    }

    @Override
    public DispatchResult dispatch(OutboxEvent event) throws Exception {
        Assert.notNull(event.getId(), "OutboxEvent ID must not be null");
        Object payload = jsonMapper.readValue(event.getPayloadJson(), Object.class);
        Map<String, Object> headers = parseHeaders(event.getHeadersJson());
        CorrelationData cd = new CorrelationData(event.getId().toString());

        rabbitTemplate.convertAndSend(event.getExchangeKey(), event.getRoutingKey(), payload, message -> {
            message.getMessageProperties().setHeaders(headers);
            // Do not force contentType here; let the MessageConverter decide (JSON or Avro)
            return message;
        }, cd);

        CorrelationData.Confirm confirm;
        try {
            confirm = cd.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            confirm = new CorrelationData.Confirm(false, "No confirm (timeout)");
        }
        // TODO (itaseski): Consider handling interrupted and execution exceptions separately,
        //  as they may indicate different issues (e.g., thread interruption vs. execution failure)

        if (confirm.ack()) {
            return DispatchResult.ok();
        } else {
            // Capture the specific reason for the failure
            String cause = confirm.reason();
            return DispatchResult.fail(cause);
        }
    }

    // TODO (itaseski): Null handling seems dubious
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseHeaders(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return jsonMapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
