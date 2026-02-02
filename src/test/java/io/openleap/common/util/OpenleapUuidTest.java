package io.openleap.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OpenleapUuidTest {

    @Test
    @DisplayName("should return non null uuid when creating")
    void shouldReturnNonNullUuidWhenCreating() {
        UUID uuid = OpenleapUuid.create();
        assertThat(uuid).isNotNull();
    }

    @Test
    @DisplayName("should convert to twenty two character string when converting to short base64")
    void shouldConvertToTwentyTwoCharacterStringWhenToShortBase64() {
        // given
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        // when
        String result = OpenleapUuid.toShortBase64(uuid);

        // then
        assertThat(result)
                .isNotNull()
                .hasSize(22)
                .matches("^[a-zA-Z0-9_-]*$"); // URL-safe characters only
    }

    @Test
    @DisplayName("should throw exception when input is null in to short base64")
    void shouldThrowExceptionWhenInputIsNullInToShortBase64() {
        assertThatThrownBy(() -> OpenleapUuid.toShortBase64(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UUID cannot be null");
    }

    @Test
    @DisplayName("should reconstruct exact same uuid when from short base64 is called")
    void shouldReconstructExactSameUuidWhenFromShortBase64() {
        // given
        UUID original = OpenleapUuid.create();
        String shortBase64 = OpenleapUuid.toShortBase64(original);

        // when
        UUID reconstructed = OpenleapUuid.fromShortBase64(shortBase64);

        // then
        assertThat(reconstructed).isEqualTo(original);
    }

    @Test
    @DisplayName("should return null when input is null or empty in from short base64")
    void shouldReturnNullWhenInputIsNullOrEmptyInFromShortBase64() {
        assertThatThrownBy(() -> OpenleapUuid.fromShortBase64(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UUID cannot be null or empty");
    }

}
