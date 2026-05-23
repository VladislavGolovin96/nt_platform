package com.loadtest.execution.kafka.consumer;

import com.loadtest.common.kafka.KafkaTopics;
import com.loadtest.common.kafka.event.ProjectBuiltEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProjectBuiltConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProjectBuiltConsumer.class);
    private static final String ARTIFACT_KEY_PREFIX = "artifact:";

    private final StringRedisTemplate redisTemplate;

    public ProjectBuiltConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = KafkaTopics.PROJECT_BUILT, groupId = "execution-service-group")
    public void onProjectBuilt(ProjectBuiltEvent event) {
        log.info("Received project.built for project {}, artifact={}", event.projectId(), event.artifactPath());
        redisTemplate.opsForValue().set(ARTIFACT_KEY_PREFIX + event.projectId(), event.artifactPath());
    }
}
