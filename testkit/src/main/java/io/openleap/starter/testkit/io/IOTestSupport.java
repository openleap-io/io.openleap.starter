package io.openleap.starter.testkit.io;

import io.openleap.starter.core.util.UncheckedIO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

public class IOTestSupport {

    private IOTestSupport() {

    }

    public static Path createTempDir(String prefix) {
        return UncheckedIO.get(() -> Files.createTempDirectory(prefix));
    }

    public static void recursiveDelete(Path path) {
        try (Stream<Path> walk = UncheckedIO.get(() -> Files.walk(path))) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> UncheckedIO.run(() -> Files.deleteIfExists(p)));
        }
    }

    public static byte[] readClasspathResource(String path) throws IOException {
        try (InputStream is = IOTestSupport.class.getResourceAsStream(path)) {
            Objects.requireNonNull(is, "Resource not found: " + path);
            return is.readAllBytes();
        }
    }

}
