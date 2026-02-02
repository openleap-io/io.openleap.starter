package io.openleap.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UncheckedIOTest {

    @Test
    @DisplayName("should call run() exactly once")
    void shouldInvokeRunnable() throws IOException {
        // given
        UncheckedIO.Runnable runnableMock = mock(UncheckedIO.Runnable.class);

        // when
        UncheckedIO.run(runnableMock);

        // then
        verify(runnableMock, times(1)).run();
    }

    @Test
    @DisplayName("should catch IOException from run() and wrap it")
    void shouldHandleRunnableException() throws IOException {
        // given
        UncheckedIO.Runnable runnableMock = mock(UncheckedIO.Runnable.class);
        IOException cause = new IOException("Disk Full");

        // when
        doThrow(cause).when(runnableMock).run();

        // then
        assertThatThrownBy(() -> UncheckedIO.run(runnableMock))
                .isInstanceOf(UncheckedIOException.class)
                .hasCause(cause);
    }

    @Test
    @DisplayName("should return value from get()")
    void shouldInvokeSupplier() throws IOException {
        // given
        UncheckedIO.Supplier<String> supplierMock = mock();
        when(supplierMock.get()).thenReturn("Hello World");

        // when
        String result = UncheckedIO.get(supplierMock);

        // then
        assertThat(result).isEqualTo("Hello World");
        verify(supplierMock).get();
    }

    @Test
    @DisplayName("should catch IOException from get() and wrap it")
    void shouldHandleSupplierException() throws IOException {
        // given
        UncheckedIO.Supplier<Integer> supplierMock = mock();
        IOException cause = new IOException("Network Timeout");

        // when
        when(supplierMock.get()).thenThrow(cause);

        // then
        assertThatThrownBy(() -> UncheckedIO.get(supplierMock))
                .isInstanceOf(UncheckedIOException.class)
                .hasCause(cause);
    }

    @Test
    @DisplayName("should call accept() with provided argument")
    void shouldInvokeConsumer() throws IOException {
        // given
        UncheckedIO.Consumer<String> consumerMock = mock();

        // when
        UncheckedIO.accept(consumerMock, "Test Input");

        // then
        verify(consumerMock, times(1)).accept("Test Input");
    }

    @Test
    @DisplayName("should catch IOException from accept() and wrap it")
    void shouldHandleConsumerException() throws IOException {
        // given
        UncheckedIO.Consumer<String> consumerMock = mock();
        IOException cause = new IOException("File Not Found");

        // when
        doThrow(cause).when(consumerMock).accept("system");

        // then
        assertThatThrownBy(() -> UncheckedIO.accept(consumerMock, "system"))
                .isInstanceOf(UncheckedIOException.class)
                .hasCause(cause);
    }

}
