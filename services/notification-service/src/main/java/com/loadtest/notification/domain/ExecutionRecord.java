package com.loadtest.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Read-only view of execution-service's executions table (shared PostgreSQL DB).
 */
@Entity
@Immutable
@Table(name = "executions")
public class ExecutionRecord {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
}
