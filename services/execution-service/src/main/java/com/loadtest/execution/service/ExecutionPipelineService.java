package com.loadtest.execution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadtest.common.kafka.event.TestFailedEvent;
import com.loadtest.common.kafka.event.TestFinishedEvent;
import com.loadtest.common.kafka.event.TestStartedEvent;
import com.loadtest.execution.domain.Execution;
import com.loadtest.execution.domain.ExecutionStatus;
import com.loadtest.execution.kafka.producer.ExecutionKafkaProducer;
import com.loadtest.execution.repository.ExecutionRepository;
import com.loadtest.execution.runner.GatlingRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ExecutionPipelineService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionPipelineService.class);
    private static final String ARTIFACT_KEY_PREFIX = "artifact:";
    private static final String STATUS_KEY_PREFIX = "execution:status:";

    private final ExecutionRepository executionRepository;
    private final MinioService minioService;
    private final GatlingRunner gatlingRunner;
    private final ExecutionKafkaProducer kafkaProducer;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ExecutionPipelineService(ExecutionRepository executionRepository,
                                    MinioService minioService,
                                    GatlingRunner gatlingRunner,
                                    ExecutionKafkaProducer kafkaProducer,
                                    SseEmitterRegistry sseEmitterRegistry,
                                    StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.minioService = minioService;
        this.gatlingRunner = gatlingRunner;
        this.kafkaProducer = kafkaProducer;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Async("taskExecutor")
    public void run(UUID executionId) {
        Path workDir = null;
        Instant startedAt = Instant.now();
        try {
            Execution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException("Execution not found: " + executionId));

            String artifactPath = redisTemplate.opsForValue().get(ARTIFACT_KEY_PREFIX + execution.getProjectId());
            if (artifactPath == null || artifactPath.isBlank()) {
                failExecution(executionId, "No built artifact found for project " + execution.getProjectId(), -1);
                return;
            }

            workDir = Files.createTempDirectory("gatling-" + executionId);
            minioService.downloadArtifact(artifactPath, workDir);

            startedAt = Instant.now();
            final Instant runStartedAt = startedAt;
            markRunning(executionId, runStartedAt);

            kafkaProducer.publishTestStarted(new TestStartedEvent(
                    executionId.toString(),
                    execution.getProjectId().toString(),
                    execution.getSimulationClass(),
                    runStartedAt.toString()));

            int exitCode = gatlingRunner.run(
                    workDir.toString(),
                    execution.getSimulationClass(),
                    executionId.toString(),
                    execution.getTestType().name(),
                    line -> sseEmitterRegistry.emit(executionId.toString(), line),
                    pid -> saveStatusToRedis(executionId.toString(), "RUNNING", runStartedAt, pid));

            Instant finishedAt = Instant.now();
            long durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
            String resultPath = gatlingRunner.getResultPath(executionId.toString());

            if (exitCode == 0) {
                markFinished(executionId, ExecutionStatus.SUCCESS, finishedAt, durationMs, resultPath);
                saveStatusToRedis(executionId.toString(), "SUCCESS", startedAt, null);
                kafkaProducer.publishTestFinished(new TestFinishedEvent(
                        executionId.toString(), "SUCCESS", durationMs, resultPath));
            } else {
                markFinished(executionId, ExecutionStatus.FAILED, finishedAt, durationMs, resultPath);
                saveStatusToRedis(executionId.toString(), "FAILED", startedAt, null);
                kafkaProducer.publishTestFailed(new TestFailedEvent(
                        executionId.toString(), "Gatling exited with code " + exitCode, exitCode));
            }

        } catch (Exception e) {
            log.error("Execution pipeline failed for {}: {}", executionId, e.getMessage(), e);
            failExecution(executionId, e.getMessage(), -1);
        } finally {
            sseEmitterRegistry.complete(executionId.toString());
            deleteDirectory(workDir);
        }
    }

    @Transactional
    public void markRunning(UUID executionId, Instant startedAt) {
        executionRepository.updateStarted(executionId, ExecutionStatus.RUNNING, startedAt);
    }

    @Transactional
    public void markFinished(UUID executionId, ExecutionStatus status,
                              Instant finishedAt, long durationMs, String resultPath) {
        executionRepository.updateFinished(executionId, status, finishedAt, durationMs, resultPath);
    }

    @Transactional
    public void markStatus(UUID executionId, ExecutionStatus status) {
        executionRepository.updateStatus(executionId, status);
    }

    private void failExecution(UUID executionId, String error, int exitCode) {
        try {
            markStatus(executionId, ExecutionStatus.FAILED);
            saveStatusToRedis(executionId.toString(), "FAILED", Instant.now(), null);
            kafkaProducer.publishTestFailed(new TestFailedEvent(executionId.toString(), error, exitCode));
        } catch (Exception ex) {
            log.error("Could not mark execution {} as failed: {}", executionId, ex.getMessage());
        }
    }

    private void saveStatusToRedis(String executionId, String status, Instant startedAt, Long pid) {
        try {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("status", status);
            statusMap.put("startedAt", startedAt.toString());
            if (pid != null) statusMap.put("pid", pid);

            String json = objectMapper.writeValueAsString(statusMap);
            redisTemplate.opsForValue().set(STATUS_KEY_PREFIX + executionId, json, 24, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize Redis status for execution {}", executionId);
        }
    }

    private void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            log.warn("Could not delete temp dir {}: {}", dir, e.getMessage());
        }
    }
}
