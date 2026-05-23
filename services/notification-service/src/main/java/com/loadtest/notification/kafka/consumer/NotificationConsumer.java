package com.loadtest.notification.kafka.consumer;

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

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.TEST_STARTED, groupId = "notification-service-group")
    public void onTestStarted(TestStartedEvent event) {
        log.info("Received test.started: executionId={}", event.executionId());
        notificationService.onTestStarted(event.executionId());
    }

    @KafkaListener(topics = KafkaTopics.TEST_FINISHED, groupId = "notification-service-group")
    public void onTestFinished(TestFinishedEvent event) {
        log.info("Received test.finished: executionId={}, status={}", event.executionId(), event.status());
        notificationService.onTestFinished(event.executionId(), event.status(), event.duration());
    }

    @KafkaListener(topics = KafkaTopics.TEST_FAILED, groupId = "notification-service-group")
    public void onTestFailed(TestFailedEvent event) {
        log.warn("Received test.failed: executionId={}, exitCode={}", event.executionId(), event.exitCode());
        notificationService.onTestFailed(event.executionId(), event.error(), event.exitCode());
    }

    @KafkaListener(topics = KafkaTopics.REPORT_GENERATED, groupId = "notification-service-group")
    public void onReportGenerated(ReportGeneratedEvent event) {
        log.info("Received report.generated: executionId={}", event.executionId());
        notificationService.onReportGenerated(event.executionId(), event.reportPath(), event.downloadUrl());
    }
}
