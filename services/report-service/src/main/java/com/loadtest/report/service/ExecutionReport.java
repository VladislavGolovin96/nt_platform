package com.loadtest.report.service;

import com.loadtest.report.domain.ExecutionRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExecutionReport(
        UUID executionId,
        UUID projectId,
        UUID userId,
        String simulationClass,
        String testType,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        String targetHost,
        String targetMode,
        GatlingStats gatlingStats,
        List<byte[]> grafanaPanels
) {
    public static ExecutionReport from(ExecutionRecord exec,
                                       GatlingStats stats,
                                       List<byte[]> grafanaPanels) {
        return new ExecutionReport(
                exec.getId(),
                exec.getProjectId(),
                exec.getUserId(),
                exec.getSimulationClass(),
                exec.getTestType(),
                exec.getStartedAt(),
                exec.getFinishedAt(),
                exec.getDurationMs() != null ? exec.getDurationMs() : 0L,
                exec.getTargetHost(),
                exec.getTargetMode(),
                stats,
                grafanaPanels
        );
    }
}
