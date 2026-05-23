package com.loadtest.report.kafka.producer;

import com.loadtest.common.kafka.KafkaTopics;
import com.loadtest.common.kafka.event.ReportGeneratedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ReportKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReportKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishReportGenerated(ReportGeneratedEvent event) {
        log.info("Publishing report.generated for execution {}", event.executionId());
        kafkaTemplate.send(KafkaTopics.REPORT_GENERATED, event.executionId(), event);
    }
}
