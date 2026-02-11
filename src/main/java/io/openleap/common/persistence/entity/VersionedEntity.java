package io.openleap.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class VersionedEntity extends PersistenceEntity {

    @Version
    @Column(name = "version", nullable = false)
    private long version;

}