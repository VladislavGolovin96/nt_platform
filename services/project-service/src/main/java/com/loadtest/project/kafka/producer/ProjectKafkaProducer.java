package com.loadtest.project.kafka.producer;

import com.loadtest.common.kafka.KafkaTopics;
import com.loadtest.common.kafka.event.ProjectBuiltEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProjectKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ProjectKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProjectKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishProjectBuilt(ProjectBuiltEvent event) {
        kafkaTemplate.send(KafkaTopics.PROJECT_BUILT, event.projectId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish project.built for {}: {}", event.projectId(), ex.getMessage());
                    } else {
                        log.info("Published project.built for projectId={}", event.projectId());
                    }
                });
    }
}
