package com.loadtest.execution.controller;

import com.loadtest.execution.dto.CreateExecutionRequest;
import com.loadtest.execution.dto.ExecutionResponse;
import com.loadtest.execution.service.ExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecutionResponse createExecution(
            @Valid @RequestBody CreateExecutionRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return executionService.createExecution(request, UUID.fromString(userId));
    }

    @GetMapping
    public List<ExecutionResponse> listExecutions(
            @RequestHeader("X-User-Id") String userId) {
        return executionService.listExecutions(UUID.fromString(userId));
    }

    @GetMapping("/{id}")
    public ExecutionResponse getExecution(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        return executionService.getExecution(id, UUID.fromString(userId));
    }

    @GetMapping(value = "/{id}/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        return executionService.streamLogs(id, UUID.fromString(userId));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stopExecution(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        executionService.stopExecution(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}
