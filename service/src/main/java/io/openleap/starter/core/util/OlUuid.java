package io.openleap.starter.core.util;

import java.util.UUID;
import java.util.Base64;
import java.nio.ByteBuffer;

/**
 * Utility class for UUID handling with compact base64 representation.
 */
public class OlUuid {
    
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
     * @return the reconstructed UUID, or null if the input is invalid
     */
    public static UUID fromShortBase64(String shortUuid) {
        if (shortUuid == null || shortUuid.isEmpty()) {
            return null;
        }
        
        try {
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
            
        } catch (IllegalArgumentException e) {
            // Invalid base64 string
            return null;
        }
    }
    
    /**
     * Example usage demonstrating the OlUuid class.
     */
    public static void main(String[] args) {
        // Create a new UUID
        UUID uuid = OlUuid.create();
        System.out.println("Original UUID: " + uuid);
        System.out.println("UUID length:   " + uuid.toString().length() + " characters");
        
        // Convert to short base64
        String shortUuid = OlUuid.toShortBase64(uuid);
        System.out.println("\nShort base64:  " + shortUuid);
        System.out.println("Short length:  " + shortUuid.length() + " characters");
        
        // Convert back from short base64
        UUID reconstructed = OlUuid.fromShortBase64(shortUuid);
        System.out.println("\nReconstructed: " + reconstructed);
        System.out.println("Match:         " + uuid.equals(reconstructed));
        
        // Test with invalid input
        UUID invalid = OlUuid.fromShortBase64("invalid-base64");
        System.out.println("\nInvalid input: " + invalid);
    }
}
