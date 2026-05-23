package com.loadtest.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TestLogEvent(
        @JsonProperty("executionId") String executionId,
        @JsonProperty("lineNumber")  int lineNumber,
        @JsonProperty("message")     String message,
        @JsonProperty("timestamp")   String timestamp
) {}
