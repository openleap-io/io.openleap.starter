package io.openleap.starter.core.util;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class Check {

    private Check() {
        // Private constructor to prevent instantiation
    }

    public static void executeIf(BooleanSupplier condition, Runnable action) {
        if (condition.getAsBoolean()) {
            action.run();
        }
    }

    public static <T> void acceptIfNotNull(T value, Consumer<T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

}
