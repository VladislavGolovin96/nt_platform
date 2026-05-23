package com.loadtest.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TestStartedEvent(
        @JsonProperty("executionId")      String executionId,
        @JsonProperty("projectId")        String projectId,
        @JsonProperty("simulationClass")  String simulationClass,
        @JsonProperty("startedAt")        String startedAt
) {}
