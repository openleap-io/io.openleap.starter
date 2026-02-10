package io.openleap.common.util;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility class for UUID handling with compact base64 representation.
 */
public class UuidUtils {

    private UuidUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a new random UUID.
     *
     * @return a randomly generated UUID
     */
    public static UUID create() {
        return UUID.randomUUID();
    }

    /**
     * Converts a UUID to a URL-safe short base64 string.
     * The UUID is converted to its 16-byte representation and encoded as base64.
     *
     * @param uuid the UUID to convert
     * @return a URL-safe base64 encoded string (22 characters)
     */
    public static String toShortBase64(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }

        // Convert UUID to 16 bytes
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        // Encode to URL-safe base64 (no padding)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    /**
     * Reconstructs a UUID from a short base64 string.
     *
     * @param shortUuid the base64 encoded UUID string
     * @return the reconstructed UUID
     */
    public static UUID fromShortBase64(String shortUuid) {
        if (shortUuid == null || shortUuid.isEmpty()) {
            throw new IllegalArgumentException("UUID cannot be null or empty");
        }

        // Decode from URL-safe base64
        byte[] bytes = Base64.getUrlDecoder().decode(shortUuid);

        // Validate length (UUID is 16 bytes)
        if (bytes.length != 16) {
            return null;
        }

        // Convert bytes back to UUID
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long mostSignificantBits = buffer.getLong();
        long leastSignificantBits = buffer.getLong();

        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}