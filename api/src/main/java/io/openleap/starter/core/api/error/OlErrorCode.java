package io.openleap.starter.core.api.error;

import org.springframework.http.HttpStatus;

public interface OlErrorCode {

    HttpStatus status();

    String message();

    static <E extends Enum<E> & OlErrorCode> E from(Class<E> enumClass, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, name.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
