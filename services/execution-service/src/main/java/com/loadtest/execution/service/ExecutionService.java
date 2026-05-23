package com.loadtest.execution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadtest.common.exception.ResourceNotFoundException;
import com.loadtest.execution.domain.Execution;
import com.loadtest.execution.domain.ExecutionStatus;
import com.loadtest.execution.domain.ExecutionLog;
import com.loadtest.execution.dto.CreateExecutionRequest;
import com.loadtest.execution.dto.ExecutionResponse;
import com.loadtest.execution.repository.ExecutionLogRepository;
import com.loadtest.execution.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);
    private static final String STATUS_KEY_PREFIX = "execution:status:";

    private final ExecutionRepository executionRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final ExecutionPipelineService pipelineService;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ExecutionService(ExecutionRepository executionRepository,
                            ExecutionLogRepository executionLogRepository,
                            ExecutionPipelineService pipelineService,
                            SseEmitterRegistry sseEmitterRegistry,
                            StringRedisTemplate redisTemplate,
                            ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.executionLogRepository = executionLogRepository;
        this.pipelineService = pipelineService;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExecutionResponse createExecution(CreateExecutionRequest request, UUID userId) {
        Execution execution = new Execution();
        execution.setProjectId(request.projectId());
        execution.setUserId(userId);
        execution.setSimulationClass(request.simulationClass());
        execution.setTestType(request.testType());
        execution.setStatus(ExecutionStatus.PENDING);

        if (request.targetConfig() != null) {
            if (request.targetConfig().host() != null) {
                execution.setTargetHost(request.targetConfig().host());
            }
            if (request.targetConfig().mode() != null) {
                execution.setTargetMode(request.targetConfig().mode());
            }
        }

        execution = executionRepository.save(execution);
        log.info("Created execution {} for project {} user {}", execution.getId(), request.projectId(), userId);

        pipelineService.run(execution.getId());

        return ExecutionResponse.from(execution);
    }

    @Transactional(readOnly = true)
    public List<ExecutionResponse> listExecutions(UUID userId) {
        return executionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ExecutionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(UUID executionId, UUID userId) {
        return executionRepository.findByIdAndUserId(executionId, userId)
                .map(ExecutionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Execution", executionId.toString()));
    }

    @Transactional
    public void stopExecution(UUID executionId, UUID userId) {
        Execution execution = executionRepository.findByIdAndUserId(executionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution", executionId.toString()));

        if (execution.getStatus() != ExecutionStatus.RUNNING) {
            log.warn("Execution {} is not running (status={}), ignoring stop", executionId, execution.getStatus());
            return;
        }

        String redisKey = STATUS_KEY_PREFIX + executionId;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json != null) {
            try {
                Map<?, ?> statusMap = objectMapper.readValue(json, Map.class);
                Object pidValue = statusMap.get("pid");
                if (pidValue != null) {
                    long pid = ((Number) pidValue).longValue();
                    ProcessHandle.of(pid).ifPresent(ProcessHandle::destroy);
                    log.info("Sent destroy to PID {} for execution {}", pid, executionId);
                }
            } catch (Exception e) {
                log.warn("Could not read PID from Redis for execution {}: {}", executionId, e.getMessage());
            }
        }

        executionRepository.updateStatus(executionId, ExecutionStatus.STOPPED);
        sseEmitterRegistry.complete(executionId.toString());
    }

    public SseEmitter streamLogs(UUID executionId, UUID userId) {
        Execution execution = executionRepository.findByIdAndUserId(executionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution", executionId.toString()));

        SseEmitter emitter = sseEmitterRegistry.register(executionId.toString());

        List<ExecutionLog> historicalLogs =
                executionLogRepository.findByExecutionIdOrderByLineNumberAsc(executionId.toString());

        for (ExecutionLog entry : historicalLogs) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(entry.getLineNumber()))
                        .data(entry.getMessage()));
            } catch (IOException e) {
                break;
            }
        }

        boolean done = execution.getStatus() != ExecutionStatus.RUNNING
                && execution.getStatus() != ExecutionStatus.PENDING;
        if (done) {
            sseEmitterRegistry.complete(executionId.toString());
        }

        return emitter;
    }
}
