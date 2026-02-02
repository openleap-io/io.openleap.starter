package io.openleap.common.util;

import java.io.IOException;
import java.io.UncheckedIOException;

public class UncheckedIO {

    private UncheckedIO() {
        // Private constructor to prevent instantiation
    }

    @FunctionalInterface
    public interface Runnable {
        void run() throws IOException;
    }

    @FunctionalInterface
    public interface Supplier<T> {
        T get() throws IOException;
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t) throws IOException;
    }

    public static void run(Runnable runnable) {
        try {
            runnable.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T get(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> void accept(Consumer<T> consumer, T t) {
        try {
            consumer.accept(t);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


}
