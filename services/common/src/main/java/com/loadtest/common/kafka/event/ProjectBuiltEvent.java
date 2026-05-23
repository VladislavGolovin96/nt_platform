package com.loadtest.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProjectBuiltEvent(
        @JsonProperty("projectId")   String projectId,
        @JsonProperty("userId")      String userId,
        @JsonProperty("artifactPath") String artifactPath,
        @JsonProperty("simulations") List<String> simulations
) {}
