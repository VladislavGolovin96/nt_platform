package com.loadtest.report.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only view of execution-service's executions table.
 * Report-service shares the same PostgreSQL DB but does not own this table.
 */
@Entity
@Immutable
@Table(name = "executions")
public class ExecutionRecord {

    @Id
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "simulation_class")
    private String simulationClass;

    @Column(name = "test_type")
    private String testType;

    @Column(name = "status")
    private String status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "target_host")
    private String targetHost;

    @Column(name = "target_mode")
    private String targetMode;

    @Column(name = "created_at")
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getUserId() { return userId; }
    public String getSimulationClass() { return simulationClass; }
    public String getTestType() { return testType; }
    public String getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public String getTargetHost() { return targetHost; }
    public String getTargetMode() { return targetMode; }
    public Instant getCreatedAt() { return createdAt; }
}
