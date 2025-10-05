package com.recrutech.common.entity;

import com.recrutech.common.util.EntityLifecycleUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base entity class that provides common fields and functionality for all entities.
 * This class includes:
 * - ID field with UUID generation
 * - Creation timestamp
 * - Methods for entity lifecycle management
 * - Equals and hashCode implementations based on ID
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false)
    protected String id;

    @Column(nullable = false)
    protected LocalDateTime createdAt;

    /**
     * Method that ensures the entity has an ID and creation timestamp.
     * This should be called by entity lifecycle methods in subclasses.
     */
    protected void initializeEntity() {
        setId(EntityLifecycleUtil.ensureId(id));
        setCreatedAt(EntityLifecycleUtil.ensureTimestamp(createdAt));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof BaseEntity e) {
            return Objects.equals(id, e.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
