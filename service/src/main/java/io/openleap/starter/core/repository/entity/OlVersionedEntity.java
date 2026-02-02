package io.openleap.starter.core.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class OlVersionedEntity extends OlPersistenceEntity {

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
