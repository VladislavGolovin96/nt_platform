package com.loadtest.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TestFailedEvent(
        @JsonProperty("executionId") String executionId,
        @JsonProperty("error")       String error,
        @JsonProperty("exitCode")    int exitCode
) {}
