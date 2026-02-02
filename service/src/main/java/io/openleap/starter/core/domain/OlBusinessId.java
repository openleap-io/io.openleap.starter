package io.openleap.starter.core.domain;

import java.util.UUID;

/**
 * Base interface for typed business identifiers.
 * This provides a common contract for ID value objects and enables generic handling.
 */
public interface OlBusinessId {

    UUID value();
}
