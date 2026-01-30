package io.openleap.starter.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckTest {

    @Test
    @DisplayName("should run action when condition is true")
    void shouldRunActionWhenTrue() {
        // given
        Runnable action = mock(Runnable.class);

        // when
        Check.executeIf(() -> true, action);

        // then
        verify(action, times(1)).run();
    }

    @Test
    @DisplayName("should NOT run action when condition is false")
    void shouldNotRunActionWhenFalse() {
        // given
        Runnable action = mock(Runnable.class);

        // when
        Check.executeIf(() -> false, action);

        // then
        verifyNoInteractions(action);
    }


    @Test
    @DisplayName("should call consumer with value when value is not null")
    void shouldCallConsumerWhenValuePresent() {
        // given
        Consumer<String> action = mock();
        String value = "hello";

        // when
        Check.acceptIfNotNull(value, action);

        // then
        verify(action, times(1)).accept(value);
    }

    @Test
    @DisplayName("should NOT call consumer when value is null")
    void shouldNotCallConsumerWhenValueNull() {
        // given
        Consumer<Object> action = mock();

        // when
        Check.acceptIfNotNull(null, action);

        // then
        verifyNoInteractions(action);
    }


    @Test
    @DisplayName("should return original value when not null")
    void shouldReturnOriginalWhenNotNull() {
        // given
        String value = "exists";
        String defaultValue = "fallback";

        // when
        String result = Check.getOrDefault(value, defaultValue);

        // then
        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("should return default value when original is null")
    void shouldReturnDefaultWhenNull() {
        // given
        String defaultValue = "fallback";

        // when
        String result = Check.getOrDefault(null, defaultValue);

        // then
        assertThat(result).isEqualTo(defaultValue);
    }

}
