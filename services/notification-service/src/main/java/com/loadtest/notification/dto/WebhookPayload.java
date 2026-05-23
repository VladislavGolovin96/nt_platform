package com.loadtest.notification.dto;

public record WebhookPayload(
        String event,
        String executionId,
        String status,
        Long duration,
        String error,
        Integer exitCode,
        String reportUrl,
        String timestamp
) {
    public static WebhookPayload ofFinished(String executionId, String status, long duration, String timestamp) {
        return new WebhookPayload("test.finished", executionId, status, duration, null, null, null, timestamp);
    }

    public static WebhookPayload ofFailed(String executionId, String error, int exitCode, String timestamp) {
        return new WebhookPayload("test.failed", executionId, "FAILED", null, error, exitCode, null, timestamp);
    }

    public static WebhookPayload ofStarted(String executionId, String timestamp) {
        return new WebhookPayload("test.started", executionId, "STARTED", null, null, null, null, timestamp);
    }

    public static WebhookPayload ofReportGenerated(String executionId, String reportUrl, String timestamp) {
        return new WebhookPayload("report.generated", executionId, "READY", null, null, null, reportUrl, timestamp);
    }
}
