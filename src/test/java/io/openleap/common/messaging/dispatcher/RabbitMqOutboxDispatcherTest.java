package io.openleap.common.messaging.dispatcher;

import io.openleap.common.messaging.OutboxTestData;
import io.openleap.common.messaging.dispatcher.rabbitmq.RabbitMqOutboxDispatcher;
import io.openleap.common.messaging.entity.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMqOutboxDispatcherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final JsonMapper jsonMapper = new JsonMapper();

    private RabbitMqOutboxDispatcher dispatcher;

    @BeforeEach
    void setup() {
        long timeout = 1000;
        dispatcher = new RabbitMqOutboxDispatcher(rabbitTemplate, jsonMapper, timeout);
    }

    @Test
    @DisplayName("Should return success when RabbitMQ returns ACK")
    void dispatch_Success_WhenAckReceived() throws Exception {
        // given
        OutboxEvent event = OutboxTestData.createEvent();
        setupMockConfirm(true, null);

        // when
        DispatchResult result = dispatcher.dispatch(event);

        // then
        assertThat(result.success()).isTrue();

        verify(rabbitTemplate).convertAndSend(
                eq("test-exchange"),
                eq("test-rk"),
                eq(Map.of("data", "test")),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    @Test
    @DisplayName("Should return failure with reason when RabbitMQ returns NACK")
    void dispatch_Failure_WhenNackReceived() throws Exception {
        // given
        OutboxEvent event = OutboxTestData.createEvent();
        setupMockConfirm(false, "Queue capacity reached");

        // when
        DispatchResult result = dispatcher.dispatch(event);

        // then
        assertThat(result)
                .returns(false, DispatchResult::success)
                .returns("Queue capacity reached", DispatchResult::reason);
    }

    @Test
    @DisplayName("Should return failure when confirm times out (confirm is null)")
    void dispatch_Failure_WhenTimeoutOccurs() throws Exception {
        // given
        OutboxEvent event = OutboxTestData.createEvent();
        setupMockTimeoutException();

        // when
        DispatchResult result = dispatcher.dispatch(event);

        // then
        assertThat(result)
                .returns(false, DispatchResult::success)
                .returns("No confirm (timeout)", DispatchResult::reason);
    }

    private void setupMockConfirm(boolean ack, String reason) {
        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(4);

            CorrelationData.Confirm confirm =
                    new CorrelationData.Confirm(ack, reason);

            // manually complete the future
            cd.getFuture().complete(confirm);

            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(Object.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    private void setupMockTimeoutException() {
        doAnswer(invocation -> {

            // won't complete future to throw TimeoutException

            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(Object.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

}