package io.openleap.core.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = "customFields")
public abstract class ExtensibleVersionedEntity extends VersionedEntity implements Extensible {

    @Column(name = "custom_fields", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> customFields = new HashMap<>();
}
