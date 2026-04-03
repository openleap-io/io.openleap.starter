package io.openleap.core.scheduling.messaging;

import io.openleap.core.messaging.RoutingKey;
import io.openleap.core.messaging.event.BaseDomainEvent;
import io.openleap.core.messaging.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskEventPublisherTest {

    private static final String APP_NAME = "audit-service";
    private static final String TASK_ID = "task-1";
    private static final String HANDLER_NAME = "audit-log";

    @Mock
    private EventPublisher eventPublisher;

    private TaskEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TaskEventPublisher(eventPublisher, APP_NAME);
    }

    @Test
    void onSubmitted_enqueuesToSubmittedRoutingKey() {
        publisher.onSubmitted(TASK_ID, HANDLER_NAME);

        BaseDomainEvent event = captureEvent();
        assertThat(captureRoutingKey().key()).isEqualTo(APP_NAME + TaskEvents.ROUTING_SUBMITTED);
        assertThat(event.getAggregateId()).isEqualTo(TASK_ID);
        assertThat(event.getAggregateType()).isEqualTo(TaskEvents.AGGREGATE_TYPE);
        assertThat(event.getChangeType()).isEqualTo(TaskEvents.SUBMITTED);
        assertThat(event.getMetadata()).containsEntry(TaskEvents.META_HANDLER, HANDLER_NAME);
    }

    @Test
    void onCompleted_enqueuesToCompletedRoutingKey() {
        publisher.onCompleted(TASK_ID, HANDLER_NAME);

        BaseDomainEvent event = captureEvent();
        assertThat(captureRoutingKey().key()).isEqualTo(APP_NAME + TaskEvents.ROUTING_COMPLETED);
        assertThat(event.getAggregateId()).isEqualTo(TASK_ID);
        assertThat(event.getChangeType()).isEqualTo(TaskEvents.COMPLETED);
        assertThat(event.getMetadata()).containsEntry(TaskEvents.META_HANDLER, HANDLER_NAME);
    }

    @Test
    void onFailed_enqueuesToFailedRoutingKey_withHandlerAndCauseInMetadata() {
        RuntimeException error = new RuntimeException("connection timeout");
        publisher.onFailed(TASK_ID, HANDLER_NAME, error);

        BaseDomainEvent event = captureEvent();
        assertThat(captureRoutingKey().key()).isEqualTo(APP_NAME + TaskEvents.ROUTING_FAILED);
        assertThat(event.getAggregateId()).isEqualTo(TASK_ID);
        assertThat(event.getChangeType()).isEqualTo(TaskEvents.FAILED);
        assertThat(event.getMetadata()).containsEntry(TaskEvents.META_HANDLER, HANDLER_NAME).containsEntry(TaskEvents.META_CAUSE, "connection timeout");
    }

    @Test
    void onCancelled_enqueuesToCancelledRoutingKey() {
        publisher.onCancelled(TASK_ID);

        BaseDomainEvent event = captureEvent();
        assertThat(captureRoutingKey().key()).isEqualTo(APP_NAME + TaskEvents.ROUTING_CANCELLED);
        assertThat(event.getAggregateId()).isEqualTo(TASK_ID);
        assertThat(event.getChangeType()).isEqualTo(TaskEvents.CANCELLED);
    }

    private BaseDomainEvent captureEvent() {
        ArgumentCaptor<BaseDomainEvent> captor = ArgumentCaptor.forClass(BaseDomainEvent.class);
        verify(eventPublisher).enqueue(any(RoutingKey.class), captor.capture(), eq(Map.of()));
        return captor.getValue();
    }

    private RoutingKey captureRoutingKey() {
        ArgumentCaptor<RoutingKey> captor = ArgumentCaptor.forClass(RoutingKey.class);
        verify(eventPublisher).enqueue(captor.capture(), any(BaseDomainEvent.class), eq(Map.of()));
        return captor.getValue();
    }
}
