package com.loadtest.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TestFinishedEvent(
        @JsonProperty("executionId") String executionId,
        @JsonProperty("status")      String status,
        @JsonProperty("duration")    long duration,
        @JsonProperty("resultPath")  String resultPath
) {}
