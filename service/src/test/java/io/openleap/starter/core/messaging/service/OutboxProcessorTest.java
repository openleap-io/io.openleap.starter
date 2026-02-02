package io.openleap.starter.core.messaging.service;

import io.openleap.starter.core.ReflectionUtils;
import io.openleap.starter.core.messaging.OutboxTestData;
import io.openleap.starter.core.messaging.dispatcher.DispatchResult;
import io.openleap.starter.core.messaging.dispatcher.OutboxDispatcher;
import io.openleap.starter.core.repository.OutboxRepository;
import io.openleap.starter.core.repository.entity.OlOutboxEvent;
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
        OlOutboxEvent event = OutboxTestData.createEvent();
        when(outboxRepository.findPending()).thenReturn(List.of(event));
        when(outboxDispatcher.dispatch(event)).thenReturn(new DispatchResult(true, null));

        // when
        outboxProcessor.processOutbox();

        // then
        assertThat(event)
                .extracting(OlOutboxEvent::isPublished, OlOutboxEvent::getNextAttemptAt)
                .containsExactly(true, null);

        verify(outboxRepository, times(1)).save(event);
        verify(outboxRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should delete event when dispatch succeeds and deleteOnAck is true")
    void processOutbox_Delete_WhenDeleteOnAckEnabled() throws Exception {
        // given
        ReflectionUtils.setField(outboxProcessor, "deleteOnAck", true);
        OlOutboxEvent event = OutboxTestData.createEvent();
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
        OlOutboxEvent event = OutboxTestData.createEvent();
        event.setAttempts(1);
        when(outboxRepository.findPending()).thenReturn(List.of(event));
        when(outboxDispatcher.dispatch(event)).thenReturn(new DispatchResult(false, "CONNECTION_TIMEOUT"));

        // when
        outboxProcessor.processOutbox();

        // then
        assertThat(event)
                .returns(2, OlOutboxEvent::getAttempts)
                .returns("CONNECTION_TIMEOUT", OlOutboxEvent::getLastError)
                .satisfies(e -> assertThat(e.getNextAttemptAt()).isAfter(Instant.now()));

        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("Should park event (DLQ) when max attempts are reached")
    void processOutbox_Park_WhenMaxAttemptsExceeded() throws Exception {
        // given
        ReflectionUtils.setField(outboxProcessor, "maxAttempts", 3);
        OlOutboxEvent event = OutboxTestData.createEvent();
        // Next failure makes it 3
        event.setAttempts(2);
        when(outboxRepository.findPending()).thenReturn(List.of(event));
        when(outboxDispatcher.dispatch(event)).thenReturn(new DispatchResult(false, "FATAL_ERROR"));

        // when
        outboxProcessor.processOutbox();

        // then
        assertThat(event)
                .returns(3, OlOutboxEvent::getAttempts)
                .returns(null, OlOutboxEvent::getNextAttemptAt)
                .returns("FATAL_ERROR", OlOutboxEvent::getLastError);

        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("Should skip events that are already parked")
    void processOutbox_Skip_WhenEventIsAlreadyParked() throws Exception {
        // given
        OlOutboxEvent parkedEvent = OutboxTestData.createEvent();
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