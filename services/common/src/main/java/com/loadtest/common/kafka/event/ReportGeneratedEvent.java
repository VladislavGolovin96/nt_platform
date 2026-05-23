package com.loadtest.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportGeneratedEvent(
        @JsonProperty("executionId")  String executionId,
        @JsonProperty("reportPath")   String reportPath,
        @JsonProperty("downloadUrl")  String downloadUrl
) {}
