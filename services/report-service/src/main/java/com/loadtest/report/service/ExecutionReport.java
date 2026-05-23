package com.loadtest.report.service;

import com.loadtest.report.client.MetricPoint;
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
        List<MetricPoint> p50Latency,
        List<MetricPoint> p95Latency,
        List<MetricPoint> p99Latency,
        List<MetricPoint> throughput,
        List<MetricPoint> errorRate,
        List<byte[]> grafanaPanels
) {
    public static ExecutionReport from(ExecutionRecord exec,
                                       List<MetricPoint> p50,
                                       List<MetricPoint> p95,
                                       List<MetricPoint> p99,
                                       List<MetricPoint> throughput,
                                       List<MetricPoint> errorRate,
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
                p50, p95, p99, throughput, errorRate, grafanaPanels
        );
    }
}
