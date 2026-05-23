package com.loadtest.report.kafka.consumer;

import com.loadtest.common.kafka.KafkaTopics;
import com.loadtest.common.kafka.event.TestFinishedEvent;
import com.loadtest.report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TestFinishedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TestFinishedConsumer.class);

    private final ReportService reportService;

    public TestFinishedConsumer(ReportService reportService) {
        this.reportService = reportService;
    }

    @KafkaListener(topics = KafkaTopics.TEST_FINISHED, groupId = "report-service-group")
    public void onTestFinished(TestFinishedEvent event) {
        log.info("Received test.finished for execution {}, status={}", event.executionId(), event.status());
        reportService.generateReport(event);
    }
}
