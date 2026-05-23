package com.loadtest.execution.dto;

import com.loadtest.execution.domain.Execution;
import com.loadtest.execution.domain.ExecutionMode;
import com.loadtest.execution.domain.ExecutionStatus;
import com.loadtest.execution.domain.TestType;

import java.time.Instant;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        String simulationClass,
        TestType testType,
        ExecutionStatus status,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        String resultPath,
        String targetHost,
        ExecutionMode targetMode,
        Instant createdAt
) {
    public static ExecutionResponse from(Execution e) {
        return new ExecutionResponse(
                e.getId(),
                e.getProjectId(),
                e.getUserId(),
                e.getSimulationClass(),
                e.getTestType(),
                e.getStatus(),
                e.getStartedAt(),
                e.getFinishedAt(),
                e.getDurationMs(),
                e.getResultPath(),
                e.getTargetHost(),
                e.getTargetMode(),
                e.getCreatedAt()
        );
    }
}
