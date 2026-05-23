package com.loadtest.execution.dto;

import com.loadtest.execution.domain.ExecutionMode;
import com.loadtest.execution.domain.TestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateExecutionRequest(
        @NotNull UUID projectId,
        @NotBlank String simulationClass,
        @NotNull TestType testType,
        TargetConfig targetConfig
) {
    public record TargetConfig(String host, ExecutionMode mode) {}
}
