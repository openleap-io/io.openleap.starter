package io.openleap.core.persistence.entity;

/**
 * Aspect interface for entities with optimistic concurrency control.
 */
public interface Versionable {

    long getVersion();
}
