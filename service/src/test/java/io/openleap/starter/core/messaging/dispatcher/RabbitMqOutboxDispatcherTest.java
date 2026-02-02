package io.openleap.starter.core.messaging.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openleap.starter.core.messaging.OutboxTestData;
import io.openleap.starter.core.repository.entity.OlOutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMqOutboxDispatcherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RabbitMqOutboxDispatcher dispatcher;

    @BeforeEach
    void setup() {
        long timeout = 1000;
        dispatcher = new RabbitMqOutboxDispatcher(rabbitTemplate, objectMapper, timeout);
    }

    @Test
    @DisplayName("Should return success when RabbitMQ returns ACK")
    void dispatch_Success_WhenAckReceived() throws Exception {
        // given
        OlOutboxEvent event = OutboxTestData.createEvent();
        setupMockConfirm(true, null);

        // when
        DispatchResult result = dispatcher.dispatch(event);

        // then
        assertThat(result.success()).isTrue();

        verify(rabbitTemplate).convertAndSend(
                eq("test-exchange"),
                eq("test-rk"),
                eq("{\"data\":\"test\"}"),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    @Test
    @DisplayName("Should return failure with reason when RabbitMQ returns NACK")
    void dispatch_Failure_WhenNackReceived() throws Exception {
        // given
        OlOutboxEvent event = OutboxTestData.createEvent();
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
        OlOutboxEvent event = OutboxTestData.createEvent();
        setupMockConfirm(false, null);

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

            if (cd != null) {
                CorrelationData.Confirm confirm = (ack || reason != null)
                        ? new CorrelationData.Confirm(ack, reason)
                        : null;
                // manually complete the future
                cd.getFuture().complete(confirm);
            }
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