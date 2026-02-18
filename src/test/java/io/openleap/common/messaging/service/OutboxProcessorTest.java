package io.openleap.common.messaging.service;

import io.openleap.common.ReflectionUtils;
import io.openleap.common.messaging.OutboxTestData;
import io.openleap.common.messaging.dispatcher.DispatchResult;
import io.openleap.common.messaging.dispatcher.OutboxDispatcher;
import io.openleap.common.messaging.repository.OutboxRepository;
import io.openleap.common.messaging.entity.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxDispatcher outboxDispatcher;

    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void setUp() {
        outboxProcessor = new OutboxProcessor(outboxRepository, outboxDispatcher);

        ReflectionUtils.setField(outboxProcessor, "maxAttempts", 3);
        ReflectionUtils.setField(outboxProcessor, "deleteOnAck", false);
    }

    @Test
    @DisplayName("Should mark event as published and clear next attempt when dispatch succeeds")
    void processOutbox_Success_WhenDispatcherReturnsAck() throws Exception {
        // given
        OutboxEvent event = OutboxTestData.createEvent();
        when(outboxRepository.findPending()).thenReturn(List.of(event));
        when(outboxDispatcher.dispatch(event)).thenReturn(new DispatchResult(true, null));

        // when
        outboxProcessor.processOutbox();

        // then
        assertThat(event)
                .extracting(OutboxEvent::isPublished, OutboxEvent::getNextAttemptAt)
                .containsExactly(true, null);

        verify(outboxRepository, times(1)).save(event);
        verify(outboxRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should delete event when dispatch succeeds and deleteOnAck is true")
    void processOutbox_Delete_WhenDeleteOnAckEnabled() throws Exception {
        // given
        ReflectionUtils.setField(outboxProcessor, "deleteOnAck", true);
        OutboxEvent event = OutboxTestData.createEvent();
        when(outboxRepository.findPending()).thenReturn(List.of(event));
        when(outboxDispatcher.dispatch(event)).thenReturn(new DispatchResult(true, null));

        // when
        outboxProcessor.processOutbox();

        // then
        verify(outboxRepository, times(1)).delete(event);
        verify(outboxRepository, never()).save(event);
    }

    @Test
    @DisplayName("Should schedule retry with backoff when dispatch fails")
    void processOutbox_Retry_WhenDispatchFails() throws Exception {
        // given
        OutboxEvent event = OutboxTestData.createEvent();
        event.setAttempts(1);
        when(outboxRepository.findPending()).thenReturn(List.of(event));
        when(outboxDispatcher.dispatch(event)).thenReturn(new DispatchResult(false, "CONNECTION_TIMEOUT"));

        // when
        outboxProcessor.processOutbox();

        // then
        assertThat(event)
                .returns(2, OutboxEvent::getAttempts)
                .returns("CONNECTION_TIMEOUT", OutboxEvent::getLastError)
                .satisfies(e -> assertThat(e.getNextAttemptAt()).isAfter(Instant.now()));

        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("Should park event (DLQ) when max attempts are reached")
    void processOutbox_Park_WhenMaxAttemptsExceeded() throws Exception {
        // given
        ReflectionUtils.setField(outboxProcessor, "maxAttempts", 3);
        OutboxEvent event = OutboxTestData.createEvent();
        // Next failure makes it 3
        event.setAttempts(2);
        when(outboxRepository.findPending()).thenReturn(List.of(event));
        when(outboxDispatcher.dispatch(event)).thenReturn(new DispatchResult(false, "FATAL_ERROR"));

        // when
        outboxProcessor.processOutbox();

        // then
        assertThat(event)
                .returns(3, OutboxEvent::getAttempts)
                .returns(null, OutboxEvent::getNextAttemptAt)
                .returns("FATAL_ERROR", OutboxEvent::getLastError);

        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("Should skip events that are already parked")
    void processOutbox_Skip_WhenEventIsAlreadyParked() throws Exception {
        // given
        OutboxEvent parkedEvent = OutboxTestData.createEvent();
        parkedEvent.setAttempts(10);
        // Parked state
        parkedEvent.setNextAttemptAt(null);

        when(outboxRepository.findPending()).thenReturn(List.of(parkedEvent));

        // when
        outboxProcessor.processOutbox();

        // then
        verify(outboxDispatcher, never()).dispatch(any());
    }

}