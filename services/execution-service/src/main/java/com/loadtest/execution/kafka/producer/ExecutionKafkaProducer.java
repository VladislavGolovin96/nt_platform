package com.loadtest.execution.kafka.producer;

import com.loadtest.common.kafka.KafkaTopics;
import com.loadtest.common.kafka.event.TestFailedEvent;
import com.loadtest.common.kafka.event.TestFinishedEvent;
import com.loadtest.common.kafka.event.TestStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExecutionKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ExecutionKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ExecutionKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTestStarted(TestStartedEvent event) {
        log.info("Publishing test.started for execution {}", event.executionId());
        kafkaTemplate.send(KafkaTopics.TEST_STARTED, event.executionId(), event);
    }

    public void publishTestFinished(TestFinishedEvent event) {
        log.info("Publishing test.finished for execution {}", event.executionId());
        kafkaTemplate.send(KafkaTopics.TEST_FINISHED, event.executionId(), event);
    }

    public void publishTestFailed(TestFailedEvent event) {
        log.warn("Publishing test.failed for execution {}", event.executionId());
        kafkaTemplate.send(KafkaTopics.TEST_FAILED, event.executionId(), event);
    }
}
