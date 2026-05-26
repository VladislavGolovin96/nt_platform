package com.loadtest.notification.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadtest.common.kafka.KafkaTopics;
import com.loadtest.common.kafka.event.ReportGeneratedEvent;
import com.loadtest.common.kafka.event.TestFailedEvent;
import com.loadtest.common.kafka.event.TestFinishedEvent;
import com.loadtest.common.kafka.event.TestStartedEvent;
import com.loadtest.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.TEST_STARTED, groupId = "notification-service-group")
    public void onTestStarted(String payload) {
        try {
            TestStartedEvent event = objectMapper.readValue(payload, TestStartedEvent.class);
            log.info("Received test.started: executionId={}", event.executionId());
            notificationService.onTestStarted(event.executionId());
        } catch (Exception e) {
            log.error("Failed to process test.started: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.TEST_FINISHED, groupId = "notification-service-group")
    public void onTestFinished(String payload) {
        try {
            TestFinishedEvent event = objectMapper.readValue(payload, TestFinishedEvent.class);
            log.info("Received test.finished: executionId={}, status={}", event.executionId(), event.status());
            notificationService.onTestFinished(event.executionId(), event.status(), event.duration());
        } catch (Exception e) {
            log.error("Failed to process test.finished: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.TEST_FAILED, groupId = "notification-service-group")
    public void onTestFailed(String payload) {
        try {
            TestFailedEvent event = objectMapper.readValue(payload, TestFailedEvent.class);
            log.warn("Received test.failed: executionId={}, exitCode={}", event.executionId(), event.exitCode());
            notificationService.onTestFailed(event.executionId(), event.error(), event.exitCode());
        } catch (Exception e) {
            log.error("Failed to process test.failed: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.REPORT_GENERATED, groupId = "notification-service-group")
    public void onReportGenerated(String payload) {
        try {
            ReportGeneratedEvent event = objectMapper.readValue(payload, ReportGeneratedEvent.class);
            log.info("Received report.generated: executionId={}", event.executionId());
            notificationService.onReportGenerated(event.executionId(), event.reportPath(), event.downloadUrl());
        } catch (Exception e) {
            log.error("Failed to process report.generated: {}", e.getMessage(), e);
        }
    }
}
